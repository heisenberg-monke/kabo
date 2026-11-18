package hm.kabo.scene;

import java.util.List;

import hm.kabo.core.GameContext;

public interface Scene 
{
    String getText(GameContext context);
    List<Choice> getChoices(GameContext context);

    default void onEnter(GameContext context) {}
    default void onExit(GameContext context) {}
}
