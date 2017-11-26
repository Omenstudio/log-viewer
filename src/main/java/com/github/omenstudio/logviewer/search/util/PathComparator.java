package com.github.omenstudio.logviewer.search.util;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Компаратор для сравнения путей к файлам/папкам.
 * В результате сравнений элементы сортируются следующим образом:
 * <ul>
 * <li>Папки сначала, файлы в конце</li>
 * <li>Папки по алфавиту от А до Я (A -> Z)</li>
 * <li>Файлы по алфавиту от А до Я (A -> Z)</li>
 * </ul>
 *
 * @author Василий
 * @see Path
 * @see File
 */
public class PathComparator implements Comparator<Path> {

    @Override
    public int compare(Path first, Path second) {
        return this.compare(first.toFile(), second.toFile());
    }

    public int compare(File first, File second) {
        if (first.isDirectory() && second.isDirectory())
            return first.compareTo(second);

        if (first.isDirectory())
            return this.compareToFile(first, second);

        if (second.isDirectory())
            return -(this.compareToFile(second, first));

        return this.compareFiles(first, second);
    }


    private int compareFiles(File first, File second) {
        File firstParentFile = first.getParentFile();
        File secondParentFile = second.getParentFile();

        if (isSubDir(firstParentFile, secondParentFile))
            return -1;

        if (isSubDir(secondParentFile, firstParentFile))
            return 1;

        return first.compareTo(second);
    }

    private int compareToFile(File directory, File file) {
        File fileParent = file.getParentFile();
        if (directory.equals(fileParent))
            return -1;

        if (isSubDir(directory, fileParent))
            return -1;

        return directory.compareTo(file);
    }

    private boolean isSubDir(File directory, File subDir) {
        for (File parentDir = directory.getParentFile(); parentDir != null; parentDir = parentDir.getParentFile()) {
            if (subDir.equals(parentDir)) {
                return true;
            }
        }

        return false;
    }
}
