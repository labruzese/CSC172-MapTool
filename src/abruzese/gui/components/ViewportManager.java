package abruzese.gui;

import java.awt.*;
import java.awt.geom.Point2D;

public class ViewportManager {
    private final MapCoordinateSystem coordSystem;
    private Rectangle viewport;
    private Point lastMousePos;
    private boolean isDragging = false;
    private static final int BUFFER_PADDING = 2000;
    private static final double ZOOM_FACTOR = 1.1;

    public ViewportManager(MapCoordinateSystem coordSystem) {
        this.coordSystem = coordSystem;
        this.viewport = new Rectangle();
    }

    public void updateViewport(int width, int height) {
        viewport.setSize(width, height);
        viewport.setLocation(BUFFER_PADDING, BUFFER_PADDING);
    }

    public void updateMapPosition(Point currentPos) {
        if (lastMousePos != null) {
            int dx = currentPos.x - lastMousePos.x;
            int dy = currentPos.y - lastMousePos.y;

            // Update viewport position
            viewport.translate(-dx, -dy);

            // Check if we need to recreate the buffer
            if (coordSystem.needsBufferUpdate(viewport)) {
                double[] centerGeo = coordSystem.bufferToGeo(
                        viewport.x + viewport.width/2,
                        viewport.y + viewport.height/2
                );
                coordSystem.updateCenter(centerGeo);
                resetViewportPosition();
            }
        }
        lastMousePos = currentPos;
    }

    public void zoom(double factor) {
        // Get the current viewport center in screen coordinates
        Point viewportCenter = new Point(
                viewport.x + viewport.width / 2,
                viewport.y + viewport.height / 2
        );

        coordSystem.zoom(factor, viewportCenter, viewport);

        boolean needNewBuffer = coordSystem.getBufferScale() / coordSystem.getScale() < 0.5 ||
                coordSystem.getBufferScale() / coordSystem.getScale() > 2.0;

        if (needNewBuffer) {
            // Reset viewport to default size
            viewport.width = viewport.width;
            viewport.height = viewport.height;

            // Position viewport at the buffer center
            resetViewportPosition();
        } else {
            // For non-rebuffer zooms, scale the viewport around its center
            int oldWidth = viewport.width;
            int oldHeight = viewport.height;
            viewport.width = (int)(viewport.width / factor);
            viewport.height = (int)(viewport.height / factor);
            viewport.x += (oldWidth - viewport.width) / 2;
            viewport.y += (oldHeight - viewport.height) / 2;
        }
    }

    public void resetViewportPosition() {
        viewport.x = BUFFER_PADDING;
        viewport.y = BUFFER_PADDING;
    }

    public void resetView() {
        coordSystem.resetView();
        resetViewportPosition();
    }

    // Mouse state management
    public void startDragging(Point mousePos) {
        lastMousePos = mousePos;
        isDragging = true;
    }

    public void stopDragging() {
        isDragging = false;
        lastMousePos = null;
    }

    public boolean isDragging() {
        return isDragging;
    }

    // Getters
    public Rectangle getViewport() {
        return viewport;
    }

    public static double getZoomFactor() {
        return ZOOM_FACTOR;
    }

    public static int getBufferPadding() {
        return BUFFER_PADDING;
    }
}