package com.github.omenstudio.logviewer.gui;

import com.github.omenstudio.logviewer.gui.listeners.FileOpenListener;
import com.github.omenstudio.logviewer.search.models.FoundFile;
import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * <p>Класс отвечает за отображение файлового дерева.
 *
 * <p>Умеет сворачивать и разворачивать все папки по запросу пользователя.
 * <br>Может автоматически разворачивать путь до добавляемого файла, см. <code>autoExpand</code>
 *
 * <p>Чтобы узнать, когда пользователь хочет открыть файл,
 * нужно подписаться на событие через слушатель <code>FileOpenListener</code>
 *
 * @see #addFile
 * @see #autoExpand
 * @see #expandAll
 * @see #collapseAll
 * @see #addFileOpenListener
 *
 * @author Василий
 */
public class FilesTree extends JPanel {
    /**
     * Файловое дерево
     */
    private final JTree tree;

    /**
     * Корневой путь. Данный путь всегд свёрнут и все файлы добавляются в него
     */
    private Path rootPath;

    /**
     * <p>Сообщает о том, следует ли разворачивать путь дерева до добавляемого файла.
     * <p>Если <code>true</code> - разворачивает путь, когда добавляется новый файл,
     * <br>если <code>false</code> никак не влияет на "свертку/развертку" папок.
     */
    private boolean autoExpand;

    /**
     * Контейнер для слушателей <code>FileOpenListener</code>
     *
     * @see FileOpenListener
     * @see #addFileOpenListener
     * @see #removeFileOpenListener
     * @see #fireFileOpenListener
     */
    private List<FileOpenListener> fileOpenListeners;

    /**
     * Дефолтный конструктор.
     */
    public FilesTree() {
        tree = new JTree();
        tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("There are no search results")));
        autoExpand = true;
        fileOpenListeners = new ArrayList<>();

        buildComponents();

        // Слушаем событие по нажатию мыши на дерево,
        // чтобы определить, когда пользователь кликнул
        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // Мышь нажата -> найдём узел дерева
                int selectedRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selectedPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selectedRow != -1 && selectedPath != null) {
                    // Всё ок, узел есть -> проверим, является ли этот узел файлом,
                    // и пробросим файл в слушатели FileOpenListener
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        if (selectedPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                            if (node.getUserObject() instanceof FoundFile) {
                                fireFileOpenListener((FoundFile) (node.getUserObject()));
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Вспомогательный метод, осуществляющий разметку компонентов
     */
    private void buildComponents() {
        // "Развернуть всё"
        JButton expandAllButton = new JButton();
        expandAllButton.addActionListener(e -> expandAll());
        expandAllButton.setToolTipText("Expand all folders");

        // "Свернуть всё"
        JButton collapseAllButton = new JButton();
        collapseAllButton.addActionListener(e -> collapseAll());
        collapseAllButton.setToolTipText("Collapse all folders");

        // "Автоматически разворачивать путь до добавляемых узлов"
        JToggleButton autoExpandButton = new JToggleButton();
        autoExpandButton.addActionListener(e -> autoExpand = autoExpandButton.isSelected());
        autoExpandButton.setToolTipText("Auto expand folders when new files are adding");
        autoExpandButton.setSelected(autoExpand);

        // Устанавливаем иконки для кнопок.
        // Используется библиотечка jiconfont-font_awesome, которая позволяет
        // добавлять иконки, используя иконочный шрифт FontAwesome.
        IconFontSwing.register(FontAwesome.getIconFont());
        final Color iconColor = new Color(103, 99, 112);
        int iconSize = 18;
        Arrays.asList(new Object[][]{
                {expandAllButton, FontAwesome.EXPAND},
                {collapseAllButton, FontAwesome.COMPRESS},
                {autoExpandButton, FontAwesome.REFRESH}
        }).forEach(data -> {
            ((AbstractButton) data[0]).setIcon(IconFontSwing.buildIcon(((FontAwesome) data[1]),iconSize,iconColor));
            ((AbstractButton) data[0]).setMaximumSize(new Dimension(iconSize + 15, iconSize + 15));
        });

        // Вертикальный тулбар
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOrientation(SwingConstants.VERTICAL);
        toolBar.add(expandAllButton);
        toolBar.add(collapseAllButton);
        toolBar.add(autoExpandButton);

        // Разметка
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.LINE_START);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        revalidate();
    }

    /**
     * Устанавливает новый корневой узел.
     * ВСЕГДА удаляет существующее дерево.
     *
     * @param rootPath путь к корневой папке
     */
    public void setRoot(Path rootPath) {
        this.rootPath = rootPath;
        getTreeModel().setRoot(new DefaultMutableTreeNode(rootPath));
    }

    /**
     * <p>Добавляет новый файл в дерево.
     * <p>Файл всегда должен содержать хотя бы 1 совпадение.
     * <p>Файл обязательно должен быть n-ым потомком <code>rootPath</code>
     *
     * @see #rootPath
     *
     * @param file обработанный файл с найденными совпадениями
     */
    public void addFile(FoundFile file) {
        SwingUtilities.invokeLater(() -> putNodeAndUpdate(file));
    }

    /**
     * @see #addFile
     *
     * @param file
     */
    private void putNodeAndUpdate(FoundFile file) {
        // Строим относительный путь
        Path relativePath = rootPath.relativize(file.getPath());

        // Рекурсивно строим дерево папок до текущего файла
        DefaultMutableTreeNode curNode = getTreeRoot();
        nextPath:
        for (int i = 0; i < relativePath.getNameCount() - 1; i++) {
            Path pathPart = relativePath.getName(i);
            DefaultMutableTreeNode childNode = null;

            for (int childIndex = 0; childIndex < curNode.getChildCount(); childIndex++) {
                childNode = (DefaultMutableTreeNode) curNode.getChildAt(childIndex);
                if (pathPart.toString().equals(childNode.toString())) {
                    curNode = childNode;
                    continue nextPath;
                }
            }

            childNode = new DefaultMutableTreeNode(pathPart);
            getTreeModel().insertNodeInto(childNode, curNode, curNode.getChildCount());
            curNode = childNode;
        }

        // Добавляем новый файл
        getTreeModel().insertNodeInto(new DefaultMutableTreeNode(file), curNode, curNode.getChildCount());

        // Разворачиваем папки, если нужно
        if (autoExpand) {
            TreePath path = new TreePath(curNode.getPath());
            tree.expandPath(path);
        } else
            tree.expandRow(0);  // hack to expand first level from root
    }

    /**
     * Полностью разворачивает всё дерево. Если дерево большое, может выполняться долго.
     */
    public void expandAll() {
        expandCollapseAll(new TreePath(getTreeRoot()), true);
    }

    /**
     * Полностью сворачивает всё дерево. Если дерево большое, может выполняться долго.
     */
    public void collapseAll() {
        expandCollapseAll(new TreePath(getTreeRoot()), false);
        tree.expandRow(0);  // hack to expand first level from root
    }

    /**
     * Вспомогательный метод, который рекурсивно проходится по всем узлам и сворачивает пути до них
     * @param nodeToProcess узел, который нужно свернуть/развернуть
     * @param isExpand true, если нужно развернуть, false если нужно свернуть
     */
    private void expandCollapseAll(TreePath nodeToProcess, boolean isExpand) {
        TreeNode node = (TreeNode) nodeToProcess.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = nodeToProcess.pathByAddingChild(n);
                expandCollapseAll(path, isExpand);
            }
        }

        if (isExpand)
            tree.expandPath(nodeToProcess);
        else
            tree.collapsePath(nodeToProcess);
    }

    /**
     * @return корень дерева, всегда не null
     */
    private DefaultMutableTreeNode getTreeRoot() {
        return (DefaultMutableTreeNode) tree.getModel().getRoot();
    }

    /**
     * @return модель дерева (чтобы оперировать с узлами)
     */
    private DefaultTreeModel getTreeModel() {
        return ((DefaultTreeModel) tree.getModel());
    }

    /**
     * <p>Добавляет (подписывает) слушатель на прослуширования событий,
     * когда пользователь хочет открыть файл.
     *
     * @see #removeFileOpenListener
     *
     * @param listener
     */
    public void addFileOpenListener(FileOpenListener listener) {
        fileOpenListeners.add(listener);
    }

    /**
     * <p>Удаляет слушатель на событие, когда пользователь хочет открыть файл.
     *
     * @see #addFileOpenListener
     *
     * @param listener
     */
    public void removeFileOpenListener(FileOpenListener listener) {
        fileOpenListeners.remove(listener);
    }

    /**
     * <p>Сообщает всем подписанным слушателям о произошедшем событии на открытие файла
     *
     * @param file файл, который нужно открыть
     */
    protected void fireFileOpenListener(FoundFile file) {
        fileOpenListeners.forEach(l -> l.fileOpen(file));
    }
}
