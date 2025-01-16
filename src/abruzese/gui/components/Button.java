package abruzese.gui.components;


import java.awt.*;
import java.awt.event.MouseEvent;

public class Button {
    private final Rectangle bounds;
    private final String text;
    private boolean isPressed;
    private boolean isEnabled;

    public Button(String text) {
        this.text = text;
        this.bounds = new Rectangle();
        this.isEnabled = true;
        this.isPressed = false;
    }

    public void setBounds(int x, int y, int width, int height) {
        bounds.setBounds(x, y, width, height);
    }

    public boolean contains(Point p) {
        return bounds.contains(p);
    }

    public void setPressed(boolean pressed) {
        this.isPressed = pressed;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    // This method was generated with AI, idk how to design a button
    public void draw(Graphics2D g2d) {
        Paint oldPaint = g2d.getPaint();
        Font oldFont = g2d.getFont();

        if (!isEnabled) {
            g2d.setColor(Color.LIGHT_GRAY);
        } else if (isPressed) {
            g2d.setColor(new Color(200, 200, 200));
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        g2d.setColor(isEnabled ? Color.BLACK : Color.GRAY);
        g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        g2d.setColor(isEnabled ? Color.BLACK : Color.GRAY);
        FontMetrics fm = g2d.getFontMetrics();
        int textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
        int textY = bounds.y + (bounds.height + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, textX, textY);

        g2d.setPaint(oldPaint);
        g2d.setFont(oldFont);
    }

    public boolean doMouseEvent(MouseEvent e) {
        if (!isEnabled) return false;

        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                if (contains(e.getPoint())) {
                    isPressed = true;
                    return true;
                }
                break;

            case MouseEvent.MOUSE_RELEASED:
                if (isPressed && contains(e.getPoint())) {
                    isPressed = false;
                    return true;
                }
                isPressed = false;
                break;
        }
        return false;
    }
}