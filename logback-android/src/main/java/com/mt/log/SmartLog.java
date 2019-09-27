package com.mt.log;

import android.text.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.Duration;

public class SmartLog {

    final static String SMART_LOG_TO_LOGCAT = "SmartLog";
    final static String SMART_LOG_TO_FILE = "File";
    final static String SMART_LOG_TO_SERVER = "Server";

    private String mLogFilePath = null;
    private String mServerHost = null;
    private int mServerPort;

    private static  SmartLog sSmartLog;

    public static SmartLog getInstance() {
        if (sSmartLog == null) {
            synchronized (SmartLog.class) {
                if (sSmartLog == null) {
                    sSmartLog = new SmartLog();
                }
            }
        }
        return sSmartLog;
    }

    private SmartLog() {

    }

    public void init(String logFilePath, String serverHost, int serverPort) {
        mLogFilePath = logFilePath;
        mServerHost = serverHost;
        mServerPort = serverPort;
        config();
    }

    public void init() {
        config();
    }

    private void config() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.stop();
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (!TextUtils.isEmpty(mLogFilePath)) {
            PatternLayoutEncoder encoder1 = new PatternLayoutEncoder();
            encoder1.setContext(lc);
            encoder1.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            encoder1.start();
            FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
            fileAppender.setName(SMART_LOG_TO_FILE);
            fileAppender.setAppend(false);
            fileAppender.setContext(lc);
            fileAppender.setFile(mLogFilePath);
            fileAppender.setEncoder(encoder1);
            fileAppender.start();
            root.addAppender(fileAppender);
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SMART_LOG_TO_FILE);
            logger.setLevel(Level.ALL);
            logger.setAdditive(false);
            logger.addAppender(fileAppender);
        }

        if (!TextUtils.isEmpty(mServerHost)) {
            SocketAppender socketAppender = new SocketAppender();
            socketAppender.setName(SMART_LOG_TO_SERVER);
            socketAppender.setContext(lc);
            socketAppender.setRemoteHost(mServerHost);
            socketAppender.setPort(mServerPort);
            socketAppender.setReconnectionDelay(new Duration(10000));
            socketAppender.start();
            root.addAppender(socketAppender);
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SMART_LOG_TO_SERVER);
            logger.setLevel(Level.ALL);
            logger.setAdditive(false);
            logger.addAppender(socketAppender);
        }

        PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
        encoder2.setContext(lc);
        encoder2.setPattern("[%thread] %msg%n");
        encoder2.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setName(SMART_LOG_TO_LOGCAT);
        logcatAppender.setContext(lc);
        logcatAppender.setEncoder(encoder2);
        logcatAppender.start();
        root.addAppender(logcatAppender);

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SMART_LOG_TO_LOGCAT);
        logger.setLevel(Level.ALL);
        logger.setAdditive(false);
        logger.addAppender(logcatAppender);
    }

    public void setLogFilePath(String logFIlePath) {
        this.mLogFilePath = logFIlePath;
    }

    public void setServerHost(String serverHost) {
        this.mServerHost = serverHost;
    }

    public void setServerPort(int serverPort) {
        this.mServerPort = serverPort;
    }

    private static boolean sEnableLog = true;

    public static void enableLog(boolean flag) {
        sEnableLog = flag;
    }

    public static void infoToLogcat(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_LOGCAT).info(msg);
        }
    }

    public static void infoToFile(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_FILE).info(msg);
        }
    }

    public static void infoToServer(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_SERVER).info(msg);
        }
    }

    public static void infoToAll(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger("root").info(msg);
        }
    }

    public static void infoToLogcat(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_LOGCAT));
                logger.info(msg);
            } else {
                LoggerFactory.getLogger(tag).info(msg);
            }
        }
    }

    public static void infoToFile(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_FILE));
                logger.info(msg);
            } else {
                LoggerFactory.getLogger(tag).info(msg);
            }
        }
    }

    public static void infoToServer(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_SERVER));
                logger.info(msg);
            } else {
                LoggerFactory.getLogger(tag).info(msg);
            }
        }
    }

    public static void infoToAll(String tag, String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(tag).info(msg);
        }
    }
}
