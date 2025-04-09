package org.flennn.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.flennn.JackpotDuel;
import org.flennn.Managers.SQLiteManager;
import org.flennn.Utils.MessageFormatting;

import java.util.UUID;

public class DuelAcceptCommand implements CommandExecutor {

    private final JackpotDuel plugin;
    private final SQLiteManager database;

    public DuelAcceptCommand(JackpotDuel plugin, SQLiteManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(MessageFormatting.errorMessage("Usage: /jackpotaccept <playername>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MessageFormatting.errorMessage("Player not found or not online."));
            return true;
        }

        UUID challengerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        String requestId = database.hasActiveRequest(targetId, challengerId);

        if (requestId == null) {
            player.sendMessage(MessageFormatting.errorMessage("There is no active duel request from " + target.getName()));
            return true;
        }

        database.deleteRequestById(Integer.parseInt(requestId));

        player.sendMessage(MessageFormatting.successMessage("You have accepted the duel challenge from " + target.getName()));
        target.sendMessage(MessageFormatting.successMessage("Your duel challenge to " + player.getName() + " has been accepted!"));

        // Implement duel GUI logic

        return true;
    }
}
