package abruzese.gui;

import abruzese.console.DirectionStep;
import abruzese.graph.Graph;
import abruzese.graph.edges.Road;
import abruzese.graph.vertices.Intersection;
import abruzese.hashtable.HashTable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class MapRenderer {
    private final Graph<Intersection, Road> streetGraph;
    private final MapCoordinateSystem coordSystem;
    private final ViewportManager viewportManager;
    private BufferedImage mapBuffer;

    // Constants moved from MapPanel
    private static final Color pathColor = new Color(0, 102, 204);
    private static final Color highlightColor = new Color(255, 69, 0);
    private static final float PATH_WIDTH = 3.0f;
    private static final float HIGHLIGHT_WIDTH = 4.0f;
    private static final int BUFFER_PADDING = 2000;

    // State for highlighting
    private List<DirectionStep> highlightedSteps = null;
    private Intersection highlightedIntersection = null;

    public MapRenderer(Graph<Intersection, Road> streetGraph,
                       MapCoordinateSystem coordSystem,
                       ViewportManager viewportManager) {
        this.streetGraph = streetGraph;
        this.coordSystem = coordSystem;
        this.viewportManager = viewportManager;
    }

    public void createMapBuffer(int width, int height) {
        if (width <= 0 || height <= 0) return;

        mapBuffer = new BufferedImage(width + 2 * BUFFER_PADDING,
                height + 2 * BUFFER_PADDING,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = mapBuffer.createGraphics();

        try {
            // Set up rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Clear with background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width + 2 * BUFFER_PADDING, height + 2 * BUFFER_PADDING);

            // Draw all roads first (in gray)
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1.0f));

            for (HashTable.Entry<Intersection, Intersection> edge : streetGraph.getEdges()) {
                Intersection from = edge.getKey();
                Intersection to = edge.getValue();
                Point fromPoint = coordSystem.geoToBuffer(from.longitude(), from.latitude());
                Point toPoint = coordSystem.geoToBuffer(to.longitude(), to.latitude());
                g2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
            }

            // Draw highlighted path if available
            if (highlightedSteps != null && !highlightedSteps.isEmpty()) {
                // Draw the highlighted path
                g2d.setStroke(new BasicStroke(HIGHLIGHT_WIDTH,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(highlightColor);

                for (DirectionStep step : highlightedSteps) {
                    Point fromPoint = coordSystem.geoToBuffer(step.from.longitude(), step.from.latitude());
                    Point toPoint = coordSystem.geoToBuffer(step.to.longitude(), step.to.latitude());
                    g2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
                }

                // Draw start and end markers
                DirectionStep firstStep = highlightedSteps.get(0);
                DirectionStep lastStep = highlightedSteps.get(highlightedSteps.size() - 1);

                drawMarker(g2d, firstStep.from, Color.GREEN, "S");  // Start marker
                drawMarker(g2d, lastStep.to, Color.RED, "E");      // End marker

                // Draw step numbers
                g2d.setColor(Color.BLACK);
                for (DirectionStep step : highlightedSteps) {
                    Point midPoint = getMidPoint(
                            coordSystem.geoToBuffer(step.from.longitude(), step.from.latitude()),
                            coordSystem.geoToBuffer(step.to.longitude(), step.to.latitude())
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
                        (highlightedIntersection == null || !intersection.equals(highlightedIntersection))) {
                    continue;
                }

                Point p = coordSystem.geoToBuffer(intersection.longitude(), intersection.latitude());

                // Highlight the specific intersection if requested
                if (highlightedIntersection != null && intersection.equals(highlightedIntersection)) {
                    g2d.setColor(highlightColor);
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
        Point p = coordSystem.geoToBuffer(intersection.longitude(), intersection.latitude());
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

    private Point getMidPoint(Point p1, Point p2) {
        return new Point((p1.x + p2.x)/2, (p1.y + p2.y)/2);
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

    public void drawScaleBar(Graphics2D g2d, int width, int height, Rectangle viewport) {
        // Position the scale bar in the bottom right corner
        int margin = 20;
        int targetBarLength = width / 5;  // Initial target length in pixels
        int y = height - margin;

        // Calculate the geographic coordinates for two points
        double[] leftPoint = coordSystem.bufferToGeo(viewport.x + width - targetBarLength - margin,
                viewport.y + y);
        double[] rightPoint = coordSystem.bufferToGeo(viewport.x + width - margin,
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

        int x2 = width - margin;
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

    private double roundToNiceNumber(double distance) {
        double exponent = Math.floor(Math.log10(distance));
        double pow10 = Math.pow(10, exponent);
        double mantissa = distance / pow10;

        if (mantissa < 1.5) return pow10;
        if (mantissa < 3.5) return 2 * pow10;
        if (mantissa < 7.5) return 5 * pow10;
        return 10 * pow10;
    }

    private String formatDistance(double distance) {
        if (distance >= 1000) {
            if (distance >= 10000) {
                return String.format("%d km", Math.round(distance/1000));
            }
            return String.format("%.1f km", distance/1000);
        }
        return String.format("%d m", Math.round(distance));
    }

    // Methods for highlighting
    public void setHighlightedSteps(List<DirectionStep> steps) {
        this.highlightedSteps = steps;
    }

    public void setHighlightedIntersection(Intersection intersection) {
        this.highlightedIntersection = intersection;
    }

    public void clearHighlights() {
        this.highlightedSteps = null;
        this.highlightedIntersection = null;
    }

    public BufferedImage getMapBuffer() {
        return mapBuffer;
    }
}