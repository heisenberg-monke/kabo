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
import hm.kabo.items.Item;
import hm.kabo.saves.SaveManager;
import hm.kabo.scene.scenes.BattleScene;
import hm.kabo.scene.scenes.ExplorationScene;
import hm.kabo.scene.scenes.SplitListScene;
import hm.kabo.scene.scenes.TitleScene;

public class SceneManager 
{
    private final String sceneResourcePath;
    private final GameContext context;
    private final Map<String, Scene> sceneCache = new HashMap<>();

    private Scene currentScene;
    private Scene previousScene;
    private Scene preBattleScene;
    private Renderer renderer;

    private final SaveManager saveManager = new SaveManager();
    private String currentGameplaySceneId = "village_square";

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
        context.replacedScenes.put(oldId, replacementId);
    }

    private SplitListScene.ListEntry buildInventoryEntry(Item item)
    {
        return new SplitListScene.ListEntry
        (
            item.name(),
            item.description(),
            item.isUsable() ? gameCtx ->
            {
                var result = gameCtx.useItem(item);
                if (renderer != null)
                {
                    if (result.applied())
                        renderer.refreshSplitView();
                    renderer.setItemDescription(result.message());
                }
            } : null
        );
    }

    public boolean isInTitleContext()
    {
        var title = sceneCache.get("title");
        return currentScene == title || (isOverlayActive() && previousScene == title);
    }

    public void confirmQuit(String cancelSceneId)
    {
        showConfirmation
        (
            "Are you sure you want to quit the game?",
            javafx.application.Platform::exit,
            sceneCache.get(cancelSceneId)
        );
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

                if (previousScene instanceof BattleScene battle)
                {
                    tabs.add(new SplitListScene.TabEntry
                    (
                        "Consumables",
                        ctx -> ctx.getItems("Consumables").stream().map(item -> new SplitListScene.ListEntry
                        (
                            item.name(),
                            item.description(),
                            gameCtx ->
                            {
                                var result = gameCtx.useItem(item);
                                if (result.applied())
                                    battle.onItemUsed(gameCtx, result.message());
                                else if (renderer != null)
                                    renderer.setItemDescription(result.message());
                            }
                        )).toList()
                    ));
                    return tabs;
                }

                tabs.add(new SplitListScene.TabEntry
                (
                    "All",
                    ctx -> ctx.getItems("All").stream().map(item -> buildInventoryEntry(item)).toList()
                ));

                for (String category : c.inventory.keySet())
                    tabs.add(new SplitListScene.TabEntry
                    (
                        category,
                        ctx -> ctx.getItems(category).stream().map(item -> buildInventoryEntry(item)).toList()
                    ));

                return tabs;
            }
        ));

        registerScene("options", new SplitListScene
        (
            "OPTIONS",
            "No options available.",
            c ->
            {
                var entries = new ArrayList<SplitListScene.ListEntry>();
                boolean startedFromTitle = (previousScene == sceneCache.get("title"));

                if (!startedFromTitle && !(previousScene instanceof BattleScene))
                {
                    entries.add(new SplitListScene.ListEntry
                    (
                        "Save Game",
                        "Save your current progress to a file.",
                        ctx -> openOverlay("save_game")
                    ));
                }

                entries.add(new SplitListScene.ListEntry
                (
                    "Load Game",
                    "Load a previously saved game.",
                    ctx -> openOverlay("load_game")
                ));

                if (!startedFromTitle)
                {
                    entries.add(new SplitListScene.ListEntry
                    (
                        "Return to Title",
                        "Exit your current session and return to the main title menu.",
                        ctx -> showConfirmation
                        (
                            "Return to the title screen?\nAny unsaved progress in your current session will be lost.",
                            () -> switchTo("title"),
                            sceneCache.get("options")
                        )
                    ));
                }

                entries.add(new SplitListScene.ListEntry
                (
                    "Quit",
                    "Exit the game.",
                    ctx -> confirmQuit("options")
                ));

                return List.of(new SplitListScene.TabEntry("General", ctx -> entries));
            }
        ));

        registerScene("save_game", new SplitListScene
        (
            "SAVE GAME",
            "No save files found.",
            c ->
            {
                var entries = new ArrayList<SplitListScene.ListEntry>();

                entries.add(new SplitListScene.ListEntry
                (
                    "+ Create New Save",
                    "Save your current progress into a brand-new save slot.",
                    ctx ->
                    {
                        saveManager.saveNewSlot(ctx, currentGameplaySceneId);
                        returnToPrevious();
                    }
                ));

                for (var slot : saveManager.listSaves())
                {
                    entries.add(new SplitListScene.ListEntry
                    (
                        slot.slotName(),
                        "Scene: %s\nSaved: %s\n\nSelect to overwrite this save slot.".formatted(slot.currentSceneId(), slot.timestamp()),
                        ctx -> showConfirmation
                        (
                            "Overwrite %s (%s)?\nAny previous progress in this slot will be lost.".formatted(slot.slotName(), slot.timestamp()),
                            () ->
                            {
                                saveManager.saveToFile(slot.fileName(), slot.slotName(), ctx, currentGameplaySceneId);
                                returnToPrevious();
                            },
                            sceneCache.get("save_game")
                        )
                    ));
                }

                return List.of(new SplitListScene.TabEntry("Saves", ctx -> entries));
            }
        ));

        registerScene("load_game", new SplitListScene
        (
            "LOAD GAME",
            "No save files found.",
            c ->
            {
                var entries = new ArrayList<SplitListScene.ListEntry>();
                boolean startedFromTitle = (previousScene == sceneCache.get("title"));

                for (var slot : saveManager.listSaves())
                {
                    entries.add(new SplitListScene.ListEntry
                    (
                        slot.slotName(),
                        "Scene: %s\nSaved: %s".formatted(slot.currentSceneId(), slot.timestamp()),
                        ctx ->
                        {
                            if (startedFromTitle)
                                saveManager.loadFromFile(slot.fileName(), ctx, this);
                            else
                                showConfirmation
                                (
                                    "Load %s (%s)?\nAny unsaved progress in your current session will be lost.".formatted(slot.slotName(), slot.timestamp()),
                                    () -> saveManager.loadFromFile(slot.fileName(), ctx, this),
                                    sceneCache.get("load_game")
                                );
                        }
                    ));
                }

                return List.of(new SplitListScene.TabEntry("Saves", ctx -> entries));
            }
        ));
    }

    public SceneManager(GameContext context, String resourcePath) 
    {
        this.sceneResourcePath = resourcePath;
        this.context = context;
        registerScenes(resourcePath);
    }

    public void startNewGame()
    {
        context.reset();
        clearCache();
        previousScene = null;
        preBattleScene = null;
        registerScenes(sceneResourcePath);
        switchTo("village_square");
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public boolean isInBattle() {
        return currentScene instanceof BattleScene;
    }

    public BattleScene getActiveBattle() {
        return currentScene instanceof BattleScene battle ? battle : null;
    }

    public void switchTo(Scene scene)
    {
        if(currentScene != null)
            currentScene.onExit(context);

        if (scene == sceneCache.get("title"))
        {
            previousScene = null;
            preBattleScene = null;
        }
        else if (scene instanceof BattleScene && currentScene != null && !(currentScene instanceof SplitListScene))
            preBattleScene = currentScene;

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

        if (!(scene instanceof SplitListScene) && !"title".equals(id) && !"confirm".equals(id))
            currentGameplaySceneId = id;

        switchTo(scene);
    }

    public boolean hasAnySaves() {
        return saveManager.hasAnySaves();
    }

    public void prepareForLoad()
    {
        context.reset();
        clearCache();
        previousScene = null;
        preBattleScene = null;
        registerScenes(sceneResourcePath);
    }

    private static class ConfirmationScene extends ExplorationScene
    {
        public ConfirmationScene(String text, List<Choice> choices) {
            super(c -> text, c -> choices);
        }
    }

    public boolean isConfirmationActive() {
        return currentScene instanceof ConfirmationScene;
    }

    private void showConfirmation(String promptText, Runnable onConfirm, Scene cancelTarget)
    {
        var confirmScene = new ConfirmationScene
        (
            promptText,
            List.of
            (
                new Choice("Confirm", ctx -> onConfirm.run()),
                new Choice("Cancel", ctx -> switchTo(cancelTarget))
            )
        );
        switchTo(confirmScene);
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
        if (previousScene != null)
        {
            var target = previousScene;

            previousScene = null;
            switchTo(target);
        }
        else if (preBattleScene != null)
        {
            var target = preBattleScene;

            preBattleScene = null;
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
