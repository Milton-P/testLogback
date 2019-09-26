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
package ch.qos.logback.classic;

import static ch.qos.logback.classic.ClassicTestConstants.ISO_REGEX;
import static ch.qos.logback.classic.ClassicTestConstants.MAIN_REGEX;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.MDC;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.pattern.ConverterTest;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.testUtil.SampleConverter;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.pattern.parser.AbstractPatternLayoutBaseTest;
import ch.qos.logback.core.testUtil.StringListAppender;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusPrinter;

@RunWith(RobolectricTestRunner.class)
public class PatternLayoutTest extends AbstractPatternLayoutBaseTest<ILoggingEvent> {

  private PatternLayout pl = new PatternLayout();
  private LoggerContext lc = new LoggerContext();
  Logger logger = lc.getLogger(ConverterTest.class);
  Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);

  String aMessage = "Some message";

  ILoggingEvent le;

  public PatternLayoutTest() {
    super();
    Exception ex = new Exception("Bogus exception");
    le = makeLoggingEvent(aMessage, ex);
  }

  @Before
  public void setUp() {
    pl.setContext(lc);
  }

  ILoggingEvent makeLoggingEvent(String msg, Exception ex) {
    return new LoggingEvent(ch.qos.logback.core.pattern.FormattingConverter.class.getName(), logger, Level.INFO, msg, ex, null);
  }

  public ILoggingEvent getEventObject() {
    return makeLoggingEvent("Some message", null);
  }

  public PatternLayoutBase<ILoggingEvent> getPatternLayoutBase() {
    return new PatternLayout();
  }

  @Test
  public void testOK() {
    pl.setPattern("%d %le [%t] %lo{30} - %m%n");
    pl.start();
    String val = pl.doLayout(getEventObject());
    // 2006-02-01 22:38:06,212 INFO [main] c.q.l.pattern.ConverterTest - Some
    // message
    //2010-12-29 19:04:26,137 INFO [pool-1-thread-47] c.q.l.c.pattern.ConverterTest - Some message
    String regex = ISO_REGEX + " INFO " + MAIN_REGEX
            + " c.q.l.c.pattern.ConverterTest - Some message\\s*";

    assertThat(val, matchesPattern(regex));
  }

  @Test
  public void testNoExeptionHandler() {
    pl.setPattern("%m%n");
    pl.start();
    String val = pl.doLayout(le);
    assertThat(val, containsString("java.lang.Exception: Bogus exception"));
  }

  @Test
  public void testCompositePattern() {
    pl.setPattern("%-56(%d %lo{20}) - %m%n");
    pl.start();
    String val = pl.doLayout(getEventObject());
    // 2008-03-18 21:55:54,250 c.q.l.c.pattern.ConverterTest - Some message
    String regex = ISO_REGEX
            + " c.q.l.c.p.ConverterTest          - Some message\\s*";
    assertThat(val, matchesPattern(regex));
  }

  @Test
  public void contextProperty() {
    pl.setPattern("%property{a}");
    pl.start();
    lc.putProperty("a", "b");

    String val = pl.doLayout(getEventObject());
    assertEquals("b", val);
  }

  @Test
  public void testNopExeptionHandler() {
    pl.setPattern("%nopex %m%n");
    pl.start();
    String val = pl.doLayout(le);
    assertThat(val, not(containsString("java.lang.Exception: Bogus exception")));
  }

  @Test
  public void testWithParenthesis() {
    pl.setPattern("\\(%msg:%msg\\) %msg");
    pl.start();
    le = makeLoggingEvent(aMessage, null);
    String val = pl.doLayout(le);
    assertEquals("(Some message:Some message) Some message", val);
  }

  @Test
  public void testWithLettersComingFromLog4j() {
    // Letters: p = level and c = logger
    pl.setPattern("%d %p [%t] %c{30} - %m%n");
    pl.start();
    String val = pl.doLayout(getEventObject());
    // 2006-02-01 22:38:06,212 INFO [main] c.q.l.pattern.ConverterTest - Some
    // message
    String regex = ClassicTestConstants.ISO_REGEX + " INFO " + MAIN_REGEX
            + " c.q.l.c.pattern.ConverterTest - Some message\\s*";
    assertThat(val, matchesPattern(regex));
  }

  @Test
  public void mdcWithDefaultValue() {
    String pattern = "%msg %mdc{foo} %mdc{bar:-[null]}";
    pl.setPattern(OptionHelper.substVars(pattern, lc));
    pl.start();
    MDC.put("foo", "foo");
    try {
      String val = pl.doLayout(getEventObject());
      assertEquals("Some message foo [null]", val);
    } finally {
      MDC.remove("foo");
    }
  }


  @Test
  public void contextNameTest() {
    pl.setPattern("%contextName");
    lc.setName("aValue");
    pl.start();
    String val = pl.doLayout(getEventObject());
    assertEquals("aValue", val);
  }

  @Test
  public void cnTest() {
    pl.setPattern("%cn");
    lc.setName("aValue");
    pl.start();
    String val = pl.doLayout(getEventObject());
    assertEquals("aValue", val);
  }

  @Override
  public Context getContext() {
    return lc;
  }

  void configure(String file) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(lc);
    jc.doConfigure(file);
  }

  @Test
  public void testConversionRuleSupportInPatternLayout() throws JoranException {
    configure(ClassicTestConstants.JORAN_INPUT_PREFIX + "conversionRule/patternLayout0.xml");
    root.getAppender("LIST");
    String msg = "Simon says";
    logger.debug(msg);
    StringListAppender<ILoggingEvent> sla = (StringListAppender<ILoggingEvent>) root.getAppender("LIST");
    assertNotNull(sla);
    assertEquals(1, sla.strList.size());
    assertEquals(SampleConverter.SAMPLE_STR + " - " + msg, sla.strList.get(0));
  }

  @Test
  public void smokeReplace() {
    pl.setPattern("%replace(a1234b){'\\d{4}', 'XXXX'}");
    pl.start();
    StatusPrinter.print(lc);
    String val = pl.doLayout(getEventObject());
    assertEquals("aXXXXb", val);
  }

  @Test
  public void replaceNewline() {
    String pattern = "%replace(A\nB){'\n', '\n\t'}";
    String substPattern = OptionHelper.substVars(pattern, null, lc);
    assertEquals(pattern, substPattern);
    pl.setPattern(substPattern);
    pl.start();
    StatusPrinter.print(lc);
    String val = pl.doLayout(makeLoggingEvent("", null));
    assertEquals("A\n\tB", val);
  }

  @Test
  public void replaceWithJoran() throws JoranException {
    configure(ClassicTestConstants.JORAN_INPUT_PREFIX + "pattern/replace0.xml");
    StatusPrinter.print(lc);
    root.getAppender("LIST");
    String msg = "And the number is 4111111111110000, expiring on 12/2010";
    logger.debug(msg);
    StringListAppender<ILoggingEvent> sla = (StringListAppender<ILoggingEvent>) root.getAppender("LIST");
    assertNotNull(sla);
    assertEquals(1, sla.strList.size());
    assertEquals("And the number is XXXX, expiring on 12/2010", sla.strList.get(0));
  }

  @Test
  public void replaceWithJoran_NEWLINE() throws JoranException {
    lc.putProperty("TAB", "\t");
    configure(ClassicTestConstants.JORAN_INPUT_PREFIX + "pattern/replaceNewline.xml");
    StatusPrinter.print(lc);
    root.getAppender("LIST");
    String msg = "A\nC";
    logger.debug(msg);
    StringListAppender<ILoggingEvent> sla = (StringListAppender<ILoggingEvent>) root.getAppender("LIST");
    assertNotNull(sla);
    assertEquals(1, sla.strList.size());
    assertEquals("A\n\tC", sla.strList.get(0));
  }
}
