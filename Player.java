// Group: [Trio Soccor Game]
// Names: Smeet, Tabir, Eli
// Date: April 27, 2026
// Player.java
// one player. position, velocity, walk + kick animation, drawing.
// run Starter.java to play.


import java.awt.*;
import java.awt.geom.*;


public class Player {
    // position + speed
    Point2D position;
    double  velocityX, velocityY;


    // stuff from the older version of our class, kept so nothing breaks
    double  speed;
    double  kickPower;
    double  AIZone;


    boolean isAI;
    boolean hasBall;
    Team    team;
    String  role;    // "G" goalie, "F" forward
    Color   color;   // jersey color


    private int width, height;


    // facing direction + walk animation
    public double facingX = 1, facingY = 0;
    public double walkPhase = 0;
    private double preferredFacingX = 0; // for goalies when they stop moving


    // kick animation stuff
    public static final int KICK_DURATION    = 18;
    public static final int KICK_APPLY_FRAME = 9;
    public int kickTimer = 0;
    public boolean kickingRightLeg = true;
    public double pendingKickDX, pendingKickDY;
    public double pendingKickPower;
    public boolean kickApplied = false;


    // shown on the player
    public String playerName   = "";
    public String playerNumber = "";


    // 1 arg constructor (matches the old code)
    public Player(Point2D playpos) {
        width  = 32;
        height = 54;
        velocityX = 0;
        velocityY = 0;
        speed     = 5.0;
        kickPower = 20;
        AIZone    = 200;
        isAI      = false;
        hasBall   = false;
        role      = "F";
        color     = Color.WHITE;
        position  = new Point2D.Double(playpos.getX(), playpos.getY());
    }


    // user pressing WASD
    public void addVelocity(double dx, double dy) {
        velocityX += dx;
        velocityY += dy;
    }


    // AI uses this to chase / support / dribble
    public void moveToward(double targetX, double targetY, double accel) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double len = Math.hypot(dx, dy);
        if (len < 2) return;
        velocityX += (dx / len) * accel;
        velocityY += (dy / len) * accel;
    }


    // called every frame: move, friction, max speed, walk phase, kick timer
    public void updateMotion() {
        position.setLocation(position.getX() + velocityX, position.getY() + velocityY);


        double spd = Math.hypot(velocityX, velocityY);
        walkPhase += 0.25 + spd * 0.32;


        // face the way we're moving, or ease back to preferred when standing still
        if (spd > 0.4) {
            facingX = velocityX / spd;
            facingY = velocityY / spd;
        } else if (preferredFacingX != 0) {
            facingX += (preferredFacingX - facingX) * 0.05;
            facingY += (0 - facingY) * 0.05;
            double fl = Math.hypot(facingX, facingY);
            if (fl > 0.001) { facingX /= fl; facingY /= fl; }
        }


        // friction
        velocityX *= 0.82;
        velocityY *= 0.82;
        if (Math.abs(velocityX) < 0.08) velocityX = 0;
        if (Math.abs(velocityY) < 0.08) velocityY = 0;


        // cap top speed otherwise it goes too fast
        double max = 4.2;
        double s = Math.hypot(velocityX, velocityY);
        if (s > max) {
            velocityX = velocityX / s * max;
            velocityY = velocityY / s * max;
        }


        if (kickTimer > 0) kickTimer--;
    }


    public void setFacingTowardX(double dir) { preferredFacingX = dir > 0 ? 1 : -1; }


    
    
    
    // teleport + stop
    public void setPosition(double x, double y) {
        position.setLocation(x, y);
        velocityX = 0;
        velocityY = 0;
    }


    // start the kick animation. ball velocity is set by Starter.applyKick.
    public void startKick(double dx, double dy, double power) {
        if (kickTimer > 0) return; // already kicking
        double len = Math.hypot(dx, dy);
        if (len < 0.001) { dx = facingX; dy = facingY; len = Math.max(0.001, Math.hypot(dx, dy)); }
        pendingKickDX    = dx / len;
        pendingKickDY    = dy / len;
        pendingKickPower = power;
        kickApplied      = false;
        kickTimer        = KICK_DURATION;
        kickingRightLeg  = !kickingRightLeg; // swap leg each kick
        facingX = pendingKickDX;
        facingY = pendingKickDY;
    }


    public boolean isKicking() { return kickTimer > 0; }


    public int getX()       { return (int) position.getX(); }
    public int getY()       { return (int) position.getY(); }
    public int getCenterX() { return (int) position.getX() + width / 2; }
    public int getCenterY() { return (int) position.getY() + height / 2; }
    public int getWidth()   { return width; }
    public int getHeight()  { return height; }


    // draws the player: shadow, legs, body + jersey number, arms, head, name
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;


        int x = (int) position.getX();
        int y = (int) position.getY();
        int w = width;
        int h = height;


        // sizes
        int headSize = 16;
        int headX = x + w / 2 - headSize / 2;
        int headY = y + 2;


        int bodyW = 18;
        int bodyH = 22;
        int bodyX = x + w / 2 - bodyW / 2;
        int bodyY = headY + headSize - 2;


        int hipY      = bodyY + bodyH;
        int hipLeftX  = bodyX + 4;
        int hipRightX = bodyX + bodyW - 4;


        // shadow
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillOval(x + w / 2 - 14, y + h - 4, 28, 7);


        // legs first so the jersey covers them
        drawLegs(g2, hipLeftX, hipRightX, hipY);


        // jersey
        g2.setColor(color);
        g2.fillRoundRect(bodyX, bodyY, bodyW, bodyH, 6, 6);
        g2.setColor(new Color(255, 255, 255, 80));
        g2.fillRect(bodyX + bodyW / 2 - 1, bodyY + 2, 2, bodyH - 4);


        // jersey number (or role letter if no number was set)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        FontMetrics fmNum = g2.getFontMetrics();
        String num = playerNumber.isEmpty() ? role : playerNumber;
        int nw = fmNum.stringWidth(num);
        g2.drawString(num, bodyX + (bodyW - nw) / 2, bodyY + 16);


        // arms
        drawArms(g2, bodyX, bodyY, bodyW);


        // head
        g2.setColor(new Color(255, 220, 185));
        g2.fillOval(headX, headY, headSize, headSize);
        // hair
        g2.setColor(new Color(40, 20, 10));
        g2.fillArc(headX, headY, headSize, headSize / 2 + 3, 0, 180);


        // eyes - shift a bit with facing
        int eyeOff = facingX > 0.2 ? 1 : facingX < -0.2 ? -1 : 0;
        g2.setColor(Color.BLACK);
        g2.fillOval(headX + 4 + eyeOff, headY + 9, 2, 2);
        g2.fillOval(headX + headSize - 6 + eyeOff, headY + 9, 2, 2);


        // outlines
        g2.setColor(new Color(0, 0, 0, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(bodyX, bodyY, bodyW, bodyH, 6, 6);
        g2.drawOval(headX, headY, headSize, headSize);


        // name pill above the head
        if (!playerName.isEmpty()) {
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int nnw = fm.stringWidth(playerName);
            int nx = x + w / 2 - nnw / 2;
            int ny = y - 4;
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(nx - 3, ny - 8, nnw + 6, 11, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(playerName, nx, ny);
        }
    }


    // arms swing opposite to legs
    private void drawArms(Graphics2D g2, int bodyX, int bodyY, int bodyW) {
        double spd = Math.hypot(velocityX, velocityY);
        double swing = Math.sin(walkPhase) * Math.min(5, spd * 1.0);
        int armY = bodyY + 4;


        double ax = facingX, ay = facingY;
        int leftHandX  = bodyX - 4            + (int)(ax * swing);
        int leftHandY  = armY + 10            + (int)(ay * swing);
        int rightHandX = bodyX + bodyW + 4    - (int)(ax * swing);
        int rightHandY = armY + 10            - (int)(ay * swing);


        // when kicking, the other arm swings forward for balance
        if (kickTimer > 0) {
            double p = (KICK_DURATION - kickTimer) / (double) KICK_DURATION;
            double swingAmt = Math.sin(p * Math.PI) * 7;
            if (kickingRightLeg) {
                leftHandX  += (int)(facingX * swingAmt);
                leftHandY  += (int)(facingY * swingAmt);
            } else {
                rightHandX += (int)(facingX * swingAmt);
                rightHandY += (int)(facingY * swingAmt);
            }
        }


        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color.darker());
        g2.drawLine(bodyX + 1,             armY, leftHandX,  leftHandY);
        g2.drawLine(bodyX + bodyW - 1,     armY, rightHandX, rightHandY);
        g2.setColor(new Color(255, 220, 185));
        g2.fillOval(leftHandX - 2,  leftHandY - 2,  4, 4);
        g2.fillOval(rightHandX - 2, rightHandY - 2, 4, 4);
    }


    // legs + shorts + shoes.
    // walking = feet swing along the facing direction (so diagonals look right).
    // kicking = one foot extends forward, the other plants. legs alternate each kick.
    private void drawLegs(Graphics2D g2, int hipLeftX, int hipRightX, int hipY) {
        int footBaseY = (int) position.getY() + height;
        double spd = Math.hypot(velocityX, velocityY);


        int leftFootX  = hipLeftX  - 1;
        int leftFootY  = footBaseY - 3;
        int rightFootX = hipRightX + 1;
        int rightFootY = footBaseY - 3;


        if (kickTimer == 0) {
            // walking
            double amp = Math.min(7, 1.5 + spd * 1.2);
            double phase = walkPhase;
            double leftSwing  = Math.sin(phase)           * amp;
            double rightSwing = Math.sin(phase + Math.PI) * amp;


            leftFootX  += (int)(facingX * leftSwing);
            leftFootY  += (int)(facingY * leftSwing);
            rightFootX += (int)(facingX * rightSwing);
            rightFootY += (int)(facingY * rightSwing);
        } else {
            // kicking
            double p = (KICK_DURATION - kickTimer) / (double) KICK_DURATION;
            double extend = Math.sin(p * Math.PI) * 24;
            if (kickingRightLeg) {
                rightFootX = hipRightX + (int)(facingX * extend);
                rightFootY = footBaseY - 3 + (int)(facingY * extend);
                leftFootX  = hipLeftX  - 1;
                leftFootY  = footBaseY - 2;
            } else {
                leftFootX  = hipLeftX + (int)(facingX * extend);
                leftFootY  = footBaseY - 3 + (int)(facingY * extend);
                rightFootX = hipRightX + 1;
                rightFootY = footBaseY - 2;
            }
        }


        // shorts
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(hipLeftX - 2, hipY - 2, (hipRightX - hipLeftX) + 4, 7, 4, 4);


        // legs
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 220, 185));
        g2.drawLine(hipLeftX,  hipY + 2, leftFootX,  leftFootY);
        g2.drawLine(hipRightX, hipY + 2, rightFootX, rightFootY);


        // socks in team color
        g2.setColor(color);
        g2.drawLine(
            (int)(leftFootX  - (leftFootX  - hipLeftX)  * 0.2),
            (int)(leftFootY  - (leftFootY  - (hipY + 2)) * 0.2),
            leftFootX, leftFootY);
        g2.drawLine(
            (int)(rightFootX - (rightFootX - hipRightX) * 0.2),
            (int)(rightFootY - (rightFootY - (hipY + 2)) * 0.2),
            rightFootX, rightFootY);


        // shoes
        g2.setColor(Color.BLACK);
        g2.fillOval(leftFootX  - 4, leftFootY  - 2, 9, 5);
        g2.fillOval(rightFootX - 4, rightFootY - 2, 9, 5);
    }
}