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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import logdog.Prefs.Directory;
import logdog.logdog;
import logdog.controller.MainController;
import logdog.controller.ViewSelListener;
import logdog.model.BlackList.BlackListListener;
import logdog.model.LogSource;
import logdog.model.LogSourceListener;
import logdog.utils.Logger;
import logdog.utils.Utils;
import logdog.view.MenuButton.MenuButtonListener;

@SuppressWarnings("serial")
public class LogSourceView
    extends JFrame
    implements ActionListener, LogSourceListener, ViewSelListener,
    MenuButtonListener, BlackListListener {

    private LogSource mLogSource;
    private MainController mController;
    private LogSourceList mLogSourceList;
    private JPopupMenu mPopupMenu;
    private JToggleButton mScrollLockToggleButton;

    private int mBookmarkNumber;

    // Actions
    private SaveAction mSaveAction = new SaveAction();
    private ClearAction mClearAction = new ClearAction();
    private PausePlayAction mPausePlayAction = new PausePlayAction();
    private CreateLLMAction mCreateLLMAction = new CreateLLMAction();
    private MarkAction mMarkAction = new MarkAction();
    private JTextField mMarkTextField = new JTextField();
    private MarkPrevAction mMarkPrevAction = new MarkPrevAction();
    private MarkNextAction mMarkNextAction = new MarkNextAction();
    private SelectAllAction mSelectAllAction = new SelectAllAction();
    private CopyAction mCopyAction = new CopyAction();
    private FontSmallerAction mFontSmallerAction = new FontSmallerAction();
    private FontLargerAction mFontLargerAction = new FontLargerAction();
    private ScrollLockAction mScrollLockAction = new ScrollLockAction();
    private InsertBookmarkAction mInsertBookmarkAction = new InsertBookmarkAction();
    private WebBrowserAction mWebBrowserAction = new WebBrowserAction(ACTION_WEBBROWSER_STR, false);
    private WebBrowserAction mWebBrowserImLuckyAction =
        new WebBrowserAction(ACTION_WEBBROWSER_IMLUCKY_STR, true);

    // BlackList actions
    private BLAddLineAction mBLAddLineAction = new BLAddLineAction();
    private BLAddThisLogTagAction mBLAddThisLogTagAction = new BLAddThisLogTagAction();
    private BLOpenAction mBLOpenAction = new BLOpenAction();
    private BLEditAction mBLEditAction = new BLEditAction();
    private BLSaveAction mBLSaveAction = new BLSaveAction();
    private BLClearAction mBLClearAction = new BLClearAction();

    private MenuButton mBLMenuButton;

    private Font mPlainFont;
    private Font mBoldFont;

    // String resources
    private static final String TOOLBAR_CAPTION_STR = "Commands";
    private static final String BLACKLIST_BUTTON_STR = "Blacklist";
    private static final String BLACKLIST_BUTTON_TOOLTIP_STR =
        "Commands for black listing log lines, clearing and saving the list etc";
    private static final String ACTION_SAVE_STR = "Save...";
    private static final String ACTION_SAVE_TOOLTIP_STR = "Save log source to file";
    private static final String ACTION_CLEAR_STR = "Clear";
    private static final String ACTION_CLEAR_TOOLTIP_STR = "Clear list content";
    private static final String ACTION_PAUSE_STR = "Pause";
    private static final String ACTION_PLAY_STR = "Resume";
    private static final String ACTION_PAUSE_TOOLTIP_STR = "Pause or resume updating the list";
    private static final String ACTION_CREATE_LLM_STR = "Create matcher";
    private static final String ACTION_CREATE_LLM_TOOLTIP_STR =
        "Create a logline matcher from the selected log line. " +
        "Click Pause to prevent the selected line from changing.";
    private static final String ACTION_MARK_STR = "Mark:";
    private static final String ACTION_MARK_TOOLTIP_STR =
        "Use bold font for lines matching the given regular expression";
    private static final String ACTION_MARK_PREVIOUS_STR = "< Previous";
    private static final String ACTION_MARK_PREVIOUS_TOOLTIP_STR =  "Find previous";
    private static final String ACTION_MARK_NEXT_STR = "Next >";
    private static final String ACTION_MARK_NEXT_TOOLTIP_STR = "Find next";
    private static final String ACTION_SELECT_ALL_STR = "Select all";
    private static final String ACTION_SELECT_ALL_TOOLTIP_STR = "Select all lines the the list";
    private static final String ACTION_COPY_STR = "Copy";
    private static final String ACTION_COPY_TOOLTIP_STR = "Copy to the clipboard";
    private static final String ACTION_DECREASE_TOOLTIP_STR = "Decrease font size";
    private static final String ACTION_INCREASE_TOOLTIP_STR = "Increase font size";
    private static final String ACTION_SCROLLLOCK_TOOLTIP_STR = "Scroll lock";
    private static final String ACTION_INSERT_BOOKMARK_STR = "Bookmark";
    private static final String ACTION_INSERT_BOOKMARK_TOOLTIP_STR = "Insert bookmark log line";
    private static final String ACTION_WEBBROWSER_STR = "Search in OpenGrok...";
    private static final String ACTION_WEBBROWSER_IMLUCKY_STR = "Search in OpenGrok (I feel lucky)";
    private static final String ACTION_WEBBROWSER_TOOLTIP_STR =
        "Try to find the given log line in OpenGrok";

    private static final String ACTION_BLACKLIST_LINE_STR = "Blacklist log line...";
    private static final String ACTION_BLACKLIST_LINE_TOOLTIP_STR = "Blacklist current/selected log line";
    private static final String ACTION_BLACKLIST_TAG_STR = "Blacklist log tag";
    private static final String ACTION_BLACKLIST_TAG_TOOLTIP_STR =
        "Blacklist all log lines having the same log tag as the current/selected log line";
    private static final String ACTION_BLACKLIST_OPEN_STR = "Open...";
    private static final String ACTION_BLACKLIST_OPEN_TOOLTIP_STR = "Open blacklist file";
    private static final String BLACKLIST_OPEN_FAILED_STR = "Failed to open";
    private static final String BLACKLIST_OPEN_LINE_FORMAT_STR = "Read error on line %d";
    private static final String ACTION_BLACKLIST_EDIT_STR = "Edit...";
    private static final String ACTION_BLACKLIST_EDIT_TOOLTIP_STR = "Edit blacklist";
    private static final String ACTION_BLACKLIST_SAVE_STR = "Save to file...";
    private static final String ACTION_BLACKLIST_SAVE_TOOLTIP_STR = "Save blacklist to file";
    private static final String BLACKLIST_SAVE_FAILED_STR = "Failed to save";
    private static final String ACTION_BLACKLIST_CLEAR_STR = "Clear";
    private static final String ACTION_BLACKLIST_CLEAR_TOOLTIP_STR = "Clear blacklist";
    private static final String BLACKLIST_ADD_CAPTION_STR = "Add blacklist log line";
    private static final String BLACKLIST_EDIT_CAPTION_STR = "Edit blacklist";
    private static final String BLACKLIST_CLEAR_CAPTION_STR = "Blacklist";
    private static final String BLACKLIST_REGEXP_LEAD_STR = "Blacklist regular expression:";
    private static final String BLACKLIST_CLEAR_Q_STR = "Clear blacklist?";

    private static final String MARK_TEXT_FIELD_TOOLTIP_STR =
        "Mark and search regular expression (case insensitive)";

    private static final String WEBBROWSER_CAPTION_STR = "OpenGrok search";
    private static final String WEBBROWSER_REGEXP_STR = "OpenGrok search string:";
    private static final String WEBBROWSER_SEARCH_CAPTION_STR = "Failed to search";
    private static final String WEBBROWSER_SEARCH_URI_MALFORMED_STR =
            "No search URL is specified in Settings or the search URL is missing " +
            "the %s place holder for the actual search string.";

    /**
     * Constructor.
     *
     * @param logSource
     * @param controller
     */
    public LogSourceView(LogSource logSource, MainController controller) {
        super(logSource.getName());

        mLogSource = logSource;
        mController = controller;

        createGUI();
        setGUIAttributes();

        // Don't put these in windowOpened() below because then we
        // might miss loglines.
        mLogSource.addListener(this);
        mLogSource.setBlackListListener(this);
        mController.addSelListener(this);
        mBLMenuButton.setListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                mLogSourceList.die();  // interrupts mAddUpdateUIThread
                mLogSource.removeListener(LogSourceView.this);
                mController.removeSelListener(LogSourceView.this);
                mBLMenuButton.removeListener();
            }
        });
    }

    private void configureMarkTextField() {
        Font defFont = UIManager.getDefaults().getFont("TabbedPane.font");
        FontMetrics fm = getFontMetrics(defFont);
        mMarkTextField.setPreferredSize(new Dimension(150, fm.getHeight() + fm.getDescent()));
        mMarkTextField.setToolTipText(MARK_TEXT_FIELD_TOOLTIP_STR);

        mMarkTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    doMark();
                }
            }
        });

        mMarkTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent arg0) {
                mMarkTextField.select(0, 0);
            }

            @Override
            public void focusGained(FocusEvent arg0) {
                mMarkTextField.selectAll();
            }
        });
    }

    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar(TOOLBAR_CAPTION_STR);
        toolbar.addButton(mSaveAction);
        toolbar.addButton(mClearAction);
        toolbar.addButton(mPausePlayAction);
        toolbar.addButton(mCreateLLMAction);

        mBLMenuButton = new MenuButton(BLACKLIST_BUTTON_STR, BLACKLIST_BUTTON_TOOLTIP_STR, true);

        //mBLMenuButton.setMnemonic(KeyEvent.VK_I);
        mBLMenuButton.add(mBLAddLineAction);
        mBLMenuButton.add(mBLAddThisLogTagAction);
        mBLMenuButton.add(mBLOpenAction);
        mBLMenuButton.add(mBLEditAction);
        mBLMenuButton.add(mBLSaveAction);
        mBLMenuButton.add(mBLClearAction);
        toolbar.add(mBLMenuButton);

        toolbar.addSeparator();

        toolbar.addButton(mMarkAction);
        configureMarkTextField();
        toolbar.add(mMarkTextField);
        toolbar.addButton(mMarkPrevAction);
        toolbar.addButton(mMarkNextAction);

        toolbar.addSeparator();

        toolbar.addButton(mFontSmallerAction);
        toolbar.addButton(mFontLargerAction);

        toolbar.addSeparator();

        mScrollLockToggleButton = toolbar.addToggleButton(mScrollLockAction);
        toolbar.addButton(mInsertBookmarkAction);

        return toolbar;
    }

    private void createPopupMenu() {
        mPopupMenu = new JPopupMenu();
        mPopupMenu.setBorder(new LineBorder(Color.gray));
        mPopupMenu.add(new JMenuItem(mSaveAction));
        mPopupMenu.add(new JMenuItem(mClearAction));
        mPopupMenu.add(new JMenuItem(mPausePlayAction));
        mPopupMenu.add(new JMenuItem(mCreateLLMAction));
        mPopupMenu.add(new JMenuItem(mWebBrowserAction));
        mPopupMenu.add(new JMenuItem(mWebBrowserImLuckyAction));
        mPopupMenu.add(new JPopupMenu.Separator());
        mPopupMenu.add(new JMenuItem(mSelectAllAction));
        mPopupMenu.add(new JMenuItem(mCopyAction));
        mPopupMenu.add(new JPopupMenu.Separator());
        mPopupMenu.add(new JMenuItem(mBLAddLineAction));
        mPopupMenu.add(new JMenuItem(mBLAddThisLogTagAction));

        // mLogSourceList.addMouseListener(new MouseAdapter() {
        // });
    }

    private void createGUI() {
        ImageIcon imageIcon = UIUtils.createIcon("logdog-32");
        if (imageIcon != null) {
            setIconImage(imageIcon.getImage());
        }

        // Default layout is BorderLayout.
        Container contentPane = getContentPane();

        ToolBar toolbar = createToolbar();
        contentPane.add(toolbar, BorderLayout.NORTH);

        mLogSourceList = new LogSourceList(mLogSource);
        mLogSourceList.setBackground(Color.white);
        contentPane.add(mLogSourceList.getScrollPane(), BorderLayout.CENTER);
        mLogSourceList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                int clickCount = evt.getClickCount();
                if (clickCount == 1) {
                    mLogSourceList.syncMarkListIndex();
                } else if (clickCount == 2) {
                    doParseAsRegExp();
                }
            }

            public void mousePressed(MouseEvent evt) {
                doPop(evt);
            }

            public void mouseReleased(MouseEvent evt) {
                doPop(evt);
            }

            private void doPop(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    mLogSourceList.selectIndexFromMousePoint(evt);
                    updateActions();
                    mPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }
        });

        createPopupMenu();

        if (mPlainFont == null) {
            mPlainFont = mBLMenuButton.getFont();
            mBoldFont = mPlainFont.deriveFont(Font.BOLD, mPlainFont.getSize());
        }
    }

    private void setGUIAttributes() {
        UIUtils.closeFrameWhenEscapePressed(rootPane, this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 600));
        setLocation(200, 200);
        setFocusable(true);
        requestFocus();
    }

    private void setBLMenuButtonText(boolean dirty) {
        mBLMenuButton.setText((dirty ? "*" : "") + BLACKLIST_BUTTON_STR);
    }

    private void doParseAsRegExp() {
        String selLine = mLogSourceList.getSelectedValue();
        if (selLine != null) {
            mController.pasteAsRegExp(LogSourceView.this, selLine);
        }
    }

    private void setMarkString() {
        mLogSourceList.setMarkRegExp(mMarkTextField.getText());
    }

    private void doMark() {
        setMarkString();
        mLogSourceList.repaint();
    }

    private void ensureIndexVisibleInCenter(int index) {
        setScrollLock(true);

        int firstVisIndex = mLogSourceList.getFirstVisibleIndex();
        int lastVisIndex = mLogSourceList.getLastVisibleIndex();
        int halfPageItemCount = (lastVisIndex - firstVisIndex + 1) / 2;

        if (index >= firstVisIndex && index <= lastVisIndex) {
            // Item is within visible range
            if (index <= (firstVisIndex + halfPageItemCount)) {
                index -= halfPageItemCount + 1;
            } else {
                index += halfPageItemCount;
            }
        } else if (index < firstVisIndex) {
            // Item is above visible index range
            index -= halfPageItemCount;
            if (index < 0) {
                index = 0;
            }
        } else if (index > lastVisIndex) {
            // Item is below visible index range
            index += halfPageItemCount;
            if (index > mLogSourceList.getModel().getSize()) {
                index = mLogSourceList.getModel().getSize() - 1;
            }
        }
        mLogSourceList.ensureIndexIsVisible(index);
    }

    private void doMarkPrev() {
        setMarkString();
        int index = mLogSourceList.getMarkPreviousIndex();
        if (index >= 0) {
            ensureIndexVisibleInCenter(index);
        }
    }

    private void doMarkNext() {
        setMarkString();
        int index = mLogSourceList.getMarkNextIndex();
        if (index >= 0) {
            ensureIndexVisibleInCenter(index);
        }
    }

    private void doSave() {
        File file = UIUtils.showFileDlg(this, true, false, UIUtils.FILEDLG_FILTER.FILTER_NONE,
                                        Directory.LOGSOURCE_FILES);
        if (file != null) {
            mLogSourceList.saveToFile(file);
        }
    }

    private boolean setScrollLock(boolean lock) {
        boolean prevScrollLock = mScrollLockToggleButton.isSelected();
        if (lock) {
            if (!mScrollLockToggleButton.isSelected()) {
                mScrollLockToggleButton.doClick();
            }
        } else if (mScrollLockToggleButton.isSelected()) {
            mScrollLockToggleButton.doClick();
        }
        return prevScrollLock;
    }

    private void updateActions() {
        boolean hasSelection = !mLogSourceList.isSelectionEmpty();
        mBLAddLineAction.setEnabled(hasSelection);
        mBLAddThisLogTagAction.setEnabled(hasSelection);
        mCopyAction.setEnabled(hasSelection);
    }

    // Actions
    private class SaveAction extends UIUtils.ActionBase {
        public SaveAction() {
            super(ACTION_SAVE_STR , ACTION_SAVE_TOOLTIP_STR, "save-16", KeyEvent.VK_S);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doSave();
        }
    }

    private class ClearAction extends UIUtils.ActionBase {
        public ClearAction() {
            super(ACTION_CLEAR_STR, ACTION_CLEAR_TOOLTIP_STR, "clear-16", KeyEvent.VK_L);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            mLogSourceList.clear();
        }
    }

    private class PausePlayAction extends UIUtils.ActionBase {
        private ImageIcon mPauseIcon;
        private ImageIcon mPlayIcon;

        public PausePlayAction() {
            super(ACTION_PAUSE_STR, ACTION_PAUSE_TOOLTIP_STR, "pause-16", KeyEvent.VK_R);
            mPauseIcon = UIUtils.createIcon("pause-16");
            mPlayIcon = UIUtils.createIcon("play-16");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            mLogSourceList.setAcceptAdd(!mLogSourceList.getAcceptAdd());
            putValue(SMALL_ICON, mLogSourceList.getAcceptAdd() ? mPauseIcon : mPlayIcon);
            putValue(NAME, mLogSourceList.getAcceptAdd() ? ACTION_PAUSE_STR : ACTION_PLAY_STR);
        }
    }

    private class CreateLLMAction extends UIUtils.ActionBase {
        public CreateLLMAction() {
            super(ACTION_CREATE_LLM_STR, ACTION_CREATE_LLM_TOOLTIP_STR, "new-16", KeyEvent.VK_M);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doParseAsRegExp();
        }
    }

    private class MarkAction extends UIUtils.ActionBase {
        public MarkAction() {
            super(ACTION_MARK_STR, ACTION_MARK_TOOLTIP_STR, "find-16", KeyEvent.VK_K);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doMark();
        }
    }

    private class MarkPrevAction extends UIUtils.ActionBase {
        public MarkPrevAction() {
            super(ACTION_MARK_PREVIOUS_STR, ACTION_MARK_PREVIOUS_TOOLTIP_STR, "", KeyEvent.VK_V);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doMarkPrev();
        }
    }

    private class MarkNextAction extends UIUtils.ActionBase {
        public MarkNextAction() {
            super(ACTION_MARK_NEXT_STR, ACTION_MARK_NEXT_TOOLTIP_STR, "", KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doMarkNext();
        }
    }

    private class SelectAllAction extends UIUtils.ActionBase {
        public SelectAllAction() {
            super(ACTION_SELECT_ALL_STR, ACTION_SELECT_ALL_TOOLTIP_STR,
                  "select-all-16", KeyEvent.VK_A);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int end = mLogSourceList.getModel().getSize() - 1;
            if (end >= 0) {
                int start = 0;
                mLogSourceList.setSelectionInterval(start, end);
            }
        }
    }

    private class CopyAction extends UIUtils.ActionBase {
        public CopyAction() {
            super(ACTION_COPY_STR, ACTION_COPY_TOOLTIP_STR, "copy-16", KeyEvent.VK_COPY);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            // Rely the action to the JList:
            Action copyAction = mLogSourceList.getActionMap().get("copy");
            ActionEvent actionEvent = new ActionEvent(mLogSourceList, ActionEvent.ACTION_PERFORMED, "");
            copyAction.actionPerformed(actionEvent);
        }
    }

    private class FontSmallerAction extends UIUtils.ActionBase {
        public FontSmallerAction() {
            super("", ACTION_DECREASE_TOOLTIP_STR, "font-smaller-16", KeyEvent.VK_LESS);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            mLogSourceList.decreaseFontSize();
        }
    }

    private class FontLargerAction extends UIUtils.ActionBase {
        public FontLargerAction() {
            super("", ACTION_INCREASE_TOOLTIP_STR, "font-larger-16", KeyEvent.VK_GREATER);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            mLogSourceList.increaseFontSize();
        }
    }

    private class ScrollLockAction extends UIUtils.ActionBase {
        public ScrollLockAction() {
            super("", ACTION_SCROLLLOCK_TOOLTIP_STR, "lock-16", KeyEvent.VK_SCROLL_LOCK);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            JToggleButton toggleButton = (JToggleButton) evt.getSource();
            mLogSourceList.setScrollLock(toggleButton.isSelected());
        }
    }

    private class InsertBookmarkAction extends UIUtils.ActionBase {
        public InsertBookmarkAction() {
            super(ACTION_INSERT_BOOKMARK_STR, ACTION_INSERT_BOOKMARK_TOOLTIP_STR, null, KeyEvent.VK_B);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            mLogSourceList.add("--------------- LOGDOG BOOKMARK " + mBookmarkNumber++ + " ---------------");
        }
    }

    private class WebBrowserAction extends UIUtils.ActionBase {
        private boolean mLucky;

        public WebBrowserAction(String caption, boolean lucky) {
            super(caption, ACTION_WEBBROWSER_TOOLTIP_STR, null, KeyEvent.VK_UNDEFINED);
            mLucky = lucky;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (mLogSourceList.isSelectionEmpty()) {
                return;
            }

            // If not lucky then show dialog so the user can edit the
            // search string:
            String logLine = LogSource.removeToLogTag(mLogSourceList.getSelectedValue());
            if (logLine == null) {
                return;
            }
            if (!mLucky) {
                InputTextDialog dlg =
                    new InputTextDialog(LogSourceView.this, WEBBROWSER_CAPTION_STR,
                                        WEBBROWSER_REGEXP_STR, logLine, false, true);
                dlg.setVisible(true);
                logLine = dlg.getText();
                if (Utils.emptyString(logLine)) {
                    return;
                }
            }

            // Create URI and start the web browser:
            URI uri = LogSource.createSearchURI(logLine);
            if (uri == null) {
                JOptionPane.showMessageDialog(LogSourceView.this, WEBBROWSER_SEARCH_URI_MALFORMED_STR,
                                              WEBBROWSER_SEARCH_CAPTION_STR, JOptionPane.OK_OPTION);
                return;
            }
            try {
                Desktop.getDesktop().browse(uri);
            } catch (IOException excep) {
                Logger.logExcep(excep);
            }
        }
    }

    private class BLAddLineAction extends UIUtils.ActionBase {
        public BLAddLineAction() {
            super(ACTION_BLACKLIST_LINE_STR, ACTION_BLACKLIST_LINE_TOOLTIP_STR,
                  "skull-16", KeyEvent.VK_UNDEFINED);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (mLogSourceList.isSelectionEmpty()) {
                return;
            }
            String logLineRegExp = LogSource.parseToRegExp(mLogSourceList.getSelectedValue());
            if (Utils.emptyString(logLineRegExp)) {
                return;
            }

            InputTextDialog dlg =
                new InputTextDialog(LogSourceView.this, BLACKLIST_ADD_CAPTION_STR,
                                    BLACKLIST_REGEXP_LEAD_STR, logLineRegExp, false, false);
            dlg.setVisible(true);
            String BLLine = dlg.getText();
            if (!Utils.emptyString(BLLine)) {
                mLogSource.addToBlackList(BLLine);
            }
        }
    }

    private class BLAddThisLogTagAction extends UIUtils.ActionBase {
        public BLAddThisLogTagAction() {
            super(ACTION_BLACKLIST_TAG_STR, ACTION_BLACKLIST_TAG_TOOLTIP_STR,
                  "skull-16", KeyEvent.VK_UNDEFINED);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (mLogSourceList.isSelectionEmpty()) {
                return;
            }
            String logLineRegExp = LogSource.parseToLogTag(mLogSourceList.getSelectedValue());
            if (Utils.emptyString(logLineRegExp)) {
                return;
            }
            mLogSource.addToBlackList(logLineRegExp);
        }
    }

    private class BLOpenAction extends UIUtils.ActionBase {
        public BLOpenAction() {
            super(ACTION_BLACKLIST_OPEN_STR, ACTION_BLACKLIST_OPEN_TOOLTIP_STR,
                  "open-16", KeyEvent.VK_O);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            JFrame owner = LogSourceView.this;
            File file = UIUtils.showFileDlg(owner, false, true, UIUtils.FILEDLG_FILTER.FILTER_BLACKLIST,
                                            Directory.BLACKLIST_FILES);
            if (file == null) {
                return;
            }
            try {
                int line = mLogSource.readBlackList(file);
                if (line >= 0) {
                    JOptionPane.showMessageDialog(owner, BLACKLIST_OPEN_FAILED_STR,
                                                  String.format(BLACKLIST_OPEN_LINE_FORMAT_STR, line),
                                                  JOptionPane.OK_OPTION);
                }
            } catch (IOException excep) {
                Logger.logExcep(excep);
                JOptionPane.showMessageDialog(owner, BLACKLIST_OPEN_FAILED_STR,
                                              excep.getMessage(), JOptionPane.OK_OPTION);
            }
        }
    }

    private class BLEditAction extends UIUtils.ActionBase {
        public BLEditAction() {
            super(ACTION_BLACKLIST_EDIT_STR, ACTION_BLACKLIST_EDIT_TOOLTIP_STR,
                  "edit-16", KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            InputTextDialog dlg = new InputTextDialog(LogSourceView.this, BLACKLIST_EDIT_CAPTION_STR,
                                                      BLACKLIST_REGEXP_LEAD_STR,
                                                      mLogSource.getBlackListAsString(), true, false);
            dlg.setVisible(true);
            mLogSource.replaceBlackList(dlg.getText());
        }
    }

    private class BLSaveAction extends UIUtils.ActionBase {
        public BLSaveAction() {
            super(ACTION_BLACKLIST_SAVE_STR, ACTION_BLACKLIST_SAVE_TOOLTIP_STR,
                  "save-16", KeyEvent.VK_S);
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            JFrame owner = LogSourceView.this;
            File file = UIUtils.showFileDlg(owner, true, false, UIUtils.FILEDLG_FILTER.FILTER_BLACKLIST,
                                            Directory.BLACKLIST_FILES);
            if (file == null) {
                return;
            }
            try {
                mLogSource.saveBlackList(file);
                setBLMenuButtonText(false);
            } catch (IOException excep) {
                Logger.logExcep(excep);
                JOptionPane.showMessageDialog(owner, BLACKLIST_SAVE_FAILED_STR,
                                              excep.getMessage(), JOptionPane.OK_OPTION);
            }
        }
    }

    private class BLClearAction extends UIUtils.ActionBase {
        public BLClearAction() {
            super(ACTION_BLACKLIST_CLEAR_STR, ACTION_BLACKLIST_CLEAR_TOOLTIP_STR,
                  "clear-16", KeyEvent.VK_C);
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int answer = JOptionPane.showConfirmDialog(LogSourceView.this, BLACKLIST_CLEAR_Q_STR,
                                                       BLACKLIST_CLEAR_CAPTION_STR,
                                                       JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                mLogSource.clearBlackList();
                setBLMenuButtonText(false);
            }
        }
    }

    public void setWindowTitle(String fileName) {
        if (fileName != null) {
            setTitle(String.format("%s - %s", logdog.getFriendlyVersion(), fileName));
        } else {
            setTitle(logdog.getFriendlyVersion());
        }
    }


    // LogSourceListener

    public void onLogLine(String line) {
        // We are not on the UI thread so we should really do the below
        // but its to slow and resource consuming.
        // EventQueue.invokeLater(new Runnable() { public void run() {
        // mLogSourceList.add(line); } });

        // This works...
        Date date = LogSource.getDate(line);  // date can be null
        mLogSourceList.add(line, date);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
    }


    // ViewSelListener

    @Override
    public void onSelection(long time) {
        // Prevent scrolling regardless if we find the time stamp or not.
        boolean prevScrollLock = setScrollLock(true);
        if (mLogSourceList.scrollTo(time)) {
            toFront();
        } else {
            setScrollLock(prevScrollLock);
        }
    }


    // MenuButtonListener

    @Override
    public void onPopup() {
        updateActions();
    }

    @Override
    public void onSelected(int id) {
    }


    // BlackListListener

    @Override
    public void onModified(boolean dirty) {
        boolean hasBlackList = mLogSource.hasBlackList();
        mBLMenuButton.setFont(hasBlackList ? mBoldFont : mPlainFont);
        setBLMenuButtonText(dirty);
        mBLSaveAction.setEnabled(hasBlackList);
        mBLClearAction.setEnabled(hasBlackList);
    }
}
