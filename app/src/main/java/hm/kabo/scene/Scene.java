package hm.kabo.scene;

import java.util.List;
import java.util.function.Function;

import hm.kabo.core.GameContext;

public abstract class Scene 
{
    private final Function<GameContext, String> textProvider;
    private final Function<GameContext, List<Choice>> choicesProvider;

    public Scene(Function<GameContext, String> textProvider, Function<GameContext, List<Choice>> choicesProvider)
    {
        this.textProvider = textProvider;
        this.choicesProvider = choicesProvider;
    }

    public Scene(String text, List<Choice> choices) {
        this(context -> text, context -> choices);
    }

    public String getText(GameContext context) {
        return textProvider.apply(context);
    }

    public List<Choice> getChoices(GameContext context) {
        return choicesProvider.apply(context);
    }

    public void onEnter(GameContext context) {}
    public void onExit(GameContext context) {}
}
