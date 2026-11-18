package hm.kabo.scene;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

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

    private String formatText(String rawText, GameContext c) {
        return rawText.replace("{lookCounter}", String.valueOf(c.lookCounter));
    }

    private Choice buildChoice(SceneData.ChoiceData data)
    {
        var isEnabled = data.enabled() == null || data.enabled();
        Consumer<GameContext> callback = null;

        if(data.action() != null)
        {
            callback = switch(data.action())
            {
                case "INCREMENT_LOOK" -> c -> ++c.lookCounter;
                default -> throw new IllegalArgumentException("Unknown action: " + data.action());
            };
        }
        
        else if(data.targetScene() != null)
            callback = c -> switchTo(data.targetScene());

        return new Choice(data.label(), callback, isEnabled);
    }

    public SceneManager(GameContext context) {
        this.context = context;
        registerScenes();
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

    public void loadScenesFromJSON(String resourcePath)
    {
        var stream = getClass().getResourceAsStream(resourcePath);

        if(stream == null) 
            throw new IllegalStateException("Could not find scene resource: " + resourcePath);

        try(var reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            var listType = new TypeToken<List<SceneData>>(){}.getType();
            List<SceneData> scenes = new Gson().fromJson(reader, listType);

            for(var data : scenes)
            {
                var choices = data.choices().stream().map(this::buildChoice).toList();

                registerScene(data.id(), new ExplorationScene(c -> formatText(data.text(), c), c -> choices));
            }
        }
        
        catch(Exception e) {
            throw new RuntimeException("Failed to load scenes from " + resourcePath);
        }
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
