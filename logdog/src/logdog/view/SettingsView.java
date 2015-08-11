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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import logdog.Prefs;

@SuppressWarnings("serial")
public class SettingsView extends JDialog implements ActionListener {

    private JFrame mOwner;
    
    // GUI controls
    private JCheckBox mShapesInCharts;
    private JTextField mSearchURL;

    // Actions
    private SaveAction mSaveAction = new SaveAction();
    private CancelAction mCancelAction = new CancelAction();

    // String resources
    private static final String TITLE_STR = "Settings";
    private static final String SHAPES_IN_CHARTS_STR = "Support shapes in chart curves";
    private static final String SEARCH_URL_STR = "OpenGrok search URL:";

    private static final String SEARCH_URL_TOOLTIP_STR =
        "Search URL to use when looking up log lines in OpenGrok. " +
        "Must contain one %s used as a place holder for the actual search string.";

    private final static String sCommands[] = {
        "Save",
        "Cancel"
    };

    // Misc
    private static final int CMD_SAVE = 0;
    private static final int CMD_CANCEL = 1;

    private boolean mSaved;

    boolean saved() {
        return mSaved;
    }

    private final int DEF_INSETS = 8;

    private Prefs mPrefs = new Prefs();

    public interface SettingsChangedListener {
        void onSaved(Prefs prefs);
    }
    private SettingsChangedListener mListener;

    /**
     * Constructor
     *
     * @param owner
     */
    public SettingsView(JFrame owner, SettingsChangedListener listener) {
        // Modal, default layout is BorderLayout.
        super(owner, TITLE_STR, true);

        mOwner = owner;
        mListener = listener;
        createGUI();
        setGUIAttributes();
        bindToGUI();
    }

    private JPanel createGUICenter() {
        JPanel centerPanel = new JPanel(new GridBagLayout());

        GridBagConstraints cons = new GridBagConstraints();
        cons.insets = new Insets(DEF_INSETS, DEF_INSETS, 0, DEF_INSETS);

        //// Left column
        cons.anchor = GridBagConstraints.FIRST_LINE_START;
        cons.weighty = 1;
        cons.gridheight = 1;

        cons.weightx = 0.25;
        cons.gridx = 0;
        cons.gridy = 1;

        JLabel label = new JLabel(SEARCH_URL_STR);
        centerPanel.add(label, cons);

        //// Right column
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.weightx = 5;
        cons.gridx = 1;
        cons.gridy = 0;

        mShapesInCharts = new JCheckBox();
        mShapesInCharts.setText(SHAPES_IN_CHARTS_STR);
        centerPanel.add(mShapesInCharts, cons);

        ++cons.gridy;
        mSearchURL = new JTextField();
        mSearchURL.setToolTipText(SEARCH_URL_TOOLTIP_STR);
        centerPanel.add(mSearchURL, cons);

        return centerPanel;
    }

    private JPanel createGUISouth() {
        JPanel panelButtons = new JPanel();

        JButton btn = new JButton(mCancelAction);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelButtons.add(btn, BorderLayout.LINE_START);

        btn = new JButton(mSaveAction);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelButtons.add(btn, BorderLayout.LINE_START);

        return panelButtons;
    }

    private void createGUI() {
        Container contentPane = getContentPane();
        contentPane.add(createGUICenter(), BorderLayout.CENTER);
        contentPane.add(createGUISouth(), BorderLayout.SOUTH);
    }

    private void setGUIAttributes() {
        UIUtils.closeFrameWhenEscapePressed(rootPane, this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 20));
        setResizable(false);
        setLocationRelativeTo(mOwner);
    }

    private void bindToGUI() {
        mShapesInCharts.setSelected(mPrefs.getShapesInCharts());
        mSearchURL.setText(mPrefs.getWebBrowserSearchString());
        pack();
        setVisible(true);
    }


    // Actions etc

    private class SaveAction extends UIUtils.ActionBase {
        public SaveAction() {
            super(sCommands[CMD_SAVE], null, "save-16", KeyEvent.VK_S);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            mPrefs.putShapesInCharts(mShapesInCharts.isSelected());
            String searchURL = mSearchURL.getText();
            mPrefs.putWebBrowserSearchString(searchURL);
            mSaved = true;
            dispose();
            if (mListener != null) {
                mListener.onSaved(mPrefs);
            }
        }
    }

    private class CancelAction extends UIUtils.ActionBase {
        public CancelAction() {
            super(sCommands[CMD_CANCEL], null, "cancel-16", KeyEvent.VK_C);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doCancel();
        }
    }

    public void doCancel() {
        mSaved = false;
        dispose();
    }

    @Override
    public void actionPerformed(ActionEvent actEvent) {
        String cmd = actEvent.getActionCommand();
        if (cmd.equals(UIUtils.ESCAPE_ACTION_NAME)) {
            doCancel();
        }
    }
}
