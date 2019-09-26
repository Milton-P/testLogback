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
package ch.qos.logback.core.joran;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import ch.qos.logback.core.CoreConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ext.IncAction;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.TrivialStatusListener;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class TrivialConfiguratorTest {

  Context context = new ContextBase();
  HashMap<ElementSelector, Action> rulesMap = new HashMap<ElementSelector, Action>();

  public void doTest(String filename) throws Exception {

    // rule store is case insensitve
    rulesMap.put(new ElementSelector("x/inc"), new IncAction());

    TrivialConfigurator trivialConfigurator = new TrivialConfigurator(rulesMap);

    trivialConfigurator.setContext(context);
    trivialConfigurator.doConfigure(filename);
  }

  @Test
  public void smoke() throws Exception {
    int oldBeginCount = IncAction.beginCount;
    int oldEndCount = IncAction.endCount;
    int oldErrorCount = IncAction.errorCount;
    doTest(CoreTestConstants.TEST_DIR_PREFIX + "input/joran/" + "inc.xml");
    assertEquals(oldErrorCount, IncAction.errorCount);
    assertEquals(oldBeginCount + 1, IncAction.beginCount);
    assertEquals(oldEndCount + 1, IncAction.endCount);
  }

  @Test
  public void nonexistentFile() {
    TrivialStatusListener tsl = new TrivialStatusListener();
    tsl.start();
    String filename = CoreTestConstants.TEST_DIR_PREFIX + "input/joran/"
            + "nothereBLAH.xml";
    context.getStatusManager().add(tsl);
    try {
      doTest(filename);
    } catch (Exception e) {
      assertTrue(e.getMessage().startsWith("Could not open ["));
    }
    assertTrue(tsl.list.size() + " should be greater than or equal to 1",
            tsl.list.size() >= 1);
    Status s0 = tsl.list.get(0);
    assertTrue(s0.getMessage().startsWith("Could not open ["));
  }

  @Test
  public void illFormedXML() {
    TrivialStatusListener tsl = new TrivialStatusListener();
    tsl.start();
    String filename = CoreTestConstants.TEST_DIR_PREFIX + "input/joran/" + "illformed.xml";
    context.getStatusManager().add(tsl);
    try {
      doTest(filename);
    } catch (Exception e) {
    }
    assertEquals(2, tsl.list.size());
    Status s0 = tsl.list.get(0);
    assertTrue(s0.getMessage().startsWith(CoreConstants.XML_PARSING));
  }

  @Test
  public void lbcore105() throws IOException, JoranException {
    String jarEntry = "buzz.xml";
    File jarFile = makeRandomJarFile();
    fillInJarFile(jarFile, jarEntry);
    URL url = asURL(jarFile, jarEntry);
    TrivialConfigurator tc = new TrivialConfigurator(rulesMap);
    tc.setContext(context);
    tc.doConfigure(url);
    // deleting an open file fails
    assertTrue(jarFile.delete());
    assertFalse(jarFile.exists());
  }

  @Test
  public void lbcore127() throws IOException, JoranException {
    String jarEntry = "buzz.xml";
    String jarEntry2 = "lightyear.xml";

    File jarFile = makeRandomJarFile();
    fillInJarFile(jarFile, jarEntry, jarEntry2);

    URL url1 = asURL(jarFile, jarEntry);
    URL url2 = asURL(jarFile, jarEntry2);

    URLConnection urlConnection2 = url2.openConnection();
    urlConnection2.setUseCaches(false);
    InputStream is = urlConnection2.getInputStream();

    TrivialConfigurator tc = new TrivialConfigurator(rulesMap);
    tc.setContext(context);
    tc.doConfigure(url1);

    is.read();
    is.close();

    // deleting an open file fails
    assertTrue(jarFile.delete());
    assertFalse(jarFile.exists());
  }

  File makeRandomJarFile() {
    File outputDir = new File(CoreTestConstants.OUTPUT_DIR_PREFIX);
    outputDir.mkdirs();
    int randomPart = RandomUtil.getPositiveInt();
    return new File(CoreTestConstants.OUTPUT_DIR_PREFIX + "foo-" + randomPart
            + ".jar");
  }

  private void fillInJarFile(File jarFile, String jarEntryName)
          throws IOException {
    fillInJarFile(jarFile, jarEntryName, null);
  }

  private void fillInJarFile(File jarFile, String jarEntryName1,
                             String jarEntryName2) throws IOException {
    JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));
    jos.putNextEntry(new ZipEntry(jarEntryName1));
    jos.write("<x/>".getBytes());
    jos.closeEntry();
    if (jarEntryName2 != null) {
      jos.putNextEntry(new ZipEntry(jarEntryName2));
      jos.write("<y/>".getBytes());
      jos.closeEntry();
    }
    jos.close();
  }

  URL asURL(File jarFile, String jarEntryName) throws IOException {
    URL innerURL = jarFile.toURI().toURL();
    return new URL("jar:" + innerURL + "!/" + jarEntryName);
  }

  @Test
  public void closesInputStreamAfterward() throws IOException, JoranException {
    // mock an input stream to verify that close() gets called
    InputStream stream = mock(InputStream.class);

    // configure an empty stream, which will cause a JoranException that
    // we can ignore...we're only interested in the stream being closed
    TrivialConfigurator trivialConfigurator = new TrivialConfigurator(rulesMap);
    trivialConfigurator.setContext(context);
    try {
      trivialConfigurator.doConfigure(stream);
    } catch (JoranException e) {
      // ignore
    }

    verify(stream, atLeastOnce()).close();
  }
}
