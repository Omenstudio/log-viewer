package com.github.omenstudio.logviewer;


import com.alee.laf.WebLookAndFeel;
import com.github.omenstudio.logviewer.gui.MainFrame;

import javax.swing.*;

/**
 * Класс для запуска приложения
 */
public class App 
{
    public static void main( String[] args )
    {
        SwingUtilities.invokeLater(() -> {
            WebLookAndFeel.install();

            JFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
