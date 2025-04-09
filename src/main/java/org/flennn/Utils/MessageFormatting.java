package org.flennn.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MessageFormatting {

    private static final String PREFIX = "<gold><bold>ᴊᴀᴄᴋᴘᴏᴛ</bold></gold> <gray>»</gray>> <reset>";

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static Component formatMessage(String message) {
        return miniMessage.deserialize(PREFIX + message);
    }

    public static Component successMessage(String message) {
        return miniMessage.deserialize(PREFIX + "<green>" + message + "</green>");
    }

    public static Component errorMessage(String message) {
        return miniMessage.deserialize(PREFIX + "<red>" + message + "</red>");
    }

    public static Component warningMessage(String message) {
        return miniMessage.deserialize(PREFIX + "<yellow>" + message + "</yellow>");
    }

    public static Component infoMessage(String message) {
        return miniMessage.deserialize(PREFIX + "<blue>" + message + "</blue>");
    }

    public static Component customColorMessage(String message, String hexColor) {
        return miniMessage.deserialize(PREFIX + "<#"+hexColor+">" + message + "</#>");
    }

    public static Component clickableMessage(String message, String command) {
        return miniMessage.deserialize(PREFIX + "<green>" + message + "</green>")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command));
    }

    public static Component hoverableMessage(String message, String hoverText) {
        return miniMessage.deserialize(PREFIX + "<blue>" + message + "</blue>")
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(miniMessage.deserialize(hoverText)));
    }

    public static Component clickableAndHoverableMessage(String message, String command, String hoverText) {
        return miniMessage.deserialize(PREFIX + "<yellow>" + message + "</yellow>")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(miniMessage.deserialize(hoverText)));
    }
}
