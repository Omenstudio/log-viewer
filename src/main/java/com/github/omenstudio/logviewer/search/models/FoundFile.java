package com.github.omenstudio.logviewer.search.models;

import com.github.omenstudio.logviewer.search.BasicSearcher;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Класс предназначен для хранения результатов поиска в конкретном файле.</p>
 *
 * <p>
 *     <b>Доступная информация:</b>
 *     <ul>
 *         <li>Адрес файла в файловой системе. См. <code>getPath</code></li>
 *         <li>Результаты поиска хранятся как множество <code>FoundMatch</code></li>
 *         <li>
 *             Определенная кодировка в формате <code>Charset</code>.
 *             <p>
 *                 В силу того, что используемая библиотека не всегда корректно определяет
 *                 кодировку файлов, по умолчанию устанавливается UTF-8, как наиболее распространенная.
 *             </p>
 *         </li>
 *         <li>
 *             Поисковый запрос (строка поиска). Доступен через <code>getPattern</code>
 *             <p>
 *                 Специальные символы, которые могут использоваться в регулярных выражениях
 *                 уже экранированы, поэтому строка может быть, например,
 *                 <br> такой: <code>"\Qtest_string\E(\n|\r)\Q123\E"</code>
 *                 <br>вместо: <code>"test_string\n123"</code>
 *             </p>
 *         </li>
 *     </ul>
 * </p>
 *
 * <p>
 * <strong>ВНИМАНИЕ:</strong>
 * По умолчанию данный класс не гарантирует того,
 * что в указанном файле были найдены какие-либо совпадения.
 * Это необходимо проверять вручную через соответствующий метод: <code>isEmpty</code>
 * </p>
 *
 * <p>
 * <strong>ВНИМАНИЕ:</strong>
 * Данный класс никак не работает и не проверяет данные, это просто контейнер.
 * Если, например, будет передан на хранение несуществующий путь, исключений выкинуто не будет.
 * </p>
 *
 * @see FoundMatch
 * @see java.nio.file.Path
 * @see java.nio.charset.Charset
 *
 * @author Василий
 */
public class FoundFile {

    /**
     * Список найденных совпадений в данном файле. Может быть пустым.
     *
     * @see #isEmpty
     * @see #getMatches
     * @see #getMatchCount
     */
    private List<FoundMatch> matches;

    /**
     * Путь к файлу
     *
     * @see #getPath
     */
    private Path path;

    /**
     * Кодировка, использовавшаяся во время поиска по файлу
     *
     * @see #getCharset
     * @see #setCharset
     */
    private Charset charset;

    /**
     * Строка поиска, уже экранирована
     *
     * @see #getPattern
     */
    private String pattern;


    /**
     * <p>
     *     Создаёт объект класса <code>FoundFile</code>
     *     Данный класс никак не работает и не проверяет данные, это просто контейнер.
     *     Если, например, будет передан несуществующий путь, никаких исключений выкинуто не будет.
     * </p>
     *
     * @param path путь к файлу
     * @param pattern строка поиска
     */
    public FoundFile(Path path, String pattern) {
        matches = new ArrayList<>();
        this.path = path;
        this.pattern = pattern;
    }

    /**
     * @return путь к файлу, в котором производился поиск
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return Кодировка, с помощью которой декодировался файл
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * <p>Задаёт кодировку, с помощью которой декодировался файл.</p>
     * <p>Выполнение этого метода не поможет изменить кодировку поиска</p>
     * @param charsetName строка с кодировкой. ANSI, UTF-8, UTF-16, ASCII и т.д.
     */
    public void setCharset(String charsetName) {
        if (charsetName == null || charsetName.isEmpty())
            charsetName = "UTF-8";
        this.charset = Charset.forName(charsetName);
    }

    /**
     *  <p>Возвращает пользовательскую строку поиска.</p>
     *  <p>
     *      Специальные символы, которые могут использоваться в регулярных выражениях
     *      уже экранированы, поэтому строка может быть, например,
     *      <br> такой: <code>"\Qtest_string\E(\n|\r)\Q123\E"</code>
     *      <br>вместо: <code>"test_string\n123"</code>
     *  </p>
     * @return Строка поиска
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * <p>
     *     Добавляет информацию об очередном совпадении.Информация хранится как объект класса <code>FoundMatch</code>.
     * </p>
     *
     * <p>
     *     Поскольку класс для поиска обрабатывает информацию кусками (чанками, побуферно),
     *     информация о совпадении хранится как набор 3 переменных:
     *     <ul>
     *         <li>Количество байт от начала файла до считанного куска.</li>
     *         <li>Количество символов от начала куска до совпадения.</li>
     *         <li>Количество линий до начала куска.</li>
     *     </ul>
     * </p>
     *
     * <p>
     *     За подробной информацией о причинах обработки хранения таким способом
     *     см. метод <code>BasicSearcher#run</code>.
     * </p>
     *
     * @see FoundMatch
     * @see BasicSearcher
     * @see BasicSearcher#run
     *
     * @param bytesFromFileStart Количество байт от начала файла
     * @param charsFromChunkStart Количество символов от начала куска до совпадения
     * @param lineCountToChunkStart Количество линий до начала куска
     */
    public void addMatch(long bytesFromFileStart, long charsFromChunkStart, long lineCountToChunkStart) {
        matches.add(new FoundMatch(bytesFromFileStart, charsFromChunkStart, lineCountToChunkStart));
    }

    /**
     * @see #addMatch
     * @see FoundMatch
     *
     * @param match объект класса <code>FoundMatch</code>
     */
    public void addMatch(FoundMatch match) {
        matches.add(match);
    }

    /**
     * @see FoundMatch
     *
     * @return Список найденных совпадений поисковой строки
     * в виде <code>List< FoundMatch > </code>
     */
    public List<FoundMatch> getMatches() {
        return matches;
    }

    /**
     * Вспомогательный метод, сообщающий о том,
     * было ли найдено хотя бы одно совпадение .
     *
     * @see #getMatches
     * @see #getMatchCount
     *
     * @return Пустой ли список совпадений.
     */
    public boolean isEmpty() {
        return matches.isEmpty();
    }

    /**
     * Вспомогательный метод, сообщающий о количестве найденных совпадений.
     *
     * @see #getMatches
     * @see #isEmpty
     *
     * @return Количество найденных совпадений в данном файле
     */
    public long getMatchCount() {
        return matches.size();
    }





    @Override
    public String toString() {
        return path.getFileName().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof FoundFile)) return false;

        return path.equals(((FoundFile) o).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
