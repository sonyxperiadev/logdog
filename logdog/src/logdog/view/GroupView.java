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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import logdog.model.LogLineMatcher;

@SuppressWarnings("serial")
public class GroupView extends JDialog implements ActionListener {

    // GUI controls
    private JDialog mOwner;

    private JTextField mGroupName;
    private JCheckBox mHasValueDiff;

    private JTextField mScaleUnit;
    private JTextField mScaleFormat;
    private JTextField mScaleRangeMin;
    private JTextField mScaleRangeMax;
    private JCheckBox mScaleIncludeZero;

    // Actions
    private SaveAction mSaveAction = new SaveAction();
    private CancelAction mCancelAction = new CancelAction();

    // String resources
    private static final String TITLE_STR = "Log line matcher group";
    private static final String SCALE_LABEL_STR = "Scale";
    private static final String NAME_LABEL_STR = "Name:";
    private static final String UNIT_LABEL_STR = "Unit:";
    private static final String FORMAT_LABEL_STR = "Format:";
    private static final String RANGE_MIN_LABEL_STR = "Min:";
    private static final String RANGE_MAX_LABEL_STR = "Max:";
    private static final String INCLUDE_ZERO_STR = "Include zero";
    private static final String ADD_VALUE_DIFF_STR = "Add value difference series";

    private static final String ADD_VALUE_DIFF_TOOLTIP_STR =
        "Compute the difference for two adjacent values and show " +
        "the differences in a separate series";

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

    private final int COMP_MARGIN = 14;
    private final int DEF_INSETS = 8;

    private LogLineMatcher.Group mGroup;

    /**
     * Constructor
     *
     * @param owner
     * @param group
     */
    public GroupView(JDialog owner, LogLineMatcher.Group group) {
        // Modal, default layout is BorderLayout.
        super(owner, TITLE_STR, true);

        mOwner = owner;
        mGroup = group;
        createGUI();
        setGUIAttributes();
        bindToGUI();
    }

    private JPanel createGUIScale() {
        JPanel scalePanel = new JPanel(new GridBagLayout());
        UIUtils.addMarginBorder(scalePanel, SCALE_LABEL_STR,
                                COMP_MARGIN, COMP_MARGIN, COMP_MARGIN, COMP_MARGIN);

        GridBagConstraints cons = new GridBagConstraints();
        cons.insets = new Insets(DEF_INSETS, DEF_INSETS, 0, DEF_INSETS);

        //// Left column
        cons.anchor = GridBagConstraints.FIRST_LINE_START;
        cons.weighty = 1;
        cons.gridheight = 1;
        cons.weightx = 0.25;

        // Scale unit
        cons.gridx = 0;
        cons.gridy = 0;
        JLabel label = new JLabel(UNIT_LABEL_STR);
        scalePanel.add(label, cons);

        // Scale format
        ++cons.gridy;
        label = new JLabel(FORMAT_LABEL_STR);
        scalePanel.add(label, cons);

        // Min range value
        ++cons.gridy;
        label = new JLabel(RANGE_MIN_LABEL_STR);
        scalePanel.add(label, cons);

        // Max range value
        ++cons.gridy;
        label = new JLabel(RANGE_MAX_LABEL_STR);
        scalePanel.add(label, cons);


        //// Right column
        // Scale unit
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.weightx = 0.75;
        cons.gridwidth = 1;
        cons.gridx = 1;
        cons.gridy = 0;
        mScaleUnit = new JTextField();
        scalePanel.add(mScaleUnit, cons);

        // Scale format
        ++cons.gridy;
        mScaleFormat = new JTextField();
        scalePanel.add(mScaleFormat, cons);

        // Min range value
        ++cons.gridy;
        mScaleRangeMin = new JTextField();
        scalePanel.add(mScaleRangeMin, cons);

        // Max range value
        ++cons.gridy;
        mScaleRangeMax = new JTextField();
        scalePanel.add(mScaleRangeMax, cons);

        // Scale include zero
        mScaleIncludeZero = new JCheckBox(INCLUDE_ZERO_STR);
        ++cons.gridy;
        scalePanel.add(mScaleIncludeZero, cons);

        return scalePanel;
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
        cons.gridy = 0;

        JLabel label = new JLabel(NAME_LABEL_STR);
        centerPanel.add(label, cons);

        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.weightx = 0.75;
        cons.gridy = 2;
        cons.gridwidth = 2;
        centerPanel.add(createGUIScale(), cons);


        //// Right column
        cons.gridwidth = 1;
        cons.gridx = 1;
        cons.gridy = 0;

        mGroupName = new JTextField();
        centerPanel.add(mGroupName, cons);

        mHasValueDiff = new JCheckBox(ADD_VALUE_DIFF_STR);
        mHasValueDiff.setToolTipText(ADD_VALUE_DIFF_TOOLTIP_STR);
        cons.gridy = 1;
        centerPanel.add(mHasValueDiff, cons);

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
        setResizable(false);
        //setMinimumSize(new Dimension(250, 250));
        //setSize(new Dimension(250, 250));
        setLocationRelativeTo(mOwner);
    }

    private void bindToGUI() {
        // Group related
        mGroupName.setText(mGroup.getName());
        mHasValueDiff.setSelected(mGroup.getHasValueDiff());

        // Scale related
        mScaleUnit.setText(mGroup.getScaleUnit());
        mScaleFormat.setText(mGroup.getScaleFormat());
        if (mGroup.hasScaleRange()) {
            mScaleRangeMin.setText(Double.toString(mGroup.getScaleRangeMin()));
            mScaleRangeMax.setText(Double.toString(mGroup.getScaleRangeMax()));
        }
        mScaleIncludeZero.setSelected(mGroup.getScaleIncludeZero());

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
            if (!UIUtils.checkRequired(GroupView.this, mGroupName)) {
                return;
            }
            mGroup.setName(mGroupName.getText());
            mGroup.setHasValueDiff(mHasValueDiff.isSelected());
            mGroup.setScaleUnit(mScaleUnit.getText().trim());
            mGroup.setScaleFormat(mScaleFormat.getText().trim());
            mGroup.assignScaleRange(mScaleRangeMin.getText().trim(),
                                    mScaleRangeMax.getText().trim());
            mGroup.setScaleIncludeZero(mScaleIncludeZero.isSelected());

            mSaved = true;
            dispose();
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
