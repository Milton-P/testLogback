/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.classic.issue.lbclassic135;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.contention.ThreadedThroughputCalculator;
import ch.qos.logback.core.util.CoreTestConstants;

/**
 * Short sample code testing the throughput of a fair lock.
 *
 * @author Ceki Gulcu
 */
public class LoggingToFileThroughput {

  static int THREAD_COUNT = 1;
  static long OVERALL_DURATION_IN_MILLIS = 5000;

  public static void main(String args[]) throws InterruptedException {

    ThreadedThroughputCalculator tp = new ThreadedThroughputCalculator(
        OVERALL_DURATION_IN_MILLIS);
    tp.printEnvironmentInfo("lbclassic135  LoggingToFileThrouhput");

    LoggerContext lc = new LoggerContext();
    Logger logger = buildLoggerContext(lc);

    for (int i = 0; i < 2; i++) {
      tp.execute(buildArray(logger));
    }

    tp.execute(buildArray(logger));
    tp.printThroughput("File:   ");
    lc.stop();
  }

  static Logger buildLoggerContext(LoggerContext lc) {
    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);

    PatternLayoutEncoder patternLayout = new PatternLayoutEncoder();
    patternLayout.setContext(lc);
    patternLayout.setPattern("%d %l [%t] - %msg%n");
    patternLayout.start();
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
    fileAppender.setContext(lc);
    fileAppender.setFile(CoreTestConstants.TARGET_DIR + "lbclassic135.log");
    fileAppender.setEncoder(patternLayout);
    fileAppender.setAppend(false);
    fileAppender.start();
    root.addAppender(fileAppender);
    return lc.getLogger(LoggingToFileThroughput.class);
  }

  static LoggingRunnable[] buildArray(Logger logger) {

    LoggingRunnable[] array = new LoggingRunnable[THREAD_COUNT];
    for (int i = 0; i < THREAD_COUNT; i++) {
      array[i] = new LoggingRunnable(logger);
    }
    return array;
  }
}

//=== lbclassic135  LoggingToFileThrouhput ===
// ******** 10 Threads *****
// synchronized doAppend() method
//
// java.runtime.version = 1.6.0_05-b13
// java.vendor          = Sun Microsystems Inc.
// os.name              = Windows XP
//
// Threads 1:   total of 485077 operations, or 97 operations per millisecond
// Threads 10:  total of 309402 operations, or 61 operations per millisecond

//* After revision 2310
//* Threads 1:  total of 462465 operations, or 92 operations per millisecond
//* Threads 10: total of 243362 operations, or 48 operations per millisecond

// ==================== Linux ========================

// java.runtime.version = 1.6.0_11-b03
// java.vendor          = Sun Microsystems Inc.
// os.name              = Linux
// os.version           = 2.6.25-gentoo-r6
// Threads 1:  total of 356355 operations, or 71 operations per millisecond
// Threads 10: total of 287943 operations, or 57 operations per millisecond

//* After revision 2310
//* Threads 1:  total of 331494 operations, or 66 operations per millisecond
//* Threads 10: total of 311104 operations, or 58 operations per millisecond

// java.runtime.version = jvmxa6460-20081105_25433
// java.vendor          = IBM Corporation
// java.version         = 1.6.0
// os.name              = Linux
// os.version           = 2.6.25-gentoo-r6
// Threads 1:  total of 280381 operations, or 56 operations per millisecond
// Threads 10  total of 142989 operations, or 28 operations per millisecond

//* After revision 2310
//* Threads 1:   total of 305638 operations, or 61 operations per millisecond
//* Threads 10:  total of 147660 operations, or 29 operations per millisecond
