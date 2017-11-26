package com.github.omenstudio.logviewer.gui;

import com.github.omenstudio.logviewer.search.ThreadSearcher;
import com.github.omenstudio.logviewer.search.models.FoundFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>Главное окно программы
 *
 * <p>
 *     Концептульно поделено на 3 блока
 *     <ul>
 *         <li><code>SearchPanel</code> - панель, где пользователь задает параметры поиска
 *         (папку, расширение, запрос) и видит real-time прогресс</li>
 *         <li><code>FilesTree</code> - файловое дерево, где отображаются найденные файлы</li>
 *         <li><code>FilesTabbedPane</code> - TabbedPane/TabFolder где открываются файлы</li>
 *     </ul>
 * </p>
 *
 * <p> За поиск отвечает класс <code>ThreadSearcher</code>
 *
 * @see SearchPanel
 * @see FilesTree
 * @see FilesTabbedPane
 * @see ThreadSearcher
 *
 * @author Василий
 */
public class MainFrame extends JFrame {
    /**
     * Панель, где пользователь задает параметры поиска
     *         (папку, расширение, запрос) и видит real-time прогресс
     */
    final FilesTree filesTree;

    /**
     * Файловое дерево, где отображаются найденные файлы
     */
    final FilesTabbedPane filesTabbedPane;

    /**
     * TabbedPane/TabFolder где открываются файлы
     */
    final SearchPanel searchPanel;

    /**
     * Объект класса <code>ThreadSearcher</code>, управляющий поиском
     */
    final ThreadSearcher searcher;

    /**
     * Конструктор.
     *
     * Для компоновки панелей используется вспомогательный метод <code>buildComponents</code>
     *
     * @see #buildComponents
     */
    public MainFrame() {
        // Инициализация компонентов
        filesTree = new FilesTree();
        filesTabbedPane = new FilesTabbedPane();
        searchPanel = new SearchPanel();
        searcher = new ThreadSearcher();

        // Этот слушатель позволяет добавлять файлы в дерево "на лету",
        // не дожидаясь окончания поиска
        searcher.addFileFoundListener(filesTree::addFile);
        // Этот слушатель нужен, чтобы запускать/останавливать поиск
        searchPanel.addSearchListener(e -> {
            if (searcher.isRunning())
                stopSearching();
            else
                startSearching();
        });
        // Этот слушатель нужен, чтобы знать, когда пользователь захочет открыть файл,
        // и прокинуть этот файл соотв. классу
        filesTree.addFileOpenListener(filesTabbedPane::openFile);

        // Конфигурация и разметка окна
        buildComponents();
        setLocationRelativeTo(null);
        stopSearching();

        // Из-за особенностей реализации Web look and feel,
        // иногда при первом показе компоненты возникают проблемы с Toolbar'ами,
        // которые расположены вертикально (кнопки растягиваются на всю ширину, будто тулбар горизонтальный).
        // Решается это простым revalidate при показе компонента
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                super.componentShown(e);
                revalidate();
            }
        });
    }

    /**
     * Вспомогательный метод, чтобы разметить панели и задать параметры окна
     */
    private void buildComponents() {
        // Конфигурация
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("LogViewer by Rutin Vasily");
        setMinimumSize(new Dimension(800, 500));

        // Разметка
        JComponent leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(filesTree, BorderLayout.CENTER);
        leftPanel.add(searchPanel, BorderLayout.PAGE_END);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, leftPanel, filesTabbedPane);

        getContentPane().add(splitPane);
        pack();
    }

    /**
     * <p>Полностью инициирует процесс поиска. Можно вызывать из любой точки программы
     * <p>Если поиск идёт сейчас, сначала произведет его остановку.
     * <p>Если пользователь задал некорректные данные поиска, выведет сообщение и прекратит выполнение.
     *
     * @see #stopSearching
     */
    public void startSearching() {
        if (searcher.isRunning())
            stopSearching();

        // Проверяем корректно ли пользователь заполнил все поля
        // и выводим сообщения, если нужно
        Path searchPath = searchPanel.getSearchPath();
        String messageToUser = "";
        if (searchPath.toString().length() == 0 || Files.notExists(searchPath)) {
            messageToUser += "* Folder must exist on filesystem.\n";
        }
        String patternString = searchPanel.getSearchPattern();
        if (patternString.length() < 3) {
            messageToUser += "* Length of a search line must be more than 3 symbols.\n";
        }
        String extension = searchPanel.getSearchExtension();
        if (extension.length() <= 0) {
            messageToUser += "* Wrong extension. It has to be at least 2 character (with dot).\n";
        }
        // Если пользователь всё таки что-то не задал - сообщаем об этом (и не начинаем поиск)
        if (messageToUser.length() > 0) {
            JOptionPane.showMessageDialog(this, messageToUser);
            return;
        }

        // Обнуляем файловое дерево и закрываем открытые файлы
        filesTree.setRoot(searchPath);
        filesTabbedPane.clear();

        // Иницируем запуск + в отдельном потоке следим за прогрессом
        searcher.start(searchPath, extension, patternString);
        (new Thread(() -> {
            while (searcher.isRunning()) {
                searchPanel.setProgress(searcher.getProgress());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopSearching();
        })).start();
        searchPanel.searchStarted();
    }

    /**
     * <p>Полностью останавливает процесс поиска. Можно вызывать из любой точки программы
     *
     * @see #startSearching
     */
    public void stopSearching() {
        if (searcher.isRunning())
            searcher.stop();
        searchPanel.searchStopped();

        long filesCount = searcher.getFoundFiles().size();
        long matchCount = searcher.getFoundFiles().stream()
                .mapToLong(FoundFile::getMatchCount)
                .sum();

        searchPanel.setSearchResult(filesCount, matchCount);
    }

}

