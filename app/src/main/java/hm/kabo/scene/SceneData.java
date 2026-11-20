package hm.kabo.scene;

import java.util.List;

public record SceneData
(
    String id,
    String type,
    String text,
    List<String> enemyIds,
    String targetScene,
    List<ChoiceData> choices
) 
{
    public record ChoiceData
    (
        String label,
        String targetScene,
        String action,
        Boolean enabled 
    ) {}
}