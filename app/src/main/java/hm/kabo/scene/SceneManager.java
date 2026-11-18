package hm.kabo.scene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import hm.kabo.core.GameContext;
import hm.kabo.core.Renderer;
import hm.kabo.scene.scenes.ExplorationScene;
import hm.kabo.scene.scenes.TitleScene;

public class SceneManager 
{
    private final GameContext context;
    private final Map<String, Scene> sceneCache = new HashMap<>();

    private Scene currentScene;
    private Renderer renderer;

    public SceneManager(GameContext context) {
        this.context = context;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public Scene getOrCreate(String id, Supplier<Scene> factory) {
        return sceneCache.computeIfAbsent(id, k -> factory.get());
    }

    public void registerScene(String id, Scene scene) {
        sceneCache.put(id, scene);
    }

    public void registerScenes()
    {
        registerScene("title", new TitleScene(this));

        registerScene("village_square", new ExplorationScene
        (
            context -> """
                You stand in a quiet village square.
                The wind carries the smell of bread from a nearby bakery.

                You have looked around %d times.
            """.formatted(context.lookCounter),

            context -> List.of
            (
                new Choice("Look around", c -> ++c.lookCounter),
                new Choice("Talk to villager", c -> switchTo("villager_chat")),
                new Choice("Open inventory", null, false),
                new Choice("Return to title", c -> switchTo("title"))
            )
        ));

        registerScene("villager_chat", new ExplorationScene
        (
            """
            An elderly villager pauses near the stone well and nods at you.
            
            "Fresh loaves just came out of the oven, traveller.
            Stay safe if you head past the eastern woods."
            """,
            List.of
            (
                new Choice("Step back to the square", c -> switchTo("village_square"))
            )
        ));
    }

    public void switchTo(Scene scene)
    {
        if(currentScene != null)
            currentScene.onExit(context);

        currentScene = scene;

        if(currentScene != null)
            currentScene.onEnter(context);

        if(renderer != null)
            renderer.refresh();
    }

    public void switchTo(String id)
    {
        var scene = sceneCache.get(id);

        if(scene == null)
            throw new IllegalArgumentException("No scene registered with ID: " + id);

        switchTo(scene);
    }

    public void switchTo(String id, Supplier<Scene> factory) {
        switchTo(getOrCreate(id, factory));
    }

    public void clearCache() {
        sceneCache.clear();
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
