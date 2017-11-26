package com.github.omenstudio.logviewer.gui;

import com.github.omenstudio.logviewer.gui.fileviewer.FileViewer;
import com.github.omenstudio.logviewer.gui.util.ButtonTabComponent;
import com.github.omenstudio.logviewer.search.models.FoundFile;
import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import java.awt.*;

/**
 * <p>Класс занимается открытием файлов
 * <ul>
 *     <li>Управляет табами (открытие, закрытие)
 *     <li>Позволяет итерироваться по совпадениям через кнопки в gui
 *     <li>Отожражает информацию о найденных совпадениях (сколько/всего)
 * </ul>
 *
 * <p>Чтением и отображением файла занимается класс <code>FileViewer</code>
 *
 * @see FileViewer
 *
 * @author Василий
 */
public class FilesTabbedPane extends JPanel {
    /**
     * Кнопка "Следующее совпадение"
     */
    final JButton nextOccurrenceButton;

    /**
     * Кнопка "Предыдущее совпадение"
     */
    final JButton previousOccurrenceButton;

    /**
     * Checkbox о том, нужно ли переносить слова
     */
    final JCheckBox wordWrapButton;

    /**
     * JLabel с информацией о совпадениях (текущее/всего)
     */
    final JLabel occurrencesInfoLabel;

    /**
     * Контейнер для табов
     */
    final JTabbedPane tabbedPane;

    /**
     * Дефолтный конструктор
     */
    public FilesTabbedPane() {
        tabbedPane = new JTabbedPane();
        nextOccurrenceButton = new JButton();
        previousOccurrenceButton = new JButton();
        wordWrapButton = new JCheckBox();
        occurrencesInfoLabel = new JLabel();

        // Разметка компонентов
        setLayout(new BorderLayout());
        add(buildToolBar(), BorderLayout.PAGE_START);
        add(tabbedPane, BorderLayout.CENTER);

        // Делаем кнопки неактивными до тех пор,
        // пока не откроется хотя бы 1 файл
        nextOccurrenceButton.setEnabled(false);
        previousOccurrenceButton.setEnabled(false);
        wordWrapButton.setEnabled(false);

        // Подписываемся на событие по смене активной табы,
        // чтобы обновить инфу о совпадениях и включить word-wrap
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getTabCount() > 0) {
                getActiveViewer().setWordWrap(wordWrapButton.isSelected());
                updateOccurrencesInfo();
            }
        });
    }

    /**
     * Вспомогательный метод, осуществляющий разметку тулбара
     *
     * @return JComponent тулбара
     */
    private JComponent buildToolBar() {
        final Color iconColor = new Color(106, 105, 112);
        final int iconSize = 18;
        IconFontSwing.register(FontAwesome.getIconFont());

        // Next occurrence toolbar button
        nextOccurrenceButton.setText("Next Occurrence");
        nextOccurrenceButton.setIcon(IconFontSwing.buildIcon(FontAwesome.ARROW_DOWN, iconSize, iconColor));
        nextOccurrenceButton.setToolTipText("Scroll to next occurrence");
        nextOccurrenceButton.addActionListener(e -> {
            getActiveViewer().toNextOccurrence();
            updateOccurrencesInfo();
        });

        // Previous occurrence toolbar button
        previousOccurrenceButton.setText("Previous Occurrence");
        previousOccurrenceButton.setIcon(IconFontSwing.buildIcon(FontAwesome.ARROW_UP, iconSize, iconColor));
        previousOccurrenceButton.setToolTipText("Scroll to previous occurrence");
        previousOccurrenceButton.addActionListener(e -> {
            getActiveViewer().toPreviousOccurrence();
            updateOccurrencesInfo();
        });

        // Auto word wrap button
        wordWrapButton.setText("Word wrap  ");
        wordWrapButton.setToolTipText("Scroll to next occurrence");
        wordWrapButton.addActionListener(e -> getActiveViewer().setWordWrap(wordWrapButton.isSelected()));

        // Occurrences info
        occurrencesInfoLabel.setText("                        ");

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOrientation(SwingConstants.HORIZONTAL);
        toolBar.add(nextOccurrenceButton);
        toolBar.add(previousOccurrenceButton);
        toolBar.add(wordWrapButton);
        toolBar.addSeparator();
        toolBar.add(occurrencesInfoLabel);

        return toolBar;
    }

    /**
     * Обновляет информацию о совпадениях (№ текущего совпадения / всего совпадений)
     */
    private void updateOccurrencesInfo() {
        occurrencesInfoLabel.setText(String.format(
                "  Occurrences: %d/%d  ",
                getActiveViewer().getCurrentOccurrence()+1,
                getActiveViewer().getTotalOccurrences()
        ));
    }

    /**
     * Закрывает все файлы, удаляет все табы
     */
    public void clear() {
        while(tabbedPane.getTabCount() > 0)
            closeFile(0);
    }

    /**
     * <p>Открывает файл. Файл должен содержать как минимум 1 совпадение.
     * За само открытие отвечает класс <code>FileViewer</code>,
     * данный метод только создает объект класса и добавляет в виде новой табы.
     *
     * <p>Если файл уже открыт - TabbedPane переключится на него
     *
     * @see FileViewer
     *
     * @param file файл с совпадениями
     */
    public void openFile(FoundFile file) {
        FileViewer viewer = new FileViewer(file);

        int tabIndex = tabbedPane.indexOfComponent(viewer);
        if (tabIndex == -1) {
            nextOccurrenceButton.setEnabled(true);
            previousOccurrenceButton.setEnabled(true);
            wordWrapButton.setEnabled(true);

            tabbedPane.add(file.toString(), viewer);
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ButtonTabComponent(this, tabbedPane));
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);

            viewer.toCurrentOccurrence();
        }
        else
            tabbedPane.setSelectedIndex(tabIndex);
    }

    /**
     * <p>Закрывает файл по индексу
     * <p>Если нужно закрыть все файлы, воспользуйтесь методом <code>clear</code>
     *
     * @see #clear
     *
     * @param tabIndex индекс табы с файлом
     */
    public void closeFile(int tabIndex) {
        if (tabIndex >= 0)
            tabbedPane.remove(tabIndex);

        if (tabbedPane.getTabCount() == 0) {
            nextOccurrenceButton.setEnabled(false);
            previousOccurrenceButton.setEnabled(false);
            wordWrapButton.setEnabled(false);
            occurrencesInfoLabel.setText("                        ");
        }
    }

    /**
     * @return открытый файл в виде объекта класса <code>FileViewer</code>
     */
    FileViewer getActiveViewer() {
        return ((FileViewer) tabbedPane.getSelectedComponent());
    }






}
