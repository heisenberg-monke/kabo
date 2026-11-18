package hm.kabo.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hm.kabo.items.Item;

public class GameContext 
{
    public int lookCounter = 0;
    public final String version = "v0.5";
    public final Map<String, List<Item>> inventory = new LinkedHashMap<>();

    public void addItem(Item item) {
        inventory.computeIfAbsent(item.category(), k -> new ArrayList<>()).add(item);
    }

    public List<Item> getItems(String category)
    {
        if("All".equalsIgnoreCase(category))
            return inventory.values().stream().flatMap(List::stream).toList();

        return inventory.getOrDefault(category, List.of());
    }

    public GameContext()
    {
        inventory.put("Equipment", new ArrayList<>());
        inventory.put("Consumables", new ArrayList<>());
        inventory.put("Key Items", new ArrayList<>());

        addItem(new Item
        (
            "rusty_key",
            "Rusty Key",
            "An old iron key with flaking rust. Might fit a cellar door.",
            "Key Items"
        ));

        addItem(new Item
        (
            "warm_loaf",
            "Warm Bread Loaf",
            "Freshly baked sourdough from the village bakery. Smells comforting.",
            "Consumables"
        ));

        addItem(new Item
        (
            "worn_tunic",
            "Traveler's Tunic",
            "A sturdy wool tunic patched at the elbows. Offers modest warmth.",
            "Equipment"
        ));
    }
}
