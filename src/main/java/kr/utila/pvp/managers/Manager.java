package kr.utila.pvp.managers;

import kr.utila.pvp.Main;

public abstract class Manager {

    protected final Main plugin;

    protected Manager(Main plugin) {
        this.plugin = plugin;
    }

    public abstract void load() throws Exception;

}
