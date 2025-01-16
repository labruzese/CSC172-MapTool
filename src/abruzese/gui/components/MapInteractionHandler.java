package abruzese.gui;

import abruzese.console.CommandHandler;
import abruzese.graph.Graph;
import abruzese.graph.edges.Road;
import abruzese.graph.vertices.Intersection;
import abruzese.hashtable.HashTable;

import java.awt.*;
import java.awt.event.*;

public class MapInteractionHandler {
    private final MapPanel mapPanel;
    private final ViewportManager viewportManager;
    private final CommandHandler commandHandler;
    private final Graph<Intersection, Road> streetGraph;
    private final MapCoordinateSystem coordSystem;
    private final Button resetButton;

    private static final int CLICK_TOLERANCE = 10; // pixels
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 25;
    private static final int BUTTON_PADDING = 10;

    public MapInteractionHandler(MapPanel mapPanel,
                                 ViewportManager viewportManager,
                                 CommandHandler commandHandler,
                                 Graph<Intersection, Road> streetGraph,
                                 MapCoordinateSystem coordSystem) {
        this.mapPanel = mapPanel;
        this.viewportManager = viewportManager;
        this.commandHandler = commandHandler;
        this.streetGraph = streetGraph;
        this.coordSystem = coordSystem;

        this.resetButton = new Button("Reset View");
        updateResetButtonPosition(mapPanel.getWidth(), mapPanel.getHeight());

        setupMouseListeners();
    }

    private void setupMouseListeners() {
        mapPanel.addMouseWheelListener(e -> {
            double zoomFactor = e.getWheelRotation() > 0 ?
                    1/ViewportManager.getZoomFactor() :
                    ViewportManager.getZoomFactor();
            viewportManager.zoom(zoomFactor);
            mapPanel.repaint();
        });

        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { // Right click
                    handleRightClick(e.getPoint());
                    return;
                }

                if (resetButton.handleMouseEvent(e)) {
                    mapPanel.repaint();
                    return;
                }
                viewportManager.startDragging(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (resetButton.handleMouseEvent(e)) {
                    viewportManager.resetView();
                    mapPanel.repaint();
                    return;
                }
                viewportManager.stopDragging();
            }
        });

        mapPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (viewportManager.isDragging()) {
                    viewportManager.updateMapPosition(e.getPoint());
                    mapPanel.repaint();
                }
            }
        });

        mapPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResetButtonPosition(mapPanel.getWidth(), mapPanel.getHeight());
            }
        });
    }

    private void handleRightClick(Point clickPoint) {
        if(commandHandler == null) return;

        // Convert screen coordinates to buffer coordinates
        Rectangle viewport = viewportManager.getViewport();
        Point bufferPoint = new Point(
                clickPoint.x + viewport.x,
                clickPoint.y + viewport.y
        );

        // First check for intersections
        Intersection nearestIntersection = findNearestIntersection(bufferPoint);
        if (nearestIntersection != null) {
            // Execute search command for intersection
            commandHandler.echo("\u001B[31m" + "search " + nearestIntersection.intersectionID() + "\u001B[0m");
            commandHandler.handleCommand("search", new String[]{nearestIntersection.intersectionID()});
            return;
        }

        // Then check for roads
        Road nearestRoad = findNearestRoad(bufferPoint);
        if (nearestRoad != null) {
            // Execute search command for road
            commandHandler.echo("\u001B[31m" + "search road " + nearestRoad.roadID + "\u001B[0m");
            commandHandler.handleCommand("search", new String[]{"road", nearestRoad.roadID});
        }
    }

    private Intersection findNearestIntersection(Point bufferPoint) {
        Intersection nearest = null;
        double minDistance = CLICK_TOLERANCE;

        for (Intersection intersection : streetGraph) {
            Point p = coordSystem.geoToBuffer(intersection.longitude(), intersection.latitude());
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

            Point p1 = coordSystem.geoToBuffer(from.longitude(), from.latitude());
            Point p2 = coordSystem.geoToBuffer(to.longitude(), to.latitude());

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

    public void updateResetButtonPosition(int width, int height) {
        resetButton.setBounds(
                width - BUTTON_WIDTH - BUTTON_PADDING,
                BUTTON_PADDING,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        );
    }

    public Button getResetButton() {
        return resetButton;
    }
}
