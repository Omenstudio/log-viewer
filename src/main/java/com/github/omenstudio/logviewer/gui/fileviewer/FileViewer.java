package com.github.omenstudio.logviewer.gui.fileviewer;

import com.github.omenstudio.logviewer.gui.util.LineNumberPanel;
import com.github.omenstudio.logviewer.search.models.FoundFile;
import com.github.omenstudio.logviewer.search.models.FoundMatch;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Панель, которая занимается отображением содержимого файла.
 *
 * <p>Особенности:
 *  <ul>
 *      <li>Файлы грузятся в память кусками(чанками), что позволяет открывать очень большие файлы</li>
 *      <li>Выполняется подсветка всех совпадений</li>
 *      <li>Менеджмент совпадений, возможность итерации по ним, автоскролл зоны видимости до текущего совпадения</li>
 *      <li>
 *          Догрузка содержимого по требованию.
 *          Если при скролле достигается конец загруженного фрагмента,
 *          программа показывает пользователю кнопку, тем спредлагая
 *          догрузить следующий или предыдущий кусок файла.
 *      </li>
 *  </ul>
 *
 * <p>ДЛя использования: создать экзамепляр класса, поместить его куда-нибудь на окно,
 * вызвать <code>toCurrentOccurrence</code>
 *
 *
 * @see #toCurrentOccurrence
 * @see #toNextOccurrence
 * @see #toPreviousOccurrence
 * @see #setWordWrap
 * @see #scrollToCurrentOccurrence
 * @see #highlightAll
 *
 * @author Василий
 */
public class FileViewer extends JLayeredPane {
    /**
     * <code>JTextArea</code>-компонент, который занимается отрисовкой текста
     */
    final JTextArea textArea;

    /**
     * <code>JScrollPane</code>-компонент,
     * который отвечает за скроллинг области видимости относительно текста
     */
    final JScrollPane scrollPane;

    /**
     * Панель, отвечающая за отрисовку номеров строк.
     */
    final LineNumberPanel lineNumberPanel;

    /**
     * Верхняя полоса с кнопкой "Загрузить предыдущие линии"
     */
    JComponent topNavigationComponent;

    /**
     * Нижняя полоса с кнопкой "Загрузить следующие линии"
     */
    JComponent bottomNavigationComponent;

    /**
     * Менеджер чанков, отвечающий за загрузки и компоновку кусков текста из файла.
     */
    ChunkManager chunkManager;

    /**
     * Текущий файл с совпадениям
     */
    final FoundFile file;

    /**
     * Номер текущего совпадения, которое показано пользователю
     */
    int currentOccurrence = 0;

    /**
     * <p>Конструктор не занимается прогрузкой текста,
     * для этого предназначен метод <code>toCurrentOccurrence</code>
     *
     * @param file файл с найденными совпадениями
     */
    public FileViewer(final FoundFile file) {
        this.file = file;
        chunkManager = new ChunkManager(file.getPath(), file.getCharset());

        // Область текста
        textArea = new JTextArea();
        textArea.setEditable(false);

        // Верхняя полоса с кнопкой "Загрузить предыдущие линии"
        topNavigationComponent = new JPanel();
        JButton previousLinesButton = new JButton("Show previous lines");
        previousLinesButton.addActionListener(e -> loadPrevious());
        topNavigationComponent.add(previousLinesButton);
        topNavigationComponent.setBackground(new Color(247, 247, 247, 160));

        // Нижняя полоса с кнопкой "Загрузить следующие линии"
        bottomNavigationComponent = new JPanel();
        JButton nextLinesButton = new JButton("Show next lines");
        nextLinesButton.addActionListener(e -> loadNext());
        bottomNavigationComponent.add(nextLinesButton);
        bottomNavigationComponent.setBackground(new Color(247, 247, 247, 160));

        // Скроллинг и номера строк
        lineNumberPanel = new LineNumberPanel(textArea);
        scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumberPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);

        // Поскольку нам нужно показывать полосы с кнопкой,
        // будем использовать JLayredPane (данный класс от него наследуется).
        // Это позволит рисовать панели с кнопками "следующие и предыдущии линии"
        // поверх текста, одновременно позволяя прогрузить нужный фрагмент текста
        // и скрывая "кривые" соединения прогруженных чанков с непрогруженными
        add(scrollPane, 0, 0);
        add(topNavigationComponent, 1, 0);
        add(bottomNavigationComponent, 2, 0);

        // JLayeredPane не имеет layout'а, поэтому на каждое изменение его размеров,
        // мы должны вручную установить размеры и положение всех компонентов
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);

                scrollPane.setBounds(0, 0, getWidth(), getHeight());
                topNavigationComponent.setBounds(0, 0, getWidth(), 36);
                bottomNavigationComponent.setBounds(0, getHeight() - 36, getWidth(), 36);
                revalidate();
                repaint();
            }
        });

        // Ловим события, когда пользователь скроллит до конца или начала
        // и предлагаем догрузить текст, если возможно
        scrollPane.getViewport().addChangeListener((l) -> {
            JScrollBar sb = scrollPane.getVerticalScrollBar();
            topNavigationComponent.setVisible(sb.getValue() == sb.getMinimum() && chunkManager.hasPrevious());
            bottomNavigationComponent.setVisible(sb.getValue() + sb.getVisibleAmount() == sb.getMaximum() && chunkManager.hasNext());
        });
    }

    /**
     * <p>Подгружает небольшой участок текста, предыдущий к загруженным (ближе к началу файла),
     * обновляет подсветку и сохраняет позиции области видимости.
     *
     * @see #loadNext
     */
    private void loadPrevious() {
        // build text
        String text = chunkManager.loadPrevious();
        int caretPosition = ((int) chunkManager.getFirstChunk().getLength());
        Point viewportPosition = scrollPane.getViewport().getViewPosition();

        // reassign text
        textArea.setText(text);
        textArea.setCaretPosition(caretPosition);

        // update viewport position
        try {
            viewportPosition.y += textArea.modelToView(caretPosition).y;
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        scrollPane.getViewport().setViewPosition(viewportPosition);

        // other updates
        topNavigationComponent.setVisible(false);
        lineNumberPanel.shiftStartLine(-chunkManager.getFirstChunk().getLineCount());

        highlightAll();
    }

    /**
     * <p>Подгружает небольшой участок текста, следующий за загруженным (ближе к концу файла),
     * обновляет подсветку и сохраняет позиции области видимости.
     *
     * @see #loadPrevious
     */
    private void loadNext() {
        long linesToRemove = chunkManager.isFull() ? chunkManager.getFirstChunk().getLineCount() : 0;

        // Вычисляем новую позицию каретки
        int caretPosition = textArea.getDocument().getLength();
        if (chunkManager.isFull())
            caretPosition -= chunkManager.getFirstChunk().getLength();

        // Сохраняем позицию viewport'а, чтобы восстановить её в последующем
        Point viewportPosition = scrollPane.getViewport().getViewPosition();
        try {
            viewportPosition.y -= textArea.modelToView(((int) chunkManager.getFirstChunk().getLength())).y;
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // Перестраиваем текст и обновляет позиции viewport'а
        String text = chunkManager.loadNext();
        textArea.setText(text);
        textArea.setCaretPosition(caretPosition);
        scrollPane.getViewport().setViewPosition(viewportPosition);

        // Оставшееся
        bottomNavigationComponent.setVisible(false);
        lineNumberPanel.shiftStartLine(linesToRemove);
        highlightAll();
    }


    /**
     * <p>Подгружает кусок файла, в котором содержится следующее за текущим совпадение,
     * подсвечивает все совпадения и скроллит область видимости к текущему совпадению.
     *
     * @see #toPreviousOccurrence
     * @see #toCurrentOccurrence
     */
    public void toNextOccurrence() {
        ++currentOccurrence;
        if (currentOccurrence >= getTotalOccurrences())
            currentOccurrence = 0;
        toCurrentOccurrence();
    }

    /**
     * <p>Подгружает кусок файла, в котором содержится предыдущее совпадение,
     * подсвечивает все совпадения и скроллит область видимости к текущему совпадению.
     *
     * @see #toNextOccurrence
     * @see #toCurrentOccurrence
     */
    public void toPreviousOccurrence() {
        --currentOccurrence;
        if (currentOccurrence < 0)
            currentOccurrence = getTotalOccurrences() - 1;
        toCurrentOccurrence();
    }

    /**
     * <p>Подгружает кусок файла, в котором содержится текущее совпадение <code>currentOccurrence</code>,
     * подсвечивает все совпадения и скроллит область видимости к текущему совпадению.
     *
     * <p> Использовать только при необходимости загрузки/обновлении участка текста в памяти.
     * Если нужно подсветить или проскроллить область видимости, лучше воспользоваться методами
     * <code>highlightAll</code> и <code>scrollToCurrentOccurrence</code> соответственно.
     *
     * @see #toNextOccurrence
     * @see #toPreviousOccurrence
     * @see #scrollToCurrentOccurrence
     * @see #highlightAll
     */
    public void toCurrentOccurrence() {
        // Загрузка текста в textArea
        FoundMatch match = file.getMatches().get(currentOccurrence);
        String text = chunkManager.load(match.getBytesFromFileStart());
        textArea.setText(text);
        textArea.setCaretPosition(0);

        // Подсветка совпадений
        highlightAll();

        // Обновление номеров строк
        long startLine = file.getMatches().get(currentOccurrence).getLineCount();
        if (file.getMatches().get(currentOccurrence).getBytesFromFileStart() != 0)
            startLine -= chunkManager.getFirstChunk().getLineCount();
        lineNumberPanel.setStartLine(startLine);

        // Скролл до текущего совпадения
        scrollToCurrentOccurrence();
    }

    /**
     *
     */
    public void scrollToCurrentOccurrence() {
        SwingUtilities.invokeLater(() -> {
            int currentStartPosition = getCurrentMatchStartPosition();

            try {
                Rectangle wordPos = textArea.modelToView(currentStartPosition);
                // Простейшие геометрические операции
                // на определение нового положение Viewport'а
                int newY = wordPos.y - getHeight() / 2;
                if (newY < 0) newY = 0;
                int newX = wordPos.x - getWidth() / 2;
                if (newX < 0) newX = 0;
                scrollPane.getViewport().setViewPosition(new Point(newX, newY));
            } catch (BadLocationException | NullPointerException e) {/* do nothing */}
        });
    }

    /**
     * <p>Подсвечивает все совпадения в текущем (загруженном) фрагменте.
     *
     * <p>Данный метод должен вызываться каждый раз при изменении содержимого или установке автопереноса строк.
     */
    public void highlightAll() {
        // Преинициализация
        final Color currentOccurrenceColor = new Color(255, 114, 0);
        final Color occurrenceColor = new Color(255, 228, 80);
        int currentStartPosition = getCurrentMatchStartPosition();

        // Для подсветки используем стандартный Highlighter
        Highlighter highlighter = textArea.getHighlighter();
        highlighter.removeAllHighlights();

        // Заного ищём все совпадения, это всё равно быстро (т.к. в памяти небольшой кусок) и надёжно
        Pattern pattern = Pattern.compile(file.getPattern());
        Matcher m = pattern.matcher(chunkManager.getCurrentContent());
        while (m.find()) {
            int foundStartPosition = m.start();
            Color highlightColor = foundStartPosition == currentStartPosition ?
                                            currentOccurrenceColor : occurrenceColor;
            try {
                highlighter.addHighlight(
                        m.start(),
                        m.end(),
                        new DefaultHighlighter.DefaultHighlightPainter(highlightColor));
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Вспомогательный метод, который вычисляет позицию текущего вхождения,
     * относительно начала загруженного фрагмента файла
     *
     * @return позиция текущего совпадения относительно начала файла.
     */
    private int getCurrentMatchStartPosition() {
        FoundMatch match = file.getMatches().get(currentOccurrence);

        long ans = match.getCharsFromStart();
        if (match.getBytesFromFileStart() != 0)
            ans += chunkManager.getFirstChunk().getLength();

        return (int)ans;
    }

    /**
     * @see #getTotalOccurrences
     * @return номер совпадения, на котором находится пользователь. Нумерация начинается с 0!
     */
    public int getCurrentOccurrence() {
        return currentOccurrence;
    }

    /**
     * @see #getCurrentOccurrence
     * @return общее количество совпадений в открытом файле
     */
    public int getTotalOccurrences() {
        return ((int) file.getMatchCount());
    }

    /**
     * Устанавливает автоматический перенос строк и обновляет дисплей.
     *
     * @param isWordWrap true, если нужен перенос строк, false иначе.
     */
    public void setWordWrap(boolean isWordWrap) {
        textArea.setLineWrap(isWordWrap);
        if (textArea.getDocument().getLength() > 0)
            highlightAll();
    }


    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof FileViewer && file.equals(((FileViewer) o).file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }
}
