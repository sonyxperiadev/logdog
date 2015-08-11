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

import org.jfree.data.Range;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import logdog.Prefs;
import logdog.Prefs.Directory;
import logdog.logdog;
import logdog.controller.MainController;
import logdog.controller.ViewSelListener;
import logdog.model.DeviceStater.KERNEL_LOG;
import logdog.model.CpuGovernor;
import logdog.model.FileLogSource;
import logdog.model.LLMMgrListener;
import logdog.model.LogLineMatcher;
import logdog.model.LogLineMatcherManager;
import logdog.model.LogSource;
import logdog.model.DeviceStater;
import logdog.model.DeviceStater.DEVICE_STATE;
import logdog.model.DeviceStater.DeviceListener;
import logdog.model.LogSourceFeedListener;
import logdog.utils.Logger;
import logdog.utils.Utils;

/**
 * This class is responsible for showing the chart with the various
 * data collected from the LogLineMatchers.
 */
public class ChartView
    extends JFrame
    implements
    ActionListener,
    MenuListener,
    LLMMgrListener,
    DeviceListener,
    ViewSelListener,
    SyncedChartPanel.ChartPanelListener,
    SettingsView.SettingsChangedListener,
    LogSourceFeedListener {

    /*------ Static class members and methods ------*/

    private static final long serialVersionUID = 1L;

    // String resources.
    private static final String FILE_MENU_STR = "File";
    private static final String CHART_MENU_STR  = "Chart";
    private static final String LOGSOURCE_MENU_STR = "Log sources";
    private static final String GOVERNOR_MENU_STR = "Cpu governor";
    private static final String ABOUT_STR = "About";

    private static final String OPEN_ACTION_STR = "Open...";
    private static final String OPEN_ACTION_TOOLTIP_STR = "Open set of log line matchers in logdog file";
    private static final String NEW_ACTION_STR = "New...";
    private static final String NEW_ACTION_TOOLTIP_STR = "Create new set of log line matchers";
    private static final String EDIT_ACTION_STR = "Edit...";
    private static final String EDIT_ACTION_TOOLTIP_STR = "Edit current log line matchers";
    private static final String EXIT_ACTION_STR = "Exit";
    private static final String EXIT_CONFIRM_STR = "Are you sure you want to exit logdog?";
    private static final String EXIT_CONFIRM_TITLE_STR = "Exit Application";

    private static final String SERIES_SHOWALL_ACTION_STR = "Show all graphs";
    private static final String SERIES_HIDEALL_ACTION_STR = "Hide all graphs";

    private static final String KERNEL_LOG_ACTION_STR = "Kernel log";
    private static final String KERNEL_LOG_ACTION_TOOLTIP_STR =
        "Emit the kernel log in logcat. This is a persistent setting on the device " +
        "i.e. it will survive reboots.";
    private static final String LS_FROM_FILE_ACTION_STR = "Open file as log source...";
    private static final String LS_FROM_FILE_ACTION_TOOLTIP_STR = "Select logdog file to use as log source";
    private static final String LOGSOURCE_ACTION_TOOLTIP_STR =
        "Start log source and/or display log source view";
    //private static final String SERIES_INSERT_FAILED_STR =
    //    "ChartView: unable to insert data into series, millis=%d value=%f";
    private static final String DEVICE_CONNECTED_STR = "Device connected  ";
    private static final String DEVICE_NOT_CONNECTED_STR = "Please connect a device  ";
    private static final String SAVING_TO_FILE_STR = "Saving log lines to file";
    private static final String LIVE_LOGSOURCE_STR = "Live log sources  ";
    private static final String PAUSE_STR = "Pause";
    private static final String PLAY_STR = "Resume";
    private static final String PAUSE_ALL_LOGSOURCES_TOOLTIP_STR =
        "Pause or resume all log sources. This also prevents lines from being saved " +
        "to file (if active) or added to the log source views.";
    private static final String SAVE_TO_FILE_STR = "Save to file";
    private static final String SAVE_TO_FILE_TOOLTIP_STR = "Start/stop saving log lines to file";
    private static final String SETTINGS_STR = "Settings...";
    private static final String SETTINGS_TOOLTIP_STR = "Persistant program settings";

    private static final String REBOOT_DEVICE_TITLE_STR = "Confirm reboot";
    private static final String REBOOT_DEVICE_Q_STR = "Reboot device?";
    private static final String REBOOT_DEVICE_STR = "Reboot device";
    private static final String REBOOT_DEVICE_TOOLTIP_STR = "Reboot device after confirmation";
    private static final String CLEAR_STR = "Clear";
    private static final String CLEAR_TOOLTIP_STR = "Clear all charts";

    private static final String HELP_STARTUP = "<html><p align=\"center\">Select File/Open or File/New from the menu <br>" +
        "to create log line matchers (LLM) needed to display graphs.<br><br>" +
        "You can also double-click on log lines in the log view to create an LLM.</p></html>";

    private DEVICE_STATE mDeviceState = DEVICE_STATE.UNKNOWN;
    private KERNEL_LOG mKernelLog = KERNEL_LOG.UNKNOWN;

    // File menu
    @SuppressWarnings("serial")
    private class FileMenu extends JMenu {
        public FileMenu() {
            super(FILE_MENU_STR);
            setMnemonic(KeyEvent.VK_F);
        }
    }

    // Series menu
    @SuppressWarnings("serial")
    private class ChartMenu extends JMenu {
        public ChartMenu() {
            super(CHART_MENU_STR );
            setMnemonic(KeyEvent.VK_H);
        }
    }

    // LogSource menu
    @SuppressWarnings("serial")
    private class LogSourceMenu extends JMenu {
        public LogSourceMenu() {
            super(LOGSOURCE_MENU_STR);
            setMnemonic(KeyEvent.VK_U);
        }
    }

    // CPU governor menu
    @SuppressWarnings("serial")
    private class GovernorMenu extends JMenu {

        private CpuGovernor mCpuGovernor = new CpuGovernor();

        public GovernorMenu() {
            super(GOVERNOR_MENU_STR);
            setMnemonic(KeyEvent.VK_G);
        }

        public void update(boolean deviceAvailable) {
            if (deviceAvailable) {
                populate();
            } else {
                removeAll();
            }
        }

        private void populate() {
            removeAll();
            if (mCpuGovernor.populate() && mCpuGovernor.hasGovernors()) {
                ButtonGroup buttonGroup = new ButtonGroup();
                String crntGovernor = mCpuGovernor.getCrntGovernor();
                String [] governors = mCpuGovernor.getGovernors();
                for (String governor : governors) {
                    boolean selected = governor.equals(crntGovernor);
                    JRadioButtonMenuItem mi = new JRadioButtonMenuItem(governor, selected);
                    mi.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent event) {
                            if (event.getSource() instanceof JRadioButtonMenuItem) {
                                JRadioButtonMenuItem radioButton = (JRadioButtonMenuItem) event.getSource();
                                if (radioButton.isSelected()) {
                                    //TODO Restore radio buttons to old value when failed?
                                    try {
                                        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                        if (!mCpuGovernor.setGovernor(radioButton.getText())) {
                                            JOptionPane.showMessageDialog(getContentPane(), "Changing governor failed.", "Error",
                                                                          JOptionPane.OK_OPTION);
                                        }
                                    } finally {
                                        getContentPane().setCursor(Cursor.getDefaultCursor());
                                    }
                                }
                            }
                        }
                    });
                    buttonGroup.add(mi);
                    add(mi);
                }
            }
        }
    }

    // About menu
    @SuppressWarnings("serial")
    private class AboutMenu extends JMenu {
        public AboutMenu() {
            super(new AboutAction());
            setMnemonic(KeyEvent.VK_O);
            addMenuListener(ChartView.this);
        }
    }

    // Menu instances
    private GovernorMenu mGovernorMenu;
    private AboutMenu mAboutMenu;

    // Actions
    private OpenAction mOpenAction = new OpenAction();
    private NewAction mNewAction = new NewAction();
    private EditAction mEditAction = new EditAction();
    private ExitAction mExitAction = new ExitAction();
    private SeriesShowAllAction mSeriesShowAllAction = new SeriesShowAllAction();
    private SeriesHideAllAction mSeriesHideAllAction = new SeriesHideAllAction();
    private KernelLogAction mKernelLogAction = new KernelLogAction();
    private LSFromFileAction mLSFromFileAction = new LSFromFileAction();
    private LogSourceAction[] mLogSourceActions = new LogSourceAction[LogSource.COUNT - 1];  // Skip "File"
    private RebootAction mRebootAction = new RebootAction();
    private ClearAllChartsAction mClearAllChartsAction = new ClearAllChartsAction();
    private ClearChartAction mClearChartAction = new ClearChartAction();
    private PausePlayAction mPausePlayAction = new PausePlayAction();
    private SaveToFileAction mSaveToFileAction = new SaveToFileAction();
    private SettingsAction mSettingsAction = new SettingsAction();

    // Counter for plot points having the same timestamp as another
    // point. This information is shown in the status bar since it
    // means plot data has been lost (the chart can only have one
    // point at each time).
    //private int mDuplicateCount;

    private File mLSFile;  // if non-null we have a FileLogSource as source
    private Map<LogLineMatcher, TimeSeriesCollection> mLLMSeries =
        new HashMap<LogLineMatcher, TimeSeriesCollection>();

    private JPanel mCenterPanel;
    private JLabel mCenterLabel;
    private static final Font sFontPlainLarge = new Font(Font.SANS_SERIF, Font.PLAIN, 24);

    // Statusbar stuff.
    private JLabel mDeviceStatusIcon;
    private ImageIcon mGreyStatus;
    private ImageIcon mGreenStatus;
    private ImageIcon mRedStatus;
    private JLabel mSaveToFileLabel;
    private JLabel mReadFromFileLabel;
    //private JLabel mDuplicateCountLabel;
    private JLabel mDataPointLabel;
    private JLabel mSeriesStatsLabel;

   //  /** Line style: line */
   //  public static final String STYLE_LINE = "line";
   //  /** Line style: dashed */
   //  public static final String STYLE_DASH = "dash";
   //  /** Line style: dotted */
   //  public static final String STYLE_DOT = "dot";

   // private BasicStroke createStroke(String style) {
   //      BasicStroke result = null;

   //      if (style != null) {
   //          float lineWidth = 0.4f;
   //          float dash[] = {5.0f};
   //          float dot[] = {lineWidth};

   //          if (style.equalsIgnoreCase(STYLE_LINE)) {
   //              result = new BasicStroke(lineWidth);
   //          } else if (style.equalsIgnoreCase(STYLE_DASH)) {
   //              result = new BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
   //          } else if (style.equalsIgnoreCase(STYLE_DOT)) {
   //              result = new BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f, dot, 0.0f);
   //          }
   //      }

   //      return result;
   //  }


    /*------ Object members and methods ------*/

    /* This is our boss, we don't do anything without asking
       her. Well, almost nothing... */
    private MainController mController;

    private Prefs mPrefs;

    private Map<Integer, SyncedChartPanel>
        mChartPanels = new HashMap<Integer, SyncedChartPanel>(5);
    private SyncedChartPanel mLastClickedChartPanel;

    private SyncedChartPanel findChartPanel(LogLineMatcher llm) {
        int presentationId = llm.getPresentationId();
        SyncedChartPanel chartPanel = mChartPanels.get(presentationId);
        if (chartPanel == null) {
            chartPanel = SyncedChartPanel.create(presentationId, mPrefs.getShapesInCharts());
            chartPanel.setChartPanelListener(this);
            mChartPanels.put(presentationId, chartPanel);
            mCenterPanel.add(chartPanel.getOuterPanel());
            mCenterPanel.revalidate();
            //mCenterPanel.repaint();
        }

        return chartPanel;
    }

    public void removeChartPanels() {
        Iterator<Entry<Integer, SyncedChartPanel>> iter = mChartPanels.entrySet().iterator();
        while (iter.hasNext()) {
            iter.next().getValue().setChartPanelListener(null);
            iter.remove();
        }
        mCenterPanel.removeAll();
        mCenterPanel.repaint();
    }

    /**
     * Constructor.
     *
     * @param title - The title of the window/view
     * @param controller
     */
    public ChartView(String title, MainController controller, Prefs prefs) {
        super(title);
        if (logdog.DEBUG) {
            Logger.log("ChartView.ChartView() entering");
        }
        mPrefs = prefs;
        setVisible(false);
        setIconImage(UIUtils.createIcon("logdog-32").getImage());
        ToolTipManager.sharedInstance().setInitialDelay(0);

        mController = controller;

        /**
         * Register for events from the LogLineMatcherManager so we know
         * when LogLineMatcher objects are either created or removed and when
         * they match a log line.
         */
        LogLineMatcherManager.addLLMMgrListener(this);

        UIUtils.closeFrameWhenEscapePressed(this.getRootPane(), this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                mController.addSelListener(ChartView.this);
                LogSource.addFeedListener(ChartView.this);  //TODO Move to Controller?
            }

            @Override
            public void windowClosing(WindowEvent e) {
                doExit((JFrame) e.getSource());
            }
        });

        addWindowStateListener(new WindowStateListener() {

            @Override
            public void windowStateChanged(WindowEvent winEvt) {
                if (logdog.DEBUG) {
                    Logger.log("ChartView.windowStateChanged() " + winEvt.paramString());
                }
            }
        });
        createGUI();
    }

    private void createGUI() {
        Container contentPane = getContentPane();
        contentPane.setVisible(false);

        createMenu();
        createToolbar();
        createEmptyCenterPanel();
        createStatusPanel();
        setFocusable(true);
        requestFocus();

        //TODO get previous size here
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setPreferredSize(new Dimension(1000, 600));
        setMinimumSize(new Dimension(500, 300));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                doOpenFile();
            }
        });

        pack();
        contentPane.setVisible(true);
      }

    private JMenu createFileMenu() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.createFileMenu() entering");
        }
        FileMenu fileMenu = new FileMenu();

        JMenuItem mi = fileMenu.add(mOpenAction);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

        mi = fileMenu.add(mNewAction);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));

        mi = fileMenu.add(mEditAction);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));

        mi = fileMenu.add(mSettingsAction);

        mi = fileMenu.add(mExitAction);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));

        fileMenu.addMenuListener(this);

        return fileMenu;
    }

    private JMenu createChartMenu() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.createChartMenu() entering");
        }
        ChartMenu chartMenu = new ChartMenu();

        chartMenu.add(mClearChartAction);
        chartMenu.add(mSeriesShowAllAction);
        chartMenu.add(mSeriesHideAllAction);

        chartMenu.addMenuListener(this);

        return chartMenu;
    }

    private JMenu createLogSourceMenu() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.createLogSourceMenu() entering");
        }
        LogSourceMenu logSourceMenu = new LogSourceMenu();

        JCheckBoxMenuItem miCB = new JCheckBoxMenuItem(mKernelLogAction);
        logSourceMenu.add(miCB);

        JMenuItem mi = logSourceMenu.add(mLSFromFileAction);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));

        logSourceMenu.addSeparator();

        // Add all log sources.
        String[] lsNames = LogSource.getSourceNames();
        for (int index = 0; index < lsNames.length - 1; ++index) {   // Skip "File"
            mLogSourceActions[index] = new LogSourceAction(lsNames[index]);
            logSourceMenu.add(mLogSourceActions[index]);
        }

        logSourceMenu.addMenuListener(this);

        return logSourceMenu;
    }

    private JMenu createGovernorMenu() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.createGovernorMenu() entering");
        }
        mGovernorMenu = new GovernorMenu();
        mGovernorMenu.populate();

        return mGovernorMenu;
    }

    private JMenu createAboutMenu() {
        mAboutMenu = new AboutMenu();
        return mAboutMenu;
    }

    private void createMenu() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.createMenu() entering");
        }
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createChartMenu());
        menuBar.add(createLogSourceMenu());
        menuBar.add(createGovernorMenu());
        menuBar.add(createAboutMenu());
        setJMenuBar(menuBar);
    }

    private void createEmptyCenterPanel() {
        mCenterPanel = new JPanel(new BorderLayout());

        mCenterLabel = new JLabel(HELP_STARTUP, SwingConstants.CENTER);
        mCenterLabel.setAlignmentX(SwingConstants.CENTER);
        mCenterLabel.setAlignmentY(SwingConstants.CENTER);
        mCenterLabel.setFont(sFontPlainLarge);
        mCenterLabel.setForeground(Color.LIGHT_GRAY);
        mCenterPanel.add(mCenterLabel);
        mCenterLabel.revalidate();

        getContentPane().add(mCenterPanel, BorderLayout.CENTER);
    }

    private void createCenterPanel() {
        getContentPane().remove(mCenterPanel);
        mCenterPanel = new JPanel();
        mCenterPanel.setLayout(new BoxLayout(mCenterPanel, BoxLayout.PAGE_AXIS));
        getContentPane().add(mCenterPanel, BorderLayout.CENTER);
    }

    private void createToolbar() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.createToolbar() entering");
        }
        ToolBar toolbar = new ToolBar("Commands");
        toolbar.addButton(mRebootAction);
        toolbar.addButton(mClearAllChartsAction);
        toolbar.addButton(mPausePlayAction);
        toolbar.addCheckBox(mSaveToFileAction);
        getContentPane().add(toolbar, BorderLayout.NORTH);
    }

    private void createStatusPanel() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.createStatusPanel() entering");
        }
        // The content pane uses BorderLayout by default.
        Container contentPane = getContentPane();

        JPanel statusPanel = new JPanel();
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusPanel.setPreferredSize(new Dimension(contentPane.getWidth(), 20));
        BoxLayout statusPanelLayout = new BoxLayout(statusPanel, BoxLayout.X_AXIS);
        statusPanel.setLayout(statusPanelLayout);

        mDeviceStatusIcon = new JLabel();
        mGreyStatus = new ImageIcon(getClass().getResource("/resources/grey-ball-16.png"));
        mGreenStatus = new ImageIcon(getClass().getResource("/resources/green-ball-16.png"));
        mRedStatus = new ImageIcon(getClass().getResource("/resources/red-ball-16.png"));
        statusPanel.add(mDeviceStatusIcon);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        statusPanel.add(sep);

        mSaveToFileLabel = new JLabel();
        statusPanel.add(mSaveToFileLabel);

        sep = new JSeparator(SwingConstants.VERTICAL);
        statusPanel.add(sep);

        mReadFromFileLabel = new JLabel();
        statusPanel.add(mReadFromFileLabel);

        sep = new JSeparator(SwingConstants.VERTICAL);
        statusPanel.add(sep);

        // mDuplicateCountLabel = new JLabel();
        // statusPanel.add(mDuplicateCountLabel);

        sep = new JSeparator(SwingConstants.VERTICAL);
        statusPanel.add(sep);

        mDataPointLabel = new JLabel();
        statusPanel.add(mDataPointLabel);

        sep = new JSeparator(SwingConstants.VERTICAL);
        statusPanel.add(sep);

        mSeriesStatsLabel = new JLabel();
        statusPanel.add(mSeriesStatsLabel);

        // After adding a JSeparator 'mSaveToFileLabel' will be
        // right-aligned and not left-aligned. Tried a lot of things
        // including different layout managers. This hack solves the
        // problem partly.
        for (int times = 0; times < 50; ++times) {
            statusPanel.add(Box.createHorizontalGlue());
        }

        contentPane.add(statusPanel, BorderLayout.SOUTH);
    }

    private void updateLogSourceMenuActions() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.updateLogSourceMenuActions() entering");
        }
        // Put update of these actions in their own method since they are only on the
        // main menu currently. The actions need not always be updated.
        boolean hasLLMMgr = mController.hasLLMMgr();
        mKernelLogAction.setSelected(mKernelLog == KERNEL_LOG.LOGCAT);
        mLSFromFileAction.setEnabled(hasLLMMgr);
        for (int index = 0; index < mLogSourceActions.length; ++index) {
            mLogSourceActions[index].updateState();
        }
    }

    private void updateActions() {
        if (logdog.DEBUG) {
            Logger.log("ChartView.updateActions() entering");
        }
        boolean hasLLMMgr = mController.hasLLMMgr();
        mEditAction.setEnabled(hasLLMMgr);
        mRebootAction.setEnabled(mDeviceState == DEVICE_STATE.AVAILABLE);
        mClearAllChartsAction.setEnabled(hasLLMMgr);
        mClearChartAction.setEnabled(hasLLMMgr);
        mPausePlayAction.setEnabled(hasLLMMgr);
        mSaveToFileAction.setEnabled(hasLLMMgr && mLSFile == null);
        mSeriesShowAllAction.setEnabled(hasLLMMgr);
        mSeriesHideAllAction.setEnabled(hasLLMMgr);

        switch (mDeviceState) {
        case NOT_AVAILABLE:
            mDeviceStatusIcon.setIcon(mRedStatus);
            mDeviceStatusIcon.setText(DEVICE_NOT_CONNECTED_STR);
            return;

        case AVAILABLE:
            mDeviceStatusIcon.setIcon(mGreenStatus);
            mDeviceStatusIcon.setText(DEVICE_CONNECTED_STR);
            return;
        }
        mDeviceStatusIcon.setIcon(mGreyStatus);

        //REMOVE when adb get-state works:
        mGovernorMenu.update(true);
    }

    public void setWindowTitle(String fileName) {
        if (fileName != null) {
            setTitle(String.format("%s - %s", logdog.getFriendlyVersion(), fileName));
        } else {
            setTitle(logdog.getFriendlyVersion());
        }
    }

    public void setFileLogSourceName(File file) {
        mLSFile = file;
        mReadFromFileLabel.setText(mLSFile == null ? LIVE_LOGSOURCE_STR : mLSFile.getName());
        updateActions();
    }

    // private void showDuplicateCount() {
    //     mDuplicateCountLabel.setText(String.format("Duplicates: %d ", mDuplicateCount));
    // }

    private void loadFromFile(File xmlPath) {
        if (logdog.DEBUG) {
            Logger.log("ChartView.loadFromFile() entering");
        }
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            setWindowTitle(null);
            setFileLogSourceName(null);  // remove file name
            // This will call unRegisterFromLogSource() for all previous
            // LogLineMatchers in mLLMSeries.
            mController.clean();
            removeChartPanels();
            createCenterPanel();
            mController.createFromFile(xmlPath);  // throws
            setWindowTitle(xmlPath.getName());
        } catch (IOException excep) {
            Logger.logExcep(excep);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    @SuppressWarnings("serial")
    private abstract class ActionBase extends AbstractAction {
        public ActionBase(String caption, String tooltip, String pngImage, int mnemonic) {
            super(caption, UIUtils.createIcon(pngImage));
            putValue(SHORT_DESCRIPTION, tooltip);
            putValue(MNEMONIC_KEY, mnemonic);
        }
    }

    @SuppressWarnings("serial")
    private class OpenAction extends ActionBase {
        public OpenAction() {
            super(OPEN_ACTION_STR, OPEN_ACTION_TOOLTIP_STR, "open-16", KeyEvent.VK_O);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doOpenFile();
        }
    }

    @SuppressWarnings("serial")
    private class NewAction extends ActionBase {
        public NewAction() {
            super(NEW_ACTION_STR, NEW_ACTION_TOOLTIP_STR, "new-16", KeyEvent.VK_N);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doNewFile();
        }
    }

    @SuppressWarnings("serial")
    private class EditAction extends ActionBase {
        public EditAction() {
            super(EDIT_ACTION_STR, EDIT_ACTION_TOOLTIP_STR, "edit-16", KeyEvent.VK_E);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doEditFile();
        }
    }

    @SuppressWarnings("serial")
    private class ExitAction extends ActionBase {
        public ExitAction() {
            super(EXIT_ACTION_STR, null, "new-16", KeyEvent.VK_X);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doExit(ChartView.this);
        }
    }

    @SuppressWarnings("serial")
    private class SeriesShowAllAction extends ActionBase {
        public SeriesShowAllAction() {
            super(SERIES_SHOWALL_ACTION_STR, null, null, KeyEvent.VK_S);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            showAllSeriesLastChart();
        }
    }

    @SuppressWarnings("serial")
    private class SeriesHideAllAction extends ActionBase {
        public SeriesHideAllAction() {
            super(SERIES_HIDEALL_ACTION_STR, null, null, KeyEvent.VK_H);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            hideAllSeriesLastChart();
        }
    }

    @SuppressWarnings("serial")
    private class KernelLogAction extends ActionBase {
        public KernelLogAction() {
            super(KERNEL_LOG_ACTION_STR, KERNEL_LOG_ACTION_TOOLTIP_STR, "", KeyEvent.VK_UNDEFINED);
            Boolean isSelected = false;
            putValue(SELECTED_KEY, isSelected);
        }

        public void setSelected(Boolean selected) {
            putValue(SELECTED_KEY, selected);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final Boolean useLogCat = (Boolean) getValue(SELECTED_KEY);
            if (mDeviceState == DEVICE_STATE.AVAILABLE) {
                Utils.adbSetKernelLogProp(useLogCat);  // separate thread
            }
        }
    }

    @SuppressWarnings("serial")
    private class LSFromFileAction extends ActionBase {
        public LSFromFileAction() {
            super(LS_FROM_FILE_ACTION_STR, LS_FROM_FILE_ACTION_TOOLTIP_STR, "open-16", KeyEvent.VK_L);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            doLogSourceFromFile();
        }
    }

    @SuppressWarnings("serial")
    private class LogSourceAction extends ActionBase {
        private String mName;

        public LogSourceAction(String name) {
            super(name, LOGSOURCE_ACTION_TOOLTIP_STR, null, KeyEvent.VK_L);
            mName = name;
        }

        public void updateState() {
            LogSource logSource = LogSource.findLogSourceByName(mName);
            boolean alive = logSource != null && logSource.isAlive();
            putValue(SMALL_ICON, alive ? mGreenStatus : mRedStatus);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                LogSource logSource = LogSource.findLogSourceByName(mName);
                boolean alive = logSource != null && logSource.isAlive();
                if (alive) {
                    mController.ensureLSView(logSource);
                } else {
                    // Call start() if not already running. This will also create a
                    // LogSourceView (LogSourceLifeListener.onStarted() in MainController).
                    LogSource.findLogSource(mName, true);
                }
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    @SuppressWarnings("serial")
    private class RebootAction extends ActionBase {
        public RebootAction() {
            super(REBOOT_DEVICE_STR, REBOOT_DEVICE_TOOLTIP_STR, "boot-16", KeyEvent.VK_B);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (JOptionPane.showConfirmDialog(ChartView.this, REBOOT_DEVICE_Q_STR,
                                              REBOOT_DEVICE_TITLE_STR,
                                              JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Runtime rt = Runtime.getRuntime();
                                rt.exec("adb reboot");
                            } catch (IOException excep) {
                                Logger.logExcep(excep);
                            }
                        }
                    }).start();
            }
        }
    }

    @SuppressWarnings("serial")
    private class ClearAllChartsAction extends ActionBase {
        public ClearAllChartsAction() {
            super(CLEAR_STR, CLEAR_TOOLTIP_STR, "clear-16", KeyEvent.VK_C);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            clearAllCharts();
        }
    }

    @SuppressWarnings("serial")
    private class ClearChartAction extends ActionBase {
        public ClearChartAction() {
            super(CLEAR_STR, CLEAR_TOOLTIP_STR, "clear-16", KeyEvent.VK_C);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            clearLastChart();
        }
    }

    @SuppressWarnings("serial")
    private class PausePlayAction extends ActionBase {
        private ImageIcon mPauseIcon;
        private ImageIcon mPlayIcon;
        private boolean mFeeding = true;

        public PausePlayAction() {
            super(PAUSE_STR, PAUSE_ALL_LOGSOURCES_TOOLTIP_STR, "pause-16", KeyEvent.VK_R);
            mPauseIcon = UIUtils.createIcon("pause-16");
            mPlayIcon = UIUtils.createIcon("play-16");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            set(!mFeeding);
            LogSource.setFeedListeners(mFeeding);
        }

        public void set(boolean feeding) {
            mFeeding = feeding;
            putValue(SMALL_ICON, mFeeding ? mPauseIcon : mPlayIcon);
            putValue(NAME, mFeeding ? PAUSE_STR : PLAY_STR);
        }
    }

    @SuppressWarnings("serial")
    private class SaveToFileAction extends ActionBase {
        public SaveToFileAction() {
            super(SAVE_TO_FILE_STR, SAVE_TO_FILE_TOOLTIP_STR, "save-16", KeyEvent.VK_S);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            JCheckBox cb = (JCheckBox) evt.getSource();
            saveLogSourcesToFile(cb.isSelected());
        }
    }

    @SuppressWarnings("serial")
    private class SettingsAction extends ActionBase {
        public SettingsAction() {
            super(SETTINGS_STR, SETTINGS_TOOLTIP_STR, null, KeyEvent.VK_UNDEFINED);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            new SettingsView(ChartView.this, ChartView.this);
        }
    }

    @SuppressWarnings("serial")
    private class AboutAction extends ActionBase {
        public AboutAction() {
            super(ABOUT_STR, null, null, KeyEvent.VK_UNDEFINED);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            new AboutDialog(ChartView.this);
        }
    }


    // Commands

    private void doOpenFile() {
        File xmlPath = UIUtils.showFileDlg(this, false, true, UIUtils.FILEDLG_FILTER.FILTER_LLM, Directory.LOGDOG_FILES);
        if (xmlPath != null) {
            loadFromFile(xmlPath);
        }
        updateActions();
    }

    private void doNewFile() {
        mController.create(this, null);
    }

    private void doEditFile() {
        mController.edit(this, null);
    }

    private void doLogSourceFromFile() {
        File file = UIUtils.showFileDlg(this, false, true, UIUtils.FILEDLG_FILTER.FILTER_NONE, Directory.LOGSOURCE_FILES);
        if (file != null && file.exists()) {
            removeChartPanels();
            FileLogSource fileLogSource = new FileLogSource(file);
            mController.setFileLogSource(fileLogSource);
            setFileLogSourceName(file);
        }
    }

    private void doExit(JFrame frame) {
        if (JOptionPane.
            showConfirmDialog(frame, EXIT_CONFIRM_STR, EXIT_CONFIRM_TITLE_STR,
                              JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                LogSource.removeFeedListener(this);  //TODO Move to Controller?
                mController.removeSelListener(this);
                mController.die();
                // This assume we are the last man standing which might not be true...:
                LogSource.stopAll();
                dispose();
                System.exit(0);
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private void showAllSeriesLastChart() {
        if (mLastClickedChartPanel != null) {
            mLastClickedChartPanel.setSeriesVisibility(true);
        }
    }

    private void hideAllSeriesLastChart() {
        if (mLastClickedChartPanel != null) {
            mLastClickedChartPanel.setSeriesVisibility(false);
        }
    }

    private void clearAllCharts() {
        Iterator<Entry<Integer, SyncedChartPanel>> iter = mChartPanels.entrySet().iterator();
        while (iter.hasNext()) {
            SyncedChartPanel chartPanel = iter.next().getValue();
            chartPanel.clear();
            mController.clearStateLLMsPresentationId(chartPanel.getId());
        }
    }

    private void clearLastChart() {
        if (mLastClickedChartPanel != null) {
            mLastClickedChartPanel.clear();
            mController.clearStateLLMsPresentationId(mLastClickedChartPanel.getId());
        }
    }

    private void saveLogSourcesToFile(boolean saveToFile) {
        LogSource.saveAllToFile(saveToFile);
        mSaveToFileLabel.setText(saveToFile ? SAVING_TO_FILE_STR : "");
    }

    @Override
    public void actionPerformed(ActionEvent actEvent) {
        String cmd = actEvent.getActionCommand();
        if (cmd.equals(UIUtils.ESCAPE_ACTION_NAME)) {
            doExit(this);
        }
    }


    // LLMMgrListener

    public void registeringLLM(LogLineMatcher llm) {
        // This is the groups the user has given names and other attributes:
        LogLineMatcher.Groups groups = llm.getGroups();

        // This is the number of groups in the regular expression:
        int regExpGroupCount = llm.getRegExpGroupCount();

        // This is the number of chart series we will create below:
        int seriesCount = regExpGroupCount;

        if (llm.hasTimeDiff()) {
            ++seriesCount;
        }

        boolean hasRegExpGroups = regExpGroupCount > 0;
        if (!hasRegExpGroups) {  // only matching a line, no value
            ++seriesCount;
        }

        boolean llmHasTimeDiff = llm.hasTimeDiff();
        int groupsIndex = groups != null && groups.size() > 0 ? 0 : -1;

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (int seriesIndex = 0; seriesIndex < seriesCount; ++seriesIndex) {
            String seriesName = null;
            String seriesUnit = null;
            String seriesFormat = null;
            String diffSeriesName = null;
            boolean standardScale = hasRegExpGroups || llmHasTimeDiff;
            Range range = null;
            boolean includeZero = false;;

            if (seriesIndex == 0 && llmHasTimeDiff) {
                seriesName = llm.getDiffSeriesName();
                if (groupsIndex >= 0) {
                    LogLineMatcher.Group group = groups.get(groupsIndex);
                    seriesUnit = "ms";
                    seriesFormat = group.getScaleFormat();
                    if (group.hasScaleRange()) {
                        range = new Range(group.getScaleRangeMin(), group.getScaleRangeMax());
                    }
                    includeZero = group.getScaleIncludeZero();
                }
            } else if (!hasRegExpGroups) {
                seriesName = llm.getName();
            } else if (groupsIndex >= 0) {
                LogLineMatcher.Group group = groups.get(groupsIndex);
                seriesName = group.getSeriesName();
                seriesUnit = group.getScaleUnit();
                seriesFormat = group.getScaleFormat();
                diffSeriesName = group.getDiffSeriesName();  // null if no difftime series
                if (group.hasScaleRange()) {
                    range = new Range(group.getScaleRangeMin(), group.getScaleRangeMax());
                }
                includeZero = group.getScaleIncludeZero();
                if (++groupsIndex >= groups.size()) {
                    groupsIndex = -1;
                }
            } else if ((seriesIndex == 0 && !llmHasTimeDiff) ||
                       (seriesIndex == 1 && llmHasTimeDiff)) {
                seriesName = llm.getName();
            } else {
                seriesName = String.format("%s (%d)", llm.getName(), seriesIndex);
            }

            SyncedChartPanel chartPanel = findChartPanel(llm);
            if (chartPanel != null) {
                TimeSeries series =
                    chartPanel.addSeries(seriesName, seriesUnit, seriesFormat,
                                         standardScale, range, includeZero, llm);
                dataset.addSeries(series);

                if (diffSeriesName != null) {
                    series = chartPanel.addSeries(diffSeriesName, seriesUnit, seriesFormat,
                                       standardScale, range, includeZero, llm);
                    dataset.addSeries(series);
                }
            }
        }
        mLLMSeries.put(llm, dataset);
        updateActions();
    }

    public void unRegisteringLLM(LogLineMatcher llm) {
        //TODO Is this really correct??? I think we need to clean up in SyncedChartPanel as well.
        mLLMSeries.remove(llm);
        updateActions();
    }

    public void onMatchedLogLine(LogLineMatcher llm, int seriesIndex, Date date, float value) {
//        synchronized (mChartLock) {
            TimeSeriesCollection dataset = mLLMSeries.get(llm);
            if (dataset != null) {
                TimeSeriesWithStats series = (TimeSeriesWithStats) dataset.getSeries(seriesIndex);
                if (series != null) {
                    // Since we can only insert one observation per time in the chart
                    // and we can get several matched log lines having the same
                    // timestamp, we increment the time for at most 9 times to get a
                    // timestamp that can be inserted (the resolution in logcat being 10
                    // ms). If we still fail after 9 retries we report it as a
                    // duplicate.
                    Millisecond millis = new Millisecond(date);
                    int failCount = 0;
                    while (failCount++ < 9) {
                        try {
                            series.add(millis, value);
                            break;
                        } catch (SeriesException excep) {
                            millis = new Millisecond(new Date(date.getTime() + 1));
                        }
                    }
                    // if (failCount == 9) {
                    //     // We assume the SeriesExceptions above are caused by duplicate
                    //     // observations being inserted. This is the most probable cause.
                    //     ++mDuplicateCount;
                    //     showDuplicateCount();
                    //     String msg = String.format(SERIES_INSERT_FAILED_STR, millis.getMillisecond(), value);
                    //     Logger.log(msg);
                    // }
                }
            }
//        }
    }


    // DeviceStater.DeviceListener

    @Override
    public void onDeviceChanged(DeviceStater stater) {
        mDeviceState = stater.getDeviceState();
        mKernelLog = stater.getKernelLog();
        updateActions();
        mGovernorMenu.update(mDeviceState == DEVICE_STATE.AVAILABLE);
    }


    // MenuListener

    @Override
    public void menuCanceled(MenuEvent arg0) {
    }

    @Override
    public void menuDeselected(MenuEvent arg0) {
    }

    @Override
    public void menuSelected(MenuEvent evt) {
        updateActions();
        updateLogSourceMenuActions();
        if (evt.getSource() == mAboutMenu) {
            mAboutMenu.getAction().actionPerformed(new ActionEvent(ChartView.this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }


    // ViewSelListener

    @Override
    public void onSelection(long time) {
    }


    // ChartPanelListener (i.e. ChartView)
    public void onChartClicked(SyncedChartPanel panel) {
        mLastClickedChartPanel = panel;
    }

    public void onChartDoubleClicked(long clickedTime) {
        mController.notifySelListeners(this, clickedTime);
    }

    public void onStatsChanged(String info) {
        mSeriesStatsLabel.setText(info);
    }

    public  void onDataPointClicked(String info) {
        mDataPointLabel.setText(info);
    }


    // SettingsView.SettingsChangedListener

    @Override
    public void onSaved(Prefs prefs) {
        // Notify all charts about change in use of chart shapes.
        Iterator<Entry<Integer, SyncedChartPanel>> iter = mChartPanels.entrySet().iterator();
        while (iter.hasNext()) {
            SyncedChartPanel chartPanel = iter.next().getValue();
            chartPanel.setShapesInChart(prefs.getShapesInCharts());
        }
    }


    // LogSourceFeedListener

    @Override
    public void onFeedingStarted(LogSource logSource) {
        mPausePlayAction.set(true);
    }

    @Override
    public void onFeedingStopped(LogSource logSource) {
        mPausePlayAction.set(false);
    }
}
