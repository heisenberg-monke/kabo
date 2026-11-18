package hm.kabo.scene.scenes;

import java.util.List;

import hm.kabo.core.GameContext;
import hm.kabo.scene.Choice;
import hm.kabo.scene.Scene;
import hm.kabo.scene.SceneManager;

public class TestExplorationScene implements Scene
{
    private final SceneManager sceneManager;
    
    public TestExplorationScene(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @Override
    public String getText(GameContext context) 
    {
        return 
            """
            You stand in a quiet village square.
            The wind carries the smell of bread from a nearby bakery.
            
            You have looked around %d times.
            """.formatted(context.lookCounter);
    }

    @Override
    public List<Choice> getChoices(GameContext context) 
    {
        return List.of
        (
            new Choice("Look around", c -> ++c.lookCounter),
            new Choice("Talk to villager", null, false),
            new Choice("Open inventory", null, false),
            new Choice("Return to title", c -> sceneManager.switchTo("title", () -> new TitleScene(sceneManager)))
        );
    }

    
}
