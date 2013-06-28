package info.tregmine.commands;

import org.bukkit.ChatColor;
import static org.bukkit.ChatColor.*;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;

public class ChangeNameCommand extends AbstractCommand
{
    public ChangeNameCommand(Tregmine tregmine)
    {
        super(tregmine, "cname");
    }

    @Override
    public boolean handlePlayer(TregminePlayer player, String[] args)
    {
        if (!player.isAdmin()) {
            return false;
        }

        ChatColor color = ChatColor.getByChar(args[0]);
        player.setTemporaryChatName(color + args[1]);
        player.sendMessage("You are now: " + player.getChatName());
        LOGGER.info(player.getName() + " changed name to " + player.getChatName());

        return true;
    }
}
