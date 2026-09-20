// Group: [Trio Soccor Game]
// Names: Smeet, Tabir, Eli
// Date: April 27, 2026
// Ball.java
// the soccer ball - position, velocity, friction, drawing.
// run Starter.java to play the game.
// no outside code, just standard java.


import java.awt.*;
import java.awt.geom.*;


public class Ball {
    Point2D position;
    double  velocityX, velocityY;
    Player  possessedBy;       // who has the ball, null if loose


    private double rotation = 0;   // for spinning the patches while it rolls


    public static final int SIZE = 20;


    // start in the middle of the field, not moving
    public Ball() {
        velocityX = 0;
        velocityY = 0;
        position = new Point2D.Double(
            Starter.VW / 2.0 - SIZE / 2.0,
            Starter.FIELD_Y + Starter.FIELD_H / 2.0 - SIZE / 2.0);
        possessedBy = null;
    }


    // teleport ball somewhere (used at kickoff)
    public void update(Point2D pos) {
        position.setLocation(pos.getX(), pos.getY());
    }


    // move ball + spin it
    public void move() {
        position.setLocation(position.getX() + velocityX,
                             position.getY() + velocityY);
        rotation += velocityX * 0.08;
    }


    public void   setVelocity(double x, double y) { velocityX = x; velocityY = y; }
    public double getDX()                         { return velocityX; }
    public double getDY()                         { return velocityY; }
    public void   setDX(double v)                 { velocityX = v; }
    public void   setDY(double v)                 { velocityY = v; }


    // friction. if speed is super tiny just snap to 0 so it stops creeping
    public void applyFriction() {
        velocityX *= 0.985;
        velocityY *= 0.985;
        if (Math.abs(velocityX) < 0.04) velocityX = 0;
        if (Math.abs(velocityY) < 0.04) velocityY = 0;
    }


    public int getCenterX() { return (int) position.getX() + SIZE / 2; }
    public int getCenterY() { return (int) position.getY() + SIZE / 2; }


    // possession flags from the old version, kept just in case
    public void    attachTo(Player p) { possessedBy = p; }
    public void    detach()           { possessedBy = null; }
    public boolean isLoose()          { return possessedBy == null; }


    // draws a soccer ball: shadow, white circle, black pentagon patches, outline
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        int cx = getCenterX();
        int cy = getCenterY();
        int r  = SIZE / 2;


        // shadow on grass
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillOval(cx - r + 1, cy + r - 2, SIZE, 5);


        // white body
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - r, cy - r, SIZE, SIZE);


        // patches rotate so it looks like its rolling
        g2.rotate(rotation, cx, cy);


        // black pentagon in the middle
        g2.setColor(Color.BLACK);
        int[] px = new int[5];
        int[] py = new int[5];
        double patchR = r * 0.45;
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + i * (2 * Math.PI / 5);
            px[i] = (int) (cx + Math.cos(a) * patchR);
            py[i] = (int) (cy + Math.sin(a) * patchR);
        }
        g2.fillPolygon(px, py, 5);


        // 5 little dots around it
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + i * (2 * Math.PI / 5) + Math.PI / 5;
            int dx = (int) (cx + Math.cos(a) * r * 0.82);
            int dy = (int) (cy + Math.sin(a) * r * 0.82);
            g2.fillOval(dx - 2, dy - 2, 4, 4);
        }


        // un-rotate before drawing the outline
        g2.rotate(-rotation, cx, cy);


        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(cx - r, cy - r, SIZE, SIZE);


        g2.dispose();
    }
}