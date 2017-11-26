package com.github.omenstudio.logviewer.gui;

import com.github.omenstudio.logviewer.gui.util.FolderFileFilter;
import com.github.omenstudio.logviewer.search.ThreadSearcher;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>Панель-форма с текстовыми полями ввода. Выведена в отдельный класс для удобства
 *
 * <p>
 *     Содержит всю визуальную информацию, связанную с поиском:
 *     <ul>
 *         <li>Ввод/выбор папки поиска</li>
 *         <li>Искомая строка</li>
 *         <li>Ввод расширения файла</li>
 *         <li>Информация о количестве найденных совпадений и обработанных файлов</li>
 *         <li><code>Progress bar</code>, который запускается во время поиска и
 *              сообщает о прогрессе поиска (сколько файлов обработано, сколько осталось)</li>
 *     </ul>
 * </p>
 *
 * <p>Класс не занимается поиском, он просто управляет визуальным оповещением.
 * Поймать событие нажатия кнопки поиска можно, добавив слушатель через метод <code>addSearchListener</code>.
 *
 * @see #setProgress
 * @see #addSearchListener
 *
 * @author Василий
 */
public class SearchPanel extends JPanel {

    /**
     * Поле для ввода пути поиска
     */
    private final JTextField pathField = new JTextField();

    /**
     * Поле для ввода поискового запроса (поисковой строки)
     */
    private final JTextArea searchTextArea = new JTextArea();

    /**
     * Поле для ввода расширения файлов, в которых будет произведен поиск.
     */
    private final JTextField extensionField = new JTextField();

    /**
     * Прогрессбар, сообщающий о прогрессе. Виден только во время поиска.
     */
    private final JProgressBar searchProgressBar = new JProgressBar();

    /**
     * <code>JLabel</code>, сообщающий о результатах поиска:
     * сколько найденно совпадений в скольких файлах.
     */
    private final JLabel messageLabel = new JLabel();

    /**
     * Кнопка инициирующая поиск
     */
    private final JButton searchButton = new JButton();


    /**
     * Конструктор, занимается только разметкой SWING-компонентов
     */
    public SearchPanel() {
        buildComponents();
    }

    /**
     * Основная инициализация и размещение компонентов на панели. Вынесена в отдельный метод для удобства
     */
    private void buildComponents() {
        // Определяем путь приложения,
        // чтобы открывать "выбор папки" по умолчанию в нём
        String appPath = ".";
        try {
            appPath = new File(".").getCanonicalPath();
        } catch (IOException e) {/* do nothing */}

        // Поле с папкой, внутри которой будет осуществляться поиск
        pathField.setEditable(true);
        pathField.setText(appPath);

        // Кнопка выбора папки для поиска, открывающая диалоговое окно JFileChooser'а
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(pathField.getText()));
            chooser.setDialogTitle("Choose folder to search");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(new FolderFileFilter());

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // Строка поиска
        searchTextArea.setBorder(new EtchedBorder());
        searchTextArea.setText("");

        // Строка расширения
        extensionField.setEditable(true);
        extensionField.setText(".log");
        extensionField.setColumns(5);

        // Кнопка поиска
        searchProgressBar.setMinimum(0);
        searchProgressBar.setMaximum(100);

        // Строим layout и добавляем элементы на панель.
        // Здесь используется сторонний layout - FormLayout.
        // Он позволяет быстро и удобно размечать сложные формы и панели
        // через табличную разметку (строки и колонки).
        FormLayout layout = new FormLayout(
                "5dlu, right:pref, 5dlu, pref, 5dlu, 10dlu:grow, 5dlu, pref, 5dlu", // столбцы
                "5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu"); // строки

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel("Folder:", cc.xy(2, 2));
        builder.add(pathField, cc.xyw(4, 2, 3));
        builder.add(browseButton, cc.xy(8, 2));
        builder.addLabel("Search string:", cc.xy(2, 4));
        builder.add(searchTextArea, cc.xyw(4, 4, 5));
        builder.addLabel("Extension:", cc.xy(2, 6));
        builder.add(extensionField, cc.xy(4, 6));
        builder.add(messageLabel, cc.xy(6, 6));
        builder.add(searchButton, cc.xy(2, 8));
        builder.add(searchProgressBar, cc.xyw(4, 8, 5));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
        setBorder(new TitledBorder("Search"));
    }

    /**
     * <p>Меняет значение кнопки поиска на "отмена" и включает прогресс бар.
     * Данный метод должен вызываться каждый раз при начале процесса поиска.
     *
     * <p>Чтобы сообщить классу текущий прогресс, нужно использовать метод <code>setProgress</code>
     *
     * <p>Метод на занимается поиском, не инициирует его.
     * Он предназначен только для изменения визуальной составляющей панели
     *
     * <p>Поймать событие нажатия кнопки поиска можно, добавив слушатель через метод <code>addSearchListener</code>.
     *
     * @see #addSearchListener
     * @see #searchStopped
     * @see #setProgress
     *
     */
    public void searchStarted() {
        searchProgressBar.setVisible(true);
        searchButton.setText("Cancel");
        messageLabel.setText("");
    }

    /**
     * <p>Меняет значение кнопки поиска на "поиск" и отключает прогресс бар.
     * Данный метод должен вызываться каждый раз при окончании процесса поиска.
     *
     * <p>Дополнительно можно сообщить классу о результатах поиска через <code>setSearchResult</code>,
     * чтобы он отобразил информацию о поиске
     *
     * @see #searchStarted
     * @see #setSearchResult
     */
    public void searchStopped() {
        searchProgressBar.setVisible(false);
        searchButton.setText("Search");
    }

    /**
     * <p>Меняет значение прогресс-бара.
     *
     * <p>Удобно использовать вместе с методом <code>ThreadSearcher#getProgress</code>
     *
     * @see ThreadSearcher#getProgress
     *
     * @param progress прогресс поиска в виде целочисленного значения в диапазоне [0;100]
     */
    public void setProgress(int progress) {
        if (progress < 0 || progress > 100)
            throw new IllegalArgumentException("Progress value must be from 0 to 100");
        searchProgressBar.setValue(progress);
    }

    /**
     * Отображает информацию о результатах поиска.
     *
     * @param filesCount в скольки файлах найдены совпадения
     * @param matchCount сколько совпадений найдено во всех файлах всего
     */
    public void setSearchResult(long filesCount, long matchCount) {
        messageLabel.setText(String.format("Found %d match in %d files", matchCount, filesCount));
    }

    /**
     * @return Путь до папки, которую указал пользователь.
     * Может быть не валидной, т.е. не существовать в файловой системе
     */
    public Path getSearchPath() {
        return Paths.get(pathField.getText());
    }

    /**
     * @return неэкранированная поисковая фраза, которую хочет найти пользователь
     */
    public String getSearchPattern() {
        return searchTextArea.getText();
    }

    /**
     * @return Расширение файлов, в которых пользователь хочет искать.
     *          Если пользователь ввёл расширение без точки в начале, точка будет добавлена автоматически.
     *          Строка может быть пустой!
     */
    public String getSearchExtension() {
        String extension = extensionField.getText();
        if (extension.length() > 0 && extension.charAt(0) != '.') {
            extension = "." + extension;
        }

        return extension;
    }

    /**
     * <p>Добавляет нового слушателя по событию, когда пользователь нажимает кнопку "искать".
     *
     * @param listener слушатель события нажатия на кнопку Search
     */
    public void addSearchListener(ActionListener listener) {
        searchButton.addActionListener(listener);
    }

    /**
     * <p>Удаляет слушателя по событию, когда пользователь нажимает кнопку "искать".
     *
     * @param listener слушатель события нажатия на кнопку Search, который был добавлен ранее
     */
    public void removeSearchListener(ActionListener listener) {
        searchButton.removeActionListener(listener);
    }
}
