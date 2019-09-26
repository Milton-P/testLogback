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
package ch.qos.logback.core.rolling;

import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.FileSize;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertFalse;

public class SizeAndTimeBasedFNATP_Test extends ScaffoldingForRollingTests {
  private SizeAndTimeBasedFNATP<Object> sizeAndTimeBasedFNATP = null;
  private RollingFileAppender<Object> rfa1 = new RollingFileAppender<Object>();
  private TimeBasedRollingPolicy<Object> tbrp1 = new TimeBasedRollingPolicy<Object>();
  private RollingFileAppender<Object> rfa2 = new RollingFileAppender<Object>();
  private TimeBasedRollingPolicy<Object> tbrp2 = new TimeBasedRollingPolicy<Object>();

  private EchoEncoder<Object> encoder = new EchoEncoder<Object>();
  private int fileSize = 0;
  private int fileIndexCounter = 0;
  private int sizeThreshold = 0;

  private void initRollingFileAppender(RollingFileAppender<Object> rfa, String filename) {
    rfa.setContext(context);
    rfa.setEncoder(encoder);
    if (filename != null) {
      rfa.setFile(filename);
    }
  }

  private void initPolicies(RollingFileAppender<Object> rfa,
                            TimeBasedRollingPolicy<Object> tbrp,
                            String filenamePattern, int sizeThreshold,
                            long givenTime) {
    sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP<Object>();
    tbrp.setContext(context);
    sizeAndTimeBasedFNATP.setMaxFileSize(new FileSize(sizeThreshold));
    tbrp.setTimeBasedFileNamingAndTriggeringPolicy(sizeAndTimeBasedFNATP);
    tbrp.setFileNamePattern(filenamePattern);
    tbrp.setParent(rfa);
    tbrp.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(givenTime);
    rfa.setRollingPolicy(tbrp);
    tbrp.start();
    rfa.start();
  }

  private void addExpectedFileNamedIfItsTime(String randomOutputDir, String testId, String msg, String compressionSuffix) {
    fileSize = fileSize + msg.getBytes().length;
    if (passThresholdTime(nextRolloverThreshold)) {
      fileIndexCounter = 0;
      fileSize = 0;
      addExpectedFileName_ByFileIndexCounter(randomOutputDir, testId, getMillisOfCurrentPeriodsStart(),
              fileIndexCounter, compressionSuffix);
      recomputeRolloverThreshold(currentTime);
      return;
    }

    // windows can delay file size changes, so we only allow for
    // fileIndexCounter 0
    if ((fileIndexCounter < 1) && fileSize > sizeThreshold) {
      addExpectedFileName_ByFileIndexCounter(randomOutputDir, testId, getMillisOfCurrentPeriodsStart(),
              fileIndexCounter, compressionSuffix);
      fileIndexCounter = fileIndexCounter + 1;
      fileSize = 0;
    }
  }

  private void generic(String testId, String stem, boolean withSecondPhase, String compressionSuffix) throws IOException {
    String file = (stem != null) ? randomOutputDir + stem : null;
    initRollingFileAppender(rfa1, file);
    sizeThreshold = 300;
    initPolicies(rfa1, tbrp1, randomOutputDir + testId + "-%d{" + DATE_PATTERN_WITH_SECONDS + ", GMT}-%i.txt" + compressionSuffix, sizeThreshold, currentTime);
    addExpectedFileName_ByFileIndexCounter(randomOutputDir, testId, getMillisOfCurrentPeriodsStart(), fileIndexCounter, compressionSuffix);
    incCurrentTime(400);
    tbrp1.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);
    int runLength = 100;
    String prefix = "Hello -----------------";

    for (int i = 0; i < runLength; i++) {
      String msg = prefix + i;
      rfa1.doAppend(msg);
      addExpectedFileNamedIfItsTime(randomOutputDir, testId, msg, compressionSuffix);
      incCurrentTime(20);
      tbrp1.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);
      add(tbrp1.compressionFuture);
      add(tbrp1.cleanUpFuture);
    }

    if (withSecondPhase) {
      secondPhase(testId, file, stem, compressionSuffix, runLength, prefix);
      runLength = runLength * 2;
    }

    if (stem != null)
      massageExpectedFilesToCorresponToCurrentTarget(file, true);

    Thread.yield();
    // wait for compression to finish
    waitForJobsToComplete();

    //StatusPrinter.print(context);
    existenceCheck(expectedFilenameList);
    sortedContentCheck(randomOutputDir, runLength, prefix);
  }

  private void secondPhase(String testId, String file, String stem, String compressionSuffix, int runLength, String prefix) {
    rfa1.stop();

    if (stem != null) {
      File f = new File(file);
      f.setLastModified(currentTime);
    }

    StatusManager sm = context.getStatusManager();
    sm.add(new InfoStatus("Time when rfa1 is stopped: " + new Date(currentTime), this));
    sm.add(new InfoStatus("currentTime%1000=" + (currentTime % 1000), this));

    initRollingFileAppender(rfa2, file);
    initPolicies(rfa2, tbrp2, randomOutputDir + testId + "-%d{"
            + DATE_PATTERN_WITH_SECONDS + ", GMT}-%i.txt" + compressionSuffix, sizeThreshold, currentTime);

    for (int i = runLength; i < runLength * 2; i++) {
      incCurrentTime(100);
      tbrp2.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);
      String msg = prefix + i;
      rfa2.doAppend(msg);
      addExpectedFileNamedIfItsTime(randomOutputDir, testId, msg, compressionSuffix);
    }
  }

  private static final boolean FIRST_PHASE_ONLY = false;
  private static final boolean WITH_SECOND_PHASE = true;
  private static final String DEFAULT_COMPRESSION_SUFFIX = "";

  @Test
  public void noCompression_FileSet_NoRestart_1() throws InterruptedException, ExecutionException, IOException {
    generic("test1", "toto.log", FIRST_PHASE_ONLY, DEFAULT_COMPRESSION_SUFFIX);
  }

  @Test
  public void noCompression_FileBlank_NoRestart_2() throws Exception {
    generic("test2", null, FIRST_PHASE_ONLY, DEFAULT_COMPRESSION_SUFFIX);
  }

  @Test
  public void noCompression_FileBlank_WithStopStart_3() throws Exception {
    generic("test3", null, WITH_SECOND_PHASE, DEFAULT_COMPRESSION_SUFFIX);
  }

  @Test
  public void noCompression_FileSet_WithStopStart_4() throws Exception {
    generic("test4", "test4.log", WITH_SECOND_PHASE, DEFAULT_COMPRESSION_SUFFIX);
  }

  @Test
  public void withGZCompression_FileSet_NoRestart_5() throws Exception {
    generic("test5", "toto.log", FIRST_PHASE_ONLY, ".gz");
  }

  @Test
  public void withGZCompression_FileBlank_NoRestart_6() throws Exception {
    generic("test6", null, FIRST_PHASE_ONLY, ".gz");
  }

  @Test
  public void withZipCompression_FileSet_NoRestart_7() throws Exception {
    generic("test7", "toto.log", FIRST_PHASE_ONLY, ".zip");
    List<String> zipFiles = filterElementsInListBySuffix(".zip");
    checkZipEntryMatchesZipFilename(zipFiles);
  }

  @Test
  public void checkMissingIntToken() {
    String stem = "toto.log";
    String testId = "checkMissingIntToken";
    String compressionSuffix = "gz";

    String file = (stem != null) ? randomOutputDir + stem : null;
    initRollingFileAppender(rfa1, file);
    sizeThreshold = 300;
    initPolicies(rfa1, tbrp1, randomOutputDir + testId + "-%d{" + DATE_PATTERN_WITH_SECONDS + ", GMT}.txt" + compressionSuffix, sizeThreshold, currentTime);

    //StatusPrinter.print(context);
    assertFalse(rfa1.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch("Missing integer token");
  }

  @Test
  public void checkDateCollision() {
    String stem = "toto.log";
    String testId = "checkDateCollision";
    String compressionSuffix = "gz";
    String file = (stem != null) ? randomOutputDir + stem : null;
    initRollingFileAppender(rfa1, file);
    sizeThreshold = 300;
    initPolicies(rfa1, tbrp1, randomOutputDir + testId + "-%d{EE, GMT}.txt" + compressionSuffix, sizeThreshold, currentTime);
    //StatusPrinter.print(context);
    assertFalse(rfa1.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch("The date format in FileNamePattern");
  }

//  @Test
//  public void testHistoryAsFileCount() throws IOException {
//    String testId = "testHistoryAsFileCount";
//    int maxHistory = 10;
//    initRollingFileAppender(rfa1, randomOutputDir + "~" + testId );
//    sizeThreshold = 50;
//    initPolicies(rfa1, tbrp1, randomOutputDir + testId + "-%d{" + DATE_PATTERN_WITH_SECONDS + ", GMT}-%i.txt", sizeThreshold, currentTime, 0, maxHistory, true);
//
//    incCurrentTime(100);
//    tbrp1.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);
//    int runLength = 1000;
//
//    for (int i = 0; i < runLength; i++) {
//      String msg = "" + i;
//      rfa1.doAppend(msg);
//      incCurrentTime(20);
//      tbrp1.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);
//      add(tbrp1.compressionFuture);
//    }
//
//    Thread.yield();
//    // wait for compression to finish
//    waitForJobsToComplete();
//
//    assertEquals(maxHistory + 1, getFilesInDirectory(randomOutputDir).length);
//    sortedContentCheck(randomOutputDir, 1000, "", 863);
//  }
}
