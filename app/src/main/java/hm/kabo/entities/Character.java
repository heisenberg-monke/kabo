package hm.kabo.entities;

import java.util.concurrent.ThreadLocalRandom;

import com.google.common.math.Stats;

import hm.kabo.items.Item;

public abstract class Character 
{
    private final String id;
    private final String name;
    private final CharacterStats stats;
    private boolean defending = false;

    protected Character(String id, String name, CharacterStats stats)
    {
        this.id = id;
        this.name = name;
        this.stats = stats;
    }

    public void onTurnStart() {
        defending = false;
    }

    public String attack(Character target)
    {
        onTurnStart();

        int rawDamage = stats.getAttack();

        if (target.isDefending())
            rawDamage = Math.max(1, rawDamage / 2);

        int dealt = target.getStats().takeDamage(rawDamage);

        return "%s attacks %s for %d damage!".formatted(name, target.getName(), dealt);
    }

    public String defend()
    {
        defending = true;
        return "%s raises their guard and prepares to defend.".formatted(name);
    }

    public String useItem(Item item)
    {
        onTurnStart();

        if (item == null)
            return "%s has no item to use.".formatted(name);

        // Placeholder hook until Item carriesConsumable effects/stats
        return "%s uses %s.".formatted(name, item.name());
    }

    public boolean tryRun(Character opponent)
    {
        onTurnStart();

        int mySpeed = stats.getSpeed();
        int oppSpeed = opponent != null ? opponent.getStats().getSpeed() : 1;
        var chance = 0.5 + (mySpeed - oppSpeed) * 0.05;

        chance = Math.max(0.15, Math.min(0.90, chance));

        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public CharacterStats getStats() { return stats; }
    public boolean isDefending() { return defending; }
    public boolean isAlive() { return stats.isAlive(); }

    public void reset()
    {
        defending = false;
        stats.restoreFullHp();
    }
}
