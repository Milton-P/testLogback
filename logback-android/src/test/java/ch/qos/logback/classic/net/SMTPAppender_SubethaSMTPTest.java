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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.dom4j.io.SAXReader;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.subethamail.smtp.auth.EasyAuthenticationHandlerFactory;
import org.subethamail.smtp.auth.LoginFailedException;
import org.subethamail.smtp.auth.UsernamePasswordValidator;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.html.XHTMLEntityResolver;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.util.StatusPrinter;

@Ignore
@RunWith(RobolectricTestRunner.class)
public class SMTPAppender_SubethaSMTPTest {
  static final String TEST_SUBJECT = "test subject";
  static final String HEADER = "HEADER\n";
  static final String FOOTER = "FOOTER\n";

  static int DIFF = 1024 + new Random().nextInt(30000);
  static Wiser WISER;

  SMTPAppender smtpAppender;
  LoggerContext loggerContext = new LoggerContext();

  int numberOfOldMessages;


  @BeforeClass
  static public void beforeClass() {
    WISER = new Wiser();
    WISER.setPort(DIFF);
    WISER.start();
  }

  @AfterClass
  static public void afterClass() throws Exception {
    WISER.stop();
  }

  @Before
  public void setUp() throws Exception {
    numberOfOldMessages = WISER.getMessages().size();
    buildSMTPAppender();
  }

  @After
  public void tearDown() {
    // clear any authentication handler factory
    WISER.getServer().setAuthenticationHandlerFactory(null);
  }

  void buildSMTPAppender() throws Exception {
    smtpAppender = new SMTPAppender();
    smtpAppender.setContext(loggerContext);
    smtpAppender.setName("smtp");
    smtpAppender.setFrom("user@host.dom");
    smtpAppender.setSMTPHost("localhost");
    smtpAppender.setSMTPPort(DIFF);
    smtpAppender.setSubject(TEST_SUBJECT);
    smtpAppender.addTo("noreply@qos.ch");
  }

  private Layout<ILoggingEvent> buildPatternLayout(LoggerContext lc) {
    PatternLayout layout = new PatternLayout();
    layout.setContext(lc);
    layout.setFileHeader(HEADER);
    layout.setPattern("%-4relative [%thread] %-5level %logger %class - %msg%n");
    layout.setFileFooter(FOOTER);
    layout.start();
    return layout;
  }

  private Layout<ILoggingEvent> buildHTMLLayout(LoggerContext lc) {
    HTMLLayout layout = new HTMLLayout();
    layout.setContext(lc);
    // layout.setFileHeader(HEADER);
    layout.setPattern("%level%class%msg");
    // layout.setFileFooter(FOOTER);
    layout.start();
    return layout;
  }


  private static String getWholeMessage(Part msg) {
    try {
      ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
      msg.writeTo(bodyOut);
      return bodyOut.toString("US-ASCII").trim();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void waitUntilEmailIsSent() throws Exception {
    System.out.println("About to wait for sending thread to finish");
    loggerContext.getExecutorService().shutdown();
    loggerContext.getExecutorService().awaitTermination(3000, TimeUnit.MILLISECONDS);
  }

  private static String getBody(Part msg) {
    String all = getWholeMessage(msg);
    int i = all.indexOf("\r\n\r\n");
    return all.substring(i + 4, all.length());
  }

  @Test
  public void smoke() throws Exception {
    smtpAppender.setLayout(buildPatternLayout(loggerContext));
    smtpAppender.start();
    Logger logger = loggerContext.getLogger("test");
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("en error", new Exception("an exception"));

    waitUntilEmailIsSent();
    System.out.println("*** " + ((ThreadPoolExecutor) loggerContext.getExecutorService()).getCompletedTaskCount());
    List<WiserMessage> wiserMsgList = WISER.getMessages();

    assertNotNull(wiserMsgList);
    assertEquals(numberOfOldMessages+1, wiserMsgList.size());
    WiserMessage wm = wiserMsgList.get(numberOfOldMessages);
    // http://jira.qos.ch/browse/LBCLASSIC-67
    MimeMessage mm = wm.getMimeMessage();
    assertEquals(TEST_SUBJECT, mm.getSubject());

    MimeMultipart mp = (MimeMultipart) mm.getContent();
    String body = getBody(mp.getBodyPart(0));
    System.out.println("[" + body);
    assertTrue(body.startsWith(HEADER.trim()));
    assertTrue(body.endsWith(FOOTER.trim()));
  }


  @Test
  public void html() throws Exception {

    smtpAppender.setLayout(buildHTMLLayout(loggerContext));
    smtpAppender.start();
    Logger logger = loggerContext.getLogger("test");
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("en error", new Exception("an exception"));
    waitUntilEmailIsSent();

    List<WiserMessage> wiserMsgList = WISER.getMessages();

    assertNotNull(wiserMsgList);
    assertEquals(numberOfOldMessages + 1, wiserMsgList.size());
    WiserMessage wm = wiserMsgList.get(numberOfOldMessages);
    MimeMessage mm = wm.getMimeMessage();
    assertEquals(TEST_SUBJECT, mm.getSubject());

    MimeMultipart mp = (MimeMultipart) mm.getContent();

    // verify strict adherence to xhtml1-strict.dtd
    SAXReader reader = new SAXReader();
    reader.setValidation(true);
    reader.setEntityResolver(new XHTMLEntityResolver());
    reader.read(mp.getBodyPart(0).getInputStream());
    // System.out.println(GreenMailUtil.getBody(mp.getBodyPart(0)));
  }

  @Test
  /**
   * Checks that even when many events are processed, the output is still
   * conforms to xhtml-strict.dtd.
   *
   * Note that SMTPAppender only keeps only 500 or so (=buffer size) events. So
   * the generated output will be rather short.
   */
  public void htmlLong() throws Exception {
    smtpAppender.setLayout(buildHTMLLayout(loggerContext));
    smtpAppender.start();
    Logger logger = loggerContext.getLogger("test");
    logger.addAppender(smtpAppender);
    for (int i = 0; i < CoreConstants.TABLE_ROW_LIMIT * 3; i++) {
      logger.debug("hello " + i);
    }
    logger.error("en error", new Exception("an exception"));
    waitUntilEmailIsSent();
    List<WiserMessage> wiserMsgList = WISER.getMessages();

    assertNotNull(wiserMsgList);
    assertEquals(numberOfOldMessages + 1, wiserMsgList.size());
    WiserMessage wm = wiserMsgList.get(numberOfOldMessages);
    MimeMessage mm = wm.getMimeMessage();
    assertEquals(TEST_SUBJECT, mm.getSubject());

    MimeMultipart mp = (MimeMultipart) mm.getContent();

    // verify strict adherence to xhtml1-strict.dtd
    SAXReader reader = new SAXReader();
    reader.setValidation(true);
    reader.setEntityResolver(new XHTMLEntityResolver());
    reader.read(mp.getBodyPart(0).getInputStream());
  }

  @Test
  public void authenticated() throws Exception {
    setupServerAuth();

    smtpAppender.setUsername("x");
    smtpAppender.setPassword("x");

    smtpAppender.setLayout(buildPatternLayout(loggerContext));
    smtpAppender.start();
    Logger logger = loggerContext.getLogger("test");
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("en error", new Exception("an exception"));
    waitUntilEmailIsSent();
    List<WiserMessage> wiserMsgList = WISER.getMessages();

    assertNotNull(wiserMsgList);
    assertEquals(numberOfOldMessages + 1, wiserMsgList.size());
    WiserMessage wm = wiserMsgList.get(numberOfOldMessages);
    // http://jira.qos.ch/browse/LBCLASSIC-67
    MimeMessage mm = wm.getMimeMessage();
    assertEquals(TEST_SUBJECT, mm.getSubject());

    MimeMultipart mp = (MimeMultipart) mm.getContent();
    String body = getBody(mp.getBodyPart(0));
    assertTrue(body.startsWith(HEADER.trim()));
    assertTrue(body.endsWith(FOOTER.trim()));
  }

  @Test
  @Ignore
  // Unfortunately, there seems to be a problem with SubethaSMTP's implementation
  // of startTLS. The same SMTPAppender code works fine when tested with gmail.
  public void authenticatedSSL() throws Exception {
    setupServerAuth();

    smtpAppender.setSTARTTLS(true);
    smtpAppender.setUsername("xx");
    smtpAppender.setPassword("xx");

    smtpAppender.setLayout(buildPatternLayout(loggerContext));
    smtpAppender.start();
    Logger logger = loggerContext.getLogger("test");
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("en error", new Exception("an exception"));

    waitUntilEmailIsSent();
    List<WiserMessage> wiserMsgList = WISER.getMessages();

    assertNotNull(wiserMsgList);
    assertEquals(1, wiserMsgList.size());
  }

  static String GMAIL_USER_NAME = "xx@gmail.com";
  static String GMAIL_PASSWORD = "xxx";

  @Test
  public void authenticatedGmailStartTLS() throws Exception {
    smtpAppender.setSMTPHost("smtp.gmail.com");
    smtpAppender.setSMTPPort(587);
    smtpAppender.setAsynchronousSending(false);
    smtpAppender.addTo(GMAIL_USER_NAME);

    smtpAppender.setSTARTTLS(true);
    smtpAppender.setUsername(GMAIL_USER_NAME);
    smtpAppender.setPassword(GMAIL_PASSWORD);

    smtpAppender.setLayout(buildPatternLayout(loggerContext));
    smtpAppender.setSubject("authenticatedGmailStartTLS - %level %logger{20} - %m");
    smtpAppender.start();
    Logger logger = loggerContext.getLogger("authenticatedGmailSTARTTLS");
    logger.addAppender(smtpAppender);
    logger.debug("authenticatedGmailStartTLS =- hello");
    logger.error("an error", new Exception("an exception"));

    StatusPrinter.print(loggerContext);
  }

  @Test
  @Ignore
  public void authenticatedGmail_SSL() throws Exception {
    smtpAppender.setSMTPHost("smtp.gmail.com");
    smtpAppender.setSMTPPort(465);

    smtpAppender.setSubject("authenticatedGmail_SSL - %level %logger{20} - %m");
    smtpAppender.addTo(GMAIL_USER_NAME);
    smtpAppender.setSSL(true);
    smtpAppender.setUsername(GMAIL_USER_NAME);
    smtpAppender.setPassword(GMAIL_PASSWORD);
    smtpAppender.setAsynchronousSending(false);
    smtpAppender.setLayout(buildPatternLayout(loggerContext));
    smtpAppender.start();
    Logger logger = loggerContext.getLogger("authenticatedGmail_SSL");
    logger.addAppender(smtpAppender);
    logger.debug("hello"+new java.util.Date());
    logger.error("an error", new Exception("an exception"));

    StatusPrinter.print(loggerContext);
  }

  @Test
  public void testMultipleTo() throws Exception {
    smtpAppender.setLayout(buildPatternLayout(loggerContext));
    smtpAppender.addTo("Test <test@example.com>, other-test@example.com");
    smtpAppender.start();
    Logger logger = loggerContext.getLogger("test");
    logger.addAppender(smtpAppender);
    logger.debug("hello");
    logger.error("en error", new Exception("an exception"));
    waitUntilEmailIsSent();

    List<WiserMessage> wiserMsgList = WISER.getMessages();
    assertNotNull(wiserMsgList);
    assertEquals(numberOfOldMessages + 3, wiserMsgList.size());
  }

  private void setupServerAuth() {
    WISER.getServer().setAuthenticationHandlerFactory(new EasyAuthenticationHandlerFactory(new UsernamePasswordValidator() {
      public void login(String username, String password) throws LoginFailedException {
        if (!username.equals(password)) {
          throw new LoginFailedException("username=" + username + ", password=" + password);
        }
      }
    }));
  }
}
