import java.io.Serializable;

public class Player implements Serializable {
    private String name;
    private int points;
    private int sessionId;
    
    public Player(String name, int initialPoints) {
        this.name = name;
        this.points = initialPoints;
        this.sessionId = -1;
    }
    
    public String getName() {
        return name;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void addPoints(int amount) {
        this.points += amount;
    }
    
    public boolean deductPoints(int amount) {
        if (points >= amount) {
            points -= amount;
            return true;
        }
        return false;
    }
    
    public int getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String toString() {
        return name + " (" + points + " points)";
    }
}