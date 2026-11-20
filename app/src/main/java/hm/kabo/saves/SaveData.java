package hm.kabo.saves;

import java.util.List;
import java.util.Map;

public record SaveData
(
    String slotName,
    String timestamp,
    String currentSceneId,
    int lookCounter,
    PlayerSnapshot player,
    List<String> inventoryItemIds,
    List<String> aliveEnemyIds,
    Map<String, String> replacedScenes
)
{
    public record PlayerSnapshot
    (
        String id,
        String name,
        int maxHP,
        int hp,
        int attack,
        int defense,
        int speed,
        int xp,
        int gold
    ) {}
}