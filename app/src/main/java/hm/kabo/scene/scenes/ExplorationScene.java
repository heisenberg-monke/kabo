package hm.kabo.scene.scenes;

import java.util.List;
import java.util.function.Function;

import hm.kabo.core.GameContext;
import hm.kabo.scene.Choice;
import hm.kabo.scene.Scene;

public class ExplorationScene extends Scene
{
    public ExplorationScene(Function<GameContext, String> textProvider, Function<GameContext, List<Choice>> choicesProvider) {
        super(textProvider, choicesProvider);
    }

    public ExplorationScene(String text, List<Choice> choices) {
        super(text, choices);
    }
}
