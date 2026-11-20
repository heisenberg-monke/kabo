package hm.kabo.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hm.kabo.entities.CharacterStats;
import hm.kabo.entities.EntityManager;
import hm.kabo.entities.Player;
import hm.kabo.items.Item;
import hm.kabo.items.ItemEffect;

public class GameContext 
{
    public int lookCounter = 0;
    public final String version = "v0.9";
    public final Map<String, List<Item>> inventory = new LinkedHashMap<>();
    public final EntityManager entities = new EntityManager("/enemies.json");
    private final Map<String, Item> itemTemplates = new LinkedHashMap<>();
    public final Map<String, String> replacedScenes = new LinkedHashMap<>();

    private void registerItemTemplate(Item item) {
        itemTemplates.put(item.id(), item);
    }

    public Item createItemById(String id)
    {
        Item template = itemTemplates.get(id);

        if (template == null)
            throw new IllegalArgumentException("Unknown item ID in save file: " + id);

        return template;
    }

    public void addItem(Item item) {
        inventory.computeIfAbsent(item.category(), k -> new ArrayList<>()).add(item);
    }

    public final void reset()
    {
        lookCounter = 0;

        inventory.clear();
        inventory.put("Equipment", new ArrayList<>());
        inventory.put("Consumables", new ArrayList<>());
        inventory.put("Key Items", new ArrayList<>());

        addItem(createItemById("rusty_key"));
        addItem(createItemById("warm_loaf"));
        addItem(createItemById("worn_tunic"));
        
        replacedScenes.clear();
        entities.reload();
        entities.setPlayer(new Player("player", "Traveler", new CharacterStats(100, 12, 4, 6)));
    }

    public List<Item> getItems(String category)
    {
        if("All".equalsIgnoreCase(category))
            return inventory.values().stream().flatMap(List::stream).toList();

        return inventory.getOrDefault(category, List.of());
    }

    public String useItem(Item item)
    {
        if (item == null || !item.isUsable())
            return null;

        var categoryList = inventory.get(item.category());
        if (categoryList == null || !categoryList.remove(item))
            return null;

        return entities.getPlayer().useItem(item);
    }

    public GameContext()
    {
        registerItemTemplate(new Item
        (
            "rusty_key",
            "Rusty Key",
            "An old iron key with flaking rust. Might fit a cellar door.",
            "Key Items"
        ));

        registerItemTemplate(new Item
        (
            "warm_loaf",
            "Warm Bread Loaf",
            "Freshly baked sourdough from the village bakery. Restores 20 HP.",
            "Consumables",
            ItemEffect.heal(20)
        ));

        registerItemTemplate(new Item
        (
            "worn_tunic",
            "Traveler's Tunic",
            "A sturdy wool tunic patched at the elbows. Offers modest warmth.",
            "Equipment"
        ));

        reset();
    }
}
