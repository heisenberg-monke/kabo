package hm.kabo.entities.enemies;

import hm.kabo.entities.CharacterStats;
import hm.kabo.entities.Enemy;

public class Goblin extends Enemy
{
    public Goblin(String id)
    {
        super
        (
            id,
            "Forest Goblin",
            new CharacterStats(30, 8, 2, 7),
            15,
            8
        );
    }
}