package hm.kabo.core;

import hm.kabo.scene.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Game extends Application
{
    @Override 
    public void start(Stage stage)
    {
        var context = new GameContext();
        var sceneManager = new SceneManager(context, "/scenes.json");
        var inputManager = new InputManager(sceneManager);
        var renderer = new Renderer(stage, sceneManager, inputManager);

        sceneManager.switchTo("title");
        renderer.show();
    }
}
