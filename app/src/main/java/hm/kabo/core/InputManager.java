package hm.kabo.core;

import java.util.List;

import hm.kabo.scene.SceneManager;
import hm.kabo.scene.scenes.SplitListScene;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class InputManager 
{
    private final SceneManager sceneManager;
    private Renderer renderer;
    
    private int selectedChoiceIndex = 0;
    private  int selectedTabIndex = 0;
    private List<SplitListScene.ListEntry> currEntries = List.of();

    public InputManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public void attach(Renderer renderer, Scene fxScene)
    {
        this.renderer = renderer;
        fxScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }

    public int getSelectedChoiceIndex() {
        return selectedChoiceIndex;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setCurrEntries(List<SplitListScene.ListEntry> entries) {
        currEntries = entries;
    }

    public void resetSelectionForScene()
    {
        selectedTabIndex = 0;
        selectedChoiceIndex = 0;

        var curr = sceneManager.getCurrentScene();

        if(curr == null)
            return;

        var choices = curr.getChoices(sceneManager.getContext());

        for(int i = 0; i < choices.size(); ++i)
        {
            if(choices.get(i).isEnabled())
            {
                selectedChoiceIndex = i;
                break;
            }
        }
    }

    public void selectTab(int tabIndex)
    {
        selectedTabIndex = tabIndex;

        if(renderer != null)
            renderer.refreshSplitView();
    }

    public void onChoiceClicked(int index)
    {
        selectedChoiceIndex = index;
        sceneManager.handleChoice(index);
    }

    public void onOverlayClicked(String overlay) {
        sceneManager.openOverlay(overlay);
    }

    public void onListSelectionChanged(int index)
    {
        if(renderer != null && index >= 0 && index < currEntries.size())
            renderer.setItemDescription(currEntries.get(index).description());
    }

    public void activateSelectedEntry()
    {
        if(renderer == null)
            return;

        int index = renderer.getSelectedListIndex();

        if(index >= 0 && index < currEntries.size())
        {
            var action = currEntries.get(index).onSelect();

            if(action != null)
            {
                var before = sceneManager.getCurrentScene();
                action.accept(sceneManager.getContext());

                if (sceneManager.getCurrentScene() == before && !(before instanceof SplitListScene))
                    renderer.refresh();
            }
        }
    }

    private void moveListSelection(int delta)
    {
        if(renderer == null || currEntries.isEmpty())
            return;

        int curr = renderer.getSelectedListIndex();
        int next = Math.floorMod(curr + delta, currEntries.size());

        renderer.selectListIndex(next);
    }

    private void moveChoiceSelection(int delta)
    {
        var curr = sceneManager.getCurrentScene();

        if(curr == null)
            return;

        var choices = curr.getChoices(sceneManager.getContext());

        if(choices.isEmpty())
            return;

        for(int step = 0; step < choices.size(); ++step)
        {
            selectedChoiceIndex = Math.floorMod(selectedChoiceIndex + delta, choices.size());

            if(choices.get(selectedChoiceIndex).isEnabled())
                break;
        }

        if(renderer != null)
            renderer.updateChoiceHighlight(choices);
    }

    private void moveTabSelection(SplitListScene splitScene, int delta)
    {
        var tabs = splitScene.getTabs(sceneManager.getContext());

        if(tabs.isEmpty())
            return;

        selectedTabIndex = Math.floorMod(selectedTabIndex + delta, tabs.size());

        if(renderer != null)
            renderer.refreshSplitView();
    }

    private int getChoiceIndexFromKey(KeyCode code)
    {
        return switch(code)
        {
            case DIGIT1, NUMPAD1 -> 0;
            case DIGIT2, NUMPAD2 -> 1;
            case DIGIT3, NUMPAD3 -> 2;
            case DIGIT4, NUMPAD4 -> 3;
            default -> -1;
        };
    }

    private void handleKeyPress(KeyEvent event)
    {
        var code = event.getCode();

        if (code == KeyCode.ESCAPE && !sceneManager.isConfirmationActive())
        {
            if (sceneManager.isOverlayActive())
                sceneManager.returnToPrevious();

            else if (!sceneManager.isOnTitle())
                sceneManager.openOverlay("options");

            event.consume();

            return;
        }

        if (code == KeyCode.F7 && !sceneManager.isConfirmationActive())
        {
            sceneManager.openOverlay("load_game");
            event.consume();

            return;
        }

        if(!sceneManager.isOnTitle() && !sceneManager.isConfirmationActive())
        {
            switch(code)
            {
                case I ->
                {
                    sceneManager.openOverlay("inventory");
                    event.consume();

                    return;
                }

                case F5 ->
                {
                    if (!sceneManager.isInBattle())
                        sceneManager.openOverlay("save_game");

                    event.consume();

                    return;
                }

                default -> {}
            }
        }

        if(sceneManager.getCurrentScene() instanceof SplitListScene splitScene)
        {
            switch(code)
            {
                case TAB -> 
                {
                    int delta = event.isShiftDown() ? -1 : 1;

                    moveTabSelection(splitScene, delta);
                    event.consume();
                }

                case UP ->
                {
                    moveListSelection(-1);
                    event.consume();
                }

                case DOWN ->
                {
                    moveListSelection(1);
                    event.consume();
                }

                case ENTER, SPACE ->
                {
                    activateSelectedEntry();
                    event.consume();
                }

                default -> {}
            }

            return;
        }

        if(code == KeyCode.LEFT || code == KeyCode.UP)
        {
            moveChoiceSelection(-1);
            event.consume();
        }

        else if(code == KeyCode.RIGHT || code == KeyCode.DOWN)
        {
            moveChoiceSelection(1);
            event.consume();
        }

        else if(code == KeyCode.ENTER || code == KeyCode.SPACE)
        {
            sceneManager.handleChoice(selectedChoiceIndex);
            event.consume();
        }

        else
        {
            int numberIndex = getChoiceIndexFromKey(code);

            if(numberIndex != -1)
            {
                if(sceneManager.handleChoice(numberIndex))
                    selectedChoiceIndex = numberIndex;

                event.consume();
            }

            else if(code == KeyCode.TAB)
                event.consume();
        }
    }
}
