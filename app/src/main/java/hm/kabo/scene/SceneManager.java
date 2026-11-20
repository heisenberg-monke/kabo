package hm.kabo.scene;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import hm.kabo.core.GameContext;
import hm.kabo.core.Renderer;
import hm.kabo.scene.scenes.BattleScene;
import hm.kabo.scene.scenes.ExplorationScene;
import hm.kabo.scene.scenes.SplitListScene;
import hm.kabo.scene.scenes.TitleScene;

public class SceneManager 
{
    private final GameContext context;
    private final Map<String, Scene> sceneCache = new HashMap<>();

    private Scene currentScene;
    private Scene previousScene;
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

    private void registerScene(String id, Scene scene) {
        sceneCache.put(id, scene);
    }

    private void loadScenesFromJSON(String resourcePath)
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
                if("BATTLE".equalsIgnoreCase(data.type()))
                    registerScene(data.id(), new BattleScene
                    (
                        data.id(),
                        data.text(),
                        data.enemyIds(),
                        data.targetScene(),
                        this
                    ));

                else
                {
                    var choices = data.choices() == null
                        ? List.<Choice>of()
                        : data.choices().stream().map(this::buildChoice).toList();

                    registerScene(data.id(), new ExplorationScene(c -> formatText(data.text(), c), c -> choices));
                }
            }
        }
        
        catch(Exception e) {
            throw new RuntimeException("Failed to load scenes from " + resourcePath, e);
        }
    }

    public void replaceScene(String oldId, String replacementId)
    {
        var replacement = sceneCache.get(replacementId);

        if (replacement == null)
            throw new IllegalArgumentException("No replacement scene registered with ID: " + replacementId);

        sceneCache.put(oldId, replacement);
    }

    private void registerScenes(String resourcePath)
    {
        loadScenesFromJSON(resourcePath);

        registerScene("title", new TitleScene(this));

        registerScene("inventory", new SplitListScene 
        (
            "INVENTORY",
            "Your bag is empty.",
            c -> 
            {
                var tabs = new ArrayList<SplitListScene.TabEntry>();

                tabs.add(new SplitListScene.TabEntry
                (
                    "All",
                    ctx -> ctx.getItems("All").stream().map(item -> new SplitListScene.ListEntry
                    (
                        item.name(),
                        item.description(),
                        null
                    )).toList()
                ));

                for(String category : c.inventory.keySet())
                    tabs.add(new SplitListScene.TabEntry
                    (
                        category,
                        ctx -> ctx.getItems(category).stream().map(item -> new SplitListScene.ListEntry
                        (
                            item.name(),
                            item.description(),
                            null
                        )).toList()
                    ));

                return tabs;
            }
        ));

        registerScene("options", new SplitListScene
        (
            "OPTIONS", 
            "No options available.", 
            c -> List.of (
                new SplitListScene.TabEntry
                (
                    "General",
                    ctx -> List.of(new SplitListScene.ListEntry
                    (
                        "Return to Title Screen",
                        "Exit your current session and return to the main title menu.",
                        context -> switchTo("title")
                    ))
                )
            )
        ));
    }

    public SceneManager(GameContext context, String resourcePath) 
    {
        this.context = context;
        registerScenes(resourcePath);
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public void switchTo(Scene scene)
    {
        if(currentScene != null)
            currentScene.onExit(context);

        if (scene == sceneCache.get("title"))
            previousScene = null;

        else if (scene instanceof BattleScene && currentScene != null && !(currentScene instanceof SplitListScene))
            previousScene = currentScene;

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

    public void clearCache() {
        sceneCache.clear();
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public GameContext getContext() {
        return context;
    }

    public boolean isOverlayActive() {
        return currentScene instanceof SplitListScene;
    }

    public boolean isOnTitle() {
        return currentScene == sceneCache.get("title");
    }

    public boolean handleChoice(int index)
    {
        if(currentScene == null)
            return false;

        var choices = currentScene.getChoices(context);

        if(index >= 0 && index < choices.size() && choices.get(index).isEnabled())
        {
            choices.get(index).execute(context);

            if(renderer != null)
                renderer.refresh();

            return true;
        }
        
        return false;
    }

    public void returnToPrevious()
    {
        if(previousScene != null)
        {
            var target = previousScene;

            previousScene = null;
            switchTo(target);
        }

        else 
            switchTo("title");
    }

    public void openOverlay(String id)
    {
        if(currentScene == sceneCache.get(id))
        {
            returnToPrevious();
            return;
        }

        if(currentScene != null && !(currentScene instanceof SplitListScene))
            previousScene = currentScene;

        switchTo(id);
    }
}
