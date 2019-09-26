package com.mt.syslogsdk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;

//logback 介绍 https://logback.qos.ch/manual/introduction.html
public class MainActivity extends AppCompatActivity {

    static final String LOGBACK_XML =
            "<configuration>" +
                    "<appender name='FILE' class='ch.qos.logback.core.FileAppender'>" +
                    "<file>/data/data/com.mt.syslogsdk/files/app.log</file>" +
                    "<append>false</append>" +
                    "<encoder>" +
                    "<pattern>%-4r [%t] %-5p %c{35} - %m%n</pattern>" +
                    "</encoder>" +
                    "</appender>" +
                    "<appender name='LOGCAT' class='ch.qos.logback.classic.android.LogcatAppender'>" +
                    "<encoder>" +
                    "<pattern>%msg</pattern>" +
                    "</encoder>" +
                    "</appender>" +
                    "<appender name='SOCKET' class='ch.qos.logback.classic.net.SocketAppender'>" +
                    "<remoteHost>10.12.32.158</remoteHost>" +
                    "<port>10544</port>" +
                    "<reconnectionDelay>10000</reconnectionDelay>" +
                    "</appender>" +
                    "<logger name='file' level='DEBUG' additivity='false'>" +
                    "<appender-ref ref='FILE' />" +
                    "</logger>" +
                    "<logger name='terminal' level='DEBUG' additivity='false'>" +
                    "<appender-ref ref='LOGCAT' />" +
                    "</logger>" +
                    "<logger name='net' level='DEBUG' additivity='false'>" +
                    "<appender-ref ref='SOCKET' />" +
                    "</logger>" +
                    "<root level='DEBUG'>" +
                    "<appender-ref ref='LOGCAT' />" +
                    "<appender-ref ref='FILE' />" +
                    "<appender-ref ref='SOCKET' />" +
                    "</root>" +
                    "</configuration>"
            ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        //configureLogbackDirectly();
        configureLogbackByString();
    }

    private void init() {
        Button button = findViewById(R.id.test);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(()-> {
                    test();
                }).start();
            }
        });
    }

    private void test() {
        Logger mainActivityLogger = LoggerFactory.getLogger(MainActivity.class);
        ch.qos.logback.classic.Logger classicLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("test");
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("root");
        classicLogger.setLevel(Level.ALL);
        classicLogger.setAdditive(false);
        classicLogger.addAppender(rootLogger.getAppender("FILE"));
        classicLogger.addAppender(rootLogger.getAppender("LOGCAT"));

        Logger fileLogger = LoggerFactory.getLogger("file");
        Logger terminalLogger = LoggerFactory.getLogger("terminal");
        Logger netLogger = LoggerFactory.getLogger("net");

        mainActivityLogger.info("milton mainActivityLogger");
        rootLogger.info("milton rootLogger");
        fileLogger.info("milton fileLogger");
        terminalLogger.info("milton info terminalLogger");
        terminalLogger.debug("milton debug terminalLogger");
        terminalLogger.error("milton error terminalLogger");
        terminalLogger.trace("milton trace terminalLogger");
        netLogger.info("milton netLogger");
        classicLogger.info("milton testClassic");

    }

    private void configureLogbackDirectly() {
        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it
        LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        lc.stop();

        // setup FileAppender
        PatternLayoutEncoder encoder1 = new PatternLayoutEncoder();
        encoder1.setContext(lc);
        encoder1.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder1.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setContext(lc);
        android.util.Log.d("milton", " path = " + this.getFileStreamPath("app.log").getAbsolutePath());
        fileAppender.setFile(this.getFileStreamPath("app.log").getAbsolutePath());
        fileAppender.setEncoder(encoder1);
        fileAppender.start();

        // setup LogcatAppender
        PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
        encoder2.setContext(lc);
        encoder2.setPattern("[%thread] %msg%n");
        encoder2.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(lc);
        logcatAppender.setEncoder(encoder2);
        logcatAppender.start();

        // add the newly created appenders to the root logger;
        // qualify Logger to disambiguate from org.slf4j.Logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppender);
        root.addAppender(logcatAppender);
    }


    private void configureLogbackByString() {
        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it
        LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        lc.stop();

        JoranConfigurator config = new JoranConfigurator();
        config.setContext(lc);

        InputStream stream = new ByteArrayInputStream(LOGBACK_XML.getBytes());
        try {
            config.doConfigure(stream);
        } catch (JoranException e) {
            e.printStackTrace();
        }
    }
}
