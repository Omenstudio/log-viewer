package com.github.omenstudio.logviewer.search;

import com.github.omenstudio.logviewer.search.listeners.FileMatchFoundListener;
import com.github.omenstudio.logviewer.search.models.FoundFile;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Добавляет дополнительный функционал к классу поиска в файлах (к BasicSearcher):</p>
 * <ul>
 *     <li>Запуск в отдельном потоке</li>
 *     <li>Заботится о корректной остановке потока, чтобы не выбросилось <code>InterruptedException</code></li>
 *     <li>Сохранение данных о прогрессе - сколько файлов всего, сколько обработано</li>
 *     <li>Позволяет слушать события успешной обработки нового файла,
 *          что позволяет работать с обработанными файлами сразу же, не дожидаясь окончания поиска.</li>
 * </ul>
 *
 * @see FileMatchFoundListener
 * @see #start
 * @see #stop
 * @see #getProgress
 *
 * @author Василий
 */
public class ThreadSearcher extends BasicSearcher {
    /**
     * Сколько файлов всего
     */
    private int totalFilesCount;

    /**
     * Сколько файлов обработано
     */
    private int handledFilesCount;

    /**
     * Слушатель по событию обработки нового файла
     */
    List<FileMatchFoundListener> fileMatchFoundListeners;

    /**
     * Хранит инфу о том, запущен ли поиск сейчас
     */
    private boolean isRunning;

    /**
     * Конструктор. Чтобы выполнять поиск см. соотвествующие методы
     *
     * @see #reset
     * @see #start
     */
    public ThreadSearcher() {
        super();

        totalFilesCount = 0;
        handledFilesCount = 0;
        isRunning = false;
        fileMatchFoundListeners = new ArrayList<>();
    }

    @Override
    public void reset(Path pathToDirectory, String extension, String pattern) {
        super.reset(pathToDirectory, extension, pattern);

        totalFilesCount = 0;
        handledFilesCount = 0;
        isRunning = false;
    }

    @Override
    public void run() {
        isRunning = true;
        super.run();
        isRunning = false;
    }

    @Override
    protected List<Path> findFiles() {
        List<Path> allFiles = super.findFiles();

        totalFilesCount = allFiles.size();
        handledFilesCount = 0;

        return allFiles;
    }

    @Override
    protected FoundFile searchInFile(Path pathToFile) {
        if (!isRunning)
            return new FoundFile(pathToFile, "");

        FoundFile file = super.searchInFile(pathToFile);
        fireFileMatchFound(file);
        ++handledFilesCount;
        return file;
    }

    @Override
    protected int searchInBuffer(
            ByteBuffer byteBuffer,
            long bufferOffset,
            int bytesCount,
            int totalLineCounts,
            FoundFile foundFile
    ) {
        if (isRunning)
            return super.searchInBuffer(byteBuffer, bufferOffset, bytesCount, totalLineCounts, foundFile);
        foundFile.getMatches().clear();
        return 0;
    }

    /**
     * <p>Запускает поиск в отдельном потоке.</p>
     *
     * @see #getProgress
     * @see #stop
     * @see #run
     *
     */
    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * <p>Вызывает <code>reset</code> и запускает поиск </p>
     *
     * @see #start
     *
     * @param pathToDirectory Путь к папке, внутри которой рекурсивно будет
     *                        производиться поиск файлов и паттернов
     * @param extension Фильтр по расширению файлов.
     *                  Поиск будет производиться только в тех файлах, которые имеют данное расширение.
     * @param pattern Неэкранированная от регулярных выражений строка поиска
     */
    public void start(Path pathToDirectory, String extension, String pattern) {
        reset(pathToDirectory, extension, pattern);
        start();
    }

    /**
     * Прекращает поиск.
     * Уже найденные результаты при этом не удаляются и
     * могут быть доступны через метод <code>getFoundFiles</code>
     *
     * @see #getFoundFiles
     */
    public void stop() {
        isRunning = false;
    }

    /**
     * @return Запущен ли поиск
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * @return прогресс в виде целочисленного значения в диапазоне [0;100]
     */
    public int getProgress() {
        return totalFilesCount == 0 ? 0 : 100 * handledFilesCount / totalFilesCount;
    }

    /**
     * Добавляет слушатель для получения уведомлений о том,
     * что очередной файл успешно обработан "поисковиком",
     * и в нем найдены совпадения (одно или больше) по искомому запросу.
     *
     * @see FileMatchFoundListener
     * @see #removeFileFoundListener
     *
     * @param listener слушатель
     */
    public void addFileFoundListener(FileMatchFoundListener listener) {
        this.fileMatchFoundListeners.add(listener);
    }

    /**
     * Отписывается от прослуширования успешной обработки файлов.
     *
     * @see FileMatchFoundListener
     * @see #addFileFoundListener
     *
     * @param listener слушатель
     */
    public void removeFileFoundListener(FileMatchFoundListener listener) {
        this.fileMatchFoundListeners.remove(listener);
    }

    /**
     * Инициирует событие успешной обработки файла.
     *
     * @param resultFile обработанный файл, в котором есть хотя бы 1 совпадение
     */
    private void fireFileMatchFound(FoundFile resultFile) {
        if (!resultFile.isEmpty())
            fileMatchFoundListeners.forEach(l -> l.fileMatchFound(resultFile));
    }

}
