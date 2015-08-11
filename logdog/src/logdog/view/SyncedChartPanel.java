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

import logdog.logdog;
import logdog.model.LogLineMatcher;
import logdog.model.LogLineMatcherManager;
import logdog.utils.Logger;
import logdog.utils.Utils;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * Since JFreeChart isn't thread safe we need to sync the various
 * threads adding data to the chart's series and the repainting of
 * the chart.
 */
@SuppressWarnings("serial")
public class SyncedChartPanel extends ChartPanel implements ChartMouseListener {

    /*------ Static class members and methods ------*/

    private static final Color[] sColors = {
        new Color(233, 10, 66),
        new Color(105, 16, 226),
        new Color(17, 64, 225),
        new Color(17, 189, 225),
        new Color(17, 225, 105),
        new Color(242, 214, 53),
        new Color(242, 100, 73),
        new Color(193, 123, 142),
        new Color(166, 123, 192),
        new Color(123, 176, 192),
        new Color(123, 192, 129),
        new Color(192, 192, 125),
        new Color(205, 121, 112),
    };

    public void setShapesInChart(boolean shapesInChart) {
        if (shapesInChart != mShapesInChart) {
            mShapesInChart = shapesInChart;
            XYPlot plot = getChart().getXYPlot();
            int datasetCount = plot.getDatasetCount();
            for (int datasetIndex = 0; datasetIndex < datasetCount; ++ datasetIndex) {
                XYDataset dataset = plot.getDataset(datasetIndex);
                if (dataset == null) {
                    continue;
                }
                int seriesCount = dataset.getSeriesCount();
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer(datasetIndex);
                renderer.setBaseShapesVisible(mShapesInChart);
                for (int seriesIndex = 0; seriesIndex < seriesCount; ++ seriesIndex) {
                    renderer.setSeriesShapesVisible(seriesIndex, mShapesInChart);
                }
            }
        }
    }
    private boolean mShapesInChart;

    private static Shape[] sShapes;
    private static final int SHAPE_COUNT = 10;
    private static final long PLOT_MAX_ITEM_AGE = 5 * 60 * 1000;  // ms
    private static final Font sFontPlain = new Font(Font.SANS_SERIF, Font.PLAIN, 7);
    private static final Font sFontBold = new Font(Font.SANS_SERIF, Font.BOLD, 7);
    private static final Color sLegendTextColor = new Color(64, 64 ,64);
    private static final Color sLegendDisabledTextColor = Color.LIGHT_GRAY;

    private static int[] intArray(double a, double b, double c) {
        return new int[] {(int) a, (int) b, (int) c};
    }

    private static int[] intArray(double a, double b, double c, double d) {
        return new int[] {(int) a, (int) b, (int) c, (int) d};
    }

    /**
     * Create one empty chart panel with default settings. No
     * associated datasets.
     */
    static SyncedChartPanel create(int id, boolean shapesInCharts) {
        if (logdog.DEBUG) {
            Logger.log("SyncedChartPanel.createChartPanel() entering");
        }

        JFreeChart chart = createTimeSeriesChart();
        chart.setBackgroundPaint(new Color(248, 248, 248));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        Color gridColor = new Color(200, 200, 200);
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        // Set up X-axis using ms-time format.
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss.SSS"));
        domainAxis.setTickLabelFont(sFontPlain);

        return new SyncedChartPanel(id, chart, shapesInCharts);

        //return new SyncedChartPanel(id, chart, shapesInCharts);

        // // The content pane uses BorderLayout by default.
                // Container contentPane = getContentPane();

                // // This adds a horizontal scrollbar but also rescales the entire
                // // component inluding fonts which make it look weird. The legend is also
                // // scrolled which isn't desirable. The height parameter has no meaning
                // // since this is the CENTER panel and it fills the entrie area allocated
                // // to it.
                // // mChartPanel.setPreferredSize(new Dimension(3000, 100));
                // // JScrollPane scrollPane = new JScrollPane(mChartPanel);
                // // contentPane.add(scrollPane, BorderLayout.CENTER);
                // /////

                // // JPanel centerPanel = new JPanel();
                // // BoxLayout centerLayout = new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS);
                // // centerPanel.setLayout(centerLayout);
                // // centerPanel.add(mChartPanel);
                // // centerPanel.add(new ChartScrollBar(JScrollBar.HORIZONTAL, mChart));
                // // contentPane.add(centerPanel, BorderLayout.CENTER);

                // contentPane.add(mChartPanel, BorderLayout.CENTER);

                // pack();
    }

    // Copied from ChartFactory.createTimeSeriesChart() and modified
    // to support hiding of series without also hiding the
    // corresponding legend item.
    private static JFreeChart createTimeSeriesChart() {
        ValueAxis timeAxis = new DateAxis();
        // Auto ranging will be removed by the scrollbar once data are
        // added to the plot.
        timeAxis.setAutoRange(true);
        timeAxis.setLowerMargin(0.02);  // reduce the default margins
        timeAxis.setUpperMargin(0.02);

        NumberAxis valueAxis = new NumberAxis();
        valueAxis.setAutoRangeIncludesZero(false);  // override default
        valueAxis.setVisible(false);

        // To be able to support hiding of series without also
        // hiding the corresponding legend item, we must override
        // the plot. This code is copied and modified somewhat from
        // XYPlot.getLegendItems().
        XYPlot plot = new XYPlot(null, timeAxis, valueAxis, null) {
            @Override
            public LegendItemCollection getLegendItems() {
                LegendItemCollection result = new LegendItemCollection();
                int count = this.getDatasetCount();
                for (int datasetIndex = 0; datasetIndex < count; datasetIndex++) {

                    XYDataset dataset = getDataset(datasetIndex);
                    if (dataset == null) {
                        continue;
                    }

                    XYItemRenderer renderer = getRenderer(datasetIndex);
                    if (renderer == null) {
                        renderer = getRenderer(0);
                    }
                    if (renderer == null) {
                        continue;
                    }

                    int seriesCount = dataset.getSeriesCount();
                    for (int i = 0; i < seriesCount; i++) {
                        // Commented
                        // if (renderer.isSeriesVisible(i)
                        //         && renderer.isSeriesVisibleInLegend(i)) {
                        if (renderer.isSeriesVisibleInLegend(i)) {
                            LegendItem item = renderer.getLegendItem(
                                datasetIndex, i);
                            if (item != null) {
                                result.add(item);
                            }
                        }
                    }
                }
                return result;
            }
        };

        XYToolTipGenerator toolTipGenerator = null;
        boolean tooltips = true;
        if (tooltips) {
            toolTipGenerator
                = StandardXYToolTipGenerator.getTimeSeriesInstance();
        }

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setBaseToolTipGenerator(toolTipGenerator);
        plot.setRenderer(renderer);

        boolean legend = true;
        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,
                plot, legend);
        new StandardChartTheme("JFree").apply(chart);

        return chart;
    }

    /**
     * This code is copied from JFreeChart so we can make the shapes larger. This is not
     * possible to do using the JFreeChart API.
     */
    private static void createShapes() {
        if (logdog.DEBUG) {
            Logger.log(".createShapes() entering");
        }
        if (sShapes != null) {
            return;
        }

        sShapes = new Shape[SHAPE_COUNT];

        double size = 6.0;
        double delta = size / 2.0;
        int[] xpoints = null;
        int[] ypoints = null;

        // square
        sShapes[0] = new Rectangle2D.Double(-delta, -delta, size, size);

        // circle
        sShapes[1] = new Ellipse2D.Double(-delta, -delta, size, size);

        // up-pointing triangle
        xpoints = intArray(0.0, delta, -delta);
        ypoints = intArray(-delta, delta, delta);
        sShapes[2] = new Polygon(xpoints, ypoints, 3);

        // diamond
        xpoints = intArray(0.0, delta, 0.0, -delta);
        ypoints = intArray(-delta, 0.0, delta, 0.0);
        sShapes[3] = new Polygon(xpoints, ypoints, 4);

        // horizontal rectangle
        sShapes[4] = new Rectangle2D.Double(-delta, -delta / 2, size, size / 2);

        // down-pointing triangle
        xpoints = intArray(-delta, +delta, 0.0);
        ypoints = intArray(-delta, -delta, delta);
        sShapes[5] = new Polygon(xpoints, ypoints, 3);

        // horizontal ellipse
        sShapes[6] = new Ellipse2D.Double(-delta, -delta / 2, size, size / 2);

        // right-pointing triangle
        xpoints = intArray(-delta, delta, -delta);
        ypoints = intArray(-delta, 0.0, delta);
        sShapes[7] = new Polygon(xpoints, ypoints, 3);

        // vertical rectangle
        sShapes[8] = new Rectangle2D.Double(-delta / 2, -delta, size / 2, size);

        // left-pointing triangle
        xpoints = intArray(-delta, delta, delta);
        ypoints = intArray(0.0, -delta, +delta);
        sShapes[9] = new Polygon(xpoints, ypoints, 3);
    }

    static {
        createShapes();
    }

    /**
     * Class for storing data of a clicked point in a series.
     */
    private class ClickData {

        private final SimpleDateFormat sDTFormat = new SimpleDateFormat("HH:mm:ss.SSS");

        public ClickData() {
        }

        public ClickData(long time, TimeSeriesWithStats timeSeries, double yValue) {
            mTime = time;
            mTimeSeries = timeSeries;
            mYValue = yValue;
        }

        public String formatTime() {
            return sDTFormat.format(mTime);
        }

        public TimeSeriesWithStats getSeries() {
            return mTimeSeries;
        }

        public long mTime = -1;
        public TimeSeriesWithStats mTimeSeries;
        public double mYValue;
    }


    /*------ Object members and methods ------*/

    public interface ChartPanelListener {
        void onChartClicked(SyncedChartPanel panel);
        void onChartDoubleClicked(long clickedTime);
        void onStatsChanged(String info);
        void onDataPointClicked(String info);
    };

    private ChartPanelListener mChartPanelListener;

    public void setChartPanelListener(ChartPanelListener listener) {
        mChartPanelListener = listener;
    }

    // Identifier/index for series in the plot. Incremented as we add
    // series to the plot.
    private int mSeriesIndex;

    private Object mChartLock = new Object();     // Lock object for access to the chart
    private int mId;
    private JPanel mOuterPanel; // includes the scrollbar
    private ChartScrollBar mChartScrollBar;
    private ClickData mPrevClickData = new ClickData();

    JPanel getOuterPanel() {return mOuterPanel;};
    int getId() {return mId;}

    /**
     * Constructor.
     *
     * @param id
     * @param chart
     *
     * @return
     */
    SyncedChartPanel(int id, JFreeChart chart, boolean shapesInCharts) {
        super(chart);
        mId = id;
        setShapesInChart(shapesInCharts);
        addChartMouseListener(this);

        mOuterPanel = new JPanel();
        mChartScrollBar = new ChartScrollBar(chart.getXYPlot());
        BoxLayout centerLayout = new BoxLayout(mOuterPanel, BoxLayout.PAGE_AXIS);
        mOuterPanel.setLayout(centerLayout);
        mOuterPanel.add(this);
        mOuterPanel.add(mChartScrollBar);

        if (logdog.DEBUG) {
            Logger.log("SyncedChartPanel.SyncedChartPanel() leaving");
        }
    }

    @Override
    public void paint(Graphics graphics) {
        if (logdog.DEBUG) {
            Logger.log("SyncedChartPanel.paint() entering");
        }
        synchronized (mChartLock) {
            super.paint(graphics);
        }
        if (logdog.DEBUG) {
            Logger.log("SyncedChartPanel.paint() leaving");
        }
    }

    /**
     * Create a new dataset with *one* series. Set various properties
     * for the two axis and then add the dataset to the plot.
     *
     * @param name Identifies this series, used in the legend.
     * @param unit
     * @param format
     * @param event If true, plot points are not interconnected with lines
     * @param standardScale
     * @param includerange
     * @param includeZero
     * @param llmMgr
     *
     * @return
     */
    public TimeSeries addSeries(String name, String unit, String format,
                                boolean standardScale, Range range, boolean includeZero,
                                LogLineMatcher llm) {
        TimeSeriesWithStats series = new TimeSeriesWithStats(name);
        LogLineMatcherManager llmMgr = llm.getLLMMgr();
        if (llmMgr.useTimeDuration()) {
            series.setMaximumItemAge(llmMgr.getTimeDuration() * 60 * 1000);  // ms
        } else if (llmMgr.useCountDuration()) {
            series.setMaximumItemCount(llmMgr.getCountDuration());
        } else {
            series.setMaximumItemAge(PLOT_MAX_ITEM_AGE);
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        Color axisColor = sColors[mSeriesIndex % sColors.length];

        // Create Y-axis.
        NumberAxis axis = null;
        if (standardScale) {
            axis = createAxis(unit, range, includeZero, axisColor);
            if (Utils.emptyString(format)) {
                format = "0.00";
            }
        } else {
            // This is for LLMs having no regexp groups defined. Since we always plot either
            // 0 or 1 we use another scale.
            axis = createAxis(unit, range, true, axisColor);
            axis.setTickUnit(new NumberTickUnit(1.0));
            format = "0";
        }

        NumberFormat rangeAxisFormat = DecimalFormat.getInstance();
        if (rangeAxisFormat instanceof DecimalFormat) {
            DecimalFormat decimalFormat = (DecimalFormat) rangeAxisFormat;
            decimalFormat.applyPattern(format);
            axis.setNumberFormatOverride(decimalFormat);
        }

        synchronized (mChartLock) {
            XYPlot plot = getChart().getXYPlot();

            plot.setRangeAxis(mSeriesIndex, axis);
            plot.setRangeAxisLocation(mSeriesIndex, AxisLocation.BOTTOM_OR_RIGHT);

            // To be able to support hiding of series without also
            // hiding the corresponding legend item, we must override
            // the renderer. This code is copied and modified somewhat from
            // XYLineAndShapeRenderer.getLegendItem().
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(!llm.isEvent(), false) {
                @Override
                public LegendItem getLegendItem(int datasetIndex, int series) {
                    XYPlot plot = getPlot();
                    if (plot == null) {
                        return null;
                    }

                    XYDataset dataset = plot.getDataset(datasetIndex);
                    if (dataset == null) {
                        return null;
                    }

                    // Commented
                    // if (!getItemVisible(series, 0)) {
                    //     return null;
                    // }
                    String label = getLegendItemLabelGenerator().generateLabel(dataset,
                                                                               series);
                    String description = label;
                    String toolTipText = null;
                    if (getLegendItemToolTipGenerator() != null) {
                        toolTipText = getLegendItemToolTipGenerator().generateLabel(
                            dataset, series);
                    }
                    String urlText = null;
                    if (getLegendItemURLGenerator() != null) {
                        urlText = getLegendItemURLGenerator().generateLabel(dataset,
                                                                            series);
                    }
                    boolean shapeIsVisible = getItemShapeVisible(series, 0);
                    Shape shape = lookupLegendShape(series);
                    boolean shapeIsFilled = getItemShapeFilled(series, 0);
                    Paint fillPaint = (this.getUseFillPaint() ? lookupSeriesFillPaint(series)
                                       : lookupSeriesPaint(series));
                    boolean shapeOutlineVisible = this.getDrawOutlines();
                    Paint outlinePaint = (this.getUseOutlinePaint() ? lookupSeriesOutlinePaint(
                                              series) : lookupSeriesPaint(series));
                    Stroke outlineStroke = lookupSeriesOutlineStroke(series);
                    boolean lineVisible = getItemLineVisible(series, 0);
                    Stroke lineStroke = lookupSeriesStroke(series);
                    Paint linePaint = lookupSeriesPaint(series);
                    LegendItem result = new LegendItem(label, description, toolTipText,
                                                       urlText, shapeIsVisible, shape, shapeIsFilled, fillPaint,
                                                       shapeOutlineVisible, outlinePaint, outlineStroke, lineVisible,
                                                       this.getLegendLine(), lineStroke, linePaint);
                    result.setLabelFont(lookupLegendTextFont(series));
                    Paint labelPaint = lookupLegendTextPaint(series);
                    if (labelPaint != null) {
                        result.setLabelPaint(labelPaint);
                    }
                    result.setSeriesKey(dataset.getSeriesKey(series));
                    result.setSeriesIndex(series);
                    result.setDataset(dataset);
                    result.setDatasetIndex(datasetIndex);

                    return result;
                }
            };
            final int seriesNumber = 0;  // always 0 since renderer is used for only one series
            renderer.setSeriesPaint(seriesNumber, axisColor);
            renderer.setBaseShapesVisible(mShapesInChart);
            renderer.setBaseShapesFilled(false);
            renderer.setDrawSeriesLineAsPath(true);
            renderer.setSeriesShape(seriesNumber, sShapes[mSeriesIndex % SHAPE_COUNT]);
            renderer.setSeriesStroke(seriesNumber, new BasicStroke(0.6f));

            // It seems creating an advanced stroke sometimes causes deadlock in the paint logic (?)
            //renderer.setSeriesStroke(seriesNumber, createStroke("dash"));

            // renderer.setSeriesToolTipGenerator(seriesNumber, new XYToolTipGenerator() {
            //         @Override
            //         public String generateToolTip(XYDataset arg0, int arg1, int arg2) {
            //             return mTimeDiffToolTipText;
            //         }
            //     });
            plot.setRenderer(mSeriesIndex, renderer);
            plot.setDataset(mSeriesIndex, dataset);
            plot.mapDatasetToRangeAxis(mSeriesIndex, mSeriesIndex);
            plot.mapDatasetToDomainAxis(mSeriesIndex, 0);

            // Set the font of the legend. The legend is created for the first dataset.
            if (mSeriesIndex++ == 0) {
                LegendTitle legendTitle = getChart().getLegend();
                legendTitle.setItemFont(sFontPlain);
                legendTitle.setNotify(true);
            }

            mChartScrollBar.setAsListener();
        }

        return series;
    }

    private NumberAxis createAxis(String label, final Range range, boolean includeZero, Color color) {
        NumberAxis numberAxis = new NumberAxis() {
            /**
             * We come here when unzooming. We need to restore
             * the range to prevent auto ranging which is the
             * default behavor.
             */
            @Override
            protected void autoAdjustRange() {
                if (range != null) {
                    setRange(range);
                } else {
                    super.autoAdjustRange();
                }
            }
        };

        if (range != null) {
            numberAxis.setRange(range);
            numberAxis.setAutoRange(false);
            numberAxis.setAutoRangeIncludesZero(false);
        } else {
            numberAxis.setAutoRangeIncludesZero(includeZero);
            numberAxis.setAutoRange(true);
        }
        numberAxis.setLabelFont(sFontBold);
        numberAxis.setLabel(label);
        numberAxis.setLabelPaint(color);
        numberAxis.setTickLabelFont(sFontPlain);
        numberAxis.setTickLabelPaint(color);

        return numberAxis;
    }

    void setSeriesVisibility(boolean visible) {
        XYPlot plot = getChart().getXYPlot();
        int datasetCount = plot.getDatasetCount();
        for (int datasetIndex = 0; datasetIndex < datasetCount; ++datasetIndex) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer(datasetIndex);
            int seriesIndex = 0;
            renderer.setSeriesVisible(seriesIndex, visible, true);
            renderer.setLegendTextPaint(seriesIndex, visible ? sLegendTextColor : sLegendDisabledTextColor);
            ValueAxis axis = plot.getRangeAxis(datasetIndex);
            axis.setVisible(visible);
        }
    }

    //// ChartMouseListener
    @Override
    public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {
        if (logdog.DEBUG) {
            Logger.log("SyncedChartPanel.chartMouseClicked() entering");
        }

        // Notify ChartView that we are the most recently clicked
        // SyncedChartPanel.
        if (mChartPanelListener != null) {
            mChartPanelListener.onChartClicked(this);
        }

        ChartEntity entity = chartMouseEvent.getEntity();

        // Handle click on legend to hide or show a series.
        if (isClickOnLegend(entity)) {
            return;
        }

        // If double-click then notify others about the selection
        // (i.e. LogSourceViews so they can scroll to the selected
        // time).
        ClickData clickData = getClickData(entity);
        if (clickData == null) {
            mPrevClickData = null;
            if (mChartPanelListener != null) {
                mChartPanelListener.onDataPointClicked("");
                mChartPanelListener.onStatsChanged("");
            }
            return;
        }
        if (logdog.DEBUG) {
            Logger.log(String.format("SyncedChartPanel.chartMouseClicked: %d", clickData.mTime));
        }

        MouseEvent mouseEvent = chartMouseEvent.getTrigger();
        int clickCount = mouseEvent.getClickCount();

        String datapointInfo = null;
        String statsInfo = null;

        // Show diff in time from previously clicked time if any:
        if (clickCount == 1) {
            if (mPrevClickData == null) {
                datapointInfo = String.format("Time=%s Value=%.2f  ",
                                              clickData.formatTime(), clickData.mYValue);
            } else {
                long timeDiff = clickData.mTime - mPrevClickData.mTime;
                String timeText = "";
                if (timeDiff > -1000 && timeDiff < 1000) {
                    timeText = String.format("%dms", timeDiff);
                } else {
                    int negative = timeDiff < 0 ? -1 : 1;
                    timeText = String.format("%s.%03ds", timeDiff / 1000, negative * timeDiff % 1000);
                }
                String allTimeText = String.format("Time diff=%s (%s - %s)",
                                                   timeText, clickData.formatTime(),
                                                   mPrevClickData.formatTime());

                String valueText = "  ";
                if (clickData.mTimeSeries == mPrevClickData.mTimeSeries) {
                    valueText = String.format("  Value diff=%.2f  (%.2f - %.2f)  ",
                                              clickData.mYValue - mPrevClickData.mYValue,
                                              clickData.mYValue, mPrevClickData.mYValue);
                }

                datapointInfo = allTimeText + valueText;
                if (logdog.DEBUG) {
                    Logger.log("SyncedChartPanel.timeText: " + timeText);
                }
            }

            statsInfo = clickData.getSeries().toString();
            mPrevClickData = clickData;
        } else if (clickCount == 2) {
            mPrevClickData = null;
            datapointInfo = "";
            statsInfo = "";
            if (mChartPanelListener != null) {
                mChartPanelListener.onChartDoubleClicked(clickData.mTime);
            }
        }

        if (mChartPanelListener != null) {
            mChartPanelListener.onDataPointClicked(datapointInfo);
            mChartPanelListener.onStatsChanged(statsInfo);
        }
        mouseEvent.consume();
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent mouseEvent) {
    }

    private boolean isClickOnLegend(ChartEntity entity) {
        if (entity instanceof LegendItemEntity) {
            LegendItemEntity itemEntity = (LegendItemEntity) entity;
            Comparable<?> key = itemEntity.getSeriesKey();
            TimeSeriesCollection dataset = (TimeSeriesCollection) itemEntity.getDataset();
            int seriesIndex = dataset.indexOf(key);
            XYPlot plot = getChart().getXYPlot();
            int datasetIndex = plot.indexOf(dataset);
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer(datasetIndex);

            LegendItemCollection coll = renderer.getLegendItems();
            LegendItem legendItem = coll.get(seriesIndex);
            boolean shapesVisible = legendItem.isShapeVisible();

            boolean seriesVisible = renderer.isSeriesVisible(seriesIndex);
            ValueAxis axis = plot.getRangeAxis(datasetIndex);
            if (seriesVisible) {
                if (shapesVisible) {
                    renderer.setSeriesShapesVisible(seriesIndex, false);
                    legendItem.setShapeVisible(false);
                } else {
                    // Hide case
                    renderer.setSeriesVisible(seriesIndex, false, false);
                    renderer.setLegendTextPaint(seriesIndex, sLegendDisabledTextColor);
                    axis.setVisible(false);
                }
            } else {
                if (mShapesInChart) {
                    renderer.setSeriesShapesVisible(seriesIndex, true);
                }
                renderer.setSeriesVisible(seriesIndex, true, false);
                renderer.setLegendTextPaint(seriesIndex, sLegendTextColor);
                axis.setVisible(true);
            }

            return true;
        }

        return false;
    }

    private ClickData getClickData(ChartEntity entity) {
        if (entity instanceof XYItemEntity) {
            XYItemEntity xyEntity = (XYItemEntity) entity;
            TimeSeriesCollection dataset = (TimeSeriesCollection) xyEntity.getDataset();
            int series = xyEntity.getSeriesIndex();
            int item = xyEntity.getItem();
            double yValue = dataset.getYValue(series, item);
            long time = (long) dataset.getXValue(series, item);
            return new ClickData(time, (TimeSeriesWithStats) dataset.getSeries(series), yValue);
        }
        return null;
    }

    void clear() {
        synchronized (mChartLock) {
            XYPlot plot = (XYPlot) getChart().getPlot();
            for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); ++datasetIndex) {
                TimeSeriesCollection dataset = (TimeSeriesCollection) plot.getDataset(datasetIndex);
                if (dataset != null) {
                    for (int seriesIndex = 0; seriesIndex < dataset.getSeriesCount(); ++seriesIndex) {
                        TimeSeriesWithStats ts = (TimeSeriesWithStats) dataset.getSeries(seriesIndex);
                        ts.clear();
                    }
                }
            }
            // Remove any zoom
            restoreAutoBounds();
            mChartScrollBar.reset();
        }
    }
}
