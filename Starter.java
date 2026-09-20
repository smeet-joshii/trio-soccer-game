// Group: [Trio Soccor Game]
// Names: Smeet, Tabir, Eli
// Date: April 27, 2026
// Starter.java
// main JFrame for the soccer game. has the game loop, AI, key handling, etc.
// >>> RUN main() IN THIS FILE TO PLAY THE GAME <<<
// no outside code, just standard java packages.


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.*;


public class Starter extends JFrame implements ActionListener, KeyListener {


    // virtual screen size - all gameplay uses these coords.
    // GamePanel scales it to fit the actual window.
    public static final int VW = 1180;
    public static final int VH = 745;


    // field rectangle inside the virtual area
    public static final int FIELD_X = 50;
    public static final int FIELD_Y = 50;
    public static final int FIELD_W = 1080;
    public static final int FIELD_H = 650;


    // goal opening (centered on field, so 300..450 because field center y = 375)
    public static final int GOAL_Y_TOP    = 300;
    public static final int GOAL_Y_BOTTOM = 450;
    public static final int GOAL_DEPTH    = 28;


    // how much the net is bulging out right now. decays each frame.
    public static double leftNetBulge  = 0;
    public static double rightNetBulge = 0;


    // kick power. while space is held, spaceCharge ramps up to MAX_CHARGE.
    public static final int MAX_CHARGE = 40;
    public static int spaceCharge      = 0;
    public static boolean spaceCharging = false;


    // buttons
    private JButton newGameButton, pauseButton, exitButton;
    private JPanel  buttonPanel;


    // game objects (static so other classes can reach them)
    protected static Team   team1, team2;
    protected static String gameState;          // MENU / PLAYING / PAUSED / ENDED
    protected static Field  field = new Field();
    protected static Ball   ball;


    // scoreboard + active player
    protected static int    timeLeft;
    protected static int    activePlayerIndex;
    protected static String lastGoalMessage = "";
    protected static int    goalMessageTimer = 0;


    // timers
    private Timer gameLoop; // ~60 FPS
    private static Timer passing;    
    private Timer matchTimer;  // 1 second clock


    // panel that draws stuff
    private GamePanel gamePanel;


    // which keys are held
    private boolean upPressed, downPressed, leftPressed, rightPressed;


    private static boolean isPassing;
    // cooldown so AI doesnt spam pass/shoot every frame
    private int[] aiDecisionCooldown = new int[2];


    public Starter() {
        setTitle("Soccer Game");
        setSize(1180, 795);
        setMinimumSize(new Dimension(760, 560));
        // DISPOSE_ON_CLOSE so closing the game window doesnt kill BlueJ
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());


        // 3 buttons at the bottom
        buttonPanel   = new JPanel(new GridLayout(1, 3, 2, 2));
        newGameButton = new JButton("New Game");
        pauseButton   = new JButton("Pause Game");
        exitButton    = new JButton("Exit Game");


        // buttons MUST NOT take keyboard focus or arrow keys stop working
        newGameButton.setFocusable(false);
        pauseButton.setFocusable(false);
        exitButton.setFocusable(false);


        newGameButton.addActionListener(this);
        pauseButton.addActionListener(this);
        exitButton.addActionListener(this);


        buttonPanel.add(newGameButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(exitButton);


        // game panel
        gamePanel = new GamePanel(this);
        add(gamePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);


        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);


        // starting values
        timeLeft = 180;            // 3 min match
        activePlayerIndex = 1;
        gameState = "MENU";


        // 60 FPS loop
        gameLoop = new Timer(16, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (gameState.equals("PLAYING")) {
                    moveUserPlayer();
                    moveAIPlayers();
                    updatePlayers();
                    updateBall();
                    handleBallPlayerCollisions();
                    handleGoals();
                    if (goalMessageTimer > 0) goalMessageTimer--;


                    // power meter charges while space is held
                    if (spaceCharging && spaceCharge < MAX_CHARGE) spaceCharge++;


                    // net bulge decays
                    leftNetBulge  *= 0.92;
                    rightNetBulge *= 0.92;
                    if (leftNetBulge  < 0.1) leftNetBulge  = 0;
                    if (rightNetBulge < 0.1) rightNetBulge = 0;
                }
                gamePanel.repaint();
            }
        });


        // 1 second clock tick
        matchTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (gameState.equals("PLAYING")) {
                    timeLeft--;
                    if (timeLeft <= 0) {
                        timeLeft = 0;
                        gameState = "ENDED";
                        pauseButton.setText("Pause Game");
                    }
                }
            }
        });


        gameLoop.start();
        matchTimer.start();


        // give keyboard focus back
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { gamePanel.requestFocusInWindow(); }
        });
    }


    // button clicks
    // Input of one ActionEvent
    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();


        if (action.equals("New Game")) {
            resetMatch();
            gameState = "PLAYING";
            pauseButton.setText("Pause Game");
        } else if (action.equals("Pause Game") || action.equals("Resume Game")) {
            if (gameState.equals("PLAYING")) {
                gameState = "PAUSED";
                pauseButton.setText("Resume Game");
            } else if (gameState.equals("PAUSED")) {
                gameState = "PLAYING";
                pauseButton.setText("Pause Game");
            }
        } else if (action.equals("Exit Game")) {
            pauseButton.setText("Pause Game");
            gameState = "ENDED";
            gamePanel.repaint();
        }


        // give focus back so keys keep working after a click
        gamePanel.requestFocusInWindow();
        gamePanel.repaint();
    }


    // reset everything for a fresh match
    //No inputs/outputs
    public void resetMatch() {
        timeLeft = 180;
        activePlayerIndex = 1;


        team1.setScore(0);
        team2.setScore(0);


        team1.resetPositions();
        team2.resetPositions();


        ball = new Ball();
        ball.update(new Point2D.Double(VW / 2.0 - 10, FIELD_Y + FIELD_H / 2.0 - 10));
        ball.setVelocity(0, 0);


        upPressed = downPressed = leftPressed = rightPressed = false;
        spaceCharging = false;
        spaceCharge = 0;
        leftNetBulge = rightNetBulge = 0;
        lastGoalMessage = "";
        goalMessageTimer = 0;
        aiDecisionCooldown[0] = 0;
        aiDecisionCooldown[1] = 0;
    }


    // user's active player movement (WASD / arrows)
    // No inputs/outputs
    private void moveUserPlayer() {
        Player active = team1.players[activePlayerIndex];
        double accel = 0.75;
        if (upPressed)    active.addVelocity(0, -accel);
        if (downPressed)  active.addVelocity(0,  accel);
        if (leftPressed)  active.addVelocity(-accel, 0);
        if (rightPressed) active.addVelocity( accel, 0);
    }


    // AI runs once per frame for both teams
    //No inputs/outputs
    private void moveAIPlayers() {
        int team1Possessor = possessorOnTeam(team1);
        int team2Possessor = possessorOnTeam(team2);


        // team1 (RED) attacks right, team2 (BLUE) attacks left
        updateTeamAI(team1, false, team1Possessor, team2Possessor >= 0);
        updateTeamAI(team2, true,  team2Possessor, team1Possessor >= 0);


        if (aiDecisionCooldown[0] > 0) aiDecisionCooldown[0]--;
        if (aiDecisionCooldown[1] > 0) aiDecisionCooldown[1]--;
    }


    // controls one team for one frame.
    // 4 inputs:
    // attackingLeft = true if this team attacks the left goal.
    // possessorIdx = index of the player on this team holding the ball (-1 if none).
    // Team: The team that we want to update the Ai of
    // opponentHasBall: a boolean that keeps track if the oppenent has possesion of the ball
    // Not outputs
    private void updateTeamAI(Team team, boolean attackingLeft,
                              int possessorIdx, boolean opponentHasBall) {
        boolean weHaveBall = possessorIdx >= 0;
        boolean isOpponent = (team == team2);   // BLUE = weaker AI


        // tuned values - opponent is slower / less accurate
        double chaseAccel   = isOpponent ? 0.70 : 0.55;
        double supportAccel = isOpponent ? 0.55 : 0.42;
        double gkRestAccel  = isOpponent ? 0.60 : 0.45;
        double gkRushAccel  = isOpponent ? 0.75 : 0.55;


        int goalX       = attackingLeft ? FIELD_X : (FIELD_X + FIELD_W);
        int ownGoalX    = attackingLeft ? (FIELD_X + FIELD_W) : FIELD_X;
        int goalCenterY = FIELD_Y + FIELD_H / 2;


        // GOALIE - sit on the line normally, rush out when ball is close
        Player gk = team.players[0];
        int gkRestX = attackingLeft ? (FIELD_X + FIELD_W - 95) : (FIELD_X + 95);


        boolean ballNearOurGoal;
        if (attackingLeft) ballNearOurGoal = ball.getCenterX() > FIELD_X + FIELD_W - 260;
        else               ballNearOurGoal = ball.getCenterX() < FIELD_X + 260;


        int gkTargetX = gkRestX;
        int gkTargetY = clamp(ball.getCenterY(), goalCenterY - 90, goalCenterY + 90);


        int gkReach = isOpponent ? 70 : 90;


        if (ballNearOurGoal && !weHaveBall
                && Math.abs(ball.getCenterX() - gkRestX) < gkReach) {
            gkTargetX = clamp(ball.getCenterX(), gkRestX - 40, gkRestX + 40);
            gkTargetY = ball.getCenterY();
            gk.moveToward(gkTargetX, gkTargetY, gkRushAccel);
        } else {
            gk.moveToward(gkTargetX, gkTargetY, gkRestAccel);
        }
        gk.setFacingTowardX(attackingLeft ? -1 : 1);


        // keeper touches ball -> clear it upfield
        if (touchingBall(gk) && !gk.isKicking()) {
            double dx = attackingLeft ? -1.0 : 1.0;
            double dy = (gk.getCenterY() < goalCenterY) ? 0.35 : -0.35;
            applyKick(gk, dx, dy, isOpponent ? 12.0 : 15.0);
        }


        // FORWARDS (index 1 and 2)
        int chaser = team.getNearestOutfieldPlayerIndex(ball);


        // for team1 don't AI-control the player the user is using
        boolean skipActive = (team == team1);
        if (skipActive && chaser == activePlayerIndex) {
            chaser = (activePlayerIndex == 1) ? 2 : 1;
        }


        for (int i = 1; i < 3; i++) {
            if (skipActive && i == activePlayerIndex) continue;
            Player p = team.players[i];


            if (i == possessorIdx) {
                // has the ball - run the carrier logic
                handleAIBallCarrier(p, team, attackingLeft);
            } else if (i == chaser) {
                // chase the ball
                p.moveToward(ball.getCenterX(), ball.getCenterY(), chaseAccel);
            } else {
                // other player: support / defend / hang out
                int supX, supY;
                if (weHaveBall) {
                    // attacking - push up for a pass
                    supX = (int)(ball.getCenterX() * 0.35 + goalX * 0.65);
                    if (attackingLeft) supX = Math.min(supX, ball.getCenterX() - 60);
                    else               supX = Math.max(supX, ball.getCenterX() + 60);
                    supY = (ball.getCenterY() > goalCenterY) ? goalCenterY - 90 : goalCenterY + 90;
                } else if (opponentHasBall) {
                    // defending - drop back
                    supX = (int)(ball.getCenterX() * 0.55 + ownGoalX * 0.45);
                    supY = (i == 1) ? goalCenterY - 70 : goalCenterY + 70;
                } else {
                    // ball is loose - hang out in the middle
                    supX = (int)(ball.getCenterX() * 0.6 + goalX * 0.4);
                    supY = (i == 1) ? goalCenterY - 80 : goalCenterY + 80;
                }
                supX = clamp(supX, FIELD_X + 80, FIELD_X + FIELD_W - 80);
                supY = clamp(supY, FIELD_Y + 60, FIELD_Y + FIELD_H - 60);
                p.moveToward(supX, supY, supportAccel);
            }
        }
    }


    // what an AI player does when they have the ball:
    // shoot if close to goal, else maybe pass, else clear if deep, else dribble
    // No outputs
    // 3 inputs:
    private void handleAIBallCarrier(Player p, Team team, boolean attackingLeft) {
        if (p.isKicking()) return;  // dont interrupt a kick


        boolean isOpponent = (team == team2);


        // tuning per team
        int    shootDist     = isOpponent ? 210 : 280;
        double shootPower    = isOpponent ? 14.0 : 17.0;
        double shootAimNoise = isOpponent ? 90   : 35;
        double passChance    = isOpponent ? 0.020 : 0.035;
        int    shootCooldown = isOpponent ? 70 : 45;
        int    passCooldown  = isOpponent ? 55 : 40;
        double dribbleAccel  = isOpponent ? 0.70 : 0.55;


        int goalX       = attackingLeft ? FIELD_X + 5 : FIELD_X + FIELD_W - 5;
        int goalCenterY = FIELD_Y + FIELD_H / 2;


        double dxGoal = goalX - p.getCenterX();
        double dyGoal = goalCenterY - p.getCenterY();
        double distToGoal = Math.hypot(dxGoal, dyGoal);


        int cdIdx = (team == team1) ? 0 : 1;


        // shoot
        if (distToGoal < shootDist && aiDecisionCooldown[cdIdx] == 0) {
            // aim a little away from the keeper, plus some random miss
            double aimY = (p.getCenterY() < goalCenterY) ? goalCenterY + 40 : goalCenterY - 40;
            aimY += (Math.random() - 0.5) * 2 * shootAimNoise;
            applyKick(p, goalX - p.getCenterX(), aimY - p.getCenterY(), shootPower);
            aiDecisionCooldown[cdIdx] = shootCooldown;
            return;
        }


        // pass sometimes
        if (aiDecisionCooldown[cdIdx] == 0) {
            Player mate = findForwardTeammate(team, p, attackingLeft);
            if (mate != null && Math.random() < passChance) {
                double pdx = mate.getCenterX() - p.getCenterX();
                double pdy = mate.getCenterY() - p.getCenterY();
                double pd  = Math.hypot(pdx, pdy);
                double power = Math.min(14.0, Math.max(7.0, pd / 55.0));
                applyKick(p, pdx, pdy, power);
                aiDecisionCooldown[cdIdx] = passCooldown;
                return;
            }
        }


        // long clear if pinned in our half
        if (aiDecisionCooldown[cdIdx] == 0) {
            if ((attackingLeft && p.getCenterX() > FIELD_X + FIELD_W - 180)
             || (!attackingLeft && p.getCenterX() < FIELD_X + 180)) {
                applyKick(p, attackingLeft ? -1 : 1, 0, isOpponent ? 13.0 : 16.0);
                aiDecisionCooldown[cdIdx] = shootCooldown;
                return;
            }
        }


        // else dribble toward goal with a small zig-zag
        double zig = Math.sin(System.currentTimeMillis() / 280.0 + (team == team1 ? 0 : 1.3)) * 55;
        double targetY = clamp((int)(goalCenterY + zig),
                               FIELD_Y + 80, FIELD_Y + FIELD_H - 80);
        p.moveToward(goalX, targetY, dribbleAccel);


        // glue ball just in front of carrier
        double ahead = 24;
        double bx = p.getCenterX() + p.facingX * ahead - 10;
        double by = p.getCenterY() + p.facingY * ahead - 10;
        ball.position.setLocation(
            ball.position.getX() + (bx - ball.position.getX()) * 0.35,
            ball.position.getY() + (by - ball.position.getY()) * 0.35);
        ball.setVelocity(p.velocityX * 0.6, p.velocityY * 0.6);
    }


    // teammate who is furthest ahead toward goal (used for passing)
    private Player findForwardTeammate(Team team, Player carrier, boolean attackingLeft) {
        Player best = null;
        double bestAhead = 80;
        for (int i = 1; i < 3; i++) {
            Player m = team.players[i];
            if (m == carrier) continue;
            double ahead = attackingLeft
                ? (carrier.getCenterX() - m.getCenterX())
                : (m.getCenterX() - carrier.getCenterX());
            if (ahead > bestAhead) {
                bestAhead = ahead;
                best = m;
            }
        }
        return best;
    }


    // who on this team is touching the ball, -1 if nobody
    private int possessorOnTeam(Team team) {
        for (int i = 0; i < 3; i++) {
            if (touchingBall(team.players[i])) return i;
        }
        return -1;
    }


    // do the kick: set ball velocity, push ball forward so it doesnt re-hit
    // the kicker, start the kick animation
    private void applyKick(Player p, double dx, double dy, double power) {
        double len = Math.hypot(dx, dy);
        if (len < 0.001) { dx = p.facingX; dy = p.facingY; len = Math.max(0.001, Math.hypot(dx, dy)); }
        double ux = dx / len, uy = dy / len;
        ball.setVelocity(ux * power, uy * power);
        ball.position.setLocation(
            p.getCenterX() + ux * 28 - 10,
            p.getCenterY() + uy * 28 - 10);
        p.startKick(dx, dy, power);
    }


    // update player positions + keep them in bounds
    private void updatePlayers() {
        for (int i = 0; i < 3; i++) {
            team1.players[i].updateMotion();
            if (i == 0) keepGoalKeeperInZone(team1.players[i], true);
            else        keepPlayerInField(team1.players[i]);
        }
        for (int i = 0; i < 3; i++) {
            team2.players[i].updateMotion();
            if (i == 0) keepGoalKeeperInZone(team2.players[i], false);
            else        keepPlayerInField(team2.players[i]);
        }
    }


    private void keepPlayerInField(Player p) {
        double minX = FIELD_X;
        double minY = FIELD_Y;
        double maxX = FIELD_X + FIELD_W - p.getWidth();
        double maxY = FIELD_Y + FIELD_H - p.getHeight();
        if (p.position.getX() < minX) p.setPosition(minX, p.position.getY());
        if (p.position.getY() < minY) p.setPosition(p.position.getX(), minY);
        if (p.position.getX() > maxX) p.setPosition(maxX, p.position.getY());
        if (p.position.getY() > maxY) p.setPosition(p.position.getX(), maxY);
    }


    private void keepGoalKeeperInZone(Player p, boolean leftSide) {
        int minX, maxX;
        int minY = GOAL_Y_TOP - 40;
        int maxY = GOAL_Y_BOTTOM - p.getHeight() + 40;
        if (leftSide) { minX = FIELD_X + 8;  maxX = FIELD_X + 130; }
        else          { minX = FIELD_X + FIELD_W - 166; maxX = FIELD_X + FIELD_W - 44; }


        if (p.position.getX() < minX) p.setPosition(minX, p.position.getY());
        if (p.position.getX() > maxX) p.setPosition(maxX, p.position.getY());
        if (p.position.getY() < minY) p.setPosition(p.position.getX(), minY);
        if (p.position.getY() > maxY) p.setPosition(p.position.getX(), maxY);
    }


    // ball physics: move, bounce off walls, hit back of net
    private void updateBall() {
        ball.move();


        // top + bottom touchlines
        if (ball.position.getY() < FIELD_Y) {
            ball.position.setLocation(ball.position.getX(), FIELD_Y);
            ball.setDY(Math.abs(ball.getDY()) * 0.75);
        }
        if (ball.position.getY() + 20 > FIELD_Y + FIELD_H) {
            ball.position.setLocation(ball.position.getX(), FIELD_Y + FIELD_H - 20);
            ball.setDY(-Math.abs(ball.getDY()) * 0.75);
        }


        // left + right walls (but ball can pass through the goal opening)
        boolean ballInGoalYRange = ball.getCenterY() >= GOAL_Y_TOP
                                && ball.getCenterY() <= GOAL_Y_BOTTOM;


        if (ball.position.getX() < FIELD_X && !ballInGoalYRange) {
            ball.position.setLocation(FIELD_X, ball.position.getY());
            ball.setDX(Math.abs(ball.getDX()) * 0.75);
        }
        if (ball.position.getX() + 20 > FIELD_X + FIELD_W && !ballInGoalYRange) {
            ball.position.setLocation(FIELD_X + FIELD_W - 20, ball.position.getY());
            ball.setDX(-Math.abs(ball.getDX()) * 0.75);
        }


        // back of nets - stop ball + bulge net
        if (ball.position.getX() < FIELD_X - GOAL_DEPTH) {
            ball.position.setLocation(FIELD_X - GOAL_DEPTH, ball.position.getY());
            double impact = Math.min(22, Math.abs(ball.getDX()) * 1.4 + 6);
            if (impact > leftNetBulge) leftNetBulge = impact;
            ball.setVelocity(-ball.getDX() * 0.15, ball.getDY() * 0.2);
        }
        if (ball.position.getX() + 20 > FIELD_X + FIELD_W + GOAL_DEPTH) {
            ball.position.setLocation(FIELD_X + FIELD_W + GOAL_DEPTH - 20, ball.position.getY());
            double impact = Math.min(22, Math.abs(ball.getDX()) * 1.4 + 6);
            if (impact > rightNetBulge) rightNetBulge = impact;
            ball.setVelocity(-ball.getDX() * 0.15, ball.getDY() * 0.2);
        }


        ball.applyFriction();
    }


    // user's player carries the ball when touching it
    private void handleBallPlayerCollisions() {
        Player active = team1.players[activePlayerIndex];
        if (!isPassing && touchingBall(active) && !active.isKicking()) {
    carryBall(active, 0.35);
}


    }


    // pull ball smoothly to a spot in front of the player
    private void carryBall(Player p, double smooth) {
        double ahead = 26;
        double bx = p.getCenterX() + p.facingX * ahead - 10;
        double by = p.getCenterY() + p.facingY * ahead - 10;
        ball.position.setLocation(
            ball.position.getX() + (bx - ball.position.getX()) * smooth,
            ball.position.getY() + (by - ball.position.getY()) * smooth);
        ball.setVelocity(p.velocityX * 0.7, p.velocityY * 0.7);
    }


    // is this player touching the ball?
    private static boolean touchingBall(Player p) {
        Rectangle playerRect = new Rectangle(
            (int)p.position.getX(), (int)p.position.getY(),
            p.getWidth(), p.getHeight());
        Rectangle ballRect = new Rectangle(
            (int)ball.position.getX() - 4, (int)ball.position.getY() - 4, 28, 28);
        return playerRect.intersects(ballRect);
    }


    // used by GamePanel for the aim arrow
    public boolean activePlayerHasBall() {
        Player active = team1.players[activePlayerIndex];
        return distance(active.getCenterX(), active.getCenterY(),
                        ball.getCenterX(), ball.getCenterY()) < 60;
    }


    // check + handle goals
    private void handleGoals() {
        int scorer = field.checkGoal(ball);
        if (scorer == 1) {
            // ball crossed right line - team1 (RED) scored
            team1.incrementScore();
            lastGoalMessage = team1.getName() + " scores!";
            goalMessageTimer = 90;
            rightNetBulge = Math.max(rightNetBulge, 20);
            resetPositionsAfterGoal();
        } else if (scorer == 2) {
            // ball crossed left line - team2 (BLUE) scored
            team2.incrementScore();
            lastGoalMessage = team2.getName() + " scores!";
            goalMessageTimer = 90;
            leftNetBulge = Math.max(leftNetBulge, 20);
            resetPositionsAfterGoal();
        }
    }


    private void resetPositionsAfterGoal() {
        team1.resetPositions();
        team2.resetPositions();
        ball.update(new Point2D.Double(VW / 2.0 - 10, FIELD_Y + FIELD_H / 2.0 - 10));
        ball.setVelocity(0, 0);
        activePlayerIndex = 1;
    }


    // helpers
    private int    clamp(int v, int lo, int hi)       { return Math.max(lo, Math.min(hi, v)); }
    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double distance(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1, dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }


    // aim direction from arrow keys (used when kicking)
    public int getAimDX() {
        if (leftPressed && !rightPressed) return -1;
        if (rightPressed && !leftPressed) return 1;
        return 0;
    }
    public int getAimDY() {
        if (upPressed && !downPressed) return -1;
        if (downPressed && !upPressed) return 1;
        return 0;
    }


    // user kick
    private void userKick(double power) {
        Player active = team1.players[activePlayerIndex];


        // aim = arrow keys if any pressed, else current facing
        int adx = getAimDX();
        int ady = getAimDY();
        double dx, dy;
        if (adx == 0 && ady == 0) {
            dx = active.facingX;
            dy = active.facingY;
        } else {
            dx = adx;
            dy = ady;
        }


        double dist = distance(active.getCenterX(), active.getCenterY(),
                               ball.getCenterX(), ball.getCenterY());
        if (dist < 70) {
            applyKick(active, dx, dy, power);
        } else {
            // out of range, just play the animation
            active.startKick(dx, dy, power);
        }
    }


    
    
    // pass to teammate further up the pitch
    protected static void passBall() {
        Player active = null;
        if (isPassing) return;


        try{
            active = team1.players[activePlayerIndex];
        }
        catch(NullPointerException e){
           return; 
        }
        if (distance(active.getCenterX(), active.getCenterY(), ball.getCenterX(), ball.getCenterY()) < 58) {
            int targetIndex;


            if (activePlayerIndex == 1) {
                targetIndex = 2;
            }
            else {
                targetIndex = 1;
            }


            Player teammate = team1.players[targetIndex];
            double dx = teammate.getCenterX() - active.getCenterX();
            double dy = teammate.getCenterY() - active.getCenterY();


            double scale = 0.12;
            ball.setVelocity(dx * scale, dy * scale);
            final Player cur = active;
            
            passing = new Timer(320, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    if(distance(teammate.getCenterX(), teammate.getCenterY(), ball.getCenterX(), ball.getCenterY()) < distance(cur.getCenterX(), cur.getCenterY(), ball.getCenterX(), ball.getCenterY())){
                    switchPlayer();
                }
                isPassing = false;
                passing.stop();
            }
            });
            if(!isPassing){
                passing.start();
                isPassing = true;
        
            } 
        }
    }


    // swap the active player
    private static void switchPlayer() {


        // SAFETY: If team or ball not ready, do nothing
        if (team1 == null || ball == null) return;
    
        int closest = team1.getNearestPlayerIndex(ball);
        
        // SAFETY: If closest is invalid, fallback to cycling
        if (closest < 0 || closest >= team1.playerCount) {
            activePlayerIndex++;
            if (activePlayerIndex >= team1.playerCount) {
                activePlayerIndex = 0;
            }
            return;
        }


            // If closest is the same player you're already controlling → cycle instead
        if (closest == activePlayerIndex) {
            activePlayerIndex++;
            if (activePlayerIndex >= team1.playerCount) {
                activePlayerIndex = 0;
            }
        } 
        else {
            activePlayerIndex = closest;
        }
    }


    // key down
    public void keyPressed(KeyEvent e) {
        int t = e.getKeyCode();
        if (!gameState.equals("PLAYING")) return;


        if (t == KeyEvent.VK_D || t == KeyEvent.VK_RIGHT) rightPressed = true;
        if (t == KeyEvent.VK_A || t == KeyEvent.VK_LEFT)  leftPressed  = true;
        if (t == KeyEvent.VK_W || t == KeyEvent.VK_UP)    upPressed    = true;
        if (t == KeyEvent.VK_S || t == KeyEvent.VK_DOWN)  downPressed  = true;


        // SPACE - start charging (ignore key repeats)
        if (t == KeyEvent.VK_SPACE) {
            if (!spaceCharging) {
                spaceCharging = true;
                spaceCharge = 0;
            }
        }
        // SHIFT - max power kick
        if (t == KeyEvent.VK_SHIFT) userKick(19.0);
        if (t == KeyEvent.VK_C)     passBall();
        if (t == KeyEvent.VK_X)     switchPlayer();
    }


    // key up
    public void keyReleased(KeyEvent e) {
        int t = e.getKeyCode();
        if (t == KeyEvent.VK_D || t == KeyEvent.VK_RIGHT) rightPressed = false;
        if (t == KeyEvent.VK_A || t == KeyEvent.VK_LEFT)  leftPressed  = false;
        if (t == KeyEvent.VK_W || t == KeyEvent.VK_UP)    upPressed    = false;
        if (t == KeyEvent.VK_S || t == KeyEvent.VK_DOWN)  downPressed  = false;


        // SPACE released - turn charge into kick power
        if (t == KeyEvent.VK_SPACE) {
            if (spaceCharging && gameState.equals("PLAYING")) {
                double t01 = spaceCharge / (double) MAX_CHARGE;
                double power = 8.0 + t01 * 12.0;   // tap=8, full=20
                userKick(power);
            }
            spaceCharging = false;
            spaceCharge = 0;
        }
    }


    public void keyTyped(KeyEvent e) { }


    // entry point - run this to play
    public static void main(String[] args) {
        ball = new Ball();


        // team1 = RED (attacks right), team2 = BLUE (attacks left)
        team1 = new Team("Red Team",  new Color(220, 50, 50));
        team2 = new Team("Blue Team", new Color(30, 90, 220));


        // RED roster (user's team)
        Player r0 = new Player(new Point2D.Double(FIELD_X + 45,  FIELD_Y + FIELD_H/2 - 27));
        r0.role = "G"; r0.color = team1.color; r0.facingX =  1;
        r0.playerName = "KEEPER";   r0.playerNumber = "1";


        Player r1 = new Player(new Point2D.Double(FIELD_X + 250, FIELD_Y + 170));
        r1.role = "F"; r1.color = team1.color; r1.facingX =  1;
        r1.playerName = "RONALDO";  r1.playerNumber = "7";


        Player r2 = new Player(new Point2D.Double(FIELD_X + 250, FIELD_Y + 400));
        r2.role = "F"; r2.color = team1.color; r2.facingX =  1;
        r2.playerName = "MESSI";    r2.playerNumber = "10";


        team1.addPlayer(r0);
        team1.addPlayer(r1);
        team1.addPlayer(r2);


        // BLUE roster (opponent)
        Player b0 = new Player(new Point2D.Double(FIELD_X + FIELD_W - 80,  FIELD_Y + FIELD_H/2 - 27));
        b0.role = "G"; b0.color = team2.color; b0.facingX = -1;
        b0.playerName = "KEEPER";   b0.playerNumber = "1";


        Player b1 = new Player(new Point2D.Double(FIELD_X + FIELD_W - 280, FIELD_Y + 170));
        b1.role = "F"; b1.color = team2.color; b1.facingX = -1;
        b1.playerName = "MBAPPE";   b1.playerNumber = "9";


        Player b2 = new Player(new Point2D.Double(FIELD_X + FIELD_W - 280, FIELD_Y + 400));
        b2.role = "F"; b2.color = team2.color; b2.facingX = -1;
        b2.playerName = "NEYMAR";   b2.playerNumber = "11";


        team2.addPlayer(b0);
        team2.addPlayer(b1);
        team2.addPlayer(b2);


        Starter game = new Starter();
        game.setVisible(true);
    }
}