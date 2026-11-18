package hm.kabo.core;

import hm.kabo.scene.SceneManager;
import hm.kabo.scene.scenes.TitleScene;
import javafx.application.Application;
import javafx.stage.Stage;

public class Game extends Application
{
    @Override 
    public void start(Stage stage)
    {
        var context = new GameContext();
        var sceneManager = new SceneManager(context);
        var renderer = new Renderer(stage, sceneManager);

        sceneManager.switchTo(new TitleScene(sceneManager));
        renderer.show();
    }
}
