package hm.kabo.scene.scenes;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import hm.kabo.core.GameContext;
import hm.kabo.scene.Scene;

public class SplitListScene extends Scene
{
    public record ListEntry
    (
        String label,
        String description,
        Consumer<GameContext> onSelect
    ) {}

    public record TabEntry 
    (
        String label,
        Function<GameContext, List<ListEntry>> entriesProvider
    ) {}

    private final String title;
    private final String emptyMessage;
    private final Function<GameContext, List<TabEntry>> tabsProvider;

    public SplitListScene
    (
        String title,
        String emptyMessage,
        Function<GameContext, List<TabEntry>> tabsProvider
    )
    {
        super(title, List.of());

        this.title = title;
        this.emptyMessage = emptyMessage;
        this.tabsProvider = tabsProvider;
    }

    public String getTitle() {
        return title;
    }

    public String getEmptyMessage() {
        return emptyMessage;
    }

    public List<TabEntry> getTabs(GameContext context) {
        return tabsProvider.apply(context);
    }
}
