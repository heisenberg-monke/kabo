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
    private final InputManager inputManager;
    
    private TextArea textArea, itemDescriptionArea;
    private VBox splitContainer;
    private HBox tabBar, choiceBox, utilityBox;
    private SplitPane splitView;
    private ListView<String> itemListView;

    private final Button[] choiceButtons = new Button[4];
    private Button inventoryButton, optionsButton;

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
        utilityBox = new HBox(10, inventoryButton, optionsButton);

        var centerStack = new StackPane(textArea, splitContainer);
        
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
            inputManager.onListSelectionChanged(newVal.intValue())
        );

        itemListView.setOnMouseClicked(event ->
        {
            if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
                inputManager.activateSelectedEntry();
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

            choiceButtons[i].setMinWidth(150);
            choiceButtons[i].setFocusTraversable(false);

            choiceButtons[i].setOnAction(e -> 
                inputManager.onChoiceClicked(index)
            );

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

        inputManager.attach(this, fxScene);

        stage.setTitle("KABO - " + context.version);
        stage.setScene(fxScene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
    }

    public void updateChoiceHighlight(List<Choice> choices)
    {
        int selectedChoice = inputManager.getSelectedChoiceIndex();

        for(int i = 0; i < 4; ++i)
        {
            if(i < choices.size() && i == selectedChoice && choices.get(i).isEnabled())
                choiceButtons[i].setStyle("-fx-border-color: #2b7de9; -fx-border-width: 2px; -fx-font-weight: bold;");

            else 
                choiceButtons[i].setStyle("");
        }
    }

    public void renderSplitScene(SplitListScene splitScene)
    {
        var context = sceneManager.getContext();
        var tabs = splitScene.getTabs(context);
        var selectedTab = inputManager.getSelectedTabIndex();

        if(selectedTab < 0 || selectedTab >= tabs.size())
        {
            selectedTab = 0;
            inputManager.selectTab(0);

            return;
        }

        tabBar.getChildren().clear();

        for (int i = 0; i < tabs.size(); ++i)
        {
            final int tabIdx = i;
            var tabButton = new Button(tabs.get(i).label());

            tabButton.setFocusTraversable(false);
            tabButton.setOnAction(e -> inputManager.selectTab(tabIdx));

            if (i == selectedTab)
                tabButton.setStyle("-fx-background-color: #2b7de9; -fx-text-fill: white; -fx-font-weight: bold;");

            else 
                tabButton.setStyle("");

            tabBar.getChildren().add(tabButton);
        }

        var hintLabel = new Label("  [Tab / Shift+Tab]");

        var entries = tabs.isEmpty() ? 
            List.<SplitListScene.ListEntry>of() : 
            tabs.get(selectedTab).entriesProvider().apply(context);

        hintLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");
        tabBar.getChildren().add(hintLabel);

        inputManager.setCurrEntries(entries);

        itemListView.getItems().setAll(
            entries.stream().map(SplitListScene.ListEntry::label).toList()
        );

        if (entries.isEmpty())
            itemDescriptionArea.setText(splitScene.getEmptyMessage());

        else
        {
            itemListView.getSelectionModel().select(0);
            itemDescriptionArea.setText(entries.get(0).description());
        }
    }

    public int getSelectedListIndex() {
        return itemListView.getSelectionModel().getSelectedIndex();
    }

    public void selectListIndex(int index)
    {
        itemListView.getSelectionModel().select(index);
        itemListView.scrollTo(index);
    }

    public void setItemDescription(String description) {
        itemDescriptionArea.setText(description);
    }

    public void refreshSplitView()
    {
        if (sceneManager.getCurrentScene() instanceof SplitListScene splitScene)
            renderSplitScene(splitScene);
    }

    public Renderer(Stage stage, SceneManager sceneManager, InputManager inputManager)
    {
        this.stage = stage;
        this.sceneManager = sceneManager;
        this.inputManager = inputManager;

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
        boolean showUtilityBox = !sceneManager.isInTitleContext() && !sceneManager.isConfirmationActive();

        utilityBox.setVisible(showUtilityBox);
        utilityBox.setManaged(showUtilityBox);

        inventoryButton.setDisable(onTitle);
        optionsButton.setDisable(onTitle);

        inputManager.resetSelectionForScene();

        if(current instanceof SplitListScene splitScene)
        {
            textArea.setVisible(false);
            splitContainer.setVisible(true);

            renderSplitScene(splitScene);
        }

        else 
        {
            splitContainer.setVisible(false);
            textArea.setVisible(true);
            textArea.setText(current.getText(context));
        }

        choiceBox.setVisible(hashChoices);
        choiceBox.setManaged(hashChoices);

        for(int i = 0; i < 4; ++i)
        {
            if(i < choices.size())
            {
                var c = choices.get(i);

                choiceButtons[i].setText((i + 1) + ". " + c.getLabel());
                choiceButtons[i].setDisable(!c.isEnabled());
                choiceButtons[i].setVisible(true);
                choiceButtons[i].setManaged(true);
            }

            else 
            {
                choiceButtons[i].setText("");
                choiceButtons[i].setDisable(true);
                choiceButtons[i].setVisible(false);
                choiceButtons[i].setManaged(false);
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
