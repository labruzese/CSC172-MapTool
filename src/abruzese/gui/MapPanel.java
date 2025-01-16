package abruzese.gui;

import abruzese.console.CommandHandler;
import abruzese.console.DirectionStep;
import abruzese.graph.Graph;
import abruzese.graph.edges.Road;
import abruzese.graph.vertices.Intersection;
import abruzese.gui.components.Button;
import abruzese.hashtable.HashTable;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class MapPanel extends Canvas {
    // Constants
    private static final double ZOOM_FACTOR = 1.1;
    private static final double MIN_QUALITY_THRESHOLD = 0.5;
    private static final double MAX_QUALITY_THRESHOLD = 2.0;
    private static final int BUFFER_PADDING = 2000;
    private static final double MIN_ZOOM = 1;
    private static final int CLICK_TOLERANCE = 10; // pixels
    private static final float HIGHLIGHT_WIDTH = 4.0f;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 25;
    private static final int BUTTON_PADDING = 10;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 69, 0);

    // Core data structures
    private final Graph<Intersection, Road> streetGraph;
    private final CommandHandler commandHandler;

    // View state
    private double minLat = Double.MAX_VALUE, maxLat = Double.MIN_VALUE;
    private double minLon = Double.MAX_VALUE, maxLon = Double.MIN_VALUE;
    private double actualScale = 1.0;
    private double bufferScale = 1.0;
    private Point2D.Double center;
    private Point lastMousePos;
    private boolean isDragging = false;
    private final Point2D.Double initialCenter;
    private final double initialScale;

    // UI Components
    private BufferedImage mapBuffer;
    private final Rectangle viewport;
    private final Button resetButton;

    // Highlighting
    private java.util.List<DirectionStep> highlightedSteps = null;
    private Intersection highlightedIntersection = null;

    // Constructor
    public MapPanel(Graph<Intersection, Road> graph, CommandHandler commandHandler) {
        this.streetGraph = graph;
        this.commandHandler = commandHandler;

        initializeBounds();
        setupMouseListeners();

        // Initialize center and store initial values
        center = new Point2D.Double(
                (maxLon + minLon) / 2,
                (maxLat + minLat) / 2
        );
        initialCenter = new Point2D.Double(center.x, center.y);
        initialScale = actualScale;

        viewport = new Rectangle();
        resetButton = new Button("Reset View");
        updateResetButtonPosition();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateViewport();
                createMapBuffer();
                updateResetButtonPosition();
            }
        });
    }

    // Public Interface Methods
    public void highlightSteps(java.util.List<DirectionStep> steps) {
        this.highlightedSteps = steps;
        createMapBuffer(); // Recreate the buffer with the highlighted path
        repaint();
    }

    public void highlightIntersection(Intersection intersection) {
        this.highlightedIntersection = intersection;
        createMapBuffer();
        repaint();
    }

    public void clearHighlights() {
        this.highlightedSteps = null;
        this.highlightedIntersection = null;
        createMapBuffer();
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        if (mapBuffer == null) {
            createMapBuffer();
            updateViewport();
            bufferScale = actualScale;
        }

        // Draw the map from buffer
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2d.drawImage(mapBuffer,
                0, 0, getWidth(), getHeight(),
                viewport.x, viewport.y, viewport.x + viewport.width, viewport.y + viewport.height,
                null);

        drawScaleBar(g2d);

        resetButton.draw(g2d);
    }

    // Mouse Event Handling
    private void setupMouseListeners() {
        addMouseWheelListener(e -> {
            double zoomFactor = e.getWheelRotation() > 0 ? 1/ZOOM_FACTOR : ZOOM_FACTOR;
            zoom(zoomFactor);
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { // Right click
                    handleRightClick(e.getPoint());
                    return;
                }

                if (resetButton.doMouseEvent(e)) {
                    repaint();
                    return;
                }
                lastMousePos = e.getPoint();
                isDragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (resetButton.doMouseEvent(e)) {
                    resetView();
                    repaint();
                    return;
                }
                isDragging = false;
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    updateMapPosition(e.getPoint());
                    repaint();
                }
            }
        });
    }

    private void handleRightClick(Point clickPoint) {
        if(commandHandler == null) return;

        Point bufferPoint = new Point(
                clickPoint.x + viewport.x,
                clickPoint.y + viewport.y
        );

        // First check for intersections
        Intersection nearestIntersection = findNearestIntersection(bufferPoint);
        if (nearestIntersection != null) {
            commandHandler.echo("\u001B[31m" + "search " + nearestIntersection.intersectionID() + "\u001B[0m");
            commandHandler.handleCommand("search", new String[]{nearestIntersection.intersectionID()});
            return;
        }

        // Then check for roads
        Road nearestRoad = findNearestRoad(bufferPoint);
        if (nearestRoad != null) {
            commandHandler.echo("\u001B[31m" + "search road " + nearestRoad.roadID + "\u001B[0m");
            commandHandler.handleCommand("search", new String[]{"road", nearestRoad.roadID});
        }
    }

    // Map Drawing Methods
    private void createMapBuffer() {
        int width = getWidth() + 2 * BUFFER_PADDING;
        int height = getHeight() + 2 * BUFFER_PADDING;

        if (width <= 0 || height <= 0) return;

        mapBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = mapBuffer.createGraphics();

        try {
            // Set up rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Clear with background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Draw all roads first (in gray)
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1.0f));

            for (HashTable.Entry<Intersection, Intersection> edge : streetGraph.getEdges()) {
                Intersection from = edge.getKey();
                Intersection to = edge.getValue();
                Point fromPoint = geoToBuffer(from.longitude(), from.latitude());
                Point toPoint = geoToBuffer(to.longitude(), to.latitude());
                g2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
            }

            // Draw highlighted path if available
            if (highlightedSteps != null && !highlightedSteps.isEmpty()) {
                // Draw the highlighted path
                g2d.setStroke(new BasicStroke(HIGHLIGHT_WIDTH,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(HIGHLIGHT_COLOR);

                for (DirectionStep step : highlightedSteps) {
                    Point fromPoint = geoToBuffer(step.from.longitude(), step.from.latitude());
                    Point toPoint = geoToBuffer(step.to.longitude(), step.to.latitude());
                    g2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
                }

                // Draw start and end markers
                DirectionStep firstStep = highlightedSteps.getFirst();
                DirectionStep lastStep = highlightedSteps.getLast();

                drawMarker(g2d, firstStep.from, Color.GREEN, "S");  // Start marker
                drawMarker(g2d, lastStep.to, Color.RED, "E");      // End marker

                // Draw step numbers
                g2d.setColor(Color.BLACK);
                for (DirectionStep step : highlightedSteps) {
                    Point midPoint = getMidPoint(
                            geoToBuffer(step.from.longitude(), step.from.latitude()),
                            geoToBuffer(step.to.longitude(), step.to.latitude())
                    );
                    if(highlightedSteps.size() < 20) drawStepNumber(g2d, midPoint, step.stepNumber);
                }
            }

            // Draw intersections
            g2d.setColor(Color.BLACK);
            int dotSize = 4;

            for (Intersection intersection : streetGraph) {
                // Skip numbered intersections unless specifically highlighted
                if (intersection.intersectionID().matches("i\\d+$") &&
                        (!intersection.equals(highlightedIntersection))) {
                    continue;
                }

                Point p = geoToBuffer(intersection.longitude(), intersection.latitude());

                // Highlight the specific intersection if requested
                if (intersection.equals(highlightedIntersection)) {
                    g2d.setColor(HIGHLIGHT_COLOR);
                    g2d.fillOval(p.x - dotSize, p.y - dotSize, dotSize * 2, dotSize * 2);

                    // Draw the intersection ID
                    drawLabel(g2d, p, intersection.intersectionID());

                    g2d.setColor(Color.BLACK);
                } else {
                    g2d.fillOval(p.x - dotSize/2, p.y - dotSize/2, dotSize, dotSize);
                }
            }

        } finally {
            g2d.dispose();
        }
    }

    private void drawMarker(Graphics2D g2d, Intersection intersection, Color color, String label) {
        Point p = geoToBuffer(intersection.longitude(), intersection.latitude());
        int size = 12;

        g2d.setColor(color);
        g2d.fillOval(p.x - size/2, p.y - size/2, size, size);

        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int textX = p.x - fm.stringWidth(label)/2;
        int textY = p.y + fm.getAscent()/2 - 1;
        g2d.drawString(label, textX, textY);
    }

    private void drawStepNumber(Graphics2D g2d, Point p, int number) {
        String text = String.valueOf(number);
        FontMetrics fm = g2d.getFontMetrics();

        // Draw white background for better visibility
        g2d.setColor(Color.WHITE);
        int padding = 2;
        g2d.fillRect(p.x - fm.stringWidth(text)/2 - padding,
                p.y - fm.getAscent() + padding,
                fm.stringWidth(text) + padding * 2,
                fm.getHeight());

        // Draw the number
        g2d.setColor(Color.BLACK);
        g2d.drawString(text,
                p.x - fm.stringWidth(text)/2,
                p.y + fm.getAscent()/2 - 1);
    }

    private void drawLabel(Graphics2D g2d, Point p, String label) {
        FontMetrics fm = g2d.getFontMetrics();
        int padding = 2;

        // Draw white background for better visibility
        g2d.setColor(Color.WHITE);
        g2d.fillRect(p.x + 8,
                p.y - fm.getAscent(),
                fm.stringWidth(label) + padding * 2,
                fm.getHeight());

        // Draw the label
        g2d.setColor(Color.BLACK);
        g2d.drawString(label, p.x + 8 + padding, p.y + fm.getAscent()/2 - 1);
    }

    private void drawScaleBar(Graphics2D g2d) {
        // Position the scale bar in the bottom right corner
        int margin = 20;
        int targetBarLength = getWidth() / 5;  // Initial target length in pixels
        int y = getHeight() - margin;

        // Calculate the geographic coordinates for two points
        double[] leftPoint = bufferToGeo(viewport.x + getWidth() - targetBarLength - margin,
                viewport.y + y);
        double[] rightPoint = bufferToGeo(viewport.x + getWidth() - margin,
                viewport.y + y);

        // Calculate the actual distance in meters
        Intersection il = new Intersection("il", leftPoint[0], leftPoint[1]);
        Intersection ir = new Intersection("ir", rightPoint[0], rightPoint[1]);
        double actualDistance = abruzese.util.Helpers.estimateDistance(il, ir);

        // Round to a nice number
        double roundedDistance = roundToNiceNumber(actualDistance);

        // Adjust the bar length based on the rounded distance
        double scaleFactor = roundedDistance / actualDistance;
        int adjustedBarLength = (int)(targetBarLength * scaleFactor);

        // Draw the scale bar
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2.0f));

        int x2 = getWidth() - margin;
        int x1 = x2 - adjustedBarLength;

        // Draw the main bar
        g2d.drawLine(x1, y, x2, y);

        // Draw end ticks
        int tickHeight = 5;
        g2d.drawLine(x1, y - tickHeight, x1, y + tickHeight);
        g2d.drawLine(x2, y - tickHeight, x2, y + tickHeight);

        // Draw background for the label to improve readability
        String label = formatDistance(roundedDistance);
        FontMetrics fm = g2d.getFontMetrics();
        int labelX = x1 + (adjustedBarLength - fm.stringWidth(label)) / 2;
        int labelY = y - tickHeight - 5;

        // Draw white background for text
        g2d.setColor(Color.WHITE);
        g2d.fillRect(labelX - 2, labelY - fm.getAscent(),
                fm.stringWidth(label) + 4, fm.getHeight());

        // Draw the text
        g2d.setColor(Color.BLACK);
        g2d.drawString(label, labelX, labelY);
    }

    // Utility Methods
    private Point getMidPoint(Point p1, Point p2) {
        return new Point((p1.x + p2.x)/2, (p1.y + p2.y)/2);
    }

    private double roundToNiceNumber(double distance) {
        // Get the order of magnitude
        double exponent = Math.floor(Math.log10(distance));
        double pow10 = Math.pow(10, exponent);
        double mantissa = distance / pow10;

        // Round to a nice number: 1, 2, 5, 10
        if (mantissa < 1.5) return pow10;
        if (mantissa < 3.5) return 2 * pow10;
        if (mantissa < 7.5) return 5 * pow10;
        return 10 * pow10;
    }

    private String formatDistance(double distance) {
        if (distance >= 1000) {
            if (distance >= 10000) {
                // For distances 10km and above, don't show decimal places
                return String.format("%d km", Math.round(distance/1000));
            }
            // For distances between 1-10km, show one decimal place
            return String.format("%.1f km", distance/1000);
        }
        // For distances under 1km, show in meters with no decimal places
        return String.format("%d m", Math.round(distance));
    }

    private void updateResetButtonPosition() {
        resetButton.setBounds(
                getWidth() - BUTTON_WIDTH - BUTTON_PADDING,
                BUTTON_PADDING,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
    }

    private void updateViewport() {
        viewport.setSize(getWidth(), getHeight());
        viewport.setLocation(BUFFER_PADDING, BUFFER_PADDING);
    }

    // Coordinate Conversion and Map Position Methods

    private Point geoToBuffer(double lon, double lat) {
        double geoWidth = (maxLon - minLon) / actualScale;
        double geoHeight = (maxLat - minLat) / actualScale;

        int bufferWidth = mapBuffer.getWidth() - 2 * BUFFER_PADDING;
        int bufferHeight = mapBuffer.getHeight() - 2 * BUFFER_PADDING;

        int x = (int)((lon - (center.x - geoWidth/2)) * bufferWidth / geoWidth) + BUFFER_PADDING;
        int y = (int)(((center.y + geoHeight/2) - lat) * bufferHeight / geoHeight) + BUFFER_PADDING;

        return new Point(x, y);
    }

    private double[] bufferToGeo(int bufferX, int bufferY) {
        double geoWidth = (maxLon - minLon) / actualScale;
        double geoHeight = (maxLat - minLat) / actualScale;

        int bufferWidth = mapBuffer.getWidth() - 2 * BUFFER_PADDING;
        int bufferHeight = mapBuffer.getHeight() - 2 * BUFFER_PADDING;

        double lon = ((bufferX - BUFFER_PADDING) * geoWidth / bufferWidth) + (center.x - geoWidth/2);
        double lat = (center.y + geoHeight/2) - ((bufferY - BUFFER_PADDING) * geoHeight / bufferHeight);

        return new double[]{lon, lat};
    }

    private void initializeBounds() {
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

        // Set the bounds so that they're a square
        minLat = centerLat - maxRange/2;
        maxLat = centerLat + maxRange/2;
        minLon = centerLon - maxRange/2;
        maxLon = centerLon + maxRange/2;
    }

    private void updateMapPosition(Point currentPos) {
        if (lastMousePos != null) {
            int dx = currentPos.x - lastMousePos.x;
            int dy = currentPos.y - lastMousePos.y;

            // Update viewport position
            viewport.translate(-dx, -dy);

            // Check if we need to recreate the buffer
            if (needsBufferUpdate()) {
                double[] centerGeo = bufferToGeo(
                        viewport.x + viewport.width/2,
                        viewport.y + viewport.height/2
                );
                center.x = centerGeo[0];
                center.y = centerGeo[1];
                createMapBuffer();
                updateViewport();
            }
        }
        lastMousePos = currentPos;
    }

    private void zoom(double factor) {
        double newScale = actualScale * factor;

        // Check max zoom constraint
        if (newScale < MIN_ZOOM) {
            newScale = MIN_ZOOM;
            factor = MIN_ZOOM / actualScale;
            if (factor == 1.0) return;
        }

        // Get the current viewport center in geographic coordinates
        Point viewportCenter = new Point(
                viewport.x + viewport.width / 2,
                viewport.y + viewport.height / 2
        );
        double[] centerGeo = bufferToGeo(viewportCenter.x, viewportCenter.y);

        // Store the old scale for ratio calculations
        double oldScale = actualScale;

        // Update scale
        actualScale = newScale;

        boolean needNewBuffer = bufferScale / actualScale < MIN_QUALITY_THRESHOLD ||
                bufferScale / actualScale > MAX_QUALITY_THRESHOLD;

        if (needNewBuffer) {
            // Update the map center to the viewport center
            center.x = centerGeo[0];
            center.y = centerGeo[1];

            // Update buffer scale AFTER storing center
            bufferScale = actualScale;

            // Create new buffer with updated scale and center
            createMapBuffer();

            // Reset viewport to default size
            viewport.width = getWidth();
            viewport.height = getHeight();

            // Position viewport at the buffer center
            viewport.x = BUFFER_PADDING;
            viewport.y = BUFFER_PADDING;
        } else {
            // For non-rebuffer zooms, scale the viewport around its center
            int oldWidth = viewport.width;
            int oldHeight = viewport.height;
            viewport.width = (int)(viewport.width / factor);
            viewport.height = (int)(viewport.height / factor);
            viewport.x += (oldWidth - viewport.width) / 2;
            viewport.y += (oldHeight - viewport.height) / 2;
        }

        constrainCenter();
    }

    private boolean needsBufferUpdate() {
        // Check if the viewport is getting too close to the buffer edges
        int threshold = BUFFER_PADDING / 2;

        // Also check if the quality ratio is outside acceptable bounds
        double qualityRatio = bufferScale / actualScale;
        boolean qualityIssue = qualityRatio < MIN_QUALITY_THRESHOLD ||
                qualityRatio > MAX_QUALITY_THRESHOLD;

        return qualityIssue ||
                viewport.x < threshold ||
                viewport.y < threshold ||
                viewport.x + viewport.width > mapBuffer.getWidth() - threshold ||
                viewport.y + viewport.height > mapBuffer.getHeight() - threshold;
    }

    private void constrainCenter() {
        if(actualScale > 2) return; //this doesn't really work when zoomed in a bunch

        // Calculate the visible geographic width and height
        double geoWidth = (maxLon - minLon) / actualScale * 0.95;
        double geoHeight = (maxLat - minLat) / actualScale * 0.95;

        // Get the original center (midpoint of the bounds)
        double originalCenterX = (maxLon + minLon) / 2;
        double originalCenterY = (maxLat + minLat) / 2;

        // Constrain the center point
        center.x = Math.max(originalCenterX - geoWidth,
                Math.min(originalCenterX + geoWidth, center.x));
        center.y = Math.max(originalCenterY - geoHeight,
                Math.min(originalCenterY + geoHeight, center.y));
    }

    private void resetView() {
        // Reset to initial values
        clearHighlights();
        actualScale = initialScale;
        bufferScale = initialScale;
        center = new Point2D.Double(initialCenter.x, initialCenter.y);
        createMapBuffer();
        updateViewport();
    }

    // Search Methods
    private Intersection findNearestIntersection(Point bufferPoint) {
        Intersection nearest = null;
        double minDistance = CLICK_TOLERANCE;

        for (Intersection intersection : streetGraph) {
            Point p = geoToBuffer(intersection.longitude(), intersection.latitude());
            double distance = bufferPoint.distance(p);

            if (distance < minDistance) {
                minDistance = distance;
                nearest = intersection;
            }
        }

        return nearest;
    }

    private Road findNearestRoad(Point bufferPoint) {
        Road nearest = null;
        double minDistance = CLICK_TOLERANCE;

        for (HashTable.Entry<Intersection, Intersection> edge : streetGraph.getEdges()) {
            Intersection from = edge.getKey();
            Intersection to = edge.getValue();
            Road road = streetGraph.get(from, to);

            Point p1 = geoToBuffer(from.longitude(), from.latitude());
            Point p2 = geoToBuffer(to.longitude(), to.latitude());

            double distance = distanceToSegment(bufferPoint, p1, p2);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = road;
            }
        }

        return nearest;
    }

    private double distanceToSegment(Point p, Point start, Point end) {
        double l2 = start.distanceSq(end);
        if (l2 == 0) return p.distance(start);

        double t = ((p.x - start.x) * (end.x - start.x) +
                (p.y - start.y) * (end.y - start.y)) / l2;

        if (t < 0) return p.distance(start);
        if (t > 1) return p.distance(end);

        return p.distance(new Point(
                (int)(start.x + t * (end.x - start.x)),
                (int)(start.y + t * (end.y - start.y))
        ));
    }
}