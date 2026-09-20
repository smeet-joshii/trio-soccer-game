// Group: [Trio Soccor Game]
// Names: Smeet, Tabir, Eli
// Date: April 27, 2026
// GamePanel.java
// draws the whole game and scales it to fit any window size.
// also draws the HUD: active marker, aim arrow, power bar, GOAL message, controls.
// run Starter.java to play.


import java.awt.*;
import javax.swing.*;


public class GamePanel extends JPanel {
    private Starter starter;


    public GamePanel(Starter s) {
        starter = s;
        setBackground(new Color(25, 25, 30));
        setDoubleBuffered(true);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);


        int pw = getWidth();
        int ph = getHeight();
        if (pw <= 0 || ph <= 0) { g2.dispose(); return; }


        // scale virtual area to fit the window, keep aspect ratio, center it
        double sx = (double) pw / Starter.VW;
        double sy = (double) ph / Starter.VH;
        double scale = Math.min(sx, sy);
        double tx = (pw - Starter.VW * scale) / 2.0;
        double ty = (ph - Starter.VH * scale) / 2.0;


        g2.translate(tx, ty);
        g2.scale(scale, scale);


        // letterbox bg
        g2.setColor(new Color(20, 20, 25));
        g2.fillRect(0, 0, Starter.VW, Starter.VH);


        // field, teams, ball
        Starter.field.drawAll(g2);
        if (Starter.team1 != null) Starter.team1.draw(g2);
        if (Starter.team2 != null) Starter.team2.draw(g2);
        if (Starter.ball != null) Starter.ball.draw(g2);


        // HUD
        drawActiveMarker(g2);
        drawPlayerArrow(g2);
        drawPowerBar(g2);
        drawGoalMessage(g2);


        // controls text at the bottom
        g2.setColor(new Color(230, 230, 230));
        g2.setFont(new Font("Arial", Font.PLAIN, 15));
        String controls = "WASD / Arrows = Move   Space = Hold to Charge Kick   Shift = Max Kick   C = Pass   X = Switch Player";
        FontMetrics fm = g2.getFontMetrics();
        int cw = fm.stringWidth(controls);
        g2.drawString(controls, (Starter.VW - cw) / 2, Starter.VH - 10);


        g2.dispose();
    }


    // yellow disc under the player you control
    private void drawActiveMarker(Graphics2D g) {
        if (Starter.team1 == null) return;
        Player active = Starter.team1.players[Starter.activePlayerIndex];


        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(active.getX() + 6, active.getY() + active.getHeight() + 8, 24, 8);


        g.setColor(new Color(255, 230, 60));
        int ix = active.getX() + active.getWidth() / 2 - 8;
        int iy = active.getY() + active.getHeight() + 3;
        g.fillOval(ix, iy, 16, 8);
        g.setColor(new Color(180, 140, 0));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(ix, iy, 16, 8);
    }


    // orange aim arrow when you have the ball
    private void drawPlayerArrow(Graphics2D g) {
        if (Starter.ball == null || Starter.team1 == null || starter == null) return;
        if (!Starter.gameState.equals("PLAYING")) return;
        if (!starter.activePlayerHasBall()) return;


        Player active = Starter.team1.players[Starter.activePlayerIndex];
        int dx = starter.getAimDX();
        int dy = starter.getAimDY();
        if (dx == 0 && dy == 0) {
            // no arrow keys held - point where the player is facing
            dx = (int) Math.signum(active.facingX);
            if (dx == 0) dx = 1;
            dy = (int) Math.signum(active.facingY);
        }


        int startX = active.getCenterX();
        int startY = active.getY() - 18;


        int length = 34;
        int endX = startX + dx * length;
        int endY = startY + dy * length;


        g.setColor(new Color(255, 150, 40));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(startX, startY, endX, endY);


        // arrow head
        int arrowSize = 10;
        double angle = Math.atan2(endY - startY, endX - startX);
        int x1 = (int) (endX - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (endY - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (endX - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (endY - arrowSize * Math.sin(angle + Math.PI / 6));
        g.drawLine(endX, endY, x1, y1);
        g.drawLine(endX, endY, x2, y2);
    }


    // power meter while space is held - green / yellow / red
    private void drawPowerBar(Graphics2D g) {
        if (!Starter.spaceCharging) return;
        if (Starter.team1 == null) return;
        Player active = Starter.team1.players[Starter.activePlayerIndex];


        int barW = 44;
        int barH = 6;
        int bx = active.getCenterX() - barW / 2;
        int by = active.getY() - 22;


        // background
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(bx - 2, by - 2, barW + 4, barH + 4, 6, 6);


        double t01 = Starter.spaceCharge / (double) Starter.MAX_CHARGE;
        int fillW = (int) (barW * t01);


        Color fill;
        if (t01 < 0.4)      fill = new Color(80, 200, 80);   // low
        else if (t01 < 0.8) fill = new Color(240, 200, 40);  // medium
        else                fill = new Color(230, 70, 50);   // full


        g.setColor(fill);
        g.fillRoundRect(bx, by, fillW, barH, 4, 4);


        g.setColor(new Color(255, 255, 255, 160));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx, by, barW, barH, 4, 4);
    }


    // GOAL banner after scoring - fades in / out
    private void drawGoalMessage(Graphics2D g) {
        if (Starter.goalMessageTimer <= 0 || Starter.lastGoalMessage.isEmpty()) return;


        int alpha = Math.min(255, Starter.goalMessageTimer * 4);
        g.setFont(new Font("Arial", Font.BOLD, 52));
        String msg = "GOAL!  " + Starter.lastGoalMessage;
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(msg);
        int x = (Starter.VW - w) / 2;
        int y = Starter.VH / 2 - 60;


        g.setColor(new Color(0, 0, 0, Math.min(180, alpha)));
        g.fillRoundRect(x - 24, y - 50, w + 48, 74, 20, 20);
        g.setColor(new Color(255, 230, 60, alpha));
        g.drawString(msg, x, y);
    }
}