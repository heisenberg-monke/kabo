package hm.kabo.items;

import hm.kabo.entities.Character;

@FunctionalInterface
public interface ItemEffect 
{
    String apply(Character target);

    static ItemEffect none() {
        return target -> target.getName() + " cannot use this item right now.";
    }

    static ItemEffect heal(int amount)
    {
        return target ->
        {
            int healed = target.getStats().heal(amount);
            return "%s recovers %d HP!".formatted(target.getName(), healed);
        };
    }

    static ItemEffect buffAttack(int bonus)
    {
        return target ->
        {
            target.getStats().setAttack(target.getStats().getAttack() + bonus);
            return "%s's attack increases by %d!".formatted(target.getName(), bonus);
        };
    }

    static ItemEffect buffDefense(int bonus)
    {
        return target ->
        {
            target.getStats().setDefense(target.getStats().getDefense() + bonus);
            return "%s's defense increases by %d!".formatted(target.getName(), bonus);
        };
    }
}
