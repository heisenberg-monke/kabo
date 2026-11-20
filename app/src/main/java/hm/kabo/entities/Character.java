package hm.kabo.entities;

import java.util.concurrent.ThreadLocalRandom;

import com.google.common.math.Stats;

import hm.kabo.items.Item;
import hm.kabo.items.ItemEffect;

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

    public ItemEffect.EffectResult useItem(Item item)
    {
        if (item == null || !item.isUsable())
            return new ItemEffect.EffectResult(false, "%s cannot use this item.".formatted(name));

        var result = item.effect().apply(this);
        if (!result.applied())
            return result;

        onTurnStart();
        return new ItemEffect.EffectResult(true, "%s uses %s. %s".formatted(name, item.name(), result.message()));
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
