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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import junit.framework.TestCase;

import org.junit.Ignore;

import ch.qos.logback.classic.net.testObjectBuilders.Builder;
import ch.qos.logback.classic.net.testObjectBuilders.MinimalSer;
import ch.qos.logback.classic.net.testObjectBuilders.MinimalSerBuilder;
import ch.qos.logback.classic.net.testObjectBuilders.TrivialLoggingEventVOBuilder;
import ch.qos.logback.classic.spi.LoggingEventVO;

@Ignore()
public class SerializationPerfTest extends TestCase {

  ObjectOutputStream oos;

  int loopNumber = 10000;
  int resetFrequency = 100;
  int pauseFrequency = 10;
  long pauseLengthInMillis = 20;

  /**
   * <p>
   * Run the test with a MockSocketServer or with a NOPOutputStream
   * 
   * <p>
   * Run with external mock can be done using the ExternalMockSocketServer. It
   * needs to be launched from a separate JVM. The ExternalMockSocketServer does
   * not consume the events but passes through the available bytes that it is
   * receiving.
   * 
   * <p>
   * For example, with 4 test methods, you can launch the
   * ExternalMockSocketServer this way:
   * </p>
   * <p>
   * <code>java ch.qos.logback.classic.net.ExternalMockSocketServer 4</code>
   * </p>
   */
  boolean runWithExternalMockServer = true;

        /**
         * Last results:
         * Data sent mesured in kilobytes.
         * Avg time mesured in microsecs.
         * 
         * NOPOutputStream: 
         *   |                |  Runs | Avg time | Data sent |
         *   | MinimalObj Ext | 10000 |          |           |
         *   | MinimalObj Ser | 10000 |          |           |
         *   | LoggEvent Ext  | 10000 |          |           |
         *   | LoggEvent Ser  | 10000 |          |           |
         * 
         * External MockServer with 45 letters-long message: on localhost
         * (always the same message)
         *       |                |  Runs | Avg time | Data sent |
         *   | MinimalObj Ext | 10000 |      -   |       -   |
         *   | MinimalObj Ser | 10000 |     74   |     248   |
         *   | LoggEvent Ext  | 10000 |      -   |       -   |
         *   | LoggEvent Ser  | 10000 |    156   |     835   |
         *       pauseFrequency = 10 and pauseLengthInMillis = 20
         *
         * External MockServer with 45 letters-long message: on localhost
         * (different message each time)
         *       |                |  Runs | Avg time | Data sent |
         *   | MinimalObj Ext | 10000 |          |           |
         *   | MinimalObj Ser | 10000 |     73   |    1139   |
         *   | LoggEvent Ext  | 10000 |          |           |
         *   | LoggEvent Ser  | 10000 |    162   |    1752   |
         *       pauseFrequency = 10 and pauseLengthInMillis = 20
         *
         * External MockServer with 45 letters-long message: on PIXIE
         * (always the same message)
         *       |                |  Runs | Avg time | Data sent |
         *   | MinimalObj Ext | 10000 |      -   |       -   |
         *   | MinimalObj Ser | 10000 |     29   |     248   |
         *   | LoggEvent Ext  | 10000 |      -   |       -   |
         *   | LoggEvent Ser  | 10000 |     42   |     835   |
         *       pauseFrequency = 10 and pauseLengthInMillis = 20
         *
         * External MockServer with 45 letters-long message: on PIXIE
         * (different message each time)
         *       |                |  Runs | Avg time | Data sent |
         *   | MinimalObj Ext | 10000 |          |           |
         *   | MinimalObj Ser | 10000 |     27   |    1139   |
         *   | LoggEvent Ext  | 10000 |          |           |
         *   | LoggEvent Ser  | 10000 |     44   |    1752   |
         *       pauseFrequency = 10 and pauseLengthInMillis = 20
         *
         */

  public void setUp() throws Exception {
    super.setUp();
    if (runWithExternalMockServer) {
      oos = new ObjectOutputStream(new Socket("localhost",
          ExternalMockSocketServer.PORT).getOutputStream());
    } else {
      oos = new ObjectOutputStream(new NOPOutputStream());
    }
  }

  public void tearDown() throws Exception {
    super.tearDown();
    oos.close();
    oos = null;
  }

  public void runPerfTest(Builder<?> builder, String label) throws Exception {
    // long time1 = System.nanoTime();

    // Object builtObject = builder.build(1);

    // first run for just in time compiler
    int resetCounter = 0;
    int pauseCounter = 0;
    for (int i = 0; i < loopNumber; i++) {
      try {
        oos.writeObject(builder.build(i));
        oos.flush();
        if (++resetCounter >= resetFrequency) {
          oos.reset();
          resetCounter = 0;
        }
        if (++pauseCounter >= pauseFrequency) {
          Thread.sleep(pauseLengthInMillis);
          pauseCounter = 0;
        }

      } catch (IOException ex) {
        fail(ex.getMessage());
      }
    }

    // second run
    Long t1;
    Long t2;
    Long total = 0L;
    resetCounter = 0;
    pauseCounter = 0;
    // System.out.println("Beginning mesured run");
    for (int i = 0; i < loopNumber; i++) {
      try {
        t1 = System.nanoTime();
        oos.writeObject(builder.build(i));
        oos.flush();
        t2 = System.nanoTime();
        total += (t2 - t1);
        if (++resetCounter >= resetFrequency) {
          oos.reset();
          resetCounter = 0;
        }
        if (++pauseCounter >= pauseFrequency) {
          Thread.sleep(pauseLengthInMillis);
          pauseCounter = 0;
        }
      } catch (IOException ex) {
        fail(ex.getMessage());
      }
    }
    total /= 1000;
    System.out.println(label + " : average time = " + total / loopNumber
        + " microsecs after " + loopNumber + " writes.");

    // long time2 = System.nanoTime();
    // System.out.println("********* -> Time needed to run the test method: " +
    // Long.toString(time2-time1));
  }

  // public void testWithMinimalExternalization() throws Exception {
  // Builder builder = new MinimalExtBuilder();
  // runPerfTest(builder, "Minimal object externalization");
  // }

  public void testWithMinimalSerialization() throws Exception {
    Builder<MinimalSer> builder = new MinimalSerBuilder();
    runPerfTest(builder, "Minimal object serialization");
  }

  // public void testWithExternalization() throws Exception {
  // Builder builder = new LoggingEventExtBuilder();
  // runPerfTest(builder, "LoggingEvent object externalization");
  // }

  public void testWithSerialization() throws Exception {
    Builder<LoggingEventVO> builder = new TrivialLoggingEventVOBuilder();
    runPerfTest(builder, "LoggingEventVO object serialization");
  }
  
  
  
}
