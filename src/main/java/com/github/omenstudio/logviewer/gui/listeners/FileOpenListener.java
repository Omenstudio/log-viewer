package com.github.omenstudio.logviewer.gui.listeners;

import com.github.omenstudio.logviewer.gui.FilesTree;
import com.github.omenstudio.logviewer.search.models.FoundFile;

/**
 * Слушатель для получения уведомлений о том,
 * что пользователь дважды счелкнул по файлу
 * в дереве файлов и папок (<code>FilesTree</code>)
 *
 * @see FilesTree
 * @see FoundFile
 *
 * @author Василий
 */
public interface FileOpenListener {

    /**
     * Срабатывает, когда пользователь хочет открыть файл.
     *
     * @see FoundFile
     *
     * @param file Файл <code>FoundFile</code>, в котором сохранены результаты поиска
     */
    public void fileOpen(FoundFile file);
}
