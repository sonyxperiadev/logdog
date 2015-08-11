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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import logdog.logdog;
import logdog.model.FileDumper;
import logdog.model.LogSource;
import logdog.utils.Logger;
import logdog.utils.Utils;

/**
 * This is a virtual JList meaning we handle a large number of items
 * efficiently by managing the data storage and retrieval ourselves and
 * discarding old items when reaching the maximum number of items
 * managed.
 */
@SuppressWarnings("serial")
public class LogSourceList extends JList<String> {

    private LogLineData mLogLineData = new LogLineData();
    private final String mFontName = Font.MONOSPACED;
    private Font mPlainFont = new Font(mFontName, Font.PLAIN, 11);
    private Font mBoldFont = new Font(mFontName, Font.BOLD, 11);
    private Renderer mRenderer;
    private JScrollPane mScrollPane;
    private boolean mScrollLock;
    private LogSource mLogSource;

    private volatile boolean mAcceptAdd = true;

    private Object mAddUpdateUISync = new Object();     // Sync object for updating the UI.
    private int ADD_UPDATE_UI_TIMEOUT = 100; // ms
    private Thread mAddUpdateUIThread;
    private volatile long mAddUpdateUICount;
    
    /**
     * Constructor.
     */
    public LogSourceList(LogSource logSource) {
        super();

        mLogSource = logSource;
        setModel(mLogLineData);
        setFont(mPlainFont);
        FontMetrics fm = getFontMetrics(mPlainFont);
        setFixedCellHeight(fm.getHeight());

        mScrollPane = new JScrollPane(this);
//        mScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mRenderer = new Renderer(this);
        setCellRenderer(mRenderer);

        // Start thread that is responsible for ensuring that the list is
        // repainted while adding items and while not having scroll lock. This
        // thread is needed to prevent an UI update for every add.
        mAddUpdateUIThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mAddUpdateUIThread.isInterrupted()) {  // does not reset the interrupt flag
                    synchronized (mAddUpdateUISync) {
                        try {
                            long timeBeforeWait = System.currentTimeMillis();
                            mAddUpdateUISync.wait(ADD_UPDATE_UI_TIMEOUT);
                            // If we have timed out and we have added items then trigger a repaint.
                            long eventSpacing = (System.currentTimeMillis() - timeBeforeWait);
                            if (eventSpacing >= ADD_UPDATE_UI_TIMEOUT &&
                                mAddUpdateUICount > 0) {
                                // Must run ensureIndexIsVisible() later because this method is
                                // called from another thread.
                                SwingUtilities.invokeLater(mEnsureVisRunnable);
                                mAddUpdateUICount = 0;
                            }
                        } catch (InterruptedException excep) {
                        }
                    }
                }
            }
        });
        mAddUpdateUIThread.setName("AddUpdateUI");
        mAddUpdateUIThread.start();
    }

    public void die() {
        try {
            mAddUpdateUIThread.interrupt();
            mAddUpdateUIThread.join(ADD_UPDATE_UI_TIMEOUT);
        } catch (InterruptedException excep) {
            excep.printStackTrace();
        }
    }

    @Override
    public void paintComponent(Graphics graphics) {
        // This doesn't seem to add any improvement:
        // ((Graphics2D) graphics).
        //     setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(graphics);
    }

    boolean getAcceptAdd() {return mAcceptAdd;}

    void setAcceptAdd(boolean acceptAdd) {
        if (acceptAdd != mAcceptAdd) {
            mAcceptAdd = acceptAdd;
            synchronized (mLogLineData) {
                String msg = mAcceptAdd ?
                    "--------------- LOGDOG RESUMING LOG OUTPUT ---------------" :
                    "--------------- LOGDOG PAUSING LOG OUTPUT ---------------" ;
                mLogLineData.add(msg, new Date());
            }
        }
    }

    private Runnable mEnsureVisRunnable = new Runnable() {
        @Override
        public void run() {
            ensureIndexIsVisible(getModel().getSize() - 1);
            synchronized (mAddUpdateUISync) {
                if (logdog.DEBUG) {
                    Logger.log(String.format("mAddUpdateUICount=%d", mAddUpdateUICount));
                }
                mAddUpdateUICount = 0;
            }
        }
    };

    void add(String logLine) {
        add(logLine, new Date());
    }

    void add(String logLine, Date date) {
        if (mAcceptAdd) {
            synchronized (mLogLineData) {
                mLogLineData.add(logLine, date);
                if (!mScrollLock) {
                    synchronized (mAddUpdateUISync) {
                        mAddUpdateUICount++;
                        mAddUpdateUISync.notify();
                    }
                }
            }
        }
    }

    void clear() {
        synchronized (mLogLineData) {
            mLogLineData.clear();
        }
    }

    void setScrollLock(boolean scrollLock) {
        mScrollLock = scrollLock;
    }

    Component getScrollPane() {
        return mScrollPane;
    }

    Color getLogLineColor(String logLine) {
        return mLogSource.getLogLineColor(logLine);
    }

    boolean scrollTo(long time) {
//        synchronized (mLogLineData) {  //OK?
        int index = mLogLineData.findIndexForTime(time);
        if (index != -1) {
            int pageSize = getVisibleRowCount();
            int pageIndex = index / pageSize;
            int indexWithinPage = index % pageSize;
            int topIndex = pageIndex * pageSize + indexWithinPage;
            Rectangle rect = getCellBounds(topIndex, topIndex + pageSize);
            if (rect != null) {
                scrollRectToVisible(rect);
                setSelectedIndex(index);
                return true;
            }
        }
        return false;
//        }
    }

    void syncMarkListIndex() {
        synchronized (mLogLineData) {
            mLogLineData.setMarkListIndex(getSelectedIndex());
        }
    }

    void setMarkRegExp(String markRegExp) {
        mRenderer.setMarkRegExp(markRegExp);
    }

    int getMarkPreviousIndex() {
        synchronized (mLogLineData) {
            return mLogLineData.getMarkPreviousIndex();
        }
    }

    int getMarkNextIndex() {
        synchronized (mLogLineData) {
            return mLogLineData.getMarkNextIndex();
        }
    }

    boolean indexIsSelectedAndContainsMarkRegExp(int index) {
        return mLogLineData.indexIsSelectedAndContainsMarkRegExp(index);
    }

    Font getBoldFont() {
        return mBoldFont;
    }

    void changeFontSize(int step) {
        // mPlainFont.deriveFont(int size) doesn't work for mono spaced font.
        int newSize = mPlainFont.getSize() + step;
        if (newSize < 8) {
            newSize = 8;
        } else if (newSize > 30) {
            newSize = 30;
        }
        mPlainFont = new Font(mFontName, Font.PLAIN, newSize);
        mBoldFont = new Font(mFontName, Font.BOLD, newSize);
        setFont(mPlainFont);

        // We need this to prevent the JList implementation to loop
        // over all items to compute the max width and height of all
        // the list cells.
        FontMetrics fm = getFontMetrics(mPlainFont);
        setFixedCellHeight(fm.getHeight());
        repaint();
    }

    void decreaseFontSize() {
        changeFontSize(-1);
    }

    void increaseFontSize() {
        changeFontSize(1);
    }

    void saveToFile(File file) {
        FileDumper fileDumper = new FileDumper(file);
        synchronized (mLogLineData) {
            ListModel<?> model = getModel();
            int count = model.getSize();
            if (fileDumper.start()) {
                for(int index = 0; index < count; ++index) {
                    fileDumper.onLogLine((String) model.getElementAt(index));
                }
                fileDumper.stop();
            }
        }
    }

    public void selectIndexFromMousePoint(MouseEvent evt) {
        setSelectedIndex(locationToIndex(evt.getPoint()));
    }

    /**
     * This is the list model storing all the data.
     */
    private class LogLineData extends AbstractListModel<String> {

        private static final int MAX_LOGLINE_COUNT = 100000;
        private String[] mLogLines = new String[MAX_LOGLINE_COUNT];
        private long[] mTimes = new long[MAX_LOGLINE_COUNT];
        private int mBeginIndex = 0;
        private int mEndIndex = -1;
        private boolean mFull;
        private int mMarkListIndex = -1;

        void setMarkListIndex(int index) {
            mMarkListIndex = index;
        }

        boolean indexIsSelectedAndContainsMarkRegExp(int index) {
            return index == mMarkListIndex && mRenderer.isMarkMatch(mLogLines[mMarkListIndex]);
        }

        int getMarkPreviousIndex() {
            if (mRenderer.hasMarkPattern()) {
                int maxIndex = getSize() - 1;
                for (int count = maxIndex; count >= 0; --count) {
                    if (--mMarkListIndex <= 0) {
                        mMarkListIndex = maxIndex;
                    }
                    if (mRenderer.isMarkMatch(mLogLines[mMarkListIndex])) {
                        fireContentsChanged(this, 0, MAX_LOGLINE_COUNT - 1);
                        return mMarkListIndex;
                    }
                }
            }
            return -1;
        }

        int getMarkNextIndex() {
            if (mRenderer.hasMarkPattern()) {
                int maxIndex = getSize() - 1;
                for (int count = 0; count <= maxIndex; ++count) {
                    if (++mMarkListIndex > maxIndex) {
                        mMarkListIndex = 0;
                    }
                    if (mRenderer.isMarkMatch(mLogLines[mMarkListIndex])) {
                        fireContentsChanged(this, 0, MAX_LOGLINE_COUNT - 1);
                        return mMarkListIndex;
                    }
                }
            }
            return -1;
        }

        void add(String logLine, Date date) {
            if (++mEndIndex == MAX_LOGLINE_COUNT) {
                mBeginIndex = 1;
                mEndIndex = 0;
                mFull = true;
                if (logdog.DEBUG) {
                    Logger.log("LogSourceList is full");
                }
            } else if (mFull && mEndIndex == mBeginIndex) {
                if (++mBeginIndex == MAX_LOGLINE_COUNT) {
                    mBeginIndex = 0;
                }
            }
            mLogLines[mEndIndex] = logLine;
            if (date == null) {
                mTimes[mEndIndex] = -1;
            } else {
                mTimes[mEndIndex] = date.getTime();
            }

            if (mFull) {
                // SwingUtilities.invokeLater(new Runnable() {
                //     @Override
                //     public void run() {
                //         fireContentsChanged(this, 0, MAX_LOGLINE_COUNT - 1);
                //     }
                // });
                fireContentsChanged(this, 0, MAX_LOGLINE_COUNT - 1);
            } else {
                // Figure out the index in the listbox and notify it
                // about the added data:
                int listIndex = mEndIndex - mBeginIndex;
                if (listIndex < 0) {
                    listIndex += MAX_LOGLINE_COUNT;
                }
                // final int index = listIndex;
                // SwingUtilities.invokeLater(new Runnable() {
                //     @Override
                //     public void run() {
                //         fireIntervalAdded(this, index, index);
                //     }
                // });
                fireIntervalAdded(this, listIndex, listIndex);
            }
        }

        void clear() {
            mBeginIndex = 0;
            mEndIndex = -1;
            mFull = false;
            mMarkListIndex = -1;
            setSelectedIndex(-1);
            clearSelection();
            fireContentsChanged(this, 0, MAX_LOGLINE_COUNT - 1);
        }

        /**
         * Search in 'mTimes' for the time closest to the given time and return
         * its index. Now uses a binary search which obviously assumes the
         * times come in consecutive order. This might not be the case if the
         * time has been changed on the phone.
         *
         * @param time Time in milliseconds.
         *
         * @return Index inot 'mTimes', -1 if not found.
         */
        int findIndexForTime(long time) {
            synchronized (mLogLineData) {
                int beginIndex = mBeginIndex;
                int endIndex = mEndIndex;
                // Let endIndex overflow to make coding easier. Compensated for below.
                if (endIndex < beginIndex) {
                    endIndex += MAX_LOGLINE_COUNT;
                }
                int range = (endIndex - beginIndex) / 2;
                int index = beginIndex + range;
                while (range > 0) {
                    int testIndex = index;
                    if (index >= MAX_LOGLINE_COUNT) {
                        testIndex -= MAX_LOGLINE_COUNT;
                    }
                    long crntTime = mTimes[testIndex];
                    if (crntTime == time) {
                        return testIndex;
                    }
                    if (time > crntTime) {
                        beginIndex = index;
                    } else {
                        endIndex = index;
                    }
                    range = (endIndex - beginIndex) / 2;
                    index = beginIndex + range;
                    // If we are at the last iteration, then select the higher index.
                    if (range == 0 && ++index >= MAX_LOGLINE_COUNT) {
                        index = 0;
                    }
                }
                return index;
            }
        }

        @Override
        public String getElementAt(int listIndex) {
//            synchronized (mLogLineData) {  //OK?
                int index = mBeginIndex + listIndex;
                if (index >= MAX_LOGLINE_COUNT) {
                    index -= MAX_LOGLINE_COUNT;
                }
                return mLogLines[index];
//            }
        }

        @Override
        public int getSize() {
//            synchronized (mLogLineData) {  //OK?
                if (mEndIndex == -1) {
                    return 0;
                }
                if (mEndIndex >= mBeginIndex) {
                    return mEndIndex - mBeginIndex + 1;
                }
                return MAX_LOGLINE_COUNT;
//            }
        }
    }

    /**
     * This is the renderer responsible for drawing every item in the list.
     */
    private class Renderer extends JLabel implements ListCellRenderer<Object> {

        // Store "owner" to prevent casting when painting.
        private LogSourceList mList;

        // If non null, lines containing this regular expression will be
        // painted with the 'mMarkStringColor'.
        private Pattern mMarkPattern;
        private Color mMarkStringColor = new Color(255, 255, 192);  // light yellow;
        // private Color mBackgroundColor = new Color(244, 244, 244);

        Renderer(LogSourceList list) {
            mList = list;
        }

        private boolean hasMarkPattern() {
            return mMarkPattern != null;
        }

        private void setMarkRegExp(String markRegExp) {
            if (Utils.emptyString(markRegExp)) {
                mMarkPattern = null;
            } else {
                mMarkPattern = Pattern.compile(markRegExp, Pattern.CASE_INSENSITIVE);
            }
        }

        private boolean isMarkMatch(final String logLine) {
            if (mMarkPattern != null) {
                Matcher matcher = mMarkPattern.matcher(logLine);
                return matcher.find();
            }
            return false;
        }

        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean hasFocus) {
            // Use casting instead of toString() for performance.
            String logLine = (String) value;
            if (selected) {
                setBackground(mList.getSelectionBackground());
                setForeground(mList.getSelectionForeground());
            } else {
                if (mMarkPattern != null && mList.indexIsSelectedAndContainsMarkRegExp(index)) {
                    setBackground(mMarkStringColor);
                } else {
                    // This doesn't seem to work:
                    setBackground(mList.getBackground());
                    // setBackground(mBackgroundColor);
                }
                Color textColor = mList.getLogLineColor(logLine);
                if (textColor == Color.white) {
                    setForeground(mList.getForeground());
                } else {
                    setForeground(textColor);
                }
            }
            boolean lineIsMarked = mMarkPattern != null && isMarkMatch(logLine);
            setFont(lineIsMarked ? mList.getBoldFont() : mList.getFont());
            setOpaque(true);
            setText(logLine);
            return this;
        }
    }
}
