package hm.kabo.scene.scenes;

import java.util.List;

import hm.kabo.core.GameContext;
import hm.kabo.scene.Choice;
import hm.kabo.scene.Scene;
import hm.kabo.scene.SceneManager;

public class TitleScene implements Scene
{
    private final SceneManager sceneManager;

    public TitleScene(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @Override 
    public String getText(GameContext context)
    {
        return 
        """
        ================================
                MY TEXT RPG
                Version 0.1
        ================================
        
        A simple scene-based foundation.
        
        Choose an option below:

        """;
    }

    @Override 
    public List<Choice> getChoices(GameContext context)
    {
        return List.of
        (
            new Choice("New Game", c -> sceneManager.switchTo(new TestExplorationScene(sceneManager))),
            new Choice("Continue", null, false),
            new Choice("Options", null, false),
            new Choice("Quit", c -> System.exit(0))
        );
    }
}
