package com.github.omenstudio.logviewer.gui.fileviewer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Deque;
import java.util.LinkedList;

/**
 * <p>Класс занимается управлением чанками.
 * <p>
 *     Поскольку большие файлы нельзя напрямую загрузить в память
 *     (если только для JVM не выделено сотни-другой гигабайт памяти),
 *     файлы необходимо считать кусками.<br>
 *     Считыванием таких кусков, их хранением и управлением данный класс и занимается.
 * </p>
 *
 * <p>Конструктор не занимается загрузкой чанков. Для этого предназначен метод <code>load</code>
 * <p>После загрузки чанков, доступны методы <code>loadNext</code> и <code>loadPrevious</code>,
 * которые подгружают соседние чанки в память и удаляют ненужные.
 *
 * @see #load
 * @see #loadNext
 * @see #loadPrevious
 *
 * @author Василий
 */
class ChunkManager {
    /**
     * Дека (двухсторонняя очередь) из чанков.
     * Целесообразно использовать именно деку, потому что нам не нужно часто обращаться к чанкам в середине,
     * зато нужно часто работать с чанками по краям (вставлять и удалять).
     */
    private Deque<FileChunk> chunks;

    /**
     * Файл, из которого будут считываться чанки
     */
    private File file;

    /**
     * Кодировка файла, из которого будут считываться чанки
     */
    private Charset charset;

    /**
     * Текущее объединенное содержимое всех чанков.
     */
    private String content;


    /**
     * <p>Конструктор.
     *
     * <p>Конструктор не занимается загрузкой чанков. Для этого предназначен метод <code>load</code>
     *
     * @see #load
     *
     * @param file Файл, из которого будут считываться чанки
     * @param charset Кодировка файла, из которого будут считываться чанки
     */
    public ChunkManager(Path file, Charset charset) {
        chunks = new LinkedList<>();
        this.file = file.toFile();

        if (charset != null)
            this.charset = charset;
        else
            this.charset = Charset.forName("UTF-8");
    }

    /**
     * <p>Строит чанки вокруг данной позиции. Возвращает их содержимое
     * <p>После загрузки чанков, доступны методы <code>loadNext</code> и <code>loadPrevious</code>,
     * которые подгружают соседние чанки в память и удаляют ненужные.     *
     *
     * @param position позиция в байтах от начала файла, которой будет соответствовать "средний" чанк
     * @return объединенная строка - содержимое всех чанков
     */
    public String load(long position) {
        // clear old data (if exists) and calculate new 3 chunks' id
        chunks.clear();
        FileChunk chunk = new FileChunk(position);
        if (position != 0)
            chunks.add(chunk.buildPrevious());
        chunks.add(chunk);
        if (position + FileChunk.CHUNK_SIZE < file.length())
            chunks.add(chunk.buildNext());

        // Chunks from file
        for (FileChunk fileChunk : chunks) {
            try {
                fileChunk.load(file, charset);
            } catch (IOException e) {/* do nothing*/}
        }

        return buildCurrentContent();
    }

    /**
     * @return есть ли следующий чанк, т.е. не достигнут ли конец файла
     */
    public boolean hasNext() {
        return chunks.peekLast().getPosition() + FileChunk.CHUNK_SIZE < file.length() - 1;
    }


    /**
     * Читает чанк, следующий за последним загруженным.
     *
     * @see #loadPrevious
     *
     * @return объединенное содержимое всех новых чанков
     */
    public String loadNext() {
        if (!hasNext())
            return getCurrentContent();

        while (chunks.size() > 2)
            chunks.pollFirst();

        chunks.addLast(chunks.getLast().buildNext());
        try {
            chunks.getLast().load(file, charset);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buildCurrentContent();
    }


    /**
     * @return есть ли предыдущий чанк, т.е. не достигнуто ли начало файла
     */
    public boolean hasPrevious() {
        return chunks.peekFirst().getPosition() > 0;
    }

    /**
     * Читает чанк, который находится перед первым загруженным.
     *
     * @see #loadNext
     *
     * @return объединенное содержимое всех новых чанков
     */
    public String loadPrevious() {
        if (!hasPrevious())
            return getCurrentContent();

        while (chunks.size() > 2)
            chunks.pollLast();

        chunks.addFirst(chunks.getFirst().buildPrevious());
        try {
            chunks.getFirst().load(file, charset);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buildCurrentContent();
    }

    /**
     * Перестраивает объединенное содержимое всех чанков и сохраняет в поле класса.
     *
     * @return объединенное содержимое всех чанков
     */
    public String buildCurrentContent() {
        StringBuilder builder = new StringBuilder();
        chunks.forEach(e -> builder.append(e.getContent()));
        this.content = builder.toString();
        return content;
    }

    /**
     * @return объединенное содержимое текущих чанков
     */
    public String getCurrentContent() {
        return content;
    }

    /**
     * @return первый загруженный в память чанк,
     *          т.е. самый младший, который соответствует первому куску содержимого
     */
    public FileChunk getFirstChunk() {
        return chunks.getFirst();
    }

    /**
     * @return последний загруженный в память чанк,
     *          т.е. самый старший, который соответствует последнему куску содержимого
     */
    public FileChunk getLastChunk() {
        return chunks.getLast();
    }

    /**
     * @return true если загружены все доступные чанки, false иначе
     */
    public boolean isFull() {
        return chunks.size() == 3;
    }

}
