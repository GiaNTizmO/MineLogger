package ru.gamania.logger.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.regex.Pattern;

public class Config
{
    private File configFile;

    private int serverUid;
    private int updateRate;
    private int connectRetryRate;

    private String dbHost;
    private String dbName;
    private String dbUser;
    private String dbPass;

    private String[] logBlacklist;
    private Pattern[] logBlacklistPatterns;

    public Config(final File file)
    {
        configFile = file;
        this.update();
    }

    public void update()
    {
        Configuration cfg = new Configuration(configFile);

        serverUid = cfg.getInt("serverUID", "general", 0, 0, Integer.MAX_VALUE,  "Server id (check https://console.gamania.ru/servers/ for id list)");
        updateRate = cfg.getInt("updateRate", "general", 250, 50, Integer.MAX_VALUE, "How often are the logs being sent (in milliseconds)");

        dbHost = cfg.getString("host", "database", "127.0.0.1", "IP address");
        dbName = cfg.getString("name", "database", "socket_logger", "Database name");
        dbUser = cfg.getString("user", "database", "root", "User");
        dbPass = cfg.getString("password", "database", "", "Password");
        connectRetryRate = cfg.getInt("retryRate", "database", 30000, 50, Integer.MAX_VALUE, "How often does the mod try to reconnect to the database (in milliseconds)");

        logBlacklist = cfg.getStringList("blacklist", "logs", new String[] {
            // Errors
            "^\\[[^/]+/(?:WARN|ERROR)\\]",
            "^\\[?(?:com|java|org|net|me)\\.",
            "^\\tat",

            // Commands
            "^\\[Server thread/INFO\\]: \\w+ issued server command: /(?:/wand|we cui|rtp)",

            // Server thread/INFO
            "^\\[Server thread/INFO\\]: Injected",
            "^\\[Server thread/INFO\\]: Sending server configs",
            "^\\[Server thread/INFO\\]: Found mod",
            "^\\[Server thread/INFO\\]: \\w+\\[.*?\\] logged in with entity",
            "^\\[Server thread/INFO\\]: \\[Server thread\\] Server side modded connection established",
            "^\\[Server thread/INFO\\]: \\[ONTIME\\]",
            "^\\[Server thread/INFO\\]: Loading dimension",

            // Other
            "^\\[pool-\\d+-thread-\\d+/INFO\\]",
            "^\\[main/INFO\\]: (?:Calling|Loading) tweak class",
            "^\\[Netty IO #\\d+/INFO\\]: (?!\\[L|\\x1b\\[0;33;22mG\\] )",
            "^\\[User Authenticator #\\d+/INFO\\]",

        }, "Logs that are ignored and are not sent to the database");

        cfg.save();

        logBlacklistPatterns = new Pattern[logBlacklist.length];
        for (int i = 0, length = logBlacklist.length; i < length; i++)
        {
            String regex = logBlacklist[i];
            if (regex.startsWith("^"))
                regex = "^(?:\\[\\d{2}:\\d{2}:\\d{2}\\] )?" + regex.substring(1);
            logBlacklistPatterns[i] = Pattern.compile(regex);
        }
    }

    public int getServerUid()
    {
        return serverUid;
    }

    public int getUpdateRate()
    {
        return updateRate;
    }

    public int getConnectRetryRate()
    {
        return connectRetryRate;
    }

    public String getDbHost()
    {
        return dbHost;
    }

    public String getDbName()
    {
        return dbName;
    }

    public String getDbUser()
    {
        return dbUser;
    }

    public String getDbPassword()
    {
        return dbPass;
    }

    public String[] getLogBlacklist()
    {
        return logBlacklist;
    }

    public Pattern[] getLogBlacklistPatterns()
    {
        return logBlacklistPatterns;
    }
}
