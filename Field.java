// Group: [Trio Soccor Game]
// Names: Smeet, Tabir, Eli
// Date: April 27, 2026
// Field.java
// draws the pitch, stadium, crowd, goals (with net bulge), and scoreboard.
// also has checkGoal() to detect when someone scores.
// run Starter.java to play.


import java.awt.*;
import java.awt.geom.*;


public class Field {
    // crowd dot positions are made once with a seeded random so they stay still
    private static int[]   crowdX, crowdY;
    private static Color[] crowdColor;


    static {
        int n = 420;
        crowdX     = new int[n];
        crowdY     = new int[n];
        crowdColor = new Color[n];
        java.util.Random rng = new java.util.Random(12345);


        // top stand
        for (int i = 0; i < n / 4; i++) {
            crowdX[i] = rng.nextInt(Starter.VW);
            crowdY[i] = 48 - rng.nextInt(44);
            crowdColor[i] = randCrowdColor(rng);
        }
        // bottom stand
        for (int i = n / 4; i < n / 2; i++) {
            crowdX[i] = rng.nextInt(Starter.VW);
            crowdY[i] = Starter.FIELD_Y + Starter.FIELD_H + 2 + rng.nextInt(40);
            crowdColor[i] = randCrowdColor(rng);
        }
        // left stand
        for (int i = n / 2; i < 3 * n / 4; i++) {
            crowdX[i] = rng.nextInt(48);
            crowdY[i] = Starter.FIELD_Y + rng.nextInt(Starter.FIELD_H);
            crowdColor[i] = randCrowdColor(rng);
        }
        // right stand
        for (int i = 3 * n / 4; i < n; i++) {
            crowdX[i] = Starter.FIELD_X + Starter.FIELD_W + 2 + rng.nextInt(46);
            crowdY[i] = Starter.FIELD_Y + rng.nextInt(Starter.FIELD_H);
            crowdColor[i] = randCrowdColor(rng);
        }
    }


    // pick a random fan shirt color
    private static Color randCrowdColor(java.util.Random r) {
        Color[] palette = {
            new Color(60, 120, 200), new Color(200, 60, 60),  new Color(230, 220, 80),
            new Color(60, 170, 90),  new Color(230, 230, 230), new Color(40, 40, 60),
            new Color(180, 80, 180), new Color(255, 140, 40)
        };
        return palette[r.nextInt(palette.length)];
    }


    public Field() {}


    // 1 = team1 scored (right line), 2 = team2 scored (left line), 0 = no goal
    public int checkGoal(Ball ball) {
        double cx = ball.getCenterX();
        double cy = ball.getCenterY();


        // ball has to be inside the goal opening height
        boolean inGoalY = cy >= Starter.GOAL_Y_TOP && cy <= Starter.GOAL_Y_BOTTOM;
        if (!inGoalY) return 0;


        if (cx < Starter.FIELD_X + 2) return 2;
        if (cx > Starter.FIELD_X + Starter.FIELD_W - 2) return 1;
        return 0;
    }


    // little helper - is this point inside the field rectangle?
    public boolean isInBounds(Point2D p) {
        return p.getX() >= Starter.FIELD_X
            && p.getY() >= Starter.FIELD_Y
            && p.getX() <= Starter.FIELD_X + Starter.FIELD_W
            && p.getY() <= Starter.FIELD_Y + Starter.FIELD_H;
    }


    // called by GamePanel every frame
    public void drawAll(Graphics2D g) {
        drawStadium(g);
        drawGrass(g);
        drawLines(g);
        drawGoals(g);
        drawTopInfo(g);
        drawGameStateMessage(g);
    }


    // dark outer + seat rows + crowd + ad boards
    private void drawStadium(Graphics2D g) {
        int fx = Starter.FIELD_X;
        int fy = Starter.FIELD_Y;
        int fw = Starter.FIELD_W;
        int fh = Starter.FIELD_H;


        g.setColor(new Color(30, 30, 40));
        g.fillRect(0, 0, Starter.VW, Starter.VH);


        // seat rows get darker further from the pitch
        Color[] seatColors = {
            new Color(95, 95, 105), new Color(80, 80, 90),
            new Color(65, 65, 75),  new Color(50, 50, 60)
        };
        for (int r = 0; r < seatColors.length; r++) {
            g.setColor(seatColors[r]);
            g.fillRect(0, fy - (r + 1) * 10, Starter.VW, 8);
            g.fillRect(0, fy + fh + r * 10 + 2, Starter.VW, 8);
            g.fillRect(fx - (r + 1) * 10, 0, 8, Starter.VH);
            g.fillRect(fx + fw + r * 10 + 2, 0, 8, Starter.VH);
        }


        // crowd dots
        for (int i = 0; i < crowdX.length; i++) {
            g.setColor(crowdColor[i]);
            g.fillRect(crowdX[i], crowdY[i], 3, 3);
        }


        // ad boards
        int adH = 10;
        String[] ads = { "KICK", "PASS", "GOAL!", "SCORE", "PLAY", "WIN" };
        Color[] adBG = {
            new Color(220, 60, 60),  new Color(30, 120, 230),
            new Color(240, 200, 60), new Color(50, 170, 90),
            new Color(180, 80, 200), new Color(240, 120, 40)
        };
        int adW = fw / ads.length;
        for (int i = 0; i < ads.length; i++) {
            // top
            g.setColor(adBG[i]);
            g.fillRect(fx + i * adW, fy - adH - 1, adW - 2, adH);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 8));
            g.drawString(ads[i], fx + i * adW + 4, fy - 3);
            // bottom (reversed)
            g.setColor(adBG[ads.length - 1 - i]);
            g.fillRect(fx + i * adW, fy + fh + 1, adW - 2, adH);
            g.setColor(Color.WHITE);
            g.drawString(ads[ads.length - 1 - i], fx + i * adW + 4, fy + fh + 9);
        }
    }


    // grass with vertical stripes
    private void drawGrass(Graphics2D g) {
        int stripes = 12;
        int sw = Starter.FIELD_W / stripes;
        for (int i = 0; i < stripes; i++) {
            g.setColor((i % 2 == 0) ? new Color(46, 152, 62) : new Color(40, 138, 54));
            g.fillRect(Starter.FIELD_X + i * sw, Starter.FIELD_Y, sw, Starter.FIELD_H);
        }
        // patch over any leftover gap on the right
        int used = stripes * sw;
        if (used < Starter.FIELD_W) {
            g.setColor(new Color(40, 138, 54));
            g.fillRect(Starter.FIELD_X + used, Starter.FIELD_Y,
                       Starter.FIELD_W - used, Starter.FIELD_H);
        }
    }


    // white pitch lines: outline, halfway, center circle, penalty + 6yd boxes,
    // penalty spots, D arcs, corner arcs
    private void drawLines(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));


        int fx = Starter.FIELD_X;
        int fy = Starter.FIELD_Y;
        int fw = Starter.FIELD_W;
        int fh = Starter.FIELD_H;


        g.drawRect(fx, fy, fw, fh);
        g.drawLine(fx + fw / 2, fy, fx + fw / 2, fy + fh);


        int r = 75;
        g.drawOval(fx + fw / 2 - r, fy + fh / 2 - r, r * 2, r * 2);
        g.fillOval(fx + fw / 2 - 4, fy + fh / 2 - 4, 8, 8);


        int penW = 130, penH = 220;
        int sixW = 50,  sixH = 120;
        int cy  = fy + fh / 2;


        // left side
        g.drawRect(fx, cy - penH / 2, penW, penH);
        g.drawRect(fx, cy - sixH / 2, sixW, sixH);
        g.fillOval(fx + penW - 90, cy - 4, 8, 8);
        g.draw(new Arc2D.Double(fx + penW - 90 - 50, cy - 50, 100, 100, 300, 120, Arc2D.OPEN));


        // right side (mirrored)
        g.drawRect(fx + fw - penW, cy - penH / 2, penW, penH);
        g.drawRect(fx + fw - sixW, cy - sixH / 2, sixW, sixH);
        g.fillOval(fx + fw - penW + 82, cy - 4, 8, 8);
        g.draw(new Arc2D.Double(fx + fw - penW + 82 - 50, cy - 50, 100, 100, 120, 120, Arc2D.OPEN));


        // corners
        int ca = 18;
        g.draw(new Arc2D.Double(fx - ca,      fy - ca,      ca * 2, ca * 2, 270, 90, Arc2D.OPEN));
        g.draw(new Arc2D.Double(fx + fw - ca, fy - ca,      ca * 2, ca * 2, 180, 90, Arc2D.OPEN));
        g.draw(new Arc2D.Double(fx - ca,      fy + fh - ca, ca * 2, ca * 2,   0, 90, Arc2D.OPEN));
        g.draw(new Arc2D.Double(fx + fw - ca, fy + fh - ca, ca * 2, ca * 2,  90, 90, Arc2D.OPEN));
    }


    // goals + the net with a bulge animation when ball hits the back.
    // bulge values come from Starter.leftNetBulge / rightNetBulge (0..22).
    // we use a sin curve so the middle bulges the most.
    private void drawGoals(Graphics2D g) {
        int gTop  = Starter.GOAL_Y_TOP;
        int gBot  = Starter.GOAL_Y_BOTTOM;
        int depth = Starter.GOAL_DEPTH;
        int fx    = Starter.FIELD_X;
        int fw    = Starter.FIELD_W;


        // LEFT goal
        double lb = Math.max(0, Math.min(22, Starter.leftNetBulge));
        g.setColor(new Color(245, 245, 245));
        g.fillRect(fx - depth, gTop, depth, gBot - gTop);


        g.setColor(new Color(170, 170, 170));
        g.setStroke(new BasicStroke(1f));
        // vertical net lines
        for (int i = 0; i <= depth; i += 5) {
            double bulgeAmt = Math.sin(Math.PI * (i / (double) depth)) * lb;
            g.drawLine((int)(fx - i - bulgeAmt), gTop, (int)(fx - i - bulgeAmt), gBot);
        }
        // horizontal net rows
        for (int y = gTop; y <= gBot; y += 10) {
            double rowBulge = Math.sin(Math.PI * (y - gTop) / (double) (gBot - gTop)) * lb;
            g.drawLine((int)(fx - depth - rowBulge), y, fx, y);
        }
        // posts + crossbar
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(fx, gTop, fx, gBot);
        g.drawLine(fx, gTop, (int)(fx - depth - lb * 0.3), gTop);
        g.drawLine(fx, gBot, (int)(fx - depth - lb * 0.3), gBot);
        g.drawLine((int)(fx - depth - lb * 0.3), gTop, (int)(fx - depth - lb * 0.3), gBot);


        // RIGHT goal (mirror of left)
        double rb = Math.max(0, Math.min(22, Starter.rightNetBulge));
        g.setColor(new Color(245, 245, 245));
        g.fillRect(fx + fw, gTop, depth, gBot - gTop);


        g.setColor(new Color(170, 170, 170));
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= depth; i += 5) {
            double bulgeAmt = Math.sin(Math.PI * (i / (double) depth)) * rb;
            g.drawLine((int)(fx + fw + i + bulgeAmt), gTop, (int)(fx + fw + i + bulgeAmt), gBot);
        }
        for (int y = gTop; y <= gBot; y += 10) {
            double rowBulge = Math.sin(Math.PI * (y - gTop) / (double) (gBot - gTop)) * rb;
            g.drawLine(fx + fw, y, (int)(fx + fw + depth + rowBulge), y);
        }
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(fx + fw, gTop, fx + fw, gBot);
        g.drawLine(fx + fw, gTop, (int)(fx + fw + depth + rb * 0.3), gTop);
        g.drawLine(fx + fw, gBot, (int)(fx + fw + depth + rb * 0.3), gBot);
        g.drawLine((int)(fx + fw + depth + rb * 0.3), gTop,
                   (int)(fx + fw + depth + rb * 0.3), gBot);
    }


    // top scoreboard pill: logos, names, scores, clock
    private void drawTopInfo(Graphics2D g) {
        int boardW = 420;
        int boardH = 42;
        int boardX = Starter.VW / 2 - boardW / 2;
        int boardY = 6;


        // background
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(boardX, boardY, boardW, boardH, 14, 14);
        g.setColor(new Color(255, 255, 255, 80));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(boardX, boardY, boardW, boardH, 14, 14);


        // team 1 (left)
        drawTeamLogo(g, Starter.team1, boardX + 10, boardY + 6, 30);
        g.setColor(new Color(255, 180, 180));
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString(Starter.team1.getName(), boardX + 46, boardY + 21);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString(String.valueOf(Starter.team1.getScore()), boardX + 46, boardY + 39);


        // team 2 (right) - aligned from the right edge
        drawTeamLogo(g, Starter.team2, boardX + boardW - 40, boardY + 6, 30);
        g.setColor(new Color(170, 210, 255));
        g.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm18 = g.getFontMetrics();
        int t2nw = fm18.stringWidth(Starter.team2.getName());
        g.drawString(Starter.team2.getName(), boardX + boardW - 46 - t2nw, boardY + 21);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm22 = g.getFontMetrics();
        String s2 = String.valueOf(Starter.team2.getScore());
        int s2w = fm22.stringWidth(s2);
        g.drawString(s2, boardX + boardW - 46 - s2w, boardY + 39);


        // match clock
        String time = formatTime(Starter.timeLeft);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fmT = g.getFontMetrics();
        int tw = fmT.stringWidth(time);
        g.setColor(Color.WHITE);
        g.drawString(time, boardX + boardW / 2 - tw / 2, boardY + 29);
    }


    // shield logo with a star inside, in the team color
    public static void drawTeamLogo(Graphics2D g, Team team, int x, int y, int size) {
        Path2D.Double shield = new Path2D.Double();
        shield.moveTo(x, y);
        shield.lineTo(x + size, y);
        shield.lineTo(x + size, y + size * 0.55);
        shield.quadTo(x + size, y + size, x + size / 2.0, y + size);
        shield.quadTo(x, y + size, x, y + size * 0.55);
        shield.closePath();


        g.setColor(team.color);
        g.fill(shield);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.draw(shield);


        drawStar(g, x + size / 2.0, y + size * 0.52, size * 0.28, Color.WHITE);
    }


    // 5 pointed star
    private static void drawStar(Graphics2D g, double cx, double cy, double r, Color col) {
        Path2D.Double star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double rr = (i % 2 == 0) ? r : r * 0.45;
            double px = cx + Math.cos(a) * rr;
            double py = cy + Math.sin(a) * rr;
            if (i == 0) star.moveTo(px, py);
            else        star.lineTo(px, py);
        }
        star.closePath();
        g.setColor(col);
        g.fill(star);
    }


    // seconds -> "M:SS"
    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return (s < 10) ? (m + ":0" + s) : (m + ":" + s);
    }


    // big banner for MENU / PAUSED / ENDED
    private void drawGameStateMessage(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 28));
 
        if (Starter.gameState.equals("MENU")) {
            g.setColor(Color.BLACK);
            g.drawString("Press New Game to Start", 420, 100);
        } else if (Starter.gameState.equals("PAUSED")) {
            g.setColor(Color.MAGENTA);
            g.drawString("GAME PAUSED", 490, 100);
        } else if (Starter.gameState.equals("ENDED")) {
            g.setColor(Color.RED);
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 55));
            g.drawString("MATCH ENDED", 400, 150);
            
            if (Starter.team1.getScore() > Starter.team2.getScore()) {
                g.setColor(Color.YELLOW);
                g.fillOval(555,340,70,70);
                g.setColor(Color.ORANGE);
                g.drawString("#1", 560, 395);
                g.drawString("Winner: " + Starter.team1.getName(), 360, 200);
                Starter.team1.wonGame();
                Starter.team2.lostGame();
                Starter.ball.update(new Point2D.Double(10000,0));
            } else if (Starter.team2.getScore() > Starter.team1.getScore()) {
                g.setColor(Color.YELLOW);
                g.fillOval(555,340,70,70);
                g.setColor(Color.ORANGE);
                g.drawString("#1", 560, 395);
                g.drawString("Winner: " + Starter.team2.getName(), 370, 200);
                Starter.team2.wonGame();
                Starter.team1.lostGame();
                Starter.ball.update(new Point2D.Double(10000,0));
            } else {
                 g.setColor(Color.ORANGE);
                g.drawString("Result: Draw", 420, 200);
                Starter.team1.tieWith(Starter.team2);
                Starter.ball.update(new Point2D.Double(10000,0));
            }
        }
    }


    // helper to draw a rounded pill with one line of text
    private void drawBanner(Graphics2D g, String msg, Color fg, Color bg) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(msg);
        int x = (Starter.VW - w) / 2;
        int y = 120;
        g.setColor(bg);
        g.fillRoundRect(x - 20, y - 32, w + 40, 46, 14, 14);
        g.setColor(fg);
        g.drawString(msg, x, y);
    }
}