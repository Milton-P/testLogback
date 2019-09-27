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

    private static SmartLog sSmartLog;

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
            encoder1.setPattern("%date [%thread] %level %logger{0}:  %msg%n");
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
        encoder2.setPattern("%msg%n");
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

    public static void debugToLogcat(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_LOGCAT).debug(msg);
        }
    }

    public static void debugToFile(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_FILE).debug(msg);
        }
    }

    public static void debugToServer(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_SERVER).debug(msg);
        }
    }

    public static void debugToAll(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger("root").debug(msg);
        }
    }

    public static void debugToLogcat(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_LOGCAT));
                logger.debug(msg);
            } else {
                LoggerFactory.getLogger(tag).debug(msg);
            }
        }
    }

    public static void debugToFile(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_FILE));
                logger.debug(msg);
            } else {
                LoggerFactory.getLogger(tag).debug(msg);
            }
        }
    }

    public static void debugToServer(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_SERVER));
                logger.debug(msg);
            } else {
                LoggerFactory.getLogger(tag).debug(msg);
            }
        }
    }

    public static void debugToAll(String tag, String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(tag).debug(msg);
        }
    }

    public static void warnToLogcat(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_LOGCAT).warn(msg);
        }
    }

    public static void warnToFile(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_FILE).warn(msg);
        }
    }

    public static void warnToServer(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_SERVER).warn(msg);
        }
    }

    public static void warnToAll(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger("root").warn(msg);
        }
    }

    public static void warnToLogcat(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_LOGCAT));
                logger.warn(msg);
            } else {
                LoggerFactory.getLogger(tag).warn(msg);
            }
        }
    }

    public static void warnToFile(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_FILE));
                logger.warn(msg);
            } else {
                LoggerFactory.getLogger(tag).warn(msg);
            }
        }
    }

    public static void warnToServer(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_SERVER));
                logger.warn(msg);
            } else {
                LoggerFactory.getLogger(tag).warn(msg);
            }
        }
    }

    public static void warnToAll(String tag, String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(tag).warn(msg);
        }
    }

    public static void errorToLogcat(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_LOGCAT).error(msg);
        }
    }

    public static void errorToFile(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_FILE).error(msg);
        }
    }

    public static void errorToServer(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(SMART_LOG_TO_SERVER).error(msg);
        }
    }

    public static void errorToAll(String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger("root").error(msg);
        }
    }

    public static void errorToLogcat(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_LOGCAT));
                logger.error(msg);
            } else {
                LoggerFactory.getLogger(tag).error(msg);
            }
        }
    }

    public static void errorToFile(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_FILE));
                logger.error(msg);
            } else {
                LoggerFactory.getLogger(tag).error(msg);
            }
        }
    }

    public static void errorToServer(String tag, String msg) {
        if (sEnableLog) {
            if (((LoggerContext) LoggerFactory.getILoggerFactory()).exists(tag) == null) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(tag);
                ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
                logger.setLevel(Level.ALL);
                logger.setAdditive(false);
                logger.addAppender(rootLogger.getAppender(SMART_LOG_TO_SERVER));
                logger.error(msg);
            } else {
                LoggerFactory.getLogger(tag).error(msg);
            }
        }
    }

    public static void errorToAll(String tag, String msg) {
        if (sEnableLog) {
            LoggerFactory.getLogger(tag).error(msg);
        }
    }
}
