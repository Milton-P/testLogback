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

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.html.XHTMLEntityResolver;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.testUtil.EnvUtilForTests;
import ch.qos.logback.core.testUtil.RandomUtil;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;

import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.MDC;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Tests {@link SMTPAppender} with GreenMail
 *
 * http://www.icegreen.com/greenmail/
 */
@Ignore
@RunWith(RobolectricTestRunner.class)
public class SMTPAppender_GreenTest {

  static final String HEADER = "HEADER\n";
  static final String FOOTER = "FOOTER\n";
  static final String DEFAULT_PATTERN = "%-4relative %mdc [%thread] %-5level %class - %msg%n";

  static final boolean SYNCHRONOUS = false;
  static final boolean ASYNCHRONOUS = true;

  int port = RandomUtil.getRandomServerPort();
  // GreenMail cannot be static. As a shared server induces race conditions
  GreenMail greenMailServer;

  SMTPAppender smtpAppender;
  LoggerContext loggerContext = new LoggerContext();
  Logger logger = loggerContext.getLogger(this.getClass());

  @Before
  public void setUp() throws Exception {
    MDC.clear();
    ServerSetup serverSetup = new ServerSetup(port, "localhost",
            ServerSetup.PROTOCOL_SMTP);
    greenMailServer = new GreenMail(serverSetup);
    greenMailServer.start();
    // give the server a head start
    if (EnvUtilForTests.isRunningOnSlowJenkins()) {
      Thread.sleep(2000);
    } else {
      Thread.sleep(50);
    }
  }

  @After
  public void tearDown() throws Exception {
    greenMailServer.stop();
  }

  private void buildSMTPAppender(String subject, boolean synchronicity) throws Exception {
    smtpAppender = new SMTPAppender();
    smtpAppender.setContext(loggerContext);
    smtpAppender.setName("smtp");
    smtpAppender.setFrom("user@host.dom");
    smtpAppender.setSMTPHost("localhost");
    smtpAppender.setSMTPPort(port);
    smtpAppender.setSubject(subject);
    smtpAppender.addTo("nospam@qos.ch");
    smtpAppender.setAsynchronousSending(synchronicity);
  }

  private Layout<ILoggingEvent> buildPatternLayout(String pattern) {
    PatternLayout layout = new PatternLayout();
    layout.setContext(loggerContext);
    layout.setFileHeader(HEADER);
    layout.setOutputPatternAsHeader(false);
    layout.setPattern(pattern);
    layout.setFileFooter(FOOTER);
    layout.start();
    return layout;
  }

  private Layout<ILoggingEvent> buildHTMLLayout() {
    HTMLLayout layout = new HTMLLayout();
    layout.setContext(loggerContext);
    layout.setPattern("%level%class%msg");
    layout.start();
    return layout;
  }

  private void waitForServerToReceiveEmails(int emailCount) throws InterruptedException {
    assertTrue("no emails received",
            greenMailServer.waitForIncomingEmail(5000, emailCount));
  }

  private MimeMultipart verifyAndExtractMimeMultipart(String subject) throws MessagingException,
          IOException, InterruptedException {
    int oldCount = 0;
    int expectedEmailCount = 1;
    // wait for the server to receive the messages
    waitForServerToReceiveEmails(expectedEmailCount);
    MimeMessage[] mma = greenMailServer.getReceivedMessages();
    assertNotNull(mma);
    assertEquals(expectedEmailCount, mma.length);
    MimeMessage mm = mma[oldCount];
    // http://jira.qos.ch/browse/LBCLASSIC-67
    assertEquals(subject, mm.getSubject());
    return (MimeMultipart) mm.getContent();
  }

  private void waitUntilEmailIsSent() throws InterruptedException {
    loggerContext.getExecutorService().shutdown();
    assertTrue("no emails sent",
            loggerContext.getExecutorService().awaitTermination(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void synchronousSmoke() throws Exception {
    String subject = "synchronousSmoke";
    buildSMTPAppender(subject, SYNCHRONOUS);

    smtpAppender.setLayout(buildPatternLayout(DEFAULT_PATTERN));
    smtpAppender.start();
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("an error", new Exception("an exception"));

    MimeMultipart mp = verifyAndExtractMimeMultipart(subject);
    String body = GreenMailUtil.getBody(mp.getBodyPart(0));
    assertTrue(body.startsWith(HEADER.trim()));
    assertTrue(body.endsWith(FOOTER.trim()));
  }

  @Test
  public void asynchronousSmoke() throws Exception {
    String subject = "asynchronousSmoke";
    buildSMTPAppender(subject, ASYNCHRONOUS);
    smtpAppender.setLayout(buildPatternLayout(DEFAULT_PATTERN));
    smtpAppender.start();
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("an error", new Exception("an exception"));

    waitUntilEmailIsSent();
    MimeMultipart mp = verifyAndExtractMimeMultipart(subject);
    String body = GreenMailUtil.getBody(mp.getBodyPart(0));
    assertTrue(body.startsWith(HEADER.trim()));
    assertTrue(body.endsWith(FOOTER.trim()));
  }

  // See also http://jira.qos.ch/browse/LOGBACK-734
  @Test
  public void callerDataShouldBeCorrectlySetWithAsynchronousSending() throws Exception {
    String subject = "LOGBACK-734";
    buildSMTPAppender("LOGBACK-734", ASYNCHRONOUS);
    smtpAppender.setLayout(buildPatternLayout(DEFAULT_PATTERN));
    smtpAppender.setIncludeCallerData(true);
    smtpAppender.start();
    logger.addAppender(smtpAppender);
    logger.debug("LOGBACK-734");
    logger.error("callerData", new Exception("ShouldBeCorrectlySetWithAsynchronousSending"));

    waitUntilEmailIsSent();
    MimeMultipart mp = verifyAndExtractMimeMultipart(subject);
    String body = GreenMailUtil.getBody(mp.getBodyPart(0));
    assertTrue("actual [" + body + "]", body.contains("DEBUG " + this.getClass().getName() + " - LOGBACK-734"));
  }

  // lost MDC
  @Test
  public void LBCLASSIC_104() throws Exception {
    String subject = "LBCLASSIC_104";
    buildSMTPAppender(subject, SYNCHRONOUS);
    smtpAppender.setAsynchronousSending(false);
    smtpAppender.setLayout(buildPatternLayout(DEFAULT_PATTERN));
    smtpAppender.start();
    logger.addAppender(smtpAppender);
    MDC.put("key", "val");
    logger.debug("hello");
    MDC.clear();
    logger.error("en error", new Exception("test"));

    MimeMultipart mp = verifyAndExtractMimeMultipart(subject);
    String body = GreenMailUtil.getBody(mp.getBodyPart(0));
    assertTrue("missing HEADER in body", body.startsWith(HEADER.trim()));
    assertTrue("missing MDC in body", body.contains("key=val"));
    assertTrue("missing FOOTER in body", body.endsWith(FOOTER.trim()));
  }

  @Test
  public void html() throws Exception {
    String subject = "html";
    buildSMTPAppender(subject, SYNCHRONOUS);
    smtpAppender.setAsynchronousSending(false);
    smtpAppender.setLayout(buildHTMLLayout());
    smtpAppender.start();
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("an error", new Exception("an exception"));

    MimeMultipart mp = verifyAndExtractMimeMultipart(subject);

    // verifyAndExtractMimeMultipart strict adherence to xhtml1-strict.dtd
    SAXReader reader = new SAXReader();
    reader.setValidation(true);
    reader.setEntityResolver(new XHTMLEntityResolver());
    byte[] messageBytes = getAsByteArray(mp.getBodyPart(0).getInputStream());
    ByteArrayInputStream bais = new ByteArrayInputStream(messageBytes);
    try {
      reader.read(bais);
    } catch (DocumentException de) {
      System.out.println("incoming message:");
      System.out.println(new String(messageBytes));
      throw de;
    }
  }

  private byte[] getAsByteArray(InputStream inputStream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    byte[] buffer = new byte[1024];
    int n = -1;
    while ((n = inputStream.read(buffer)) != -1) {
      baos.write(buffer, 0, n);
    }
    return baos.toByteArray();
  }

  private void configure(int port, String file) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(loggerContext);
    loggerContext.putProperty("port", "" + port);
    jc.doConfigure(file);
  }

  @Test
  public void testCustomEvaluator() throws Exception {
    configure(greenMailServer.getSmtp().getPort(),
        ClassicTestConstants.JORAN_INPUT_PREFIX + "smtp/customEvaluator.xml");

    logger.debug("test");
    String msg2 = "CustomEvaluator";
    logger.debug(msg2);
    logger.debug("invisible");
    waitUntilEmailIsSent();
    MimeMultipart mp = verifyAndExtractMimeMultipart("testCustomEvaluator " + this.getClass().getName() + " - " + msg2);
    String body = GreenMailUtil.getBody(mp.getBodyPart(0));
    assertEquals("testCustomEvaluator", body);
  }

  @Test
  public void testCustomBufferSize() throws Exception {
    configure(greenMailServer.getSmtp().getPort(),
        ClassicTestConstants.JORAN_INPUT_PREFIX
            + "smtp/customBufferSize.xml");

    logger.debug("invisible1");
    logger.debug("invisible2");
    String msg = "hello";
    logger.error(msg);
    waitUntilEmailIsSent();
    MimeMultipart mp = verifyAndExtractMimeMultipart("testCustomBufferSize " + this.getClass().getName() + " - " + msg);
    String body = GreenMailUtil.getBody(mp.getBodyPart(0));
    assertEquals(msg, body);
  }

  // this test fails intermittently on Jenkins.
  @Test
  public void testMultipleTo() throws Exception {
    buildSMTPAppender("testMultipleTo", SYNCHRONOUS);
    smtpAppender.setLayout(buildPatternLayout(DEFAULT_PATTERN));
    // buildSMTPAppender() already added one destination address
    smtpAppender.addTo("Test <test@example.com>, other-test@example.com");
    smtpAppender.start();
    logger.addAppender(smtpAppender);
    logger.debug("testMultipleTo hello");
    logger.error("testMultipleTo en error", new Exception("an exception"));
    Thread.yield();
    int expectedEmailCount = 3;
    waitForServerToReceiveEmails(expectedEmailCount);
    MimeMessage[] mma = greenMailServer.getReceivedMessages();
    assertNotNull(mma);
    assertEquals(expectedEmailCount, mma.length);
  }

  // http://jira.qos.ch/browse/LBCLASSIC-221
  @Test
  public void bufferShouldBeResetBetweenMessages() throws Exception {
    buildSMTPAppender("bufferShouldBeResetBetweenMessages", SYNCHRONOUS);
    smtpAppender.setLayout(buildPatternLayout(DEFAULT_PATTERN));
    smtpAppender.start();
    logger.addAppender(smtpAppender);
    String msg0 = "hello zero";
    logger.debug(msg0);
    logger.error("error zero");

    String msg1 = "hello one";
    logger.debug(msg1);
    logger.error("error one");

    Thread.yield();
    int oldCount = 0;
    int expectedEmailCount = oldCount + 2;
    waitForServerToReceiveEmails(expectedEmailCount);

    MimeMessage[] mma = greenMailServer.getReceivedMessages();
    assertNotNull(mma);
    assertEquals(expectedEmailCount, mma.length);

    MimeMessage mm0 = mma[0];
    MimeMultipart content0 = (MimeMultipart) mm0.getContent();
    String body0 = GreenMailUtil.getBody(content0.getBodyPart(0));
    assertTrue(body0.contains(msg0));

    MimeMessage mm1 = mma[oldCount + 1];
    MimeMultipart content1 = (MimeMultipart) mm1.getContent();
    String body1 = GreenMailUtil.getBody(content1.getBodyPart(0));
    // second body should not contain content from first message
    assertFalse(body1.contains(msg0));
  }

  @Test
  public void multiLineSubjectTruncatedAtFirstNewLine() throws Exception {
    String line1 = "line 1 of subject";
    String subject = line1 + "\nline 2 of subject\n";
    buildSMTPAppender(subject, ASYNCHRONOUS);

    smtpAppender.setLayout(buildPatternLayout(DEFAULT_PATTERN));
    smtpAppender.start();
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("en error", new Exception("an exception"));

    Thread.yield();
    waitUntilEmailIsSent();
    waitForServerToReceiveEmails(1);

    MimeMessage[] mma = greenMailServer.getReceivedMessages();
    assertEquals(1, mma.length);
    assertEquals(line1, mma[0].getSubject());
  }
}
