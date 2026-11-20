package hm.kabo.scene.scenes;

import java.util.List;

import hm.kabo.scene.Choice;
import hm.kabo.scene.Scene;
import hm.kabo.scene.SceneManager;
import javafx.application.Platform;

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
                new Choice("Continue", c -> sceneManager.openOverlay("load_game")),
                new Choice("Options", c -> sceneManager.openOverlay("options")),
                new Choice("Quit", c -> sceneManager.confirmQuit("title"))
            )
        );
    }
}