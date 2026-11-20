package hm.kabo.entities;

public class CharacterStats 
{
    private int maxHP;
    private int hp;
    private int attack;
    private int defense;
    private int speed;
    
    public CharacterStats(int maxHP, int attack, int defense, int speed)
    {
        this.maxHP = Math.max(1, maxHP);
        this.hp = this.maxHP;
        this.attack = Math.max(0, attack);
        this.defense = Math.max(0, defense);
        this.speed = Math.max(1, speed);
    }

    public int takeDamage(int rawDamage)
    {
        int actual = Math.max(1, rawDamage - defense);

        hp = Math.max(0, hp - actual);

        return actual;
    }

    public int heal(int amount)
    {
        int before = hp;

        hp = Math.min(maxHP, hp + Math.max(0, amount));

        return hp - before;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public int getHP() {
        return hp;
    }

    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }

    public void setMaxHP(int maxHP)
    {
        this.maxHP = Math.max(1, maxHP);
        this.hp = Math.min(this.hp, this.maxHP);
    }

    public void setAttack(int attack) { this.attack = Math.max(0, attack); }
    public void setDefense(int defense) { this.defense = Math.max(0, defense); }
    public void setSpeed(int speed) { this.speed = Math.max(1, speed); }

    public void restoreFullHp() {
        this.hp = this.maxHP;
    }
}
