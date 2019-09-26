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
package ch.qos.logback.classic.rolling;

import java.text.ParseException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.ScaffoldingForRollingTests;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class TimeBasedRollingWithConfigFileTest extends
        ScaffoldingForRollingTests {

  private LoggerContext lc = new LoggerContext();
  private StatusChecker statusChecker = new StatusChecker(lc);
  private Logger logger = lc.getLogger(this.getClass());
  private int fileSize = 0;
  private int fileIndexCounter = -1;
  private int sizeThreshold;

  @Before
  @Override
  public void setUp() throws ParseException {
    lc.setName("test");
    super.setUp();
    lc.putProperty("randomOutputDir", randomOutputDir);
  }

  @After
  public void tearDown() throws Exception {
  }

  private void loadConfig(String configFile) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(lc);
    jc.doConfigure(configFile);
    currentTime = System.currentTimeMillis();
    recomputeRolloverThreshold(currentTime);
  }

  @Test
  public void basic() throws Exception {
    String testId = "basic";
    lc.putProperty("testId", testId);
    loadConfig(ClassicTestConstants.JORAN_INPUT_PREFIX + "rolling/" + testId
            + ".xml");
    statusChecker.assertIsErrorFree();

    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);

    expectedFilenameList.add(randomOutputDir + "z" + testId);

    RollingFileAppender<ILoggingEvent> rfa = (RollingFileAppender<ILoggingEvent>) root
            .getAppender("ROLLING");

    TimeBasedRollingPolicy tprp = (TimeBasedRollingPolicy<ILoggingEvent>) rfa
            .getTriggeringPolicy();
    TimeBasedFileNamingAndTriggeringPolicy tbnatp = tprp
            .getTimeBasedFileNamingAndTriggeringPolicy();

    String prefix = "Hello---";
    int runLength = 4;
    for (int i = 0; i < runLength; i++) {
      logger.debug(prefix + i);
      addExpectedFileNamedIfItsTime_ByDate(randomOutputDir, testId, false);
      incCurrentTime(500);
      tbnatp.setCurrentTime(currentTime);
    }

    existenceCheck(expectedFilenameList);
    sortedContentCheck(randomOutputDir, runLength, prefix);
  }

  @Test
  public void depratedSizeAndTimeBasedFNATPWarning() throws Exception {
    String testId = "depratedSizeAndTimeBasedFNATPWarning";
    lc.putProperty("testId", testId);
    loadConfig(ClassicTestConstants.JORAN_INPUT_PREFIX + "rolling/" + testId + ".xml");
    statusChecker.assertContainsMatch(Status.WARN, CoreConstants.SIZE_AND_TIME_BASED_FNATP_IS_DEPRECATED);
  }

  @Test
  public void timeAndSize() throws Exception {
    String testId = "timeAndSize";
    lc.putProperty("testId", testId);
    String prefix = "Hello-----";

    // the number of times the log file will be written to before time based
    // roll-over occurs
    int approxWritesPerPeriod = 64;
    sizeThreshold = prefix.length() * approxWritesPerPeriod;
    lc.putProperty("sizeThreshold", "" + sizeThreshold);
    loadConfig(ClassicTestConstants.JORAN_INPUT_PREFIX + "rolling/" + testId
            + ".xml");

    // Test http://jira.qos.ch/browse/LOGBACK-1236
    statusChecker.assertNoMatch(CoreConstants.SIZE_AND_TIME_BASED_FNATP_IS_DEPRECATED);

    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);

    expectedFilenameList.add(randomOutputDir + "z" + testId);

    RollingFileAppender<ILoggingEvent> rfa = (RollingFileAppender<ILoggingEvent>) root
            .getAppender("ROLLING");

    statusChecker.assertIsErrorFree();

    TimeBasedRollingPolicy tprp = (TimeBasedRollingPolicy<ILoggingEvent>) rfa
            .getTriggeringPolicy();
    TimeBasedFileNamingAndTriggeringPolicy tbnatp = tprp
            .getTimeBasedFileNamingAndTriggeringPolicy();

    int timeIncrement = 1000 / approxWritesPerPeriod;
    int runLength = approxWritesPerPeriod * 3;
    for (int i = 0; i < runLength; i++) {
      String msg = prefix + i;
      logger.debug(msg);
      addExpectedFileNamedIfItsTime(testId, msg, false);
      incCurrentTime(timeIncrement);
      tbnatp.setCurrentTime(currentTime);
    }

    sortedContentCheck(randomOutputDir, runLength, prefix);
    int eCount = existenceCount(expectedFilenameList);
    // for various reasons, it is extremely difficult to have the files
    // match exactly the expected archive files. Thus, we aim for
    // an approximate match
    assertTrue("exitenceCount=" + eCount + ", expectedFilenameList.size="
            + expectedFilenameList.size(), eCount >= 4
            && eCount > expectedFilenameList.size() / 2);
  }

  @Test
  public void timeAndSizeWithoutIntegerToken() throws Exception {
    String testId = "timeAndSizeWithoutIntegerToken";
    loadConfig(ClassicTestConstants.JORAN_INPUT_PREFIX + "rolling/" + testId + ".xml");
    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    expectedFilenameList.add(randomOutputDir + "z" + testId);
    RollingFileAppender<ILoggingEvent> rfa = (RollingFileAppender<ILoggingEvent>) root.getAppender("ROLLING");
//        StatusPrinter.print(lc);

    statusChecker.assertContainsMatch("Missing integer token");
    assertFalse(rfa.isStarted());
  }

  // see also LOGBACK-1176
  @Test
  public void timeAndSizeWithoutMaxFileSize() throws Exception {
    String testId = "timeAndSizeWithoutMaxFileSize";
    loadConfig(ClassicTestConstants.JORAN_INPUT_PREFIX + "rolling/" + testId + ".xml");
    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    //expectedFilenameList.add(randomOutputDir + "z" + testId);
    RollingFileAppender<ILoggingEvent> rfa = (RollingFileAppender<ILoggingEvent>) root.getAppender("ROLLING");

    //statusChecker.assertContainsMatch("Missing integer token");
    assertFalse(rfa.isStarted());
  }

  @Test
  public void totalSizeCapSmallerThanMaxFileSize() throws Exception {
    String testId = "totalSizeCapSmallerThanMaxFileSize";
    lc.putProperty("testId", testId);
    loadConfig(ClassicTestConstants.JORAN_INPUT_PREFIX + "rolling/" + testId + ".xml");
    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    //expectedFilenameList.add(randomOutputDir + "z" + testId);
    RollingFileAppender<ILoggingEvent> rfa = (RollingFileAppender<ILoggingEvent>) root.getAppender("ROLLING");

    statusChecker.assertContainsMatch("totalSizeCap of \\[\\d* \\w*\\] is smaller than maxFileSize \\[\\d* \\w*\\] which is non-sensical");
    assertFalse(rfa.isStarted());
  }

  void addExpectedFileNamedIfItsTime(String testId, String msg,
                                     boolean gzExtension) {
    fileSize += msg.getBytes().length;

    if (passThresholdTime(nextRolloverThreshold)) {
      fileIndexCounter = 0;
      fileSize = 0;
      addExpectedFileName(testId, getDateOfPreviousPeriodsStart(),
              fileIndexCounter, gzExtension);
      recomputeRolloverThreshold(currentTime);
      return;
    }

    // windows can delay file size changes, so we only allow for
    // fileIndexCounter 0 and 1
    if ((fileIndexCounter < 1) && fileSize > sizeThreshold) {
      addExpectedFileName(testId, getDateOfPreviousPeriodsStart(),
              ++fileIndexCounter, gzExtension);
      fileSize = -1;
      return;
    }
  }

  void addExpectedFileName(String testId, Date date, int fileIndexCounter,
                           boolean gzExtension) {

    String fn = randomOutputDir + testId + "-" + SDF.format(date) + "."
            + fileIndexCounter;
    System.out.println("Adding " + fn);
    if (gzExtension) {
      fn += ".gz";
    }
    expectedFilenameList.add(fn);
  }

  @Override
  protected void addExpectedFileNamedIfItsTime_ByDate(String outputDir, String testId,
                                                      boolean gzExtension) {
    if (passThresholdTime(nextRolloverThreshold)) {
      addExpectedFileName_ByDate(outputDir, testId, getDateOfPreviousPeriodsStart(),
              gzExtension);
      recomputeRolloverThreshold(currentTime);
    }
  }
}
