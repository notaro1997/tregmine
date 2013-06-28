package info.tregmine.database;

import info.tregmine.database.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DBChestBlessDAO
{
    private Connection conn;

    public DBChestBlessDAO(Connection conn)
    {
        this.conn = conn;
    }

    public void deleteBless(int checksum, String player, String world)
    {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM chestbless WHERE checksum = ?");
            stmt.setInt(1, checksum);
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
    }

    public void saveBless(int checksum, String player, String world)
    {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("insert into chestbless (checksum, world,  player) values (?,?,?)");
            stmt.setInt(1, checksum);
            stmt.setString(2, world);
            stmt.setString(3, player);
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
    }

    public Map<Integer, String> loadBlessedChests()
    {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Map<Integer, String> chests = new HashMap<Integer, String>();

            stmt = conn.prepareStatement("SELECT * FROM  chestbless");
            stmt.execute();

            rs = stmt.getResultSet();
            while (rs.next()) {
                chests.put(rs.getInt("checksum"), rs.getString("player"));
            }

            return chests;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
    }
}
