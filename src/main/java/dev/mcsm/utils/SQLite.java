package dev.mcsm.utils;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SQLite {

    static String url = "jdbc:sqlite:storage.db";
    static Connection connection;

    public static void init() {
        try {
            connection = DriverManager.getConnection(url);

            connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS storage (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                s TEXT NOT NULL)""");
            connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS updates (
                guildId INTEGER NOT NULL PRIMARY KEY,
                channelId INTEGER NOT NULL )""");
            connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS bugs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            userId INTEGER NOT NULL,
            bug TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'pending',
            messageId INTEGER NOT NULL DEFAULT 0,
            threadId INTEGER NOT NULL DEFAULT 0)
            """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void store(@NotNull List<String> data) {
        String sql = "INSERT INTO storage (s) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String s : data) {
                statement.setString(1, s);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static List<String> getData() {
        List<String> dataList = new ArrayList<>();
        String sql = "SELECT s FROM storage";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String data = resultSet.getString("s");
                dataList.add(data);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return dataList;
    }

    public static void deleteAllData() {
        String sql = "DELETE FROM storage";
        try {
            connection.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setUpdateChannel(int guildId, int channelId) {
        String sql = "INSERT INTO updates (guildId, channelId) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, guildId);
            statement.setLong(2, channelId);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Long> fetchAllUpdateChannels() {
        List<Long> channelIds = new ArrayList<>();
        String sql = "SELECT channelId FROM updates";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                long channelId = resultSet.getLong("channelId");
                channelIds.add(channelId);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return channelIds;
    }

    public static boolean hasUpdateChannel(long guildId) {
        String sql = "SELECT channelId FROM updates WHERE guildId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, guildId);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void newBug(long userId, String bug) {
        String sql = "INSERT INTO bugs (userId, bug) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, bug);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateBugStatus(int id, @NotNull BugStatus status) {
        String sql = "UPDATE bugs SET status = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getBugId(String bug) {
        String sql = "SELECT id FROM bugs WHERE bug = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bug);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getUserId(int id) {
        String sql = "SELECT userId FROM bugs WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong("userId");
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setMessageId(int id, long messageId) {
        String sql = "UPDATE bugs SET messageId = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getMessageId(int id) {
        String sql = "SELECT messageId FROM bugs WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong("messageId");
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setThreadId(int id, long threadId) {
        String sql = "UPDATE bugs SET threadId = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, threadId);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getThreadId(int id) {
        String sql = "SELECT threadId FROM bugs WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong("threadId");
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getBug(int id) {
        String sql = "SELECT bug FROM bugs WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("bug");
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
