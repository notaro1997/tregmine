package info.tregmine.listeners;

import java.io.IOException;
//import java.net.InetAddress;
import java.net.InetSocketAddress;
//import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
//import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

import info.tregmine.Tregmine;
import info.tregmine.database.ConnectionPool;
import info.tregmine.database.DBWalletDAO;
import info.tregmine.api.TregminePlayer;

public class PlayerLookupListener implements  Listener
{
    private final Tregmine plugin;
    private LookupService cl = null;

    public PlayerLookupListener(Tregmine instance)
    {
        plugin = instance;
        try {
            cl = new LookupService("GeoIPCity.dat", LookupService.GEOIP_MEMORY_CACHE );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        InetSocketAddress sock = player.getAddress();
        String ip = sock.getAddress().getHostAddress();
        String host = sock.getAddress().getCanonicalHostName();
        TregminePlayer tregminePlayer = plugin.getPlayer(player);

        if (cl != null) {
            Location l1 = cl.getLocation(ip);
            if (l1 == null) {
                return;
            }

            Tregmine.LOGGER.info(event.getPlayer().getName() + ": " + l1.countryName + ", " + l1.city + ", " + ip + ", " + l1.postalCode + ", " + l1.region + ", " + host);
            tregminePlayer.setMetaString("countryName", l1.countryName);
            tregminePlayer.setMetaString("city", l1.city);
            tregminePlayer.setMetaString("ip", ip);
            tregminePlayer.setMetaString("postalCode", l1.postalCode);
            tregminePlayer.setMetaString("region", l1.region);
            tregminePlayer.setMetaString("hostname", host);

            if (!event.getPlayer().isOp()) {
                if(tregminePlayer.getMetaBoolean("hiddenlocation")) {
                } else {
                    this.plugin.getServer().broadcastMessage(ChatColor.DARK_AQUA + "Welcome! " + tregminePlayer.getChatName() + ChatColor.DARK_AQUA + " from " +l1.countryName);
                    event.getPlayer().sendMessage(ChatColor.DARK_AQUA + l1.city + " - " + l1.postalCode);
                }
            }
        }

        if (tregminePlayer.getNameColor() == ChatColor.GOLD ||
            tregminePlayer.getNameColor() == ChatColor.DARK_PURPLE ||
            tregminePlayer.isAdmin() ||
            tregminePlayer.isGuardian() ||
            tregminePlayer.getMetaBoolean("builder")) {

            event.getPlayer().sendMessage("You are allowed to fly");
            event.getPlayer().setAllowFlight(true);
            //tregminePlayer.setAllowFlight(true);
        } else {
            event.getPlayer().sendMessage("no-z-cheat");
            event.getPlayer().sendMessage("You are NOT allowed to fly");
            event.getPlayer().setAllowFlight(false);
        }

        event.getPlayer().sendMessage(ChatColor.WHITE + "<" + ChatColor.RED + "GOD"+ ChatColor.WHITE + ">"+ ChatColor.GREEN + " Don't forget that you can get free stuff by voting http://treg.co/82");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String all = "";

        try {
            conn = ConnectionPool.getConnection();

            stmt = conn.prepareStatement("SELECT user.player FROM user, " +
                    "user_settings WHERE user.uid=user_settings.id AND " +
                    "minecraft.user_settings.value=? ORDER BY time DESC LIMIT 5");
            stmt.setString(1, ip);
            stmt.execute();

            rs = stmt.getResultSet();

            while (rs.next()) {
                String name =  rs.getString("player");
                all = name + ", "+ all;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {}
            }
        }

        Tregmine.LOGGER.info("ALIAS: " + all);

        Player[] players = plugin.getServer().getOnlinePlayers();
        for (Player allplayer : players) {
            TregminePlayer allP = plugin.getPlayer(allplayer);

            if (allP.isAdmin() || allP.isGuardian()) {
                if(!tregminePlayer.getMetaBoolean("hiddenlocation")) {
                    allP.sendMessage(ChatColor.YELLOW + "This player have also used names: " + all);
                }
            }
        }

        if (tregminePlayer.getMetaBoolean("builder")) {
            event.getPlayer().setGameMode(GameMode.CREATIVE);
        } else {
            if (!tregminePlayer.isOp()) {
                event.getPlayer().setGameMode(GameMode.SURVIVAL);
            }
        }

        final Player mcplayer = event.getPlayer();
        if (mcplayer.isOnline()) {

            final ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard board = manager.getNewScoreboard();

            Objective objective = board.registerNewObjective("1", "2");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName("" + ChatColor.DARK_RED + "" + ChatColor.BOLD + "Welcome to Tregmine!");

            conn = null;
            try {
                conn = ConnectionPool.getConnection();
                DBWalletDAO walletDAO = new DBWalletDAO(conn);

                Score score = objective.getScore(Bukkit.getOfflinePlayer(ChatColor.BLACK + "Your Balance:")); //Get a fake offline player
                score.setScore((int)walletDAO.balance(mcplayer.getName()));
                mcplayer.setScoreboard(board);

                Bukkit.getServer()
                      .getScheduler()
                      .scheduleSyncDelayedTask(plugin,
                            new Runnable() {
                                public void run() {
                                    mcplayer.setScoreboard(manager.getNewScoreboard());
                                }
                            }, 400); //400 = 20 seconds. 1 second = 20 ticks, 20*20=400
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
            finally {
                if (conn != null) {
                    try { conn.close(); } catch (SQLException e) {}
                }
            }

        }
    }
}
