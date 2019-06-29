package org.openbase.bco.bcozy.controller.powerterminal;

import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.MatrixPane;
import eu.hansolo.fx.charts.PixelMatrix;
import eu.hansolo.fx.charts.heatmap.HeatMap;
import eu.hansolo.fx.charts.heatmap.OpacityDistribution;
import eu.hansolo.fx.charts.series.MatrixItemSeries;
import eu.hansolo.fx.charts.tools.ColorMapping;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.chart.ChartData;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import org.influxdata.query.FluxRecord;
import org.influxdata.query.FluxTable;
import org.openbase.bco.bcozy.controller.powerterminal.chartattributes.DateRange;
import org.openbase.bco.bcozy.controller.powerterminal.chartattributes.VisualizationType;
import org.openbase.bco.bcozy.model.InfluxDBHandler;
import org.openbase.bco.bcozy.model.powerterminal.ChartStateModel;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.visual.javafx.control.AbstractFXController;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


import eu.hansolo.fx.charts.data.MatrixChartItem;


public class PowerChartVisualizationController extends AbstractFXController {

    public static final VisualizationType DEFAULT_VISUALISATION_TYPE = VisualizationType.BAR;

    @FXML
    FlowGridPane pane;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PowerChartVisualizationController.class);

    public static final String WEBENGINE_ALERT_MESSAGE = "Webengine alert detected!";
    public static final String WEBENGINE_ERROR_MESSAGE = "Webengine error detected!";
//    public static String CHRONOGRAPH_URL = "http://192.168.75.100:9999/orgs/03e2c6b79272c000/dashboards/03e529b61ff2c000?lower=now%28%29%20-%2024h";
    public static String CHRONOGRAPH_URL = "http://localhost:9999";
    public static final int TILE_WIDTH = (int) Screen.getPrimary().getVisualBounds().getWidth();
    public static final int TILE_HEIGHT = (int) Screen.getPrimary().getVisualBounds().getHeight();
    private static final int REFRESH_TIMEOUT_MINUTES = 1;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerChartVisualizationController.class);


    String unit;

    private int dataStep;

    private boolean firstRun = true;

    private WebEngine webEngine;
    private Future task;

    Timestamp olddataTime;
    private ChartStateModel chartStateModel;

    VisualizationType visualizationType;


    public PowerChartVisualizationController() {
        this.unit = "Watt";
        this.dataStep = 10;
    }

    @Override
    public void updateDynamicContent() throws CouldNotPerformException {

    }

    @Override
    public void initContent() throws InitializationException {
        pane.setMinSize(Screen.getPrimary().getVisualBounds().getWidth(),Screen.getPrimary().getVisualBounds().getHeight() - 600);
        setChartType(DEFAULT_VISUALISATION_TYPE);
        this.visualizationType = DEFAULT_VISUALISATION_TYPE;
    }

    private MatrixPane<MatrixChartItem> generateHeatMapOld (double[][] u, int runnings) {
        calculateHeatMap(u, runnings);

       List<MatrixChartItem> matrixData = new ArrayList<>();
       for (int col = 0; col < u.length; col++) {
            for (int row = 0; row < u[col].length; row++) {
                        matrixData.add(new MatrixChartItem(col, row, u[col][row]));
            }
        }

        MatrixPane<MatrixChartItem> matrixHeatMap =
                new MatrixPane(new MatrixItemSeries(matrixData, ChartType.MATRIX_HEATMAP));
        matrixHeatMap.getMatrix().setPixelShape(PixelMatrix.PixelShape.SQUARE);

        matrixHeatMap.getMatrix().setColsAndRows(u.length,u[0].length);
        matrixHeatMap.setPrefSize(TILE_WIDTH/2, TILE_HEIGHT);
        matrixHeatMap.setMaxSize(TILE_WIDTH/2, TILE_HEIGHT);

        return matrixHeatMap;
    }

    private HeatMap generateHeatmapNew(double[][] u, int runnings) {
        calculateHeatMap(u, runnings);
        double value = u[5][5];

        Image bla = createEventImage(value);

        HeatMap heatMap = new HeatMap();
        heatMap.setSize(TILE_WIDTH/2, TILE_HEIGHT);
        heatMap.addSpot(50,60, bla, 0, 0);

        return heatMap;
    }

    public Image createEventImage(double value) {
        int radius = 40;

        Stop[] stops = new Stop[11];
        for (int i = 0; i < 11; i++) {
            double opacity = value - (double) i/10;
            if (opacity < 0)
                opacity = 0;
            stops[i] = new Stop(i * 0.1, Color.rgb(255, 255, 255, opacity));
        }

        int size = (int) (radius * 2);
        WritableImage raster        = new WritableImage(size, size);
        PixelWriter pixelWriter   = raster.getPixelWriter();
        double        maxDistFactor = 1 / radius;
        Color pixelColor;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double deltaX   = radius - x;
                double deltaY   = radius - y;
                double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
                double fraction = maxDistFactor * distance;
                for (int i = 0; i < 10; i++) {
                    if (Double.compare(fraction, stops[i].getOffset()) >= 0 && Double.compare(fraction, stops[i + 1].getOffset()) <= 0) {
                        pixelColor = (Color) Interpolator.LINEAR.interpolate(stops[i].getColor(), stops[i + 1].getColor(), (fraction - stops[i].getOffset()) / 0.1);
                        pixelWriter.setColor(x, y, pixelColor);
                        break;
                    }
                }
            }
        }
        return raster;
    }

    private void calculateHeatMap (double[][] u, int runnings) {
        //h = Gitteraufloesung
        //CFL Bedingung: delta_t/h^2 <= 1/4
        double h = 1;
        double delta_t = 0.1;
        double[][] v = new double[u.length][u[0].length];
        for (int runs = 0; runs < runnings; runs ++) {
            double biggestData = 0;
            for (int col = 1; col < u.length - 1; col++) {
                for (int row = 1; row < u[col].length - 1; row++) {
                    v[col][row] = (u[col - 1][row] + u[col + 1][row] + u[col][row - 1] + u[col][row + 1] - 4*u[col][row]) / (h * h);
                    if (v[col][row] > biggestData)
                        biggestData = v[col][row];
                }
            }
            for (int col = 0; col < u.length; col++) {
                for (int row = 0; row < u[col].length; row++) {
                    u[col][row] = u[col][row] + delta_t * v[col][row];
                }
            }
        }
    }

    private WebView generateWebView() {
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setOnAlert((WebEvent<String> event) -> {
            ExceptionPrinter.printHistory(new InvalidStateException(WEBENGINE_ALERT_MESSAGE, new CouldNotPerformException(event.toString())), logger);
        });
        webEngine.setOnError((WebErrorEvent event) -> {
            ExceptionPrinter.printHistory(new InvalidStateException(WEBENGINE_ERROR_MESSAGE, new CouldNotPerformException(event.toString())), logger);
        });
        task = GlobalCachedExecutorService.submit(() -> {
            HttpURLConnection connection = null;
            try {
                URL myurl = new URL(CHRONOGRAPH_URL);
                connection = (HttpURLConnection) myurl.openConnection();
                connection.setRequestMethod("HEAD");
                int code = connection.getResponseCode();
            } catch (MalformedURLException e) {
                CHRONOGRAPH_URL = "https://www.google.com/";
            } catch (IOException e) {
                CHRONOGRAPH_URL = "https://www.google.com/";
            }
            Platform.runLater(() -> {
                webEngine.load(CHRONOGRAPH_URL);
            });
        });
        webView.setMaxHeight(TILE_HEIGHT);
        webView.setMaxWidth(TILE_WIDTH/2);
        webView.setMinHeight(TILE_HEIGHT/2 + TILE_HEIGHT/4);
        webView.setMinWidth(TILE_WIDTH/2 );
        return webView;
    }


    /**
     * Depending on the skinType the correct DataType is added to the Chart
     */
    private void addCorrectDataType(Tile.SkinType skinType, Tile chart, List<ChartData> data) {
        XYChart.Series<String, Number> series = new XYChart.Series();

        switch (skinType) {
            case MATRIX:
                chart.setAnimated(true);
                chart.setChartData(data);
                //The Matrix skinType does not show any data if they are not updated (Bug in tilesfx)
                if (skinType.equals(Tile.SkinType.MATRIX)) {
                    GlobalScheduledExecutorService.execute(() -> {
                        for (ChartData datum : data) {
                            datum.setValue(datum.getValue());
                        }
                    });
                }
                break;
            case SMOOTHED_CHART:
                for (ChartData datum : data) {
                    series.getData().add(new XYChart.Data(datum.getName(), datum.getValue()));
                }
                chart.addSeries(series);
                break;
        }
        chart.setSkinType(skinType);
    }


    /**
     * Refreshes all data every Minute
     * @param refreshTimeout
     * @param data
     */
    private void enableDataRefresh(int refreshTimeout, List<ChartData> data) {
        try {
            GlobalScheduledExecutorService.scheduleAtFixedRate(() -> {
                Platform.runLater(() -> {
                    setChartType(this.visualizationType);
                });
            }, 1, refreshTimeout, TimeUnit.MINUTES);
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not refresh power chart data", ex, LOGGER);
        }
    }


    /**
     * Initializes the previous energy consumption
     *
     * @return List of ChartData with previous Energy Consumption
     */
    private List<ChartData> initializePreviousEntries(String interval, long startTime, long endTime) {
        List<ChartData> datas = new ArrayList<ChartData>();
        int change = 0;
        olddataTime = new Timestamp(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        try {
            List<FluxTable> energy = InfluxDBHandler.getAveragePowerConsumptionTables(
                    interval, TimeUnit.MILLISECONDS.toSeconds(startTime), TimeUnit.MILLISECONDS.toSeconds(endTime), "consumption");
            for (FluxTable fluxTable : energy) {
                List<FluxRecord> records = fluxTable.getRecords();
                for (FluxRecord fluxRecord : records) {
                    if (fluxRecord.getValueByKey("_value") == null) {
                        ChartData temp = new ChartData(String.valueOf(change), 0, Tile.ORANGE);
                        temp.setName(String.valueOf(change));
                        datas.add(temp);
                    } else {
                        ChartData temp = new ChartData(String.valueOf(change), (double) fluxRecord.getValueByKey("_value") / dataStep, Tile.ORANGE);
                        temp.setName(String.valueOf(change));
                        datas.add(temp);
                    }

                }
                change++;
            }
        } catch (CouldNotPerformException e) {
            e.printStackTrace();
        }
        return datas;
    }

    /**
     * Connects the given chart attribute properties to the chart by creating listeners incorporating the changes
     * into the chart
     * @param chartStateModel StateModel that describes the state of the chart as configured by other panes
     */
    public void initChartState(ChartStateModel chartStateModel) {

        this.chartStateModel = chartStateModel;

        chartStateModel.visualizationTypeProperty().addListener(
                (ChangeListener<? super VisualizationType>) (dont, care, newVisualizationType) -> {
                    this.visualizationType = newVisualizationType;
                    setChartType(newVisualizationType);
                });

        chartStateModel.dateRangeProperty().addListener((ChangeListener<? super DateRange>) (dont, care, newDateRange) -> {
            System.out.println("===========> Date Picked!");
            setChartType(this.visualizationType);
        });

    }

    private void setChartType(VisualizationType newVisualizationType) {
        pane.getChildren().clear();
        Node node;
        if (newVisualizationType == VisualizationType.WEBVIEW) {
            double[][] datas = new double[30][30];
            for (int i = 0; i < 30; i++)
                for (int j = 0; j < 30; j++) {
                    if (i == 5 && j == 5)
                        datas[i][j] = 1;
                    else if (i == 20 && j == 25)
                        datas[i][j] = 0.8;
                }
            node = generateHeatmapNew(datas, 3);
            //node = generateWebView();
        } else {
            if (chartStateModel == null) {
                node = generateTilesFxChart(newVisualizationType);
            } else {
                node = generateTilesFxChart(newVisualizationType, chartStateModel.getDateRange());
            }
        }
        pane.getChildren().add(node);
    }

    private Tile generateTilesFxChart(VisualizationType newVisualizationType) {
        LocalDate endTime = LocalDate.now();
        LocalDate startTime = LocalDate.now().minus(Period.of(0,0,1));
        DateRange defaultDateRange = new DateRange(startTime, endTime);
        return generateTilesFxChart(newVisualizationType, defaultDateRange);
    }

    private Tile generateTilesFxChart(VisualizationType visualizationType, DateRange dateRange) {
        if (visualizationType == VisualizationType.WEBVIEW) return null;
        Tile chart = new Tile();
        chart.setPrefSize(TILE_WIDTH, TILE_HEIGHT);
        Tile.SkinType skinType = visualizationType == VisualizationType.BAR ?
                Tile.SkinType.MATRIX : Tile.SkinType.SMOOTHED_CHART;//TODO there are also other chart types!
        chart.setTextAlignment(TextAlignment.RIGHT);

        String interval = dateRange.getDefaultIntervalSize();

        String duration = "?";
        switch (interval) {
            case "30d":
                duration = "month";
                break;
            case "1w":
                duration = "week";
                break;
            case "1d":
                duration = "day";
                break;
            case "1h":
                duration = "hour";
                break;
            case "1m":
                duration = "minute";
                break;

        }
        chart.setTitle("Average Consumption in " + unit + "/" + dataStep + " per " + duration);
        chart.setText(duration);

        System.out.println("Interval String is: " + interval);
        System.out.println("Start time is " + dateRange.getStartDate().toString() + ", as Timestamp it is " + dateRange.getStartDateAtCurrentTime().getTime());
        System.out.println("End time is " + dateRange.getEndDate().toString() + ", as Timestamp it is " + dateRange.getEndDateAtCurrentTime().getTime());
        List<ChartData> data = initializePreviousEntries(interval, dateRange.getStartDateAtCurrentTime().getTime(), dateRange.getEndDateAtCurrentTime().getTime());
        addCorrectDataType(skinType, chart, data);
        if(firstRun) {//TODO: Replace quick workaround with more beautiful solution
            enableDataRefresh(REFRESH_TIMEOUT_MINUTES, data);
            firstRun = false;
        }
        return chart;
    }

}