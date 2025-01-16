package abruzese.gui;

import abruzese.graph.Graph;
import abruzese.graph.edges.Road;
import abruzese.graph.vertices.Intersection;

import java.awt.*;
import java.awt.geom.Point2D;

public class MapCoordinateSystem {
    private final Graph<Intersection, Road> streetGraph;

    // Bounds of the map
    private double minLat = Double.MAX_VALUE, maxLat = Double.MIN_VALUE;
    private double minLon = Double.MAX_VALUE, maxLon = Double.MIN_VALUE;

    // View state
    private double scale = 1.0;
    private double bufferScale = 1.0;
    private Point2D.Double center;
    private static final double MIN_ZOOM = 1;
    private static final int BUFFER_PADDING = 2000;

    // Initial state
    private final Point2D.Double initialCenter;
    private final double initialScale;

    // Quality thresholds for buffer updates
    private static final double MIN_QUALITY_THRESHOLD = 0.5;
    private static final double MAX_QUALITY_THRESHOLD = 2.0;

    public MapCoordinateSystem(Graph<Intersection, Road> streetGraph) {
        this.streetGraph = streetGraph;
        calculateBounds();

        // Initialize center and store initial values
        center = new Point2D.Double(
                (maxLon + minLon) / 2,
                (maxLat + minLat) / 2
        );
        initialCenter = new Point2D.Double(center.x, center.y);
        initialScale = scale;
    }

    private void calculateBounds() {
        // First find the actual min/max values
        double actualMinLat = Double.MAX_VALUE;
        double actualMaxLat = -Double.MAX_VALUE;
        double actualMinLon = Double.MAX_VALUE;
        double actualMaxLon = -Double.MAX_VALUE;

        for (Intersection intersection : streetGraph) {
            actualMinLat = Math.min(actualMinLat, intersection.latitude());
            actualMaxLat = Math.max(actualMaxLat, intersection.latitude());
            actualMinLon = Math.min(actualMinLon, intersection.longitude());
            actualMaxLon = Math.max(actualMaxLon, intersection.longitude());
        }

        // Calculate the ranges
        double latRange = actualMaxLat - actualMinLat;
        double lonRange = actualMaxLon - actualMinLon;

        // Use the larger range to ensure square proportions
        double maxRange = Math.max(latRange, lonRange);
        maxRange *= 1.1; // Add a 10% buffer

        // Calculate the center points
        double centerLat = (actualMaxLat + actualMinLat) / 2;
        double centerLon = (actualMaxLon + actualMinLon) / 2;

        // Set the bounds to maintain square proportions
        minLat = centerLat - maxRange/2;
        maxLat = centerLat + maxRange/2;
        minLon = centerLon - maxRange/2;
        maxLon = centerLon + maxRange/2;
    }

    public Point geoToBuffer(double lon, double lat) {
        // Convert geographic coordinates to buffer coordinates
        double geoWidth = (maxLon - minLon) / scale;
        double geoHeight = (maxLat - minLat) / scale;

        int bufferWidth = getBufferWidth() - 2 * BUFFER_PADDING;
        int bufferHeight = getBufferHeight() - 2 * BUFFER_PADDING;

        int x = (int)((lon - (center.x - geoWidth/2)) * bufferWidth / geoWidth) + BUFFER_PADDING;
        int y = (int)(((center.y + geoHeight/2) - lat) * bufferHeight / geoHeight) + BUFFER_PADDING;

        return new Point(x, y);
    }

    public double[] bufferToGeo(int bufferX, int bufferY) {
        double geoWidth = (maxLon - minLon) / scale;
        double geoHeight = (maxLat - minLat) / scale;

        int bufferWidth = getBufferWidth() - 2 * BUFFER_PADDING;
        int bufferHeight = getBufferHeight() - 2 * BUFFER_PADDING;

        double lon = ((bufferX - BUFFER_PADDING) * geoWidth / bufferWidth) + (center.x - geoWidth/2);
        double lat = (center.y + geoHeight/2) - ((bufferY - BUFFER_PADDING) * geoHeight / bufferHeight);

        return new double[]{lon, lat};
    }

    public void zoom(double factor, Point viewportCenter, Rectangle viewport) {
        double newScale = scale * factor;

        // Check max zoom constraint
        if (newScale < MIN_ZOOM) {
            newScale = MIN_ZOOM;
            factor = MIN_ZOOM / scale;
            if (factor == 1.0) return;
        }

        // Get the current viewport center in geographic coordinates
        double[] centerGeo = bufferToGeo(viewportCenter.x, viewportCenter.y);

        // Store the old scale for ratio calculations
        scale = newScale;

        boolean needNewBuffer = bufferScale / scale < MIN_QUALITY_THRESHOLD ||
                bufferScale / scale > MAX_QUALITY_THRESHOLD;

        if (needNewBuffer) {
            // Update the map center to the viewport center
            center.x = centerGeo[0];
            center.y = centerGeo[1];
            // Update buffer scale
            bufferScale = scale;
        }

        constrainCenter();
    }

    public void constrainCenter() {
        if(scale > 2) return; //this doesn't really work when zoomed in a bunch

        // Calculate the visible geographic width and height
        double geoWidth = (maxLon - minLon) / scale * 0.95;
        double geoHeight = (maxLat - minLat) / scale * 0.95;

        // Get the original center (midpoint of the bounds)
        double originalCenterX = (maxLon + minLon) / 2;
        double originalCenterY = (maxLat + minLat) / 2;

        // Constrain the center point
        center.x = Math.max(originalCenterX - geoWidth,
                Math.min(originalCenterX + geoWidth, center.x));
        center.y = Math.max(originalCenterY - geoHeight,
                Math.min(originalCenterY + geoHeight, center.y));
    }

    public void updateCenter(double[] newCenter) {
        center.x = newCenter[0];
        center.y = newCenter[1];
        constrainCenter();
    }

    public void resetView() {
        scale = initialScale;
        bufferScale = initialScale;
        center = new Point2D.Double(initialCenter.x, initialCenter.y);
    }

    public boolean needsBufferUpdate(Rectangle viewport) {
        int threshold = BUFFER_PADDING / 2;
        double qualityRatio = bufferScale / scale;
        boolean qualityIssue = qualityRatio < MIN_QUALITY_THRESHOLD ||
                qualityRatio > MAX_QUALITY_THRESHOLD;

        return qualityIssue ||
                viewport.x < threshold ||
                viewport.y < threshold ||
                viewport.x + viewport.width > getBufferWidth() - threshold ||
                viewport.y + viewport.height > getBufferHeight() - threshold;
    }

    // Getters
    public double getScale() { return scale; }
    public double getBufferScale() { return bufferScale; }
    public void setBufferScale(double scale) { this.bufferScale = scale; }
    public Point2D.Double getCenter() { return center; }
    private int getBufferWidth() { return 2 * BUFFER_PADDING; }
    private int getBufferHeight() { return 2 * BUFFER_PADDING; }
}