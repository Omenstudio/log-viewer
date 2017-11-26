package com.github.omenstudio.logviewer.gui.util;

import com.github.omenstudio.logviewer.gui.FilesTabbedPane;
import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;

/**
 * <p>
 *     Класс-отрисовщик табов для <code>FilesTabbedPane</code>.
 *     Добавляет кнопку для закрытия таба.
 * </p>
 *
 * <p>
 *     Взято и изменено от сюда:
 *     <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/tabbedpane.html">
 *         "How to Use Tabbed Panes"</a>
 * </p>
 *
 * <p>
 * Component to be used as tabComponent;
 * Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 * </p>
 *
 */
public class ButtonTabComponent extends JPanel {
    private final FilesTabbedPane pane;
    private final JTabbedPane paneComponent;

    public ButtonTabComponent(final FilesTabbedPane pane, final JTabbedPane tabbedPane) {
        //unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (pane == null || tabbedPane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.pane = pane;
        this.paneComponent = tabbedPane;
        setOpaque(false);
        //make JLabel read titles from JTabbedPane
        JLabel label = new JLabel() {
            public String getText() {
                int i = paneComponent.indexOfTabComponent(ButtonTabComponent.this);
                if (i != -1) {
                    return paneComponent.getTitleAt(i);
                }
                return null;
            }
        };
        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        //tab button
        JButton button = new TabButton();
        //add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        setLayout(new BorderLayout());
        add(button, BorderLayout.LINE_END);
        add(label, BorderLayout.CENTER);
    }

    private class TabButton extends JButton implements ActionListener {
        private final Icon iconNormal;
        private final Icon iconHover;

        public TabButton() {
            final int size = 18;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("close this tab");
            //Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            //Make it transparent
            setContentAreaFilled(false);
            //No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            // Icons
            iconNormal = IconFontSwing.buildIcon(FontAwesome.TIMES_CIRCLE_O, size, new Color(105, 105, 105));
            iconHover = IconFontSwing.buildIcon(FontAwesome.TIMES_CIRCLE_O, size, new Color(255, 3, 4));
            //Making nice rollover effect
            //we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            //Close the proper tab by clicking the button
            addActionListener(this);
            //
            setHover(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = paneComponent.indexOfTabComponent(ButtonTabComponent.this);
            if (i != -1) {
                pane.closeFile(i);
            }
        }

        //we don't want to update UI for this button
        @Override
        public void updateUI() {
        }

        void setHover(boolean isHover) {
            setIcon(isHover ? iconHover : iconNormal);
        }

    }

    private final static MouseListener buttonMouseListener = new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof TabButton) {
                TabButton button = (TabButton) component;
                button.setHover(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof TabButton) {
                TabButton button = (TabButton) component;
                button.setHover(false);
            }
        }
    };


}