package hm.kabo.items;

public record Item
(
    String id,
    String name,
    String description,
    String category,
    ItemEffect effect
) 
{
    public Item(String id, String name, String description, String category) {
        this(id, name, description, category, ItemEffect.none());
    }

    public boolean isUsable() {
        return effect != null;
    }
}
