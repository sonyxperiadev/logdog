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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class InputTextDialog extends JDialog implements ActionListener {

    private JTextArea mTextArea;   // when multiline
    private JTextField mText;
    private boolean mMultiline;
    private String mActionBtnCaption;
    private String mActionBtnIcon;
    private static final String CANCEL_STR = "Cancel";
    private boolean mCancelled;

    //TODO refactor to builder.
    public InputTextDialog(Frame owner, String title, String lead, String value, boolean multline,
                           boolean search) {
        super(owner, true);  // modal, BorderLayout

        setTitle(title);
        mMultiline = multline;
        mActionBtnCaption = search ? "Search" : "Save";
        mActionBtnIcon = search ? "find-16" : "save-16";

        // Create GUI
        Container contentPane = getContentPane();
        contentPane.add(createGUINorth(lead), BorderLayout.NORTH);
        if (mMultiline) {
            contentPane.add(createGUICenter(title, value), BorderLayout.CENTER);
        } else {
            mText = new JTextField(value);
            contentPane.add(mText, BorderLayout.CENTER);
        }
        contentPane.add(createGUISouth(), BorderLayout.SOUTH);

        setMinimumSize(new Dimension(600, mMultiline ? 200 : 20));
        setResizable(false);
        pack();
        setLocationRelativeTo(null);  // center on screen

        UIUtils.closeFrameWhenEscapePressed(this.getRootPane(), this);
    }

    public String getText() {
        if (mCancelled) {
            return null;
        }
        return mMultiline ? mTextArea.getText() : mText.getText();
    }

    private JButton createButton(String text, int mnemonic, String icon) {
        JButton btn = new JButton(text);
        btn.setMnemonic(mnemonic);
        btn.setIcon(UIUtils.createIcon(icon));
        btn.addActionListener(this);
        return btn;
    }

    private JLabel createGUINorth(String lead) {
        JLabel leadLabel = new JLabel(lead);
        leadLabel.setBorder(new EmptyBorder(6, 2, 6, 0));   // top, left, bottom, right
        return leadLabel;
    }

    private ScrollPane createGUICenter(String title, String value) {
        mTextArea = new JTextArea(value);
        UIUtils.useTabToChangeFocus(mTextArea);
        mTextArea.setRows(4);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.add(mTextArea);

        return scrollPane;
    }

    private JPanel createGUISouth() {
        JButton cancelBtn = createButton(CANCEL_STR, KeyEvent.VK_C, "cancel-16");
        JButton saveBtn = createButton(mActionBtnCaption, KeyEvent.VK_S, mActionBtnIcon);

        JPanel panelButtons = new JPanel();
        panelButtons.add(cancelBtn, BorderLayout.LINE_START);
        panelButtons.add(saveBtn, BorderLayout.LINE_START);

        return panelButtons;
    }

    @Override
    public void actionPerformed(ActionEvent actEvent) {
        String cmd = actEvent.getActionCommand();
        if (cmd.equals(mActionBtnCaption) ||
            cmd.equals(UIUtils.ESCAPE_ACTION_NAME)) {
            mCancelled = false;
        } else if (cmd.equals(CANCEL_STR)) {
            mCancelled = true;
        }
        setVisible(false);
    }
}
