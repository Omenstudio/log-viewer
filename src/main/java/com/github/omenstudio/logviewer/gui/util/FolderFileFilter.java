package com.github.omenstudio.logviewer.gui.util;

import com.alee.extended.filefilter.AbstractFileFilter;

import javax.swing.*;
import java.io.File;

/**
 * <p>Класс - фильтр "только папки" для JFileChooser'а.
 * Предназначен для внедрения только на WebLookAndFeel. Дополнительно показывает иконку папки в фильтре</p>
 *
 * <p>
 *     Несмотря на то, что у JFileChooser'а есть метод
 *     <br><code>setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)</code><br>
 *     иногда стоит переопределить фильтр.
 * </p>
 *
 * <p>Во-первых, если выбрать нужно папку,
 * то с точки зрения usability фильтр "Папки" выглядит гораздо лучше чем "Все файлы"</p>
 *
 * <p>Во-вторых, фильтр поддерживает отрисовку иконки, т.к. наследуется не от стандартного
 * <code>FileFilter</code>, а от <code>AbstractFileFilter</code>, который лежит в библиотеке WebLaF.</p>
 *
 * @see com.alee.laf.WebLookAndFeel
 * @see com.alee.extended.filefilter.AbstractFileFilter
 * @see javax.swing.filechooser.FileFilter
 */
public class FolderFileFilter extends AbstractFileFilter {

    @Override
    public ImageIcon getIcon() {
        return ((ImageIcon) UIManager.getIcon("Tree.openIcon"));
    }

    @Override
    public String getDescription() {
        return "Folders";
    }

    @Override
    public boolean accept(File f) {
        return f.isDirectory();
    }

}
