package hm.kabo.entities;

public class Player extends Character 
{
    private int gold = 0;
    private int xp = 0;
    
    public Player(String id, String name, CharacterStats stats) {
        super(id, name, stats);
    }

    public int getGold() { return gold; }
    public int getXP() { return xp; }

    public void addGold(int amount) { this.gold = Math.max(0, this.gold + amount); }
    public void addXP(int amount) { this.xp = Math.max(0, this.xp + amount); }
}
