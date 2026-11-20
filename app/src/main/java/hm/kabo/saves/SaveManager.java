package hm.kabo.saves;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;

import hm.kabo.core.GameContext;
import hm.kabo.entities.CharacterStats;
import hm.kabo.entities.Enemy;
import hm.kabo.items.Item;
import hm.kabo.entities.Player;
import hm.kabo.scene.SceneManager;

public class SaveManager
{
    private static final Path SAVES_DIR = Paths.get("saves");
    private static final String EXTENSION = ".sav";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final byte[] SECRET_KEY = "KaboRpgSecretKey".getBytes(StandardCharsets.UTF_8);

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Gson gson = new Gson();

    public record SaveSlotInfo(String fileName, String slotName, String timestamp, String currentSceneId) {}

    public SaveManager()
    {
        try {
            Files.createDirectories(SAVES_DIR);
        } 
        
        catch (Exception e) {
            throw new RuntimeException("Failed to initialize saves directory.", e);
        }
    }

    public boolean hasAnySaves() {
        return !listSaves().isEmpty();
    }

    public List<SaveSlotInfo> listSaves()
    {
        if (!Files.exists(SAVES_DIR))
            return List.of();

        try (Stream<Path> files = Files.list(SAVES_DIR))
        {
            return files
                .filter(p -> p.toString().endsWith(EXTENSION))
                .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
                .map(this::readSlotInfo)
                .filter(info -> info != null)
                .toList();
        }
        catch (Exception e) {
            return List.of();
        }
    }

    private SaveSlotInfo readSlotInfo(Path file)
    {
        try 
        {
            SaveData data = decryptAndDeserialize(Files.readAllBytes(file));
            return new SaveSlotInfo(file.getFileName().toString(), data.slotName(), data.timestamp(), data.currentSceneId());
        }


        catch (Exception e) {
            return null;
        }
    }

    public void saveNewSlot(GameContext context, String currentSceneId)
    {
        String stamp = LocalDateTime.now().format(FILE_FMT);
        String fileName = "save_" + stamp + EXTENSION;
        String slotName = "Save " + LocalDateTime.now().format(DISPLAY_FMT);

        saveToFile(fileName, slotName, context, currentSceneId);
    }

    public void saveToFile(String fileName, String slotName, GameContext context, String currentSceneId)
    {
        Player p = context.entities.getPlayer();
        CharacterStats s = p.getStats();

        var playerSnap = new SaveData.PlayerSnapshot
        (
            p.getId(),
            p.getName(),
            s.getMaxHP(),
            s.getHP(),
            s.getAttack(),
            s.getDefense(),
            s.getSpeed(),
            p.getXP(),
            p.getGold()
        );

        List<String> itemIds = context.getItems("All").stream().map(Item::id).toList();
        List<String> aliveEnemies = context.entities.getAll().stream().map(Enemy::getId).toList();

        SaveData data = new SaveData
        (
            slotName,
            LocalDateTime.now().format(DISPLAY_FMT),
            currentSceneId,
            context.lookCounter,
            playerSnap,
            itemIds,
            aliveEnemies,
            Map.copyOf(context.replacedScenes)
        );

        try
        {
            Files.createDirectories(SAVES_DIR);
            byte[] encrypted = serializeAndEncrypt(data);
            Files.write(SAVES_DIR.resolve(fileName), encrypted);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to save game to " + fileName, e);
        }
    }

    public void loadFromFile(String fileName, GameContext context, SceneManager sceneManager)
    {
        try
        {
            byte[] raw = Files.readAllBytes(SAVES_DIR.resolve(fileName));
            SaveData data = decryptAndDeserialize(raw);

            sceneManager.prepareForLoad();
            context.lookCounter = data.lookCounter();

            var ps = data.player();
            var stats = new CharacterStats(ps.maxHP(), ps.attack(), ps.defense(), ps.speed());
            
            stats.setHP(ps.hp());

            Player restoredPlayer = new Player(ps.id(), ps.name(), stats);

            restoredPlayer.addXP(ps.xp());
            restoredPlayer.addGold(ps.gold());
            context.entities.setPlayer(restoredPlayer);

            context.inventory.values().forEach(List::clear);

            for (String itemId : data.inventoryItemIds())
                context.addItem(context.createItemById(itemId));

            List<String> allLoadedIds = new ArrayList<>(context.entities.getAll().stream().map(Enemy::getId).toList());

            for (String id : allLoadedIds)
            {
                if (!data.aliveEnemyIds().contains(id))
                    context.entities.remove(id);
            }

            for (var entry : data.replacedScenes().entrySet())
                sceneManager.replaceScene(entry.getKey(), entry.getValue());

            sceneManager.switchTo(data.currentSceneId());
        }

        catch (Exception e) {
            throw new RuntimeException("Failed to load save file: " + fileName, e);
        }
    }

    private byte[] serializeAndEncrypt(SaveData data) throws Exception
    {
        byte[] jsonBytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[GCM_IV_LENGTH];

        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SECRET_KEY, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));

        byte[] cipherText = cipher.doFinal(jsonBytes);

        return ByteBuffer.allocate(iv.length + cipherText.length)
            .put(iv)
            .put(cipherText)
            .array();
    }

    private SaveData decryptAndDeserialize(byte[] payload) throws Exception
    {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] iv = new byte[GCM_IV_LENGTH];

        buffer.get(iv);

        byte[] cipherText = new byte[buffer.remaining()];

        buffer.get(cipherText);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SECRET_KEY, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));

        byte[] plainBytes = cipher.doFinal(cipherText);

        return gson.fromJson(new String(plainBytes, StandardCharsets.UTF_8), SaveData.class);
    }
}
