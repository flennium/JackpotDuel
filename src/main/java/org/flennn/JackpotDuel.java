package org.flennn;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.flennn.Commands.DuelAcceptCommand;
import org.flennn.Commands.DuelCommand;
import org.flennn.Managers.SQLiteManager;

import java.util.Objects;

public class JackpotDuel extends JavaPlugin {
    private SQLiteManager database;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize the database manager
        database = new SQLiteManager(this);
        if (database.init()) {
            getLogger().info("Database connected successfully!");
        } else {
            getLogger().severe("Failed to connect to the database!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        Objects.requireNonNull(getCommand("duel")).setExecutor(new DuelCommand(this, database));
        Objects.requireNonNull(getCommand("duel")).setExecutor(new DuelAcceptCommand(this, database));

        new BukkitRunnable() {
            @Override
            public void run() {
                database.checkAndExpireRequests();
            }
        }.runTaskTimer(this, 0L, 20L * 10);
    }


    // SQLiteManager instance
    public SQLiteManager getDatabase() {
        return database;
    }

}
