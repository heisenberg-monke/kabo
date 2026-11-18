package hm.kabo.core;

import java.util.List;

import hm.kabo.scene.Choice;
import hm.kabo.scene.SceneManager;
import hm.kabo.scene.scenes.SplitListScene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Renderer 
{
    private final Stage stage;
    private final SceneManager sceneManager;
    
    private TextArea textArea, itemDescriptionArea;
    private VBox splitContainer;
    private HBox tabBar;
    private HBox choiceBox;
    private SplitPane splitView;
    private ListView<String> itemListView;

    private final Button[] choiceButtons = new Button[4];
    private Button inventoryButton, optionsButton;

    private int selectedChoiceIndex = 0;
    private int selectedTabIndex = 0;
    private List<SplitListScene.ListEntry> currentEntries = List.of();

    private void updateChoiceHighlight(List<Choice> choices)
    {
        for(int i = 0; i < 4; ++i)
        {
            if(i < choices.size() && i == selectedChoiceIndex && choices.get(i).isEnabled())
                choiceButtons[i].setStyle("-fx-border-color: #2b7de9; -fx-border-width: 2px; -fx-font-weight: bold;");

            else 
                choiceButtons[i].setStyle("");
        }
    }

    private void renderSplitScene(SplitListScene splitScene)
    {
        var tabs = splitScene.getTabs(sceneManager.getContext());

        if (selectedTabIndex < 0 || selectedTabIndex >= tabs.size())
            selectedTabIndex = 0;

        tabBar.getChildren().clear();

        for(int i = 0; i < tabs.size(); ++i)
        {
            final int tabIdx = i;
            var tabButton = new Button(tabs.get(i).label());

            tabButton.setFocusTraversable(false);
            tabButton.setOnAction(e -> 
            {
                selectedTabIndex = tabIdx;
                renderSplitScene(splitScene);
            });

            if(i == selectedTabIndex)
                tabButton.setStyle("-fx-background-color: #2b7de9; -fx-text-fill: white; -fx-font-weight: bold;");

            else 
                tabButton.setStyle("");

            tabBar.getChildren().add(tabButton);
        }

        var hintLabel = new Label("  [Tab / Shift+Tab]");

        hintLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");
        tabBar.getChildren().add(hintLabel);

        if(tabs.isEmpty())
            currentEntries = List.of();

        else 
            currentEntries = tabs.get(selectedTabIndex).entriesProvider().apply(sceneManager.getContext());

        itemListView.getItems().setAll(
            currentEntries.stream().map(SplitListScene.ListEntry::label).toList()
        );

        if(currentEntries.isEmpty())
            itemDescriptionArea.setText(splitScene.getEmptyMessage());

        else {
            itemListView.getSelectionModel().select(0);
            itemDescriptionArea.setText(currentEntries.get(0).description());
        }
    }

    private void moveListSelection(int delta)
    {
        if(currentEntries.isEmpty())
            return;

        int current = itemListView.getSelectionModel().getSelectedIndex();
        int next = Math.floorMod(current + delta, currentEntries.size());

        itemListView.getSelectionModel().select(next);
        itemListView.scrollTo(next);
    }

    private void moveChoiceSelection(int delta)
    {
        var current = sceneManager.getCurrentScene();

        if(current == null)
            return;

        var choices = current.getChoices(sceneManager.getContext());

        if(choices.isEmpty())
            return;

        for(int step = 0; step < choices.size(); ++step)
        {
            selectedChoiceIndex = Math.floorMod(selectedChoiceIndex + delta, choices.size());

            if(choices.get(selectedChoiceIndex).isEnabled())
                break;
        }

        updateChoiceHighlight(choices);
    }

    private void moveTabSelection(SplitListScene splitScene, int delta)
    {
        var tabs = splitScene.getTabs(sceneManager.getContext());

        if (tabs.isEmpty())
            return;

        selectedTabIndex = Math.floorMod(selectedTabIndex + delta, tabs.size());

        renderSplitScene(splitScene);
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

        if(!sceneManager.isOnTitle())
        {
            switch(code)
            {
                case I ->
                {
                    sceneManager.openOverlay("inventory");
                    event.consume();

                    return;
                }

                case ESCAPE ->
                {
                    sceneManager.openOverlay("options");
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

        else {
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

    private void activateSelectedEntry()
    {
        int idx = itemListView.getSelectionModel().getSelectedIndex();

        if(idx >= 0 && idx < currentEntries.size())
        {
            var action = currentEntries.get(idx).onSelect();

            if(action != null)
            {
                action.accept(sceneManager.getContext());
                refresh();
            }
        }
    }

    private void buildUI(GameContext context)
    {
        textArea = new TextArea();
        tabBar = new HBox(6);
        choiceBox = new HBox(10);
        itemDescriptionArea = new TextArea();
        itemListView = new ListView<>();
        splitView = new SplitPane(itemListView, itemDescriptionArea);
        splitContainer = new VBox(tabBar, splitView);
        inventoryButton = new Button("Inventory [I]");
        optionsButton = new Button("Options [Esc]");

        var centerStack = new StackPane(textArea, splitContainer);
        var utilityBox = new HBox(10, inventoryButton, optionsButton);
        var bottomContainer = new VBox(8, choiceBox, utilityBox);
        var root = new BorderPane();
        var fxScene = new Scene(root, 800, 520);

        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setFocusTraversable(false);
        textArea.setStyle("-fx-font-size: 16px;");

        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(0, 0, 6, 0));

        itemListView.setFocusTraversable(false);
        itemListView.setStyle("-fx-font-size: 15px;");

        itemDescriptionArea.setEditable(false);
        itemDescriptionArea.setWrapText(true);
        itemDescriptionArea.setFocusTraversable(false);
        itemDescriptionArea.setStyle("-fx-font-size: 15px;");

        itemListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) ->
        {
            int idx = newVal.intValue();

            if(idx >= 0 && idx < currentEntries.size())
                itemDescriptionArea.setText(currentEntries.get(idx).description());
        });

        itemListView.setOnMouseClicked(event ->
        {
            if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
                activateSelectedEntry();
        });

        splitView.setDividerPositions(0.4);
        VBox.setVgrow(splitView, Priority.ALWAYS);

        splitContainer.setVisible(false);
        BorderPane.setMargin(centerStack, new Insets(10));

        choiceBox.setAlignment(Pos.CENTER);

        for(int i = 0; i < 4; ++i)
        {
            final int index = i;

            choiceButtons[i] = new Button();

            choiceButtons[i].setMaxWidth(Double.MAX_VALUE);
            choiceButtons[i].setFocusTraversable(false);

            HBox.setHgrow(choiceButtons[i], Priority.ALWAYS);

            choiceButtons[i].setOnAction(e -> 
            {
                selectedChoiceIndex = index;
                sceneManager.handleChoice(index);
            });

            choiceBox.getChildren().add(choiceButtons[i]);
        }

        inventoryButton.setMaxWidth(Double.MAX_VALUE);
        inventoryButton.setFocusTraversable(false);
        inventoryButton.setOnAction(e -> sceneManager.openOverlay("inventory"));

        optionsButton.setMaxWidth(Double.MAX_VALUE);
        optionsButton.setFocusTraversable(false);
        optionsButton.setOnAction(e -> sceneManager.openOverlay("options"));

        HBox.setHgrow(inventoryButton, Priority.ALWAYS);
        HBox.setHgrow(optionsButton, Priority.ALWAYS);

        utilityBox.setAlignment(Pos.CENTER);
        bottomContainer.setPadding(new Insets(0, 10, 10, 10));

        root.setCenter(centerStack);
        root.setBottom(bottomContainer);

        fxScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        stage.setTitle("KABO - " + context.version);
        stage.setScene(fxScene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
    }

    public Renderer(Stage stage, SceneManager sceneManager)
    {
        this.stage = stage;
        this.sceneManager = sceneManager;

        sceneManager.setRenderer(this);
        buildUI(sceneManager.getContext());
    }

    public void refresh()
    {
        var current = sceneManager.getCurrentScene();

        if(current == null)
            return;

        var context = sceneManager.getContext();
        var choices = current.getChoices(context);
        var onTitle = sceneManager.isOnTitle();
        var hashChoices = !choices.isEmpty();

        inventoryButton.setDisable(onTitle);
        optionsButton.setDisable(onTitle);

        if(current instanceof SplitListScene splitScene)
        {
            textArea.setVisible(false);
            splitContainer.setVisible(true);

            selectedTabIndex = 0;
            renderSplitScene(splitScene);
        }

        else {
            splitContainer.setVisible(false);
            textArea.setVisible(true);
            textArea.setText(current.getText(context));
        }

        choiceBox.setVisible(hashChoices);
        choiceBox.setManaged(hashChoices);

        selectedChoiceIndex = 0;

        for (int i = 0; i < choices.size(); i++) 
        {
            if (choices.get(i).isEnabled()) 
            {
                selectedChoiceIndex = i;
                break;
            }
        }

        for(int i = 0; i < 4; ++i)
        {
            if(i < choices.size())
            {
                var c = choices.get(i);

                choiceButtons[i].setText((i + 1) + ". " + c.getLabel());
                choiceButtons[i].setDisable(!c.isEnabled());
                choiceButtons[i].setVisible(true);
            }

            else {
                choiceButtons[i].setText("");
                choiceButtons[i].setDisable(true);
                choiceButtons[i].setVisible(false);
            }
        }

        updateChoiceHighlight(choices);
    }

    public void show()
    {
        stage.show();
        refresh();
    }
}
