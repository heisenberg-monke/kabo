package hm.kabo.scene.scenes;

import java.util.List;

import hm.kabo.scene.Choice;
import hm.kabo.scene.Scene;
import hm.kabo.scene.SceneManager;

public class TitleScene extends Scene
{
    public TitleScene(SceneManager sceneManager) 
    {
        super
        (
            context -> 
            """
            ================================
                    MY TEXT RPG
                    %s
            ================================
            
            A simple scene-based foundation.
            
            Choose an option below:

            """.formatted(context.version),
            context -> List.of 
            (
                new Choice("New Game", c -> sceneManager.startNewGame()),
                new Choice("Continue", null, false),
                new Choice("Options", null, false),
                new Choice("Quit", c -> System.exit(0))
            )
        );
    }
}
