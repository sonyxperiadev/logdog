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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Caret;

import logdog.Prefs.Directory;
import logdog.controller.LLMController;
import logdog.model.LLMEditListener;
import logdog.model.LogLineMatcher;
import logdog.model.LogLineMatcher.Group;
import logdog.model.LogLineMatcherManager;
import logdog.model.LogSource;
import logdog.model.LogSourceTriggerList;
import logdog.utils.Utils;
import logdog.view.MenuButton.MenuButtonListener;

@SuppressWarnings("serial")
public class LLMView extends JDialog
    implements ActionListener, LLMEditListener, MenuButtonListener {

    // GUI controls
    private JFrame mOwner;
    private JPanel mCenterPanel;
    private DefaultListModel<LogLineMatcher> mLLMListModel;
    private JList<LogLineMatcher> mLLMList;
    private boolean mLBValueChangedEnabled = true;
    private JRadioButton mTimeDurationRB;
    private JRadioButton mCountDurationRB;
    private JTextField mTimeDurationField;
    private JTextField mCountDurationField;
    private JTextField mNameTextField;
    private JTextField mRegexpTextField;
    private DefaultListModel<LogLineMatcher.Group> mGroupListModel;
    private JList<LogLineMatcher.Group> mGroupList;
    private JComboBox<String> mLogSourceCombo;
    private JComboBox<String> mPresentationIdCombo;
    private JComboBox<String> mTriggerCombo;
    private JCheckBox mEventCB;
    private JCheckBox mEnabledCB;
    private JCheckBox mTimeDiffCB;

    // Actions
    private SaveAction mSaveAction = new SaveAction();
    private CancelAction mCancelAction = new CancelAction();
    private AddAction mAddAction = new AddAction();
    private DeleteAction mDeleteAction = new DeleteAction();
    private MoveUpAction mMoveUpAction = new MoveUpAction();
    private MoveDownAction mMoveDownAction = new MoveDownAction();
    private GroupAddAction mGroupAddAction = new GroupAddAction();
    private GroupEditAction mGroupEditAction = new GroupEditAction();
    private GroupDeleteAction mGroupDeleteAction = new GroupDeleteAction();
    private GroupMoveUpAction mGroupMoveUpAction = new GroupMoveUpAction();
    private GroupMoveDownAction mGroupMoveDownAction = new GroupMoveDownAction();

    // Current index when accessing LogLineMatchers using mLLMCtrl:
    private LogLineMatcher mSelectedLLM;
    private int mLLMIndex = -1;

    // String resources
    private static final String TITLE_NEW_STR = "New";
    private static final String TITLE_EDIT_STR = "Edit";
    private static final String DATA_COLL_DURATION_CAPTION_STR = "Data collection duration";
    private static final String DATA_COLL_DURATION_MINUTES_STR = "Duration (minutes):";
    private static final String DATA_COLL_DURATION_COUNT_STR = "Data point count:";
    private static final String LLMS_CAPTION_STR = "Log line matchers";
    private static final String LLM_CAPTION_STR = "Selected log line matcher";
    private static final String LLM_NAME_LABEL_STR = "Name:";
    private static final String LLM_REGEXP_LABEL_STR = "Regular expression:";
    private static final String LLM_GROUPS_LABEL_STR = "Groups:";
    private static final String LLM_LOGSOURCE_LABEL_STR = "Log source:";
    private static final String LLM_PRESENTATIONID_LABEL_STR = "Chart:";
    private static final String LLM_TRIGGER_LABEL_STR = "Trigger:";
    private static final String LLM_HELP_STR =
        "Click Add to create a new log line matcher and enter a unique name for it " +
        "in the 'Name' field.\n\n" +
        "Log lines matching the given regular expression will " +
        "be plotted. By enclosing a sub regexp in '()' you create a regexp group. " +
        "The value from a group is parsed as an integer, float or a hex value " +
        "and plotted. For instance, this regexp has one group parsed as an int value:\n" +
        ".* V Preeffect: EFFECT_CMD_SET_VOLUME vol = ([0-9]+)$\n" +
        "Click the Paste-button to easily paste standard regexps.\n\n" +
        "Every group in the regexp can be given a name and other attributes. " +
        "Select a line and click any of the buttons to edit or deleted a " +
        "group or create a new one.";
    private static final String LLM_INVALID_LMM_STR = "Invalid LogLineMatcher";
    private static final String MISSING_PATH_STR = "Missing path";
    private static final String UNHANDLED_EXCEP_STR = "Wups... unhandled RuntimeException";
    private static final String LLM_DELETE_Q_STR = "Are you sure you want to delete this LogLineMatcher?";
    private static final String DELETE_TITLE_STR = "Delete";
    private static final String PASTE_STR = "Paste...";
    private static final String PASTE_TOOLTIP_STR =
        "Various commands for pasting regexps into the regexp field.";
    private static final String CB_EVENT_STR = "Event";
    private static final String CB_ENABLED_STR = "Enabled";
    private static final String CB_CREATE_TIME_DIFF_SERIES_STR = "Create log time series (ms)";
    private static final String GROUP_DELETE_Q_STR =
        "Are you sure you want to delete this regular expression group?";

    private final static String sCommands[] = {
        "Save",
        "Cancel",
        "Add",
        "Delete",
        "Up",
        "Down",
        "From clipboard as regexp",
        "Whatever (.*)",
        "Integer group",
        "Float group",
        "Hex group",
        "Add",
        "Edit",
        "Delete",
        "Up",
        "Down",
    };

    private final static String sCommandsTooltip[] = {
        "Save all log line matchers to file and close dialog",
        "Close dialog without saving",
        "Add log line matcher",
        "Delete the selected log line matcher",
        "Move the selected log line matcher one position up",
        "Move the selected log line matcher one position down",
        "Parse what's on the clipboard assuming it is a logcat log line, " +
        "escaping regexp special characters etc",
        "Paste regexp matching zero or more occurences of any character",
        "Paste integer regexp group",
        "Paste float regexp group",
        "Paste hex regexp group",
        "Add new regexp group",
        "Edit selected regexp group",
        "Delete selected regexp group",
        "Move regexp group up",
        "Move regexp group down",
    };

    // Misc
    private static final int CMD_SAVE = 0;
    private static final int CMD_CANCEL = 1;
    private static final int CMD_ADD = 2;
    private static final int CMD_DELETE = 3;
    private static final int CMD_MOVE_UP = 4;
    private static final int CMD_MOVE_DOWN = 5;
    private static final int CMD_PASTE_AS_REGEXP = 6;
    private static final int CMD_PASTE_WHATEVER = 7;
    private static final int CMD_PASTE_INT_GROUP = 8;
    private static final int CMD_PASTE_FLOAT_GROUP = 9;
    private static final int CMD_PASTE_HEX_GROUP = 10;
    private static final int CMD_GROUP_ADD = 11;
    private static final int CMD_GROUP_EDIT = 12;
    private static final int CMD_GROUP_DELETE = 13;
    private static final int CMD_GROUP_MOVE_UP = 14;
    private static final int CMD_GROUP_MOVE_DOWN = 15;

    private MenuButton mPasteRegExp;

    private LLMController mLLMCtrl;

    private final int COMP_MARGIN = 14;
    private final int DEF_INSETS = 8;

    /**
     * Constructor
     *
     * @param owner
     * @param isNew
     * @param pasteAsRegExp if non-null then a new LLM is created, the
     * regexp is parsed and then inserted into 'mRegexpTextField'.
     * @param llmCtrl
     */
    public LLMView(JFrame owner, boolean isNew, String pasteAsRegExp, LLMController llmCtrl) {
        // Modal, default layout is BorderLayout.
        super(owner, isNew ? TITLE_NEW_STR : TITLE_EDIT_STR, true);
        assert owner != null : "LLMView(): no owning frame";
        assert llmCtrl != null : "LLMView(): no controller";

        mOwner = owner;
        mLLMCtrl = llmCtrl;

        createGUI();
        setGUIAttributes();
        bindToGUI(isNew, pasteAsRegExp);
    }

    public void die() {
        LogLineMatcherManager.removeLLMEditListener(this);
        mPasteRegExp.removeListener();
        setVisible(false);
        dispose();
    }

    private JPanel createGUINorth() {
        JPanel durationPanel = new JPanel(new GridBagLayout());
        UIUtils.addMarginBorder(durationPanel, DATA_COLL_DURATION_CAPTION_STR,
                                COMP_MARGIN, COMP_MARGIN, COMP_MARGIN, COMP_MARGIN);

        GridBagConstraints gbCons = new GridBagConstraints();
        Insets insetsRow0 = new Insets(DEF_INSETS, DEF_INSETS, DEF_INSETS, 0);  // top, left, bottom, right
        gbCons.insets = insetsRow0;

        gbCons.anchor = GridBagConstraints.FIRST_LINE_START;
        gbCons.weighty = 1;
        gbCons.gridheight = 1;
        gbCons.weightx = 0.25;

        gbCons.gridx = 0;
        gbCons.gridy = 0;
        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                Object rb = changeEvent.getSource();
                if (rb == mTimeDurationRB && mTimeDurationRB.isSelected()) {
                    mTimeDurationField.setEnabled(true);
                    mCountDurationField.setEnabled(false);
                } else if (rb == mCountDurationRB && mCountDurationRB.isSelected()) {
                    mCountDurationField.setEnabled(true);
                    mTimeDurationField.setEnabled(false);
                }
            }
        };

        mTimeDurationRB = new JRadioButton(DATA_COLL_DURATION_MINUTES_STR);
        mTimeDurationRB.addChangeListener(changeListener);
        durationPanel.add(mTimeDurationRB, gbCons);

        gbCons.gridy = 1;
        Insets insetsRow1 = new Insets(0, DEF_INSETS, DEF_INSETS, 0);
        gbCons.insets = insetsRow1;
        mCountDurationRB = new JRadioButton(DATA_COLL_DURATION_COUNT_STR);
        mCountDurationRB.addChangeListener(changeListener);
        durationPanel.add(mCountDurationRB, gbCons);

        ButtonGroup durationGroup = new ButtonGroup();
        durationGroup.add(mTimeDurationRB);
        durationGroup.add(mCountDurationRB);

        gbCons.weightx = 3.0;
        gbCons.gridx = 1;

        gbCons.gridy = 0;
        gbCons.insets = insetsRow0;
        mTimeDurationField = new JTextField("30");
        Font defFont = UIManager.getDefaults().getFont("TextField.font");
        FontMetrics fm = getFontMetrics(defFont);
        Dimension dimen = new Dimension(fm.charWidth('0') * 6, fm.getHeight() + fm.getDescent());
        mTimeDurationField.setPreferredSize(dimen);
        durationPanel.add(mTimeDurationField, gbCons);

        gbCons.gridy = 1;
        gbCons.insets = insetsRow1;
        mCountDurationField = new JTextField("1000");
        mCountDurationField.setPreferredSize(dimen);
        durationPanel.add(mCountDurationField, gbCons);

        return durationPanel;
    }

    private JPanel createGUIWest() {
        JPanel westPanel = new JPanel();
        BoxLayout westLayout = new BoxLayout(westPanel, BoxLayout.PAGE_AXIS);
        westPanel.setLayout(westLayout);

        //westPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        UIUtils.addMarginBorder(westPanel, LLMS_CAPTION_STR, 0, COMP_MARGIN, COMP_MARGIN, COMP_MARGIN);

        // Buttons
        JPanel panelLLMButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(panelLLMButtons, mAddAction);
        addButton(panelLLMButtons, mDeleteAction);
        addButton(panelLLMButtons, mMoveUpAction);
        addButton(panelLLMButtons, mMoveDownAction);

        westPanel.add(panelLLMButtons);

        // LLM listbox
        // To make a JList modifiable we must use DefaultListModel.
        mLLMListModel = new DefaultListModel<LogLineMatcher>();
        mLLMList = new JList<LogLineMatcher>(mLLMListModel);
        mLLMList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                if (mLBValueChangedEnabled && !evt.getValueIsAdjusting()) {
                    updateLLM();
                    showLLM(mLLMList.getSelectedValue());
                }
            }
        });
        mLLMList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mLLMList.setVisibleRowCount(10);
        JScrollPane listScrollPane = new JScrollPane(mLLMList);
        listScrollPane.setPreferredSize(new Dimension(240, 1000));

        westPanel.add(listScrollPane);

        return westPanel;
    }

    private JPanel createGUICenter() {
        mCenterPanel = new JPanel(new GridBagLayout());
        UIUtils.addMarginBorder(mCenterPanel, LLM_CAPTION_STR, 0, 0, COMP_MARGIN, COMP_MARGIN);

        GridBagConstraints cons = new GridBagConstraints();
        cons.insets = new Insets(DEF_INSETS, DEF_INSETS, 0, DEF_INSETS);

        //// Left column
        cons.anchor = GridBagConstraints.FIRST_LINE_START;
        cons.weighty = 1;
        cons.gridheight = 1;
        cons.weightx = 0.25;
        cons.gridx = 0;
        cons.gridy = 0;

        JLabel label = new JLabel(LLM_NAME_LABEL_STR);
        mCenterPanel.add(label, cons);

        label = new JLabel(LLM_REGEXP_LABEL_STR);
        ++cons.gridy;
        mCenterPanel.add(label, cons);

        label = new JLabel(LLM_GROUPS_LABEL_STR);
        ++cons.gridy;
        mCenterPanel.add(label, cons);

        label = new JLabel(LLM_LOGSOURCE_LABEL_STR);
        ++cons.gridy;
        mCenterPanel.add(label, cons);

        label = new JLabel(LLM_PRESENTATIONID_LABEL_STR);
        ++cons.gridy;
        mCenterPanel.add(label, cons);

        label = new JLabel(LLM_TRIGGER_LABEL_STR);
        ++cons.gridy;
        mCenterPanel.add(label, cons);

        label = new JLabel("");
        ++cons.gridy;
        mCenterPanel.add(label, cons);


        //// Center column
        cons.fill = GridBagConstraints.HORIZONTAL;

        cons.weightx = 2;
        cons.gridx = 1;
        cons.gridy = 0;

        mNameTextField = new JTextField();
        mCenterPanel.add(mNameTextField, cons);

        // Regular expression
        ++cons.gridy;
        mRegexpTextField = new JTextField();
        mCenterPanel.add(mRegexpTextField, cons);

        // Group listbox
        mGroupListModel = new DefaultListModel<LogLineMatcher.Group>();
        mGroupList = new JList<LogLineMatcher.Group>(mGroupListModel);
        mGroupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mGroupList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                updateGUIControlsGroupsDependent();
            }
        });
        mGroupList.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    mGroupEditAction.actionPerformed(null);
                }
            }
        });
        final int rowCount = 6;
        mGroupList.setVisibleRowCount(rowCount);
        JScrollPane scrollPane = new JScrollPane(mGroupList);
        GridBagConstraints consGroups = new GridBagConstraints();
        consGroups.insets = new Insets(DEF_INSETS, DEF_INSETS, 0, DEF_INSETS);
        consGroups.gridy = 2;
        consGroups.weighty = rowCount;
        consGroups.fill = GridBagConstraints.BOTH;
        mCenterPanel.add(scrollPane, consGroups);

        // Log source combobox
        DefaultComboBoxModel<String> comboBoxModel =
                new DefaultComboBoxModel<String>(LogSource.getSourceNames());
        // Remove "File":
        comboBoxModel.removeElementAt(comboBoxModel.getSize() - 1);
        mLogSourceCombo = new JComboBox<String>(comboBoxModel);
        cons.gridy = 3;
        mCenterPanel.add(mLogSourceCombo, cons);

        // PresentationId combo
        comboBoxModel = new DefaultComboBoxModel<String>(new String[] {"Chart 0", "Chart 1", "Chart 2", "Chart 3", "Chart 5"});
        mPresentationIdCombo = new JComboBox<String>(comboBoxModel);
        ++cons.gridy;
        mCenterPanel.add(mPresentationIdCombo, cons);

        // Trigger type combo
        LogSourceTriggerList.Type[] triggerTypes = LogSourceTriggerList.Type.values();
        int typesLength = triggerTypes.length;
        String[] triggerStrings = new String[typesLength];
        for (int i = 0; i < typesLength; ++i) {
            triggerStrings[i] = triggerTypes[i].name();
        }
        comboBoxModel = new DefaultComboBoxModel<String>(triggerStrings);
        mTriggerCombo = new JComboBox<String>(comboBoxModel);
        ++cons.gridy;
        mCenterPanel.add(mTriggerCombo, cons);

        // Options
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints consOptions = new GridBagConstraints();
        consOptions.anchor = GridBagConstraints.FIRST_LINE_START;
        consOptions.weighty = 1;
        consOptions.gridheight = 1;
        consOptions.weightx = 0.5;
        consOptions.gridx = 0;
        consOptions.gridy = 0;
        mEventCB = new JCheckBox(CB_EVENT_STR);
        optionsPanel.add(mEventCB, consOptions);

        consOptions.gridx = 1;
        mEnabledCB = new JCheckBox(CB_ENABLED_STR);
        optionsPanel.add(mEnabledCB, consOptions);

        consOptions.gridx = 0;
        consOptions.gridy = 1;
        mTimeDiffCB = new JCheckBox(CB_CREATE_TIME_DIFF_SERIES_STR);
        optionsPanel.add(mTimeDiffCB, consOptions);

        ++cons.gridy;
        cons.weighty = 2;
        mCenterPanel.add(optionsPanel, cons);
        cons.weighty = 1;

        // Add help text.
        ++cons.gridy;
        // Use hefty y-weight to push all other controls upwards:
        cons.weighty = 100;
        JTextArea helpText = UIUtils.createInfoTextArea(LLM_HELP_STR);
        mCenterPanel.add(helpText, cons);


        //// Right column
        cons.gridx = 2;
        cons.weightx = 0.25;
        cons.weighty = 1;

        cons.gridy = 1;
        mPasteRegExp = new MenuButton(PASTE_STR, PASTE_TOOLTIP_STR, false);
        mPasteRegExp.setIcon(UIUtils.createIcon("paste-16"));
        //mPasteAsRegExp.setMnemonic(KeyEvent.VK_P);  // does currently not work in MenuButton
        mPasteRegExp.setListener(this);
        for (int id = CMD_PASTE_AS_REGEXP; id <= CMD_PASTE_HEX_GROUP; ++id) {
            mPasteRegExp.add(id, sCommands[id], sCommandsTooltip[id]);
        }
        mCenterPanel.add(mPasteRegExp, cons);

        // Group buttons
        JPanel groupsButtonsPanel = new JPanel();
        groupsButtonsPanel.setLayout(new BoxLayout(groupsButtonsPanel, BoxLayout.PAGE_AXIS));

        addGroupButton(groupsButtonsPanel, mGroupAddAction);
        addGroupButton(groupsButtonsPanel, mGroupEditAction);
        addGroupButton(groupsButtonsPanel, mGroupDeleteAction);
        addGroupButton(groupsButtonsPanel, mGroupMoveUpAction);
        addGroupButton(groupsButtonsPanel, mGroupMoveDownAction);

        // This line fixes the height of each button:
        groupsButtonsPanel.add(Box.createGlue());

        ++cons.gridy;
        mCenterPanel.add(groupsButtonsPanel, cons);

        return mCenterPanel;
    }

    private JPanel createGUISouth() {
        JPanel panelButtons = new JPanel();
        addButton(panelButtons, mCancelAction);
        addButton(panelButtons, mSaveAction);
        return panelButtons;
    }

    private void createGUI() {
        Container contentPane = getContentPane();
        contentPane.add(createGUINorth(), BorderLayout.NORTH);
        contentPane.add(createGUIWest(), BorderLayout.WEST);
        contentPane.add(createGUICenter(), BorderLayout.CENTER);
        contentPane.add(createGUISouth(), BorderLayout.SOUTH);
    }

    private void addGroupButton(JPanel panel, Action action) {
        JButton btn = new JButton(action);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        final int btnHeight = 20;  // This is not the real height
        btn.setMaximumSize(new Dimension(100, btnHeight));
        panel.add(btn);
    }

    private void addButton(JPanel panel, Action action) {
        JButton btn = new JButton(action);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(btn, BorderLayout.LINE_START);
    }

    private void setGUIAttributes() {
        UIUtils.closeFrameWhenEscapePressed(rootPane, this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(1000, 700));
        setSize(new Dimension(1200, 700));
        setLocationRelativeTo(mOwner);
    }

    private void gotoNameField() {
        mNameTextField.requestFocus();
        mNameTextField.selectAll();
    }

    private void bindToGUI(boolean isNew, String pasteAsRegExp) {
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                if (mLLMListModel.getSize() > 0) {
                    gotoNameField();
                }
            }
        });

        LogLineMatcherManager.addLLMEditListener(this);
        LogLineMatcherManager llmMgr = mLLMCtrl.getLLMMgr();
        if (llmMgr.useTimeDuration_Edit()) {
            mTimeDurationRB.setSelected(true);
        } else {
            mCountDurationRB.setSelected(true);
        }
        mTimeDurationField.setText(llmMgr.getTimeDuration_Edit());
        mCountDurationField.setText(llmMgr.getCountDuration_Edit());

        if (isNew) {
            // This will fire onAdded():
            doAdd();
            if (pasteAsRegExp != null) {
                String regexp = LogSource.parseToRegExp(pasteAsRegExp);
                mRegexpTextField.setText(regexp);
            }
        } else {
            boolean gotLLMs = populateLLMList();
            if (pasteAsRegExp != null) {
                doAdd();
                String regexp = LogSource.parseToRegExp(pasteAsRegExp);
                mRegexpTextField.setText(regexp);
            } else if (gotLLMs) {
                // This fires event that calls showLLM() which updates mLLMIndex.
                mLLMList.setSelectedIndex(0);
            }
        }
        updateGUIControls();
    }

    private void updateGUIControls() {
        updateGUIControlsLLMDependent();
        updateGUIControlsGroupsDependent();
    }

    private void updateGUIControlsLLMDependent() {
        int selIndex = mLLMList.getSelectedIndex();
        int count = mLLMListModel.getSize();
        mCenterPanel.setVisible(selIndex >= 0);
        mDeleteAction.setEnabled(selIndex >= 0);
        mMoveUpAction.setEnabled(selIndex > 0 && count > 1);
        mMoveDownAction.setEnabled(selIndex >= 0 && selIndex < count - 1);
    }

    private void updateGUIControlsGroupsDependent() {
        int selIndex = mGroupList.getSelectedIndex();
        int count = mGroupListModel.getSize();
        mGroupEditAction.setEnabled(selIndex >= 0);
        mGroupDeleteAction.setEnabled(selIndex >= 0);
        mGroupMoveUpAction.setEnabled(selIndex > 0 && count > 1);
        mGroupMoveDownAction.setEnabled(selIndex >= 0 && selIndex < count - 1);
    }

    /**
     * Populate the group list with groups from the current
     * LLM. Should only be called when mSelectedLLM != null.
     */
    private void populateGroupList() {
        mGroupListModel.removeAllElements();
        LogLineMatcher.Groups groups = mSelectedLLM.getGroups_Edit();
        if (groups != null) {
            for (LogLineMatcher.Group group : groups) {
                mGroupListModel.addElement(group);
            }
        }
    }

    /**
     * Populate the list of LogLineMatchers.
     *
     * @return true if any LogLineMatchers have been added.
     */
    private boolean populateLLMList() {
        mLLMListModel.removeAllElements();
        ArrayList<LogLineMatcher> llms = mLLMCtrl.getLLMs();
        if (llms != null) {
            for (LogLineMatcher llm : llms) {
                mLLMListModel.addElement(llm);
            }
            return llms.size() > 0;
        }
        return false;
    }

    private LogLineMatcher.Groups getGroupsFromGroupList() {
        final int groupCount = mGroupListModel.getSize();
        LogLineMatcher.Groups groups = new LogLineMatcher.Groups(groupCount);
        for (int index = 0; index < groupCount; ++index) {
            groups.add(mGroupListModel.get(index));
        }
        return groups;
    }

    /**
     * Retrieve data from all GUI elements and update the edit members
     * of the current LogLineMatcher.
     */
    private boolean updateLLM() {
        boolean modified = false;
        if (mSelectedLLM != null) {
            modified =
                mLLMCtrl.editUpdateLLM(mSelectedLLM,
                                       mNameTextField.getText(),
                                       mEventCB.isSelected(),
                                       mEnabledCB.isSelected(),
                                       mTimeDiffCB.isSelected(),
                                       (String) mLogSourceCombo.getSelectedItem(),
                                       mRegexpTextField.getText(),
                                       getGroupsFromGroupList(),
                                       mPresentationIdCombo.getSelectedIndex(),
                                       (String) mTriggerCombo.getSelectedItem());
            if (modified) {
                mLLMListModel.set(mLLMIndex, mSelectedLLM);
            }
        }
        return modified;
    }

    /**
     * Update all GUI elements with data from the given
     * LogLineMatcher.
     *
     * @param llm
     */
    private void showLLM(LogLineMatcher llm) {
        mSelectedLLM = llm;
        mLLMIndex = mLLMList.getSelectedIndex();
        boolean hasSelection = mSelectedLLM != null;
        if (hasSelection) {
            mNameTextField.setText(llm.getName_Edit());
            mEventCB.setSelected(llm.getEvent_Edit());
            mEnabledCB.setSelected(llm.getEnabled_Edit());
            mTimeDiffCB.setSelected(llm.getTimeDiff_Edit());
            mRegexpTextField.setText(llm.getRegExp_Edit());
            populateGroupList();

            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) mLogSourceCombo.getModel();
            int comboRow = model.getIndexOf(llm.getSourceName_Edit());
            if (comboRow >= 0) {
                mLogSourceCombo.setSelectedIndex(comboRow);
            }

            mPresentationIdCombo.setSelectedIndex(llm.getPresentationId_Edit());
            mTriggerCombo.setSelectedItem(llm.getTriggerTypeAsString_Edit());

            gotoNameField();
        }
        updateGUIControls();
    }

    private void showDlgInvalidLLM(LogLineMatcher.InvalidException excep) {
        JOptionPane.showMessageDialog(this, excep.getMessage(),
                                      LLM_INVALID_LMM_STR, JOptionPane.OK_OPTION);
    }

    private class SaveAction extends UIUtils.ActionBase {
        public SaveAction() {
            super(sCommands[CMD_SAVE], sCommandsTooltip[CMD_SAVE], "save-16", KeyEvent.VK_S);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                mLLMCtrl.editUpdateLLMMgr(mTimeDurationRB.isSelected(), mTimeDurationField.getText(),
                                          mCountDurationRB.isSelected(), mCountDurationField.getText());
                updateLLM();

                if (!mLLMCtrl.hasFilePath()) {
                    File xmlPath = UIUtils.showFileDlg(LLMView.this, true, false,
                                                       UIUtils.FILEDLG_FILTER.FILTER_LLM,
                                                       Directory.LOGDOG_FILES);
                    if (xmlPath == null) {
                        return;
                    }
                    mLLMCtrl.setFilePath(xmlPath);
                }

                // Throws if no path or other problemos (illegal regexp
                // for instance).
                mLLMCtrl.editSave();
                mLLMCtrl.editDone();  // calls die()
            } catch (LogLineMatcher.InvalidException excep) {
                showDlgInvalidLLM(excep);
            } catch (LogLineMatcherManager.NoPathException excep) {
                JOptionPane.showMessageDialog(LLMView.this, excep.getMessage(),
                                              MISSING_PATH_STR,
                                              JOptionPane.OK_OPTION);
            } catch (RuntimeException excep) {
                JOptionPane.showMessageDialog(LLMView.this, excep.getMessage(),
                                              UNHANDLED_EXCEP_STR,
                                              JOptionPane.OK_OPTION);
                excep.printStackTrace();
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private void doCancel() {
        mLLMCtrl.editCancel();
        mLLMCtrl.editDone();
    }

    private class CancelAction extends UIUtils.ActionBase {
        public CancelAction() {
            super(sCommands[CMD_CANCEL], sCommandsTooltip[CMD_CANCEL], "cancel-16", KeyEvent.VK_C);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doCancel();
        }
    }

    private void doAdd() {
        try {
            mLLMCtrl.editAdd();   // onAdded() will be called
        } catch (LogLineMatcher.InvalidException excep) {
            showDlgInvalidLLM(excep);
        }
    }

    private class AddAction extends UIUtils.ActionBase {
        public AddAction() {
            super(sCommands[CMD_ADD], sCommandsTooltip[CMD_ADD], null, KeyEvent.VK_A);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doAdd();
        }
    }

    private class DeleteAction extends UIUtils.ActionBase {
        public DeleteAction() {
            super(sCommands[CMD_DELETE], sCommandsTooltip[CMD_DELETE], null, KeyEvent.VK_D);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            LogLineMatcher llm = mLLMList.getSelectedValue();
            if (llm != null &&
                JOptionPane.
                showConfirmDialog(getContentPane(), LLM_DELETE_Q_STR,
                                  DELETE_TITLE_STR,
                                  JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                mLLMCtrl.editDelete(llm);  // onDeleted() will be called
            }
        }
    }

    private class MoveUpAction extends UIUtils.ActionBase {
        public MoveUpAction() {
            super(sCommands[CMD_MOVE_UP], sCommandsTooltip[CMD_MOVE_UP], null, KeyEvent.VK_U);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int indexCrnt = mLLMList.getSelectedIndex();
            if (indexCrnt >= 1) {
                int indexAbove = indexCrnt - 1;
                mLLMCtrl.moveLLM(indexCrnt, indexAbove);  // onMoved() will be called
            }
        }
    }

    private class MoveDownAction extends UIUtils.ActionBase {
        public MoveDownAction() {
            super(sCommands[CMD_MOVE_DOWN], sCommandsTooltip[CMD_MOVE_DOWN], null, KeyEvent.VK_W);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int indexCrnt = mLLMList.getSelectedIndex();
            if (indexCrnt >= 0 && indexCrnt < mLLMListModel.getSize() - 1) {
                int indexBelow = indexCrnt + 1;
                mLLMCtrl.moveLLM(indexCrnt, indexBelow);  // onMoved() will be called
            }
        }
    }

    private class GroupAddAction extends UIUtils.ActionBase {
        public GroupAddAction() {
            super(sCommands[CMD_GROUP_ADD], sCommandsTooltip[CMD_GROUP_ADD], null, 0);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (mSelectedLLM != null) {
                Group group = new Group("New group");
                GroupView groupView = new GroupView(LLMView.this, group);
                if (groupView.saved()) {
                    mLLMCtrl.editAddGroup(mSelectedLLM, group);  // onGroupAdded() will be called
                }
            }
        }
    }

    private class GroupEditAction extends UIUtils.ActionBase {
        public GroupEditAction() {
            super(sCommands[CMD_GROUP_EDIT], sCommandsTooltip[CMD_GROUP_EDIT], null, 0);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Group group = mGroupList.getSelectedValue();
            if (group != null) {
                GroupView groupView = new GroupView(LLMView.this, group);
                if (groupView.saved()) {
                    mGroupList.repaint();
                }
            }
        }
    }

    private class GroupDeleteAction extends UIUtils.ActionBase {
        public GroupDeleteAction() {
            super(sCommands[CMD_GROUP_DELETE], sCommandsTooltip[CMD_GROUP_DELETE], null, 0);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int selIndex = mGroupList.getSelectedIndex();
            if (selIndex >= 0 && mSelectedLLM != null &&
                JOptionPane.showConfirmDialog(getContentPane(), GROUP_DELETE_Q_STR,
                                              DELETE_TITLE_STR,
                                              JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                mLLMCtrl.editDeleteGroup(mSelectedLLM, selIndex);   // onGroupDeleted() will be called
            }
        }
    }

    private class GroupMoveUpAction extends UIUtils.ActionBase {
        public GroupMoveUpAction() {
            super(sCommands[CMD_GROUP_MOVE_UP], sCommandsTooltip[CMD_GROUP_MOVE_UP], null,
                  KeyEvent.VK_UNDEFINED);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int indexCrnt = mGroupList.getSelectedIndex();
            if (indexCrnt >= 1 && mSelectedLLM != null) {
                int indexAbove = indexCrnt - 1;
                // onGroupMoved() will be called:
                mLLMCtrl.editMoveGroup(mSelectedLLM, indexCrnt, indexAbove);
                updateGUIControlsGroupsDependent();
            }
        }
    }

    private class GroupMoveDownAction extends UIUtils.ActionBase {
        public GroupMoveDownAction() {
            super(sCommands[CMD_GROUP_MOVE_DOWN], sCommandsTooltip[CMD_GROUP_MOVE_DOWN], null,
                  KeyEvent.VK_UNDEFINED);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int indexCrnt = mGroupList.getSelectedIndex();
            if (indexCrnt >= 0 && indexCrnt < mGroupListModel.getSize() - 1 && mSelectedLLM != null) {
                int indexBelow = indexCrnt + 1;
                // onGroupMoved() will be called:
                mLLMCtrl.editMoveGroup(mSelectedLLM, indexCrnt, indexBelow);
                updateGUIControlsGroupsDependent();
            }
        }
    }

    public void doPasteAsRegExp() {
        String clpbrdString = UIUtils.getClipboardContent();
        if (!Utils.emptyString(clpbrdString)) {
            String regexp = LogSource.parseToRegExp(clpbrdString);
            // Don't allow everything
            if (!regexp.equals(".*?$")) {
                mRegexpTextField.setText(regexp);
            }
        }
    }

    private void doPasteRegExpGroup(String groupRegExp) {
        String crntRegExp = mRegexpTextField.getText();
        Caret caret = mRegexpTextField.getCaret();
        int mark = caret.getMark();
        int dot = caret.getDot();
        if (mark > dot) {
            int tmp = mark;
            mark = dot;
            dot = tmp;
        }
        String before = crntRegExp.substring(0, mark);
        String after = crntRegExp.substring(dot);
        String newRegExp = String.format("%s%s%s", before, groupRegExp, after);
        mRegexpTextField.setText(newRegExp);
    }

    @Override
    public void actionPerformed(ActionEvent actEvent) {
        String cmd = actEvent.getActionCommand();
        if (cmd.equals(UIUtils.ESCAPE_ACTION_NAME)) {
            doCancel();
        }
    }


    // LLMEditListener

    @Override
    public void onEditCommitBegin(LogLineMatcherManager llmMgr) {
    }

    @Override
    public void onAdded(LogLineMatcher llm) {
        mLLMListModel.addElement(llm);
        // Fires an event that calls showLLM() which updates mLLMIndex.
        int selIndex = mLLMListModel.getSize() - 1;
        mLLMList.setSelectedIndex(selIndex);
    }

    @Override
    public void onDeleted(LogLineMatcher llm) {
        // Prevent trigger of updateLLM() and showLLM():
        mLBValueChangedEnabled = false;
        int llmIndex = mLLMList.getSelectedIndex();

        // For some reason this line crashes when removing the last
        // element in the list (all other elements can be removed).
        // Therefore must use index based removal instead.
        // if (mLLMListModel.removeElement(llm)) {
        if (mLLMListModel.remove(llmIndex) != null) {
            if (llmIndex < mLLMListModel.getSize()) {
                mLLMList.setSelectedIndex(llmIndex);
                showLLM(mLLMList.getSelectedValue());
            } else if (--llmIndex >= 0) {
                mLLMList.setSelectedIndex(llmIndex);
                showLLM(mLLMList.getSelectedValue());
            } else {
                mSelectedLLM = null;
                mLLMIndex = -1;
                updateGUIControls();
            }
        }
        mLBValueChangedEnabled = true;
    }

    private void setSelectedLLMWithoutEvent(int index) {
        // Prevent trigger of showLLM():
        mLBValueChangedEnabled = false;
        mLLMIndex = index;
        mLLMList.setSelectedIndex(index);
        mLBValueChangedEnabled = true;
        updateGUIControls();
    }

    @Override
    public void onMoved(int fromIndex, int toIndex) {
        if (toIndex > fromIndex) {         // down
            if (fromIndex >= 0 && fromIndex < mLLMListModel.getSize() - 1) {
                LogLineMatcher llmBelow = mLLMListModel.get(toIndex);
                LogLineMatcher llmCrnt = mLLMListModel.get(fromIndex);
                mLLMListModel.set(toIndex, llmCrnt);
                mLLMListModel.set(fromIndex, llmBelow);
                setSelectedLLMWithoutEvent(toIndex);
            }
        } else if (toIndex < fromIndex) {  // up
            if (fromIndex >= 1) {
                LogLineMatcher llmAbove = mLLMListModel.get(toIndex);
                LogLineMatcher llmCrnt = mLLMListModel.get(fromIndex);
                mLLMListModel.set(toIndex, llmCrnt);
                mLLMListModel.set(fromIndex, llmAbove);
                setSelectedLLMWithoutEvent(toIndex);
            }
        }
    }

    @Override
    public void onGroupAdded(LogLineMatcher llm, Group group) {
        if (llm != mSelectedLLM) {
            return;
        }
        // Add and select item last in list:
        int index = mGroupListModel.getSize();
        mGroupListModel.add(index, group);
        mGroupList.setSelectedIndex(index);
    }

    @Override
    public void onGroupDeleted(LogLineMatcher llm, int groupIndex) {
        if (llm != mSelectedLLM) {
            return;
        }
        if (mGroupListModel.remove(groupIndex) != null) {
            // Select closest item in list:
            if (mGroupListModel.getSize() > 0) {
                if (groupIndex == mGroupListModel.getSize()) {
                    --groupIndex;
                }
                mGroupList.setSelectedIndex(groupIndex);
            }
            updateGUIControlsGroupsDependent();
        }
    }

    @Override
    public void onGroupMoved(LogLineMatcher llm, int fromIndex, int toIndex) {
        if (llm != mSelectedLLM) {
            return;
        }
        if (toIndex > fromIndex) {         // down
            if (fromIndex >= 0 && fromIndex < mGroupListModel.getSize() - 1) {
                Group groupBelow = mGroupListModel.get(toIndex);
                Group groupCrnt = mGroupListModel.get(fromIndex);
                mGroupListModel.set(toIndex, groupCrnt);
                mGroupListModel.set(fromIndex, groupBelow);
                mGroupList.setSelectedIndex(toIndex);
            }
        } else if (toIndex < fromIndex) {  // up
            if (fromIndex >= 1) {
                Group groupAbove = mGroupListModel.get(toIndex);
                Group groupCrnt = mGroupListModel.get(fromIndex);
                mGroupListModel.set(toIndex, groupCrnt);
                mGroupListModel.set(fromIndex, groupAbove);
                mGroupList.setSelectedIndex(toIndex);
            }
        }
    }


    // MenuButtonListener

    @Override
    public void onPopup() {
    }

    @Override
    public void onSelected(int id) {
        switch (id) {
        case CMD_PASTE_AS_REGEXP:
            doPasteAsRegExp();
            break;
        case CMD_PASTE_WHATEVER:
            doPasteRegExpGroup(".*");
            break;
        case CMD_PASTE_INT_GROUP:
            doPasteRegExpGroup("(-?[0-9]+)");
            break;
        case CMD_PASTE_FLOAT_GROUP:
            doPasteRegExpGroup("(-?[0-9]+\\.[0-9]+)");
            break;
        case CMD_PASTE_HEX_GROUP:
            doPasteRegExpGroup("(0[xX][0-9a-fA-F]+)");
            break;
        }
    }
}
