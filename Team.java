// Group: [Trio Soccor Game]
// Names: Smeet, Tabir, Eli
// Date: April 27, 2026
// Team.java
// one team. has 3 players (0 = goalie, 1 and 2 = forwards), name, color, score.
// run Starter.java to play the game.


import java.awt.*;


public class Team {
    Player[] players = new Player[3];


    int     score;
    String  name;
    Color   color;
    boolean attackingRight; // true = this team attacks the right side
    int     playerCount;


    // which side they attack is decided by the jersey color.
    // red-ish = attacks right, blue-ish = attacks left.
    public Team(String toCall, Color col) {
        score = 0;
        name = toCall;
        color = col;
        playerCount = 0;
        attackingRight = (col.getRed() > col.getBlue());
        for (int i = 0; i < players.length; i++) players[i] = null;
    }


    public void   setScore(int newScore) { score = newScore; }
    public void   incrementScore()       { score++; }
    public String getName()              { return name; }
    public int    getScore()             { return score; }


    // add a player into the first empty slot
    public void addPlayer(Player p) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null) {
                players[i] = p;
                p.team = this;
                playerCount++;
                break;
            }
        }
    }


    // closest outfield player to the ball (skip the keeper).
    // AI uses this to pick who chases.
    public int getNearestOutfieldPlayerIndex(Ball ball) {
        int bestIndex = 1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 1; i < playerCount; i++) {
            double dx = players[i].getCenterX() - ball.getCenterX();
            double dy = players[i].getCenterY() - ball.getCenterY();
            double d  = Math.hypot(dx, dy);
            if (d < bestDist) { bestDist = d; bestIndex = i; }
        }
        return bestIndex;
    }


    public void wonGame(){
        players[0].setPosition(570, 430);
        players[1].setPosition(510, 310);
        players[2].setPosition(640, 310);
    }
    
    public void lostGame(){
        players[0].setPosition(10000, 450);
        players[1].setPosition(51000, 330);
        players[2].setPosition(64000, 330);
    }
    
    public void tieWith(Team otherT){
        players[0].setPosition(570, 430);
        players[1].setPosition(510, 310);
        players[2].setPosition(640, 310);
        otherT.players[0].setPosition(570, 240);
        otherT.players[1].setPosition(510, 390);
        otherT.players[2].setPosition(640, 390);
    }
    
    public int getNearestPlayerIndex(Ball ball) {
        int bestIndex = 0;
        double bestDist = Double.MAX_VALUE;


        for (int i = 0; i < playerCount; i++) {
            Player p = players[i];
            if (p == null) 
            continue;


            double dx = p.position.getX() - ball.position.getX();
            double dy = p.position.getY() - ball.position.getY();
            double dist = Math.sqrt(dx*dx + dy*dy);


            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }


        return bestIndex;
    }
    
    // line everyone up at kickoff. depends on which side we attack.
    public void resetPositions() {
        if (attackingRight) {
            // we are on the LEFT side, attacking RIGHT
            players[0].setPosition(Starter.FIELD_X + 45,
                                   Starter.FIELD_Y + Starter.FIELD_H / 2.0 - 27);
            players[1].setPosition(Starter.FIELD_X + 250, Starter.FIELD_Y + 170);
            players[2].setPosition(Starter.FIELD_X + 250, Starter.FIELD_Y + 400);
            for (int i = 0; i < 3; i++) {
                players[i].facingX = 1;
                players[i].facingY = 0;
            }
        } else {
            // we are on the RIGHT side, attacking LEFT
            players[0].setPosition(Starter.FIELD_X + Starter.FIELD_W - 80,
                                   Starter.FIELD_Y + Starter.FIELD_H / 2.0 - 27);
            players[1].setPosition(Starter.FIELD_X + Starter.FIELD_W - 280,
                                   Starter.FIELD_Y + 170);
            players[2].setPosition(Starter.FIELD_X + Starter.FIELD_W - 280,
                                   Starter.FIELD_Y + 400);
            for (int i = 0; i < 3; i++) {
                players[i].facingX = -1;
                players[i].facingY = 0;
            }
        }
    }


    // draw all our players
    public void draw(Graphics2D g) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null) players[i].draw(g);
        }
    }
}