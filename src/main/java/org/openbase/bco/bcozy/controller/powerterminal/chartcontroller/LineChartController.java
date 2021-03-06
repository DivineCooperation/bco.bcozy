package org.openbase.bco.bcozy.controller.powerterminal.chartcontroller;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.chart.ChartData;
import javafx.scene.chart.XYChart;
import org.openbase.bco.bcozy.controller.powerterminal.PowerChartVisualizationController;
import org.openbase.bco.bcozy.controller.powerterminal.chartattributes.DateRange;
import org.openbase.bco.bcozy.controller.powerterminal.chartattributes.VisualizationType;
import org.openbase.bco.bcozy.model.LanguageSelection;
import org.openbase.bco.bcozy.model.powerterminal.ChartStateModel;

import java.util.List;

public class LineChartController extends TilesFxChartController {

    PowerChartVisualizationController parentController;

    @Override
    public void init(ChartStateModel chartStateModel, PowerChartVisualizationController powerChartVisualizationController) {
        DateRange dateRange = chartStateModel.getDateRange();
        setupView(LanguageSelection.getLocalized(POWERTERMINAL_CHART_HEADER_IDENTIFIER),
                Tile.SkinType.SMOOTHED_CHART, LanguageSelection.getProperty(dateRange.getDefaultIntervalSize().name()));
        parentController = powerChartVisualizationController;
    }

    @Override
    public void updateChart(List<ChartData> data) {
        if (parentController.getChartStateModel().getVisualizationType() != VisualizationType.LINE_CHART) {//Preventing race conditions
            return;
        }
        XYChart.Series<String, Number> series = new XYChart.Series();
        for (ChartData datum : data) {
            series.getData().add(new XYChart.Data(datum.getName(), datum.getValue()));
        }

        parentController.getPane().getChildren().clear();
        parentController.getPane().getChildren().add(
                TileBuilder.create()
                        .skinType(Tile.SkinType.SMOOTHED_CHART)
                        .prefSize(TILE_WIDTH, TILE_HEIGHT)
                        .title(LanguageSelection.getLocalized(POWERTERMINAL_CHART_HEADER_IDENTIFIER))
                        .smoothing(false)
                        .series(series)
                        .build());
    }
}
