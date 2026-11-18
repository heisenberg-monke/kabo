package hm.kabo;

import hm.kabo.core.Game;

public class App 
{
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        Game.launch(Game.class, args);
    }
}
