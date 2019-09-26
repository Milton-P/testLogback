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
package ch.qos.logback.classic.net;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.nio.charset.Charset;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.net.mock.MockSyslogServer;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.net.SyslogConstants;
import ch.qos.logback.core.recovery.RecoveryCoordinator;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.StatusPrinter;

@RunWith(RobolectricTestRunner.class)
public class SyslogAppenderTest {

  private static final String SYSLOG_PREFIX_REGEX = "<\\d{2}>\\w{3} [\\d ]\\d \\d{2}(:\\d{2}){2} [\\w.-]* ";

  LoggerContext lc = new LoggerContext();
  SyslogAppenderFriend sa;// = new SyslogAppenderFriend();
  MockSyslogServer mockServer;
  String loggerName = this.getClass().getName();
  Logger logger = lc.getLogger(loggerName);

  @Before
  public void setUp() throws Exception {
    lc.setName("test");
    sa = new SyslogAppenderFriend();
    sa.setContext(lc);
  }

  @After
  public void tearDown() throws Exception {
  }

  public void setMockServerAndConfigure(int expectedCount) throws InterruptedException {
    setMockServerAndConfigure(expectedCount, true);
  }

  public void setMockServerAndConfigure(int expectedCount, boolean start)
      throws InterruptedException {
    int port = RandomUtil.getRandomServerPort();

    mockServer = new MockSyslogServer(expectedCount, port);
    mockServer.start();
    // give MockSyslogServer head start

    Thread.sleep(100);

    sa.setSyslogHost("localhost");
    sa.setFacility("MAIL");
    sa.setPort(port);
    sa.setSuffixPattern("[%thread] %logger %msg");
    sa.setStackTracePattern("[%thread] foo "+CoreConstants.TAB);
    if (start) {
      sa.start();
      assertTrue(sa.isStarted());
    }
    String loggerName = this.getClass().getName();
    Logger logger = lc.getLogger(loggerName);
    logger.addAppender(sa);

  }

  @Test
  public void basic() throws InterruptedException {

    setMockServerAndConfigure(1);
    String logMsg = "hello";
    logger.debug(logMsg);

    // wait max 8 seconds for mock server to finish. However, it should
    // much sooner than that.
    mockServer.join(8000);

    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
    String msg = new String(mockServer.getMessageList().get(0));

    String threadName = Thread.currentThread().getName();

    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    checkRegexMatch(msg, SYSLOG_PREFIX_REGEX + "\\[" + threadName + "\\] " + loggerName + " "
        + logMsg);

  }

  @Test
  public void tException() throws InterruptedException {
    setMockServerAndConfigure(21);

    String logMsg = "hello";
    String exMsg = "just testing";
    Exception ex = new Exception(exMsg);
    logger.debug(logMsg, ex);
    StatusPrinter.print(lc);

    // wait max 2 seconds for mock server to finish. However, it should
    // much sooner than that.
    mockServer.join(8000);
    assertTrue(mockServer.isFinished());

    // message + 20 lines of stacktrace
    assertEquals(21, mockServer.getMessageList().size());
    // int i = 0;
    // for (String line: mockServer.msgList) {
    // System.out.println(i++ + ": " + line);
    // }

    String msg = new String(mockServer.getMessageList().get(0));
    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    String threadName = Thread.currentThread().getName();
    String regex = SYSLOG_PREFIX_REGEX + "\\[" + threadName + "\\] " + loggerName
        + " " + logMsg;
    checkRegexMatch(msg, regex);

    msg = new String(mockServer.getMessageList().get(1));
    assertTrue(msg.contains(ex.getClass().getName()));
    assertTrue(msg.contains(ex.getMessage()));

    msg = new String(mockServer.getMessageList().get(2));
    assertTrue(msg.startsWith(expected));
    regex = SYSLOG_PREFIX_REGEX + "\\[" + threadName + "\\] " +  "foo "+CoreConstants.TAB + "at ch\\.qos.*";
    checkRegexMatch(msg, regex);
  }

  private void checkRegexMatch(String s, String regex) {
    assertTrue("The string [" + s + "] did not match regex [" + regex + "]", s
        .matches(regex));
  }

  @Test
  public void large() throws Exception {
    setMockServerAndConfigure(2);
    StringBuilder largeBuf = new StringBuilder();
    for (int i = 0; i < 2 * 1024 * 1024; i++) {
      largeBuf.append('a');
    }
    logger.debug(largeBuf.toString());

    String logMsg = "hello";
    logger.debug(logMsg);
    Thread.sleep(RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN+10);
    logger.debug(logMsg);

    mockServer.join(8000);
    assertTrue(mockServer.isFinished());

    // both messages received
    assertEquals(2, mockServer.getMessageList().size());

    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    String threadName = Thread.currentThread().getName();

    // large message is truncated
    final int maxMessageSize = sa.getMaxMessageSize();
    String largeMsg = new String(mockServer.getMessageList().get(0));
    assertTrue(largeMsg.startsWith(expected));
    String largeRegex = SYSLOG_PREFIX_REGEX + "\\[" + threadName + "\\] " + loggerName
        + " " + "a{" + (maxMessageSize - 2000) + "," + maxMessageSize + "}";
    checkRegexMatch(largeMsg, largeRegex);

    String msg = new String(mockServer.getMessageList().get(1));
    assertTrue(msg.startsWith(expected));
    String regex = SYSLOG_PREFIX_REGEX + "\\[" + threadName + "\\] " + loggerName
        + " " + logMsg;
    checkRegexMatch(msg, regex);
  }

  @Test
  public void LBCLASSIC_50() throws JoranException {

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(lc);
    lc.reset();
    configurator.doConfigure(ClassicTestConstants.JORAN_INPUT_PREFIX
        + "syslog_LBCLASSIC_50.xml");

    org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info("hello");
  }

  @Test
  public void unlazyAppenderIsConnectedAtStart() throws InterruptedException {
    setMockServerAndConfigure(1, false);
    sa.setLazy(false);
    assertFalse(sa.isInitialized());
    sa.start();
    assertTrue(sa.isInitialized());
  }

  @Test
  public void lazyAppenderIsNotConnectedAtStart() throws InterruptedException {
    setMockServerAndConfigure(1, false);
    sa.setLazy(true);
    assertFalse(sa.isInitialized());
    sa.start();
    assertFalse(sa.isInitialized());
  }

  @Test
  public void lazyAppenderIsConnectedOnAppend() throws InterruptedException {
    setMockServerAndConfigure(1, false);
    sa.setLazy(true);
    assertFalse(sa.isInitialized());
    sa.start();
    assertFalse(sa.isInitialized());

    logger.debug("hello world");
    mockServer.join(8000);

    assertTrue(sa.isInitialized());
  }

  @Test
  public void lazyAppenderMessageIsDelivered() throws InterruptedException {
    setMockServerAndConfigure(1, false);
    sa.setLazy(true);
    sa.start();

    Marker marker = MarkerFactory.getMarker("testMarker");
    assertFalse(sa.isInitialized());
    assertTrue(mockServer.getMessageList().isEmpty());
    logger.debug(marker, "test msg");
    assertTrue(sa.isInitialized());

    mockServer.join(8000);
    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
  }

  @Test
  public void unknownHostShouldNotCauseStopToFail() {
    // See LOGBACK-960
    sa.setSyslogHost("unknown.host");
    sa.setFacility("MAIL");
    sa.start();
    sa.stop();
  }

  @Test
  public void nonAsciiMessageEncoding() throws Exception {
    // See LOGBACK-732
    setMockServerAndConfigure(1);

    // Use a string that can be encoded in a somewhat odd encoding (ISO-8859-4) to minimize
    // the probability of the encoding test to work by accident
    String logMsg = "R\u0129ga";  // Riga spelled with the i having a tilda on top

    Charset ISO_8859_4 = Charset.forName("ISO-8859-4");
    sa.setCharset(ISO_8859_4);
    logger.debug(logMsg);

    // wait max 8 seconds for mock server to finish. However, it should
    // be done much sooner than that.
    mockServer.join(8000);

    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
    String msg = new String(mockServer.getMessageList().get(0), ISO_8859_4);
    String threadName = Thread.currentThread().getName();

    String expected = "<"
            + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    checkRegexMatch(msg, SYSLOG_PREFIX_REGEX + "\\[" + threadName + "\\] " + loggerName + " "
            + logMsg);

  }

  class SyslogAppenderFriend extends SyslogAppender {
    public boolean isInitialized() {
      return this.sos != null;
    }
  }
}
