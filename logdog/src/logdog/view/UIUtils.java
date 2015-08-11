/**
 * Copyright (c) 2013, Sony Mobile Communications Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This file is part of logdog.
 */

package logdog.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import logdog.Prefs;
import logdog.Prefs.Directory;
import logdog.utils.Logger;
import logdog.utils.Utils;

public class UIUtils {

    private static Prefs mPrefs;
    private static final SimpleDateFormat sDTFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static LLMFileFilter sLLMFileFilter = new LLMFileFilter();
    private static BlackListFileFilter sBlackListFileFilter = new BlackListFileFilter();

    public static final String ESCAPE_ACTION_NAME = "ESCAPE";
    private static final String REQUIRED_VALUE_CAPTION_STR = "Required value";
    private static final String REQUIRED_VALUE_STR = "This field must have a value.";

    public enum FILEDLG_FILTER {
        FILTER_NONE,
        FILTER_LLM,
        FILTER_BLACKLIST
    };

    public static void load(Prefs prefs) {
        mPrefs = prefs;
        setLookAndFeel();
        setAntiAlisingFont();
    }

    private static void setLookAndFeel() {
        try {
            // Properties listed here:
            // http://www.rgagnon.com/javadetails/JavaUIDefaults.txt
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            //Border emptyBorder = new EmptyBorder(0, 0, 0, 0);
            UIManager.put("Button.font", font);
            UIManager.put("CheckBox.font", font);
            UIManager.put("CheckBoxMenuItem.font", font);
            UIManager.put("CheckBoxMenuItem.acceleratorFont", font);
            UIManager.put("ComboBox.font", font);
            UIManager.put("EditorPane.font", font);
            UIManager.put("InternalFrame.font", font);
            UIManager.put("InternalFrame.titleFont", font);
            UIManager.put("Label.font", font);
            UIManager.put("List.font", font);
            UIManager.put("Menu.font", font);
            UIManager.put("MenuItem.font", font);
            UIManager.put("MenuBar.font", font);
            UIManager.put("Menu.acceleratorFont", font);
            UIManager.put("Panel.font", font);
            UIManager.put("PopupMenu.font", font);
            UIManager.put("RadioButton.font", font);
            UIManager.put("RadioButtonMenuItem.acceleratorFont", font);
            UIManager.put("RadioButtonMenuItem.font", font);
            UIManager.put("ScrollPane.font", font);
            UIManager.put("TextArea.font", font);
            UIManager.put("TextField.font", font);
            UIManager.put("TextPane.font", font);
            UIManager.put("TitledBorder.font", font);
            UIManager.put("ToggleButton.font", font);
            UIManager.put("ToolBar.font", font);
            UIManager.put("ToolTip.font", font);

            //UIManager.put("Label.font", UIManager.getFont("Label.font").deriveFont(8.0f));
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            //UIManager.put("Label.font", UIManager.getFont("Label.font").deriveFont(10.0f));
            //SwingUtilities.updateComponentTreeUI(yourJFrame);

            //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            //LookAndFeelInfo li[] = UIManager.getInstalledLookAndFeels();
            //UIManager.setLookAndFeel(li[0].getName());
        } catch (Exception excep) {
            Logger.log("Error setting native LAF: " + excep);
        }
    }

    private static void setAntiAlisingFont() {
        // This makes font use antialiasing.
        //System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
    }

    static void useTabToChangeFocus(Component comp) {
        Set<KeyStroke>
            strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("pressed TAB")));
        comp.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, strokes);
        strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("shift pressed TAB")));
        comp.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes);
    }

    static void closeFrameWhenEscapePressed(final JRootPane panel, final ActionListener actListener) {

        @SuppressWarnings("serial")
            Action escAction = new AbstractAction(ESCAPE_ACTION_NAME) {
                    public void actionPerformed(ActionEvent evt) {
                        actListener.actionPerformed(evt);
                    }
                };
        escAction.putValue(Action.ACTION_COMMAND_KEY, ESCAPE_ACTION_NAME);

        KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, ESCAPE_ACTION_NAME);
        panel.getActionMap().put(ESCAPE_ACTION_NAME, escAction);
    }

    private static class LogDogFileFilter extends FileFilter {
        public final String[] mFileExtensions;
        public final String mDescription;

        protected LogDogFileFilter(String[] fileExtensions, String description) {
            mFileExtensions = fileExtensions;
            mDescription = description;
        }

        /**
         * Add file extension if it is missing.
         *
         * @param file
         *
         * @return new File object if the extension has been added
         * else the given File argument
         */
        public File checkFileExtension(File file) {
            String fileName = file.getName();
            if (!fileName.endsWith(mFileExtensions[0])) {
                file = new File(file.getAbsolutePath() + mFileExtensions[0]);
            }
            return file;
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }

            for (String extension : mFileExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return mDescription;
        }
    }

    private static class LLMFileFilter extends LogDogFileFilter {
        public LLMFileFilter() {
            super(new String[] {".logdog"}, "Logdog file (*.logdog)");
        }
    }

    private static class BlackListFileFilter extends LogDogFileFilter {
        public BlackListFileFilter() {
            super(new String[] {".blklst"}, "Logdog black list file (*.blklst)");
        }
    }

    static File showFileDlg(Window owner, boolean newFile,
                            boolean open, FILEDLG_FILTER useFileFilter,
                            Prefs.Directory directory) {
        final JFileChooser dlg = new JFileChooser(open ? "Open" : "Save");
        File dir = new File(mPrefs.getDirectory(directory));
        dlg.setCurrentDirectory(dir);
        dlg.setPreferredSize(new Dimension(800, 800));

        // Set filter if given:
        LogDogFileFilter fileFilter = null;
        switch (useFileFilter) {
        case FILTER_LLM:
            fileFilter = sLLMFileFilter;
            break;
        case FILTER_BLACKLIST:
            fileFilter = sBlackListFileFilter;
            break;
        }
        if (fileFilter != null) {
            dlg.setFileFilter(fileFilter);
        }

        int answer = open ? dlg.showOpenDialog(owner) : dlg.showSaveDialog(owner);
        if (answer == JFileChooser.APPROVE_OPTION) {
            mPrefs.putDirectory(directory, dlg.getCurrentDirectory().getAbsolutePath());
            File file = dlg.getSelectedFile();
            if (newFile && file.exists() &&
                JOptionPane.showConfirmDialog(owner,
                                              "Overwrite file?", "Confirm overwrite",
                                              JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return null;
            }

            if (fileFilter != null) {
                file = fileFilter.checkFileExtension(file);
            }
            return file;
        }
        return null;
    }

    static boolean checkRequired(Component parent, JTextField textField) {
        String text = textField.getText();
        if (Utils.emptyString(text)) {
            textField.requestFocus();
            JOptionPane.showMessageDialog(parent, REQUIRED_VALUE_STR, REQUIRED_VALUE_CAPTION_STR,
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public static void showOnScreen(int screen, JFrame frame) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        if (screen >= 0 && screen < gs.length) {
            //gs[screen].setFullScreenWindow(frame);
        } else if (gs.length > 0) {
//            gs[0].setFullScreenWindow(frame);
        } else {
            throw new RuntimeException("No Screens Found");
        }
    }

    private static File getTimeStampedFile(String dir, String prefix) {
        Date now = new Date();
        File file = new File(String.format("%s/%s-%s", dir, prefix, sDTFormat.format(now)));
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException excep) {
                Logger.logExcep(excep);
                file = null;
            }
        }
        return file;
    }

    public static File getCurrentDirTimeStampedFile(String prefix) {
        String dirPath = mPrefs.getDirectory(Directory.LOGDOG_FILES);
        return getTimeStampedFile(new File(dirPath).getAbsolutePath(), prefix);
    }

    public static File getTempDirTimeStampedFile(String prefix) {
        String dir = System.getProperty("java.io.tmpdir");  // /tmp
        return getTimeStampedFile(dir, prefix);
    }

    static String getClipboardContent() {
        String result = null;
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
            contents != null &&
            contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException excep) {
                Logger.logExcep(excep);
            } catch (IOException excep) {
                Logger.logExcep(excep);
            }
        }
        return result;
    }

    static ImageIcon createIcon(String pngImage) {
        if (pngImage != null) {
            URL imageURL = UIUtils.class.getResource("/resources/" + pngImage + ".png");
            if (imageURL != null) {
                return new ImageIcon(imageURL);
            }
        }
        return null;
    }

    public static JTextArea createInfoTextArea(String text) {
        JTextArea infoArea = new JTextArea(text);
        infoArea.setEditable(false);
        infoArea.setCursor(null);
        infoArea.setOpaque(false);
        infoArea.setFocusable(false);
        infoArea.setWrapStyleWord(true);
        infoArea.setLineWrap(true);
        return infoArea;
    }

    public static void addMarginBorder(JPanel panel, String title,
            int top, int left, int bottom, int right) {
        Border border = BorderFactory.createTitledBorder(title);
        Border marginBorder = new EmptyBorder(top, left, bottom, right);
        panel.setBorder(new CompoundBorder(marginBorder, border));
    }

    @SuppressWarnings("serial")
    public static abstract class ActionBase extends AbstractAction {
        public ActionBase(String caption, String tooltip, String pngImage, int mnemonic) {
            super(caption, createIcon(pngImage));
            putValue(SHORT_DESCRIPTION, tooltip);
            putValue(MNEMONIC_KEY, mnemonic);
        }
    }
}
