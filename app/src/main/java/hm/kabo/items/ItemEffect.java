package hm.kabo.items;

import hm.kabo.entities.Character;

@FunctionalInterface
public interface ItemEffect
{
    record EffectResult(boolean applied, String message) {}

    EffectResult apply(Character target);

    static ItemEffect none() {
        return target -> new EffectResult(false, target.getName() + " cannot use this item right now.");
    }

    static ItemEffect heal(int amount)
    {
        return target ->
        {
            int healed = target.getStats().heal(amount);
            if (healed <= 0)
                return new EffectResult(false, "%s is already at full HP!".formatted(target.getName()));

            return new EffectResult(true, "%s recovers %d HP!".formatted(target.getName(), healed));
        };
    }

    static ItemEffect buffAttack(int bonus)
    {
        return target ->
        {
            target.getStats().setAttack(target.getStats().getAttack() + bonus);
            return new EffectResult(true, "%s's attack increases by %d!".formatted(target.getName(), bonus));
        };
    }

    static ItemEffect buffDefense(int bonus)
    {
        return target ->
        {
            target.getStats().setDefense(target.getStats().getDefense() + bonus);
            return new EffectResult(true, "%s's defense increases by %d!".formatted(target.getName(), bonus));
        };
    }
}