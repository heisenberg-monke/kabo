package hm.kabo.scene;

import java.util.function.Consumer;

import hm.kabo.core.GameContext;

public class Choice 
{
    private final String label;
    private final Consumer<GameContext> action;
    private final boolean enabled;

    public Choice(String label, Consumer<GameContext> action, boolean enabled)
    {
        this.label = label;
        this.action = action;
        this.enabled = enabled;
    }
    
    public Choice(String label, Consumer<GameContext> action) {
        this(label, action, true);
    }

    public String getLabel() {
        return label;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void execute(GameContext context)
    {
        if(enabled && action != null)
            action.accept(context);
    }
}
