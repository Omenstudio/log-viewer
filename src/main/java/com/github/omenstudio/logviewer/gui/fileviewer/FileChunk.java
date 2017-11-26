package com.github.omenstudio.logviewer.gui.fileviewer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * <p>Класс представляет из себя 1 чанк (кусок) файла, который загружается в память.
 *
 * <p>Построить чанк можно через
 * <ul>
 *     <li>Конструктор</li>
 *     <li>Метод <code>buildNext</code></li>
 *     <li>Метод <code>buildPrevious</code></li>
 * </ul>
 *
 * <p>При построении чанка не происходит считывания данных из файла и его загрузки в память.
 * Для этого существует специальный метод <code>load</code>
 *
 * @see #load
 * @see #buildNext
 * @see #buildPrevious
 *
 * @author Василий
 */
class FileChunk {
    /**
     * Размер загружаемого в память чанка в байтах
     */
    static int CHUNK_SIZE = 16*1024;

    /**
     * Позиция текущего чанка относительно начала файла в байтах
     */
    private long position;

    /**
     * Фактическая длина чанка в байтах. Необходимо, т.к. размер чанка не всегда совпадает с <code>CHUNK_SIZE</code>
     */
    private long length;

    /**
     * Декодированное содержимое чанка (строка)
     */
    private String content;

    /**
     * Количество линий (точнее количество '\n') в текущем чанке
     */
    private long lineCount;

    /**
     * <p>Конструктор.
     * <p>При построении чанка не происходит считывания данных из файла и его загрузки в память.
     * Для этого существует специальный метод <code>load</code>
     *
     * @see #load
     *
     * @param position позиция в байтах от начала файла
     */
    public FileChunk(long position) {
        this.position = position;
        this.length = CHUNK_SIZE;
        if (position < 0) {
            length -= Math.abs(position);
            this.position = 0;
        }
    }

    /**
     * <p>Считывает данные из файла, декодирует, считает количество линий и грузит в память (в поле <code>content</code>)
     *
     * <p>Если чанк невалидный (<code>position > file.length()</code>) запишет пустой контент.
     *
     * @param file файл на чтение
     * @param charset кодировка
     * @throws IOException при невозможности открыть файл
     */
    public void load(File file, Charset charset) throws IOException {
        // RandomAccessFile считывает очень быстро со случайно позиции
        FileChannel inputChannel = new RandomAccessFile(file, "r").getChannel();

        // Считаем в байт-буфер
        ByteBuffer byteBuffer = ByteBuffer.allocate(((int) length));
        int bytesReadCount = inputChannel.read(byteBuffer, position);
        if (bytesReadCount <= 0) {
            lineCount = 0;
            content = "";
            return;
        }
        byteBuffer.rewind();
        byteBuffer.limit(bytesReadCount);

        // На основе байт-буфера строим контент
        CharBuffer charBuffer = charset.decode(byteBuffer);
        content = new String(charBuffer.array()).replace("\0", "");
        lineCount = content.chars().filter(ch -> ch == '\n').count();
    }

    /**
     * @return Позиция текущего чанка относительно начала файла в байтах
     */
    public long getPosition() {
        return position;
    }

    /**
     * @return Декодированное содержимое чанка (строка)
     */
    public String getContent() {
        return content;
    }

    /**
     * @return Количество символов в текущем чанке
     */
    public long getLength() {
        return content.length();
    }

    /**
     * @return количество символов '\n' в текущем чанке
     */
    public long getLineCount() {
        return lineCount;
    }

    /**
     * @return чанк, следующий за данным, т.е. чанк с позицией
     * <code>position + length</code>
     */
    public FileChunk buildNext() {
        return new FileChunk(position + CHUNK_SIZE);
    }

    /**
     * @return чанк, находящийся перед текущим, т.е. чанк с позицией
     * <code>position - length</code>
     */
    public FileChunk buildPrevious() {
        return new FileChunk(position - CHUNK_SIZE);
    }

}
