package org.flennn.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.flennn.JackpotDuel;
import org.flennn.Managers.SQLiteManager;
import org.flennn.Utils.MessageFormatting;

import java.util.UUID;

public class DuelCommand implements CommandExecutor {

    private final JackpotDuel plugin;
    private final SQLiteManager database;

    public DuelCommand(JackpotDuel plugin, SQLiteManager database) {
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
            player.sendMessage(MessageFormatting.errorMessage("Usage: /jackpotduel <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MessageFormatting.errorMessage("Player not found or not online."));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(MessageFormatting.errorMessage("You cannot duel yourself."));
            return true;
        }

        UUID challengerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (database.hasActiveRequest(challengerId, targetId) != null) {
            player.sendMessage(MessageFormatting.errorMessage("You already sent a duel request to this player."));
            return true;
        }

        database.insertRequest(challengerId, targetId, System.currentTimeMillis());

        player.sendMessage(MessageFormatting.successMessage("Duel request sent to " + target.getName()));

        String message = "<yellow>" + player.getName() + " has challenged you to a duel!</yellow>\n" +
                "<gray>Type </gray><green>/jackpotaccept " + player.getName() + "<gray> to accept </gray>" +
                "<red>/jackpotdeny<gray> to deny.</gray>";

        Component formattedMessage = MessageFormatting.formatMessage(message);

        target.sendMessage(formattedMessage);

        target.sendMessage(MessageFormatting.clickableAndHoverableMessage(
                "/jackpotaccept " + player.getName(),
                "/jackpotaccept " + player.getName(),
                "Click to accept the duel!"));

        target.sendMessage(MessageFormatting.clickableAndHoverableMessage(
                "/jackpotdeny",
                "/jackpotdeny",
                "Click to deny the duel."));

        return true;
    }
}
