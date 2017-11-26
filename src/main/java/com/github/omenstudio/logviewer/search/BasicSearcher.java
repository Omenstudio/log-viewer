package com.github.omenstudio.logviewer.search;

import com.github.omenstudio.logviewer.search.models.FoundFile;
import com.github.omenstudio.logviewer.search.models.FoundMatch;
import com.github.omenstudio.logviewer.search.util.PathComparator;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Базовый класс для поиска строк в файлах.</p>
 *
 * <p>
 *     Если требуется поддержка поиска в отдельном потоке,
 *     real-time наблюдение за прогрессом обработки, то используйте класс <code>ThreadSearcher</code>
 * </p>
 *
 * <ul>
 *     <li>Автоматически определяет кодировку</li>
 *     <li>Обрабатывает файлы более 4гб </li>
 *     <li>Поддерживает поиск запросов в нескольк строк (MULTILINE)</li>
 * </ul>
 *
 * <p>Чтобы использовать необходимо создать объект класса,
 * настроить поисковый запрос через <code>reset</code> и вызвать метод <code>run</code></p>
 *
 * <p>Файл читается "кусками", побуферно, что позволяет эффективно обрабатывать очень большие файлы,
 *  но накладывает ограничения на хранение результатов поиска.
 *  Подробнее см <code>FoundFile</code>, <code>FoundMatch</code>, <code>#run</code></p>
 *
 * <p>По окончанию поиска хранит список совпадений в виде объектов класса <code>FoundFile</code>,
 * который может быть получен через метод <code>#getFoundFiles</code></p>
 *
 * @see #reset
 * @see #run
 * @see #getFoundFiles
 *
 * @see ThreadSearcher
 * @see FoundFile
 * @see FoundMatch
 *
 * @author Василий
 */
public class BasicSearcher implements Runnable {
    /**
     * Размер буфера, который загружается в память из файла.
     */
    private static int BUFFER_SIZE = 8 * 1024;

    /**
     * Путь к папке, внутри которой будет рекурсивно производиться поиск файлов и паттернов.
     *
     * @see #reset
     */
    private Path searchPath;

    /**
     * Фильтр по расширению файлов.
     * Поиск будет производиться только в тех файлах, которые имеют данное расширение.
     *
     * @see #reset
     */
    private String extension;

    /**
     * Строка поиска.
     * <p>
     *      Специальные символы, которые могут использоваться в регулярных выражениях
     *      уже экранированы, поэтому строка может быть, например,
     *      <br> такой: <code>"\Qtest_string\E(\n|\r)\Q123\E"</code>
     *      <br>вместо: <code>"test_string\n123"</code>
     *  </p>
     *  <p>Подробнее про экранирование см. <code>escapePatternString</code></p>
     *
     *  @see #escapePatternString
     *  @see #reset
     */
    private Pattern pattern;

    /**
     * Список найденных совпадений в виде объектов класса <code>FoundFile</code>
     *
     * @see #getFoundFiles
     */
    private List<FoundFile> foundFiles;


    /**
     * Конструктор. Чтобы выполнять поиск см. соотвествующие методы
     *
     * @see #reset
     * @see #run
     */
    public BasicSearcher() {
        foundFiles = new ArrayList<>();
    }

    /**
     * Сбрасывает текущие параметры поиска (если они уже были)
     * и устанавливает новые, после чего можно запускать поиск через <code>run</code>
     *
     * @see #run
     *
     * @exception java.lang.IllegalArgumentException если указанная директория не существует
     *
     * @param pathToDirectory Путь к папке, внутри которой рекурсивно будет
     *                        производиться поиск файлов и паттернов
     * @param extension Фильтр по расширению файлов.
     *                  Поиск будет производиться только в тех файлах, которые имеют данное расширение.
     * @param pattern Неэкранированная от регулярных выражений строка поиска
     */
    public void reset(Path pathToDirectory, String extension, String pattern) {
        if (Files.notExists(pathToDirectory))
            throw new IllegalArgumentException("Path is not exists");

        foundFiles.clear();
        this.searchPath = pathToDirectory;
        this.extension = extension;

        this.pattern = Pattern.compile(escapePatternString(pattern), Pattern.MULTILINE);
    }

    /**
     * Экранирует строку поиска. Это нужно, чтобы одновременно позволить
     * как искать мультистроковые запросы,
     * так и предотвратить поиск по регулярным выражениям (если есть неэкранированные символы ()[]^|...)
     *
     * @param pattern Искомый пользовательский запрос
     * @return Экранированный поисковый запрос
     */
    private String escapePatternString(String pattern) {
        StringBuilder newPattern = new StringBuilder();

        // Экранирование производим через символы \Q и \E.
        // При этом из-за особенностей переноса кареток,
        // на каждый символ переноса строки мы ставим регулярное выражение
        // (\n|\r){1,2} , удовлетворяющее строкам \n, \r\n, \n\r
        newPattern.append("\\Q");
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '\n') {
                newPattern.append("\\E");
                newPattern.append("(\n|\r){1,2}");
                newPattern.append("\\Q");
            }
            else
                newPattern.append(pattern.charAt(i));
        }
        newPattern.append("\\E");
        return newPattern.toString();
    }


    /**
     * <p>
     *     Запускает процесс поиска. Реализует интферфейс <code>Runnable</code>,
     *     что позволяет передавать объект этого класса в качестве параметра в <code>Thread</code>
     * </p>
     *
     * <p>Если параметры поиска не были заданы, поиск не начнётся. См. <code>reset</code></p>
     *
     * <p>Результаты поиска будут доступны в контейнере <code>foundFiles</code>,
     * получить который можно через геттер <code>getFoundFiles</code>.</p>
     *
     *
     * @see #searchInFile
     * @see #getFoundFiles
     * @see #reset
     * @see java.lang.Runnable
     *
     */
    @Override
    public void run() {
        if (!isValid())
            return;

        findFiles().stream()
                .sorted(new PathComparator())
                .map(this::searchInFile)
                .filter(fileMatches -> !fileMatches.isEmpty())
                .collect(Collectors.toCollection(() -> foundFiles));
    }

    /**
     * Ищет все файлы с расширением <code>extension</code> в указанной папке и во всех подпапках.
     *
     * @see java.nio.file.Path
     * @see #extension
     *
     * @return список путей, объектов класса <code>Path</code>
     */
    protected List<Path> findFiles() {
        List<Path> files = new ArrayList<>();
        try {
            Files.find(searchPath, Integer.MAX_VALUE,
                    (p, bfa) -> bfa.isRegularFile() && p.toString().endsWith(extension))
                    .collect(Collectors.toCollection(() -> files));
        } catch (IOException e) {/* do nothing */}
        return files;
    }

    /**
     * <p>Выполняет поиск в указанном файле. Возвращает объект класса <code>FoundFile</code>,
     * содержащий информацию о найденных совпадениях.</p>
     *
     * <p>Поиск выполняется побуферно, что позволяет обрабатывать большие файлы.
     * За поиск в буфере отвечает метод <code>searchInBuffer</code></p>
     *
     * <p>
     *     <b>Внимание:</b>
     *     объект <code>FoundFile</code> возвращается всегда, даже если ничего не найдено.
     *     Наличие совпадение нужно проверять через метод <code>!isEmpty</code>
     * </p>
     *
     * @see #searchInBuffer
     * @see FoundFile
     * @see FoundFile#isEmpty
     *
     * @param pathToFile путь к файлу, в котором необходимо произвести поиск
     * @return Объект класса <code>FoundFile</code>, содержащий информацию о найденных совпадениях
     */
    protected FoundFile searchInFile(Path pathToFile) {
        // Общая идея.
        //
        // Считываем блоки друг за другом и ищем фразу в них.
        // Поскольку искомая фраза может находиться на стыке 2 блоков,
        // будем считывать блоки с небольшим смещением

        FoundFile foundFile = new FoundFile(pathToFile, pattern.pattern());
        int lineCounts = 0;

        try {
            FileChannel inputChannel = new RandomAccessFile(pathToFile.toFile(), "r").getChannel();

            ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            long currentChunkPosition = 0;
            int bytesReadCount = BUFFER_SIZE;

            while (bytesReadCount == BUFFER_SIZE) {
                // Сначала читаем в буфер
                bytesReadCount = inputChannel.read(byteBuffer, currentChunkPosition);
                if (bytesReadCount <= 0)
                    break; // конец файла достигнут
                byteBuffer.rewind();
                byteBuffer.limit(bytesReadCount);

                // Выполняем поиск в буфере
                lineCounts += searchInBuffer(byteBuffer, currentChunkPosition, bytesReadCount, lineCounts, foundFile);

                // Подготавливаем буфер для следующего считывания
                byteBuffer.clear();
                currentChunkPosition += bytesReadCount - pattern.pattern().length() + 1;
            }
            inputChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return foundFile;
    }

    /**
     * <p>Производит поиск в буфере. Здесь же определяется кодировка файла</p>
     *
     * @see java.nio.ByteBuffer
     * @see java.nio.CharBuffer
     * @see FoundFile
     *
     * @param byteBuffer считанный байтовый буфер
     * @param bufferOffset смещение буфера относительно начала файла
     * @param bytesCount количество считанных байт
     * @param totalLineCounts количество линий от начала файла до текущего блока
     * @param foundFile объект, куда нужно записывать найденные совпадения
     *
     * @return количество линий в текущем блоке (буфере)
     */
    protected int searchInBuffer(
            ByteBuffer byteBuffer,
            long bufferOffset,
            int bytesCount,
            int totalLineCounts,
            FoundFile foundFile) {
        // Авто-определение кодировки. Основано на библиотеке "juniversalchardet"
        // Работает не всегда, может вернуть null. В этом случае будет установлена "UTF-8"
        if (foundFile.getCharset() == null) {
            UniversalDetector charsetDetector = new UniversalDetector(null);
            charsetDetector.handleData(byteBuffer.array(), 0, bytesCount);
            charsetDetector.dataEnd();
            foundFile.setCharset(charsetDetector.getDetectedCharset());
        }

        // Байтовый буфер нужно декодировать в буфер символов,
        // чтобы мы могли производить по нему поиск
        CharBuffer charBuffer = foundFile.getCharset().decode(byteBuffer);
        charBuffer.limit(bytesCount);

        // Стандартный java-поиск строк
        Matcher m = pattern.matcher(charBuffer);
        while (m.find()) {
            foundFile.addMatch(bufferOffset, m.start(), totalLineCounts);
        }

        // Для вывода информации о количестве строк на экран,
        // нам необходимо знать сколько строк было в каждом блоке.
        // Здесь производиться из подсчет.
        int charBufferPtr = 0;
        int lineCtr = 0;
        int lineCtrToCut = 0;
        int cutOffset = charBuffer.length() - pattern.pattern().length() + 1;
        while (charBufferPtr < charBuffer.length()) {
            if (charBuffer.get(charBufferPtr) == '\n') {
                ++lineCtr;
                if (charBufferPtr >= cutOffset) {
                    ++lineCtrToCut;
                }
            }
            ++charBufferPtr;
        }

        //
        return lineCtr - lineCtrToCut;
    }


    /**
     * @see FoundFile
     *
     * @return Список найденных совпадений в виде объектов класса <code>FoundFile</code>.
     * Может быть пустым.
     */
    public List<FoundFile> getFoundFiles() {
        return foundFiles;
    }

    /**
     * Определяет все ли поисковые параметры были сообщены объекту данного класса.
     *
     * @see #reset
     *
     * @return true если можно начинать поиск, false иначе
     */
    public boolean isValid() {
        return searchPath != null && extension != null && pattern != null;
    }

}
