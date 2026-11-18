package hm.kabo.scene.scenes;

import java.util.List;
import java.util.function.Function;

import hm.kabo.core.GameContext;
import hm.kabo.scene.Choice;
import hm.kabo.scene.Scene;

public class ExplorationScene implements Scene
{
    private final Function<GameContext, String> textProvider;
    private final Function<GameContext, List<Choice>> choicesProvider;

    public ExplorationScene(Function<GameContext, String> textProvider, Function<GameContext, List<Choice>> choicesProvider)
    {
        this.textProvider = textProvider;
        this.choicesProvider = choicesProvider;
    }

    public ExplorationScene(String text, List<Choice> choices) {
        this(context -> text, context -> choices);
    }

    @Override
    public String getText(GameContext context) {
        return textProvider.apply(context);
    }

    @Override
    public List<Choice> getChoices(GameContext context) {
        return choicesProvider.apply(context);
    }
    
}
