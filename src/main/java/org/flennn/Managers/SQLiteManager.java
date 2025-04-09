package org.flennn.Managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.flennn.Utils.ItemUtils;

import java.sql.*;
import java.util.UUID;

public class SQLiteManager {
    private Connection connection;
    private final JavaPlugin plugin;

    public SQLiteManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/duels.db");
            createTableIfNeeded();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error initializing the SQLite database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void createTableIfNeeded() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS duel_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    challenger TEXT NOT NULL,
                    target TEXT NOT NULL,
                    timestamp LONG NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS duels (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player01 BLOB NOT NULL,
                    player02 BLOB NOT NULL,
                    items01 TEXT,
                    items02 TEXT,
                    state TEXT NOT NULL CHECK(state IN ('CONFIRMING', 'ROLLING', 'END')),
                    winner BLOB,
                    loser BLOB,
                    created_at LONG NOT NULL
                )
            """);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String hasActiveRequest(UUID challenger, UUID target) {
        String query = "SELECT * FROM duel_requests WHERE challenger = ? AND target = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, challenger.toString());
            stmt.setString(2, target.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return String.valueOf(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking active request: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    public void insertRequest(UUID challenger, UUID target, long timestamp) {
        String query = "INSERT INTO duel_requests (challenger, target, timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, challenger.toString());
            stmt.setString(2, target.toString());
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error inserting duel request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteRequestById(int id) {
        String query = "DELETE FROM duel_requests WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting duel request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void checkAndExpireRequests() {
        long now = System.currentTimeMillis();
        long expiryDurationMillis = 3 * 60 * 1000;
        long expiryThreshold = now - expiryDurationMillis;

        String query = "SELECT id, challenger, target, timestamp FROM duel_requests";

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                UUID challengerId = UUID.fromString(rs.getString("challenger"));
                UUID targetId = UUID.fromString(rs.getString("target"));
                long timestamp = rs.getLong("timestamp");

                if (timestamp < expiryThreshold) {
                    Player challenger = Bukkit.getPlayer(challengerId);
                    Player target = Bukkit.getPlayer(targetId);

                    if (challenger != null && challenger.isOnline()) {
                        challenger.sendMessage("§cYour duel request to " + target.getName() + " has expired.");
                    }

                    if (target != null && target.isOnline()) {
                        target.sendMessage("§cThe duel request from " + challenger.getName() + " has expired.");
                    }

                    deleteRequestById(id);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking expired duel requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startDuel(UUID p1, UUID p2, ItemStack[] i1, ItemStack[] i2) {
        String query = """
            INSERT INTO duels (player01, player02, items01, items02, state, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setBytes(1, ItemUtils.uuidToBytes(p1));
            stmt.setBytes(2, ItemUtils.uuidToBytes(p2));
            stmt.setString(3, ItemUtils.serializeItems(i1));
            stmt.setString(4, ItemUtils.serializeItems(i2));
            stmt.setString(5, DuelState.CONFIRMING.name());
            stmt.setLong(6, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to start duel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void finishDuel(int duelId, UUID winner, UUID loser) {
        String query = "UPDATE duels SET state = ?, winner = ?, loser = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, DuelState.END.name());
            stmt.setBytes(2, ItemUtils.uuidToBytes(winner));
            stmt.setBytes(3, ItemUtils.uuidToBytes(loser));
            stmt.setInt(4, duelId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to finish duel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public DuelState getDuelState(int duelId) {
        String query = "SELECT state FROM duels WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, duelId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String stateString = rs.getString("state");
                    return DuelState.valueOf(stateString);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting duel state: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Invalid duel state retrieved: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing SQLite connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public enum DuelState {
        CONFIRMING,
        ROLLING,
        END
    }
}
