package hm.kabo.scene;

import hm.kabo.core.GameContext;
import hm.kabo.core.Renderer;

public class SceneManager 
{
    private final GameContext context;
    private Scene currentScene;
    private Renderer renderer;

    public SceneManager(GameContext context) {
        this.context = context;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public void switchTo(Scene newScene)
    {
        if(currentScene != null)
            currentScene.onExit(context);

        currentScene = newScene;

        if(renderer != null)
            renderer.refresh();
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public GameContext getContext() {
        return context;
    }

    public void handleChoice(int index)
    {
        if(currentScene == null)
            return;

        var choices = currentScene.getChoices(context);

        if(index >= 0 && index < choices.size())
        {
            choices.get(index).execute(context);

            if(renderer != null)
                renderer.refresh();
        }
    }
}
