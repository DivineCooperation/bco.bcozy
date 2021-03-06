package org.openbase.bco.bcozy.controller.powerterminal.heatmapattributes;

import javafx.geometry.Point2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Datatype containing the relevant heatmap values like a list of the locations, a list with the spots,
 * the matrix for calulating the heatmap and the x and y Translation of the locations
 */
public class HeatmapValues {
    private List<Polygon> locations;
    private List<HeatmapSpot> spots;
    private double[][] grid;

    public HeatmapValues(List<List<Point2D>> locationPoints, List<HeatmapSpot> spots, double[][] grid, double xTranslation, double yTranslation) {
        this.locations = generatePolygons(locationPoints, xTranslation, yTranslation);
        this.spots = spots;
        this.grid = grid;
    }

    private List<Polygon> generatePolygons(List<List<Point2D>> locationPoints, double xTranslation, double yTranslation) {
        List<Polygon> locationTemp = new ArrayList<>();

        for (List<Point2D> locationPoint : locationPoints) {
            Polygon temp = new Polygon();
            for (Point2D point2D : locationPoint)
                temp.addPoint((int) (point2D.getY() + yTranslation), (int) (point2D.getX() + xTranslation));
            locationTemp.add(temp);
        }
        return locationTemp;
    }

    public List<Polygon> getLocations() {
        return locations;
    }

    public List<HeatmapSpot> getSpots() {
        return spots;
    }

    public double[][] getGrid() {
        return grid;
    }

    public void setGrid(double[][] uNew) {
        this.grid = uNew;
    }

    public void setSpots(List<HeatmapSpot> spotsNew) {
        this.spots = spotsNew;
    }

    public boolean isInsideLocation(double x, double y, double centerx, double centery) {
        for (Polygon location : locations) {
            if (location.contains(centerx, centery) && location.contains(x, y))
                return true;
        }
        return false;
    }

    public boolean isInsideLocation(double centerX, double centerY) {
        for (Polygon location : locations) {
            if (location.contains(centerX, centerY)) {
                return true;
            }
        }
        return false;
    }
}
