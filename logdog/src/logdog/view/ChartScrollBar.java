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

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.Timeline;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JScrollBar;

@SuppressWarnings("serial")
public class ChartScrollBar extends JScrollBar
    implements AdjustmentListener, AxisChangeListener, MouseListener, DatasetChangeListener {
//    implements AdjustmentListener, AxisChangeListener, DatasetChangeListener {

    static private int STEPS = 100000;

    private boolean mFirstTime = true;
    private XYPlot mPlot;
    private DateAxis mDomainAxis;
    private double mRatio;
    private volatile boolean mUpdating;
    private double mDataRangeMin;
    private double mDataRangeMax;
    private double mViewLength;

    private int mPageSizeMillis = 30 * 1000; //TODO Get from settings/prefs

    /**
     * C-tor.
     * Prerequisite: the domain axis for 'plot' must be a DateAxis.
     *
     * @param plot
     *
     * @return
     */
    public ChartScrollBar(XYPlot plot) {
        super(HORIZONTAL);
        mPlot = plot;
        assert mPlot != null : "mPlot must be non-null";
        mDomainAxis = (DateAxis) mPlot.getDomainAxis();
        reset();
    }

    void setAsListener() {
        mDomainAxis.addChangeListener(this);
        addAdjustmentListener(this);
        Dataset dataset = mPlot.getDataset();
        if (dataset != null) {
            dataset.addChangeListener(this);
        }
        updateAxis();
        addMouseListener(this);
    }

    void reset() {
        mFirstTime = true;
        setVisible(false);
    }

    private void updateAxis() {
        if (mUpdating) {
            return;
        }
        mUpdating = true;

        double viewMin = 0;
        double viewMax = 0;

        Range dataRange = mPlot.getDataRange(mDomainAxis);
        if (dataRange != null) {
            mDataRangeMin = dataRange.getLowerBound();
            mDataRangeMax = dataRange.getUpperBound();

            if (mFirstTime) {
                mDomainAxis.setAutoRange(false);
                mDomainAxis.setRange(mDataRangeMin, mDataRangeMin + mPageSizeMillis);  // 30s
            }

            viewMin = mDomainAxis.getLowerBound();
            viewMax = mDomainAxis.getUpperBound();

            Timeline tl = mDomainAxis.getTimeline();
            mDataRangeMin = tl.toTimelineValue((long) mDataRangeMin);
            mDataRangeMax = tl.toTimelineValue((long) mDataRangeMax);
            viewMin = tl.toTimelineValue((long) viewMin);
            viewMax = tl.toTimelineValue((long) viewMax);

            mViewLength = viewMax - viewMin;
            mRatio = STEPS / (mDataRangeMax - mDataRangeMin);

            if (mFirstTime) {
                // Must do this to support page scrolling when
                // clicking in the scrollbar "gutter". Make the
                // blockIncrement somewhat smaller than the view size
                // so teh user sees the old value when page scrolling.
                int blockIncrement = (int) (mPageSizeMillis * 0.9 * STEPS / mViewLength);
                setBlockIncrement(blockIncrement);
                setUnitIncrement(blockIncrement / 20);
                mFirstTime = false;
                setVisible(true);
            }
        } else {
            mDataRangeMin = 0;
            mDataRangeMax = 0;
            mViewLength = 0;
            mRatio = 1;
        }

        int newMin = 0;
        int newMax = STEPS;
        int newExtent = (int) (mViewLength * mRatio);
        int newValue = (int) ((viewMin - mDataRangeMin) * mRatio);

        setValues(newValue, newExtent, newMin, newMax);
        mUpdating = false;
    }


    // AxisChangeListener
    public void axisChanged(AxisChangeEvent event) {
        updateAxis();
    }


    // DatasetChangeListener
    public void datasetChanged(DatasetChangeEvent event) {
        updateAxis();
    }


    // AdjustmentListener
    /**
     * Notification when the scrollbar's model changes.
     *
     * @param event
     */
    public void adjustmentValueChanged(AdjustmentEvent event) {
        if (mUpdating) {
            return;
        }

        mUpdating = true;

        double start = getValue() / mRatio + mDataRangeMin;
        double end = start + mViewLength;

        if (end > start) {
            Timeline tl = ((DateAxis) mDomainAxis).getTimeline();
            start = tl.toMillisecond((long) start);
            end = tl.toMillisecond((long) end);
            mDomainAxis.setRange(start, end);
            //System.out.println(String.format("start=%f end=%f", start, end));
        }

        mUpdating = false;
    }

    public void zoomFull() {
        mPlot.getDomainAxis().setAutoRange(true);
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            zoomFull();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
