package hm.kabo.entities;

public abstract class Enemy extends Character
{
    private final int xpReward;
    private final int goldReward;
    
    protected Enemy(String id, String name, CharacterStats stats, int xpReward, int goldReward)
    {
        super(id, name, stats);

        this.xpReward = Math.max(0, xpReward);
        this.goldReward = Math.max(0, goldReward);
    }

    public int getXPReward() { return xpReward; }
    public int getGoldReward() { return goldReward; }

    public String takeTurn(Character target)
    {
        if (getStats().getHP() <= getStats().getMaxHP() / 4 && Math.random() < 0.3)
            return defend();

        return attack(target);
    }
}
