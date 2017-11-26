package com.github.omenstudio.logviewer.search.models;

import com.github.omenstudio.logviewer.search.BasicSearcher;

/**
 * <p>Класс предназначен для хранения информации об одном найденном совпадении</p>
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
 *     За подробной информацией о причинах обработки и хранения таким способом
 *     см. метод <code>BasicSearcher#run</code>.
 * </p>
 *
 * @see FoundFile
 * @see BasicSearcher
 * @see BasicSearcher#run
 *
 * @author Василий
 */
public class FoundMatch {
    /**
     * Количество байт от начала файла до считанного куска
     */
    private long bytesFromFileStart;

    /**
     * Количество символов от начала куска до совпадения
     */
    private long charsFromStart;

    /**
     * Количество линий до начала куска
     */
    private long lineCountToChunkStart;

    /**
     * Конструктор объекта <code>FoundMatch</code>
     *
     * @see FoundMatch
     *
     * @param bytesFromFileStart Количество байт от начала файла до считанного куска
     * @param charsFromChunkStart Количество символов от начала куска до совпадения
     * @param lineCountToChunkStart Количество линий до начала куска
     */
    public FoundMatch(long bytesFromFileStart, long charsFromChunkStart, long lineCountToChunkStart) {
        this.bytesFromFileStart = bytesFromFileStart;
        this.charsFromStart = charsFromChunkStart;
        this.lineCountToChunkStart = lineCountToChunkStart;
    }

    /**
     * @return Количество байт от начала файла до считанного куска (буфера, чанка)
     */
    public long getBytesFromFileStart() {
        return bytesFromFileStart;
    }

    /**
     * @return Количество символов от начала куска до совпадения
     */
    public long getLineCount() {
        return lineCountToChunkStart;
    }

    /**
     * @return Количество линий до начала куска
     */
    public long getCharsFromStart() {
        return charsFromStart;
    }

}
