package hm.kabo.core;

import hm.kabo.scene.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class Renderer 
{
    private final Stage stage;
    private final SceneManager sceneManager;
    
    private TextArea textArea;
    private Button[] choiceButtons = new Button[4];

    private void buildUI(GameContext context)
    {
        textArea = new TextArea();

        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-size: 16px;");

        var buttonBox = new HBox(10);
        var root = new BorderPane();
        var scene = new Scene(root, 800, 500);

        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        for(int i = 0; i < 4; ++i)
        {
            final int index = i;

            choiceButtons[i] = new Button();

            choiceButtons[i].setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(choiceButtons[i], Priority.ALWAYS);
            choiceButtons[i].setOnAction(e -> sceneManager.handleChoice(index));
            buttonBox.getChildren().add(choiceButtons[i]);
        }

        root.setCenter(textArea);
        root.setBottom(buttonBox);
        BorderPane.setMargin(textArea, new Insets(10));

        stage.setTitle("KABO - " + context.version);
        stage.setScene(scene);
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

        textArea.setText(current.getText(context));

        for(int i = 0; i < 4; ++i)
        {
            if(i < choices.size())
            {
                var c = choices.get(i);

                choiceButtons[i].setText((i + 1) + ", " + c.getLabel());
                choiceButtons[i].setDisable(!c.isEnabled());
                choiceButtons[i].setVisible(true);
            }

            else {
                choiceButtons[i].setText("");
                choiceButtons[i].setDisable(true);
                choiceButtons[i].setVisible(false);
            }
        }
    }

    public void show()
    {
        stage.show();
        refresh();
    }
}
