package hm.kabo.scene.scenes;

import java.util.ArrayList;
import java.util.List;

import hm.kabo.core.GameContext;
import hm.kabo.entities.Enemy;
import hm.kabo.entities.Player;
import hm.kabo.scene.Choice;
import hm.kabo.scene.Scene;
import hm.kabo.scene.SceneManager;

public class BattleScene extends Scene
{
    private enum BattleState { PLAYER_TURN, VICTORY, ESCAPED, DEFEAT }

    private final String sceneId;
    private final String introText;
    private final List<String> enemyIds;
    private final String targetSceneId;
    private final SceneManager sceneManager;

    private final List<Enemy> activeEnemies = new ArrayList<>();
    private final List<String> combatLog = new ArrayList<>();
    private BattleState state = BattleState.PLAYER_TURN;

    private boolean selectingItem = false;

    public BattleScene
    (
        String sceneId,
        String introText, 
        List<String> enemyIds, 
        String targetSceneId, 
        SceneManager sceneManager
    )
    {
        super(c -> "", c -> List.of());

        this.sceneId = sceneId;
        this.introText = introText;
        this.enemyIds = enemyIds;
        this.targetSceneId = targetSceneId;
        this.sceneManager = sceneManager;
    }

    public void onItemUsed(GameContext context, String resultMessage)
    {
        sceneManager.returnToPrevious();
        executePlayerTurn(context, () -> combatLog.add(resultMessage));
    }

   @Override
    public void onEnter(GameContext context)
    {
        if (!activeEnemies.isEmpty() && state == BattleState.PLAYER_TURN)
            return;

        activeEnemies.clear();
        combatLog.clear();

        state = BattleState.PLAYER_TURN;

        for (String id : enemyIds)
        {
            var enemy = context.entities.get(id);

            enemy.reset();
            activeEnemies.add(enemy);
        }

        combatLog.add(introText);
    }

    @Override
    public void onExit(GameContext context) 
    {
        if (!selectingItem)
            activeEnemies.clear();
    }

    private List<Enemy> getLivingEnemies() {
        return activeEnemies.stream().filter(Enemy::isAlive).toList();
    }

    private Enemy getFirstLivingEnemy() {
        return activeEnemies.stream().filter(Enemy::isAlive).findFirst().orElse(null);
    }

    private void executePlayerTurn(GameContext context, Runnable playerAction)
    {
        if (state != BattleState.PLAYER_TURN)
            return;

        combatLog.clear();
        playerAction.run();

        if (state != BattleState.PLAYER_TURN)
            return;

        if (getLivingEnemies().isEmpty())
        {
            handleVictory(context);
            return;
        }

        var player = context.entities.getPlayer();

        for (var enemy : getLivingEnemies())
        {
            if (!player.isAlive())
                break;

            combatLog.add(enemy.takeTurn(player));
        }

        if (!player.isAlive())
        {
            state = BattleState.DEFEAT;
            combatLog.add("\nYou have fallen in battle...");
        }
    }

    private void handleVictory(GameContext context)
    {
        state = BattleState.VICTORY;
        Player player = context.entities.getPlayer();

        int totalXp = 0;
        int totalGold = 0;

        for (Enemy enemy : activeEnemies)
        {
            totalXp += enemy.getXPReward();
            totalGold += enemy.getGoldReward();
            context.entities.remove(enemy.getId());
        }

        player.addXP(totalXp);
        player.addGold(totalGold);

        // Permanently replace this defeated battle scene with its post-battle exploration scene
        sceneManager.replaceScene(sceneId, targetSceneId);

        combatLog.add("\nVictory! You gained %d XP and %d gold.".formatted(totalXp, totalGold));
    }

    @Override
    public String getText(GameContext context)
    {
        Player player = context.entities.getPlayer();
        var sb = new StringBuilder();

        sb.append("=== BATTLE ===\n");
        sb.append("%s: %d / %d HP%s\n\n".formatted
        (
            player.getName(),
            player.getStats().getHP(),
            player.getStats().getMaxHP(),
            player.isDefending() ? " [Defending]" : ""
        ));

        sb.append("Enemies:\n");

        for (var enemy : activeEnemies)
        {
            if (enemy.isAlive())
            {
                sb.append(" - %s (%d / %d HP)%s\n".formatted(
                    enemy.getName(),
                    enemy.getStats().getHP(),
                    enemy.getStats().getMaxHP(),
                    enemy.isDefending() ? " [Defending]" : ""
                ));
            }

            else
                sb.append(" - %s [DEFEATED]\n".formatted(enemy.getName()));
        }

        sb.append("\n------------------------------\n");

        for (var line : combatLog)
            sb.append(line).append("\n");

        return sb.toString();
    }

    @Override
    public List<Choice> getChoices(GameContext context)
    {
        if (state == BattleState.VICTORY)
            return List.of(new Choice("Continue", c -> sceneManager.switchTo(targetSceneId)));

        if (state == BattleState.ESCAPED)
            return List.of(new Choice("Continue", c -> sceneManager.returnToPrevious()));

        if (state == BattleState.DEFEAT)
            return List.of(new Choice("Return to Title", c -> sceneManager.switchTo("title")));

        Player player = context.entities.getPlayer();
        Enemy target = getFirstLivingEnemy();

        return List.of
        (
            new Choice("Attack", c -> executePlayerTurn(c, () ->
            {
                if (target != null)
                    combatLog.add(player.attack(target));
            })),

            new Choice("Defend", c -> executePlayerTurn(c, () ->
                combatLog.add(player.defend())
            )),

            new Choice("Use Item", c -> sceneManager.openOverlay("inventory"), !context.getItems("Consumables").isEmpty()),

            new Choice("Try to Run", c -> executePlayerTurn(c, () ->
            {
                if (player.tryRun(target))
                {
                    state = BattleState.ESCAPED;
                    combatLog.add("You managed to escape safely!");
                }

                else
                    combatLog.add("You tried to run, but the enemy cut off your escape!");
            }))
        );
    }
}
