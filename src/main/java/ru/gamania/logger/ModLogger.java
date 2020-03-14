package ru.gamania.logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

import ru.gamania.logger.config.Config;
import ru.gamania.logger.streams.FileReaderStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

@Mod(
    modid = "modlogger",
    name = "ModLogger",
    version="1.0",
    acceptableRemoteVersions = "*"
)
public class ModLogger
{
    @Instance
    public static ModLogger instance;

    public Logger logger = null;

    private Config config;
    
    private final Timer timer1 = new Timer();
    private final Timer timer2 = new Timer();

    private FileReaderStream fileReaderStream;

    private int lastSize;

    // Database connection
    private Connection conn = null;

    // Message buffer used for log4j-based logging TODO uncomment
    // private List<String> msgBuffer = new ArrayList<String>();

    @Mod.EventHandler
    public void onPreInit(final FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        config = new Config(event.getSuggestedConfigurationFile());
        fileReaderStream = new FileReaderStream(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void onServerStarted(final FMLServerStartedEvent event)
    {
        config.update();

        // Configurator.initialize("MyName", "file:/D:/source/minecraft/mods/SocketLogger/src/main/resources/log4j.xml"); TODO fix

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.fatal("Cannot find jdbc driver", e);
        }

        timer1.schedule(new TimerTask() {
            boolean stackTracePrinted = false;

            @Override
            public synchronized void run() {
                logger.info("[ModLogger] Trying to connect to database...");
                try {
                    conn = DriverManager.getConnection(
                        "jdbc:mysql://" + config.getDbHost() + "/" + config.getDbName() + "?useUnicode=true&serverTimezone=UTC",
                        config.getDbUser(),
                        config.getDbPassword()
                    );
                    logger.info("[ModLogger] Connected to database successfully");
                    timer1.cancel();
                    timer1.purge();
                    startLogging();
                } catch (SQLException e) {
                    final String msg = "[ModLogger] Unable to connect to database, retrying in " + config.getConnectRetryRate() + "ms";
                    // Print stacktrace only for the first time of being unable to connect
                    if (stackTracePrinted) {
                        logger.error(msg);
                    } else {
                        logger.error(msg, e);
                        stackTracePrinted = true;
                    }
                }
            }
        }, 0, config.getConnectRetryRate());
    }

    private void startLogging()
    {
        logger.info("[ModLogger] Started logging");
        timer2.scheduleAtFixedRate(new TimerTask() {
            @Override
            public synchronized void run() {
                List<String> lines = fileReaderStream.getLines();

                if (lastSize != lines.size()) {
                    StringBuilder sql = new StringBuilder("INSERT INTO `logs` (`server_uid`, `line`) VALUES");
                    List<String> linesToSend = new ArrayList<>();

                    ListIterator<String> it = lines.listIterator(lastSize);
                    lastSize = lines.size();

                    while (it.hasNext()) {
                        String line = it.next();
                        if (isBlacklisted(line)) {
                            continue;
                        }

                        sql.append(" (?, ?),");
                        linesToSend.add(line);
                    }

                    if (linesToSend.isEmpty()) {
                        return;
                    }

                    try (PreparedStatement stmt = conn.prepareStatement(sql.substring(0, sql.length() - 1))){
                        int paramIndex = 1;
                        for (String line : linesToSend) {
                            stmt.setInt(paramIndex++, config.getServerUid());
                            stmt.setString(paramIndex++, line);
                        }

                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        logger.error("[ModLogger] Unable to send logs", e);
                    }
                }
            }
        }, 0, config.getUpdateRate());
    }

    private boolean isBlacklisted(final String line)
    {
        for (Pattern ptr : config.getLogBlacklistPatterns()){
            Matcher matcher = ptr.matcher(line);
            if (matcher.find()) {
                return true;
            }
        }

        return false;
    }

    /* TODO uncomment and use for log4j-based logging
    public static void log(String message) {
        ModLogger.instance.msgBuffer.add(message);
    }
    */

    @Mod.EventHandler
    public void onServerStopping(final FMLServerStoppingEvent event)
    {
        if (null != conn) {
            logger.info("[ModLogger] Closing database connection...");
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("[ModLogger] An error occurred while attemptint to close the database connection", e);
            }
        }
        timer1.cancel();
        timer1.purge();
        timer2.cancel();
        timer2.purge();
    }
}
