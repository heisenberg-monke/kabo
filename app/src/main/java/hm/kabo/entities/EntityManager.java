package hm.kabo.entities;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;

import hm.kabo.entities.enemies.Goblin;

public class EntityManager 
{
    private final String resourcePath;
    private Player player;
    private final Map<String, Enemy> entities = new LinkedHashMap<>();

    public EntityManager(String resourcePath) 
    {
        this.resourcePath = resourcePath;
        loadEnemiesFromJSON(resourcePath);
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void register(Enemy enemy)
    {
        if (enemy == null)
            throw new IllegalArgumentException("Cannot register a null entity.");

        entities.put(enemy.getId(), enemy);
    }

    public Enemy get(String id) 
    {
        Enemy enemy = entities.get(id);

        if (enemy == null)
            throw new IllegalArgumentException("No enemy registered with ID: " + id);

        return enemy;
    }

    public Enemy remove(String id) {
        return entities.remove(id);
    }

    public boolean contains(String id) {
        return entities.containsKey(id);
    }

    public Collection<Enemy> getAll() {
        return Collections.unmodifiableCollection(entities.values());
    }

    public void clearEntities() {
        entities.clear();
    }

    public void reload()
    {
        entities.clear();
        loadEnemiesFromJSON(resourcePath);
    }

    private void loadEnemiesFromJSON(String resourcePath)
    {
        var stream = getClass().getResourceAsStream(resourcePath);
        if (stream == null)
            throw new IllegalStateException("Could not find enemy resource: " + resourcePath);

        JsonDeserializer<Enemy> enemyDeserializer = (json, typeOfT, ctx) ->
        {
            var obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();

            return switch (type.toUpperCase())
            {
                case "GOBLIN" -> ctx.deserialize(obj, Goblin.class);
                default -> throw new JsonParseException("Unknown enemy type: " + type);
            };
        };

        var gson = new GsonBuilder()
            .registerTypeAdapter(Enemy.class, enemyDeserializer)
            .create();

        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            var listType = new TypeToken<List<Enemy>>(){}.getType();
            List<Enemy> loaded = gson.fromJson(reader, listType);

            for (Enemy enemy : loaded)
            {
                enemy.reset();
                entities.put(enemy.getId(), enemy);
            }
        }

        catch (Exception e) {
            throw new RuntimeException("Failed to load enemies from " + resourcePath, e);
        }
    }
}
