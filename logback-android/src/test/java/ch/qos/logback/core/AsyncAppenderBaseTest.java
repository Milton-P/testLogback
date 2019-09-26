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
package ch.qos.logback.core;

import ch.qos.logback.core.helpers.NOPAppender;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.testUtil.DelayingListAppender;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.NPEAppender;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ceki G&uuml;lc&uuml;
 * @author Torsten Juergeleit
 */
public class AsyncAppenderBaseTest {


  Context context = new ContextBase();
  AsyncAppenderBase<Integer> asyncAppenderBase = new AsyncAppenderBase<Integer>();
  LossyAsyncAppender lossyAsyncAppender = new LossyAsyncAppender();
  DelayingListAppender<Integer> delayingListAppender = new DelayingListAppender<Integer>();
  ListAppender<Integer> listAppender = new ListAppender<Integer>();
  StatusChecker statusChecker = new StatusChecker(context);

  @Before
  public void setUp() {
    asyncAppenderBase.setContext(context);
    lossyAsyncAppender.setContext(context);

    listAppender.setContext(context);
    listAppender.setName("list");
    listAppender.start();

    delayingListAppender.setContext(context);
    delayingListAppender.setName("list");
    delayingListAppender.start();
  }

  @Test(timeout = 2000)
  public void smoke() {
    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.doAppend(0);
    asyncAppenderBase.stop();
    verify(listAppender, 1);
  }

  @Test
  public void exceptionsShouldNotCauseHalting() throws InterruptedException {
    NPEAppender<Integer> npeAppender = new NPEAppender<Integer>();
    npeAppender.setName("bad");
    npeAppender.setContext(context);
    npeAppender.start();

    asyncAppenderBase.addAppender(npeAppender);
    asyncAppenderBase.start();
    assertTrue(asyncAppenderBase.isStarted());
    for (int i = 0; i < 10; i++)
      asyncAppenderBase.append(i);

    asyncAppenderBase.stop();
    assertFalse(asyncAppenderBase.isStarted());
    assertEquals(AppenderBase.ALLOWED_REPEATS, statusChecker.matchCount("Appender \\[bad\\] failed to append."));
  }

  @Test(timeout = 2000)
  public void emptyQueueShouldBeStoppable() {
    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.stop();
    verify(listAppender, 0);
  }

  @Test(timeout = 2000)
  public void workerShouldStopEvenIfInterruptExceptionConsumedWithinSubappender() {
    delayingListAppender.delay = 100;
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.doAppend(0);
    asyncAppenderBase.stop();
    verify(delayingListAppender, 1);
    assertTrue(delayingListAppender.interrupted);
    Thread.interrupted();
  }

  @Test(timeout = 2000)
  public void noEventLoss() {
    int bufferSize = 10;
    int loopLen = bufferSize * 2;
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.setQueueSize(bufferSize);
    asyncAppenderBase.start();
    for (int i = 0; i < loopLen; i++) {
      asyncAppenderBase.doAppend(i);
    }
    asyncAppenderBase.stop();
    verify(delayingListAppender, loopLen);
  }

  @Test(timeout = 2000)
   public void eventLossIfNeverBlock() {
    int bufferSize = 10;
    int loopLen = bufferSize * 2;
    delayingListAppender.setDelay(5000); // something greater than the test timeout
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.setQueueSize(bufferSize);
    asyncAppenderBase.setNeverBlock(true);
    asyncAppenderBase.start();
    for (int i = 0; i < loopLen; i++) {
      asyncAppenderBase.doAppend(i);
    }
    asyncAppenderBase.stop();
    // ListAppender size isn't a reliable test here, so just make sure we didn't
    // have any errors, and that we could complete the test in time.
    statusChecker.assertIsErrorFree();
  }

  @Test(timeout = 2000)
  public void lossyAppenderShouldOnlyLoseCertainEvents() {
    int bufferSize = 5;
    int loopLen = bufferSize * 2;
    lossyAsyncAppender.addAppender(delayingListAppender);
    lossyAsyncAppender.setQueueSize(bufferSize);
    lossyAsyncAppender.setDiscardingThreshold(1);
    lossyAsyncAppender.start();
    for (int i = 0; i < loopLen; i++) {
      lossyAsyncAppender.doAppend(i);
    }
    lossyAsyncAppender.stop();
    // events 0, 3, 6 and 9 are discardable. However, for events 0 and 3
    // the buffer is not not yet full. Thus, only events 6 and 9 will be
    // effectively discarded.
    verify(delayingListAppender, loopLen - 2);
  }

  @Test(timeout = 2000)
  public void lossyAppenderShouldBeNonLossyIfDiscardingThresholdIsZero() {
    int bufferSize = 5;
    int loopLen = bufferSize * 2;
    lossyAsyncAppender.addAppender(delayingListAppender);
    lossyAsyncAppender.setQueueSize(bufferSize);
    lossyAsyncAppender.setDiscardingThreshold(0);
    lossyAsyncAppender.start();
    for (int i = 0; i < loopLen; i++) {
      lossyAsyncAppender.doAppend(i);
    }
    lossyAsyncAppender.stop();
    verify(delayingListAppender, loopLen);
  }

  @Test
  public void invalidQueueCapacityShouldResultInNonStartedAppender() {
    asyncAppenderBase.addAppender(new NOPAppender<Integer>());
    asyncAppenderBase.setQueueSize(0);
    assertEquals(0, asyncAppenderBase.getQueueSize());
    asyncAppenderBase.start();
    assertFalse(asyncAppenderBase.isStarted());
    statusChecker.assertContainsMatch("Invalid queue size");
  }

  @SuppressWarnings("deprecation")
  @Test
  public void workerThreadFlushesOnStop() {
    int loopLen = 5;
    int maxRuntime = (loopLen + 1) * Math.max(1000, delayingListAppender.delay);
    ListAppender<Integer> la = delayingListAppender;
    asyncAppenderBase.addAppender(la);
    asyncAppenderBase.setDiscardingThreshold(0);
    asyncAppenderBase.setMaxFlushTime(maxRuntime);
    asyncAppenderBase.start();
    asyncAppenderBase.worker.suspend();

    for (int i = 0; i < loopLen; i++) {
      asyncAppenderBase.doAppend(i);
    }
    assertEquals(loopLen, asyncAppenderBase.getNumberOfElementsInQueue());
    assertEquals(0, la.list.size());

    asyncAppenderBase.worker.resume();
    asyncAppenderBase.stop();

    assertEquals(0, asyncAppenderBase.getNumberOfElementsInQueue());
    verify(la, loopLen);
  }

  // @SuppressWarnings("deprecation")
  @Test
  public void stopExitsWhenMaxRuntimeReached() throws InterruptedException {
    int maxFlushTime = 1; // runtime of 0 means wait forever, so use 1 ms instead
    int loopLen = 10;
    ListAppender<Integer> la = delayingListAppender;
    asyncAppenderBase.addAppender(la);
    asyncAppenderBase.setMaxFlushTime(maxFlushTime);
    asyncAppenderBase.start();

    for (int i = 0; i < loopLen; i++) {
      asyncAppenderBase.doAppend(i);
    }

    asyncAppenderBase.stop();

    // confirms that stop exited when runtime reached
    statusChecker.assertContainsMatch("Max queue flush timeout \\(" + maxFlushTime + " ms\\) exceeded.");

    asyncAppenderBase.worker.join();

    // confirms that all entries do end up being flushed if we wait long enough
    verify(la, loopLen);
  }

  // Interruption of current thread when in doAppend method should not be consumed
  // by async appender. See also http://jira.qos.ch/browse/LOGBACK-910
  @Test
  public void verifyInterruptionIsNotSwallowed() {
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.start();
    Thread.currentThread().interrupt();
    asyncAppenderBase.doAppend(0);
    assertTrue(Thread.currentThread().isInterrupted());
    // clear interrupt flag for subsequent tests
    Thread.interrupted();
  }

  // Interruption of current thread should not prevent logging.
  // See also http://jira.qos.ch/browse/LOGBACK-910
  // and https://jira.qos.ch/browse/LOGBACK-1247
  @Test
  public void verifyInterruptionDoesNotPreventLogging() {
    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.doAppend(0);
    Thread.currentThread().interrupt();
    asyncAppenderBase.doAppend(1);
    asyncAppenderBase.doAppend(1);
    assertTrue(Thread.currentThread().isInterrupted());
    asyncAppenderBase.stop();
    verify(listAppender, 3);
    // clear interrupt flag for subsequent tests
    Thread.interrupted();
  }

  @Test
  public void verifyInterruptionOfWorkerIsSwallowed() {
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.stop();
    assertFalse(asyncAppenderBase.worker.isAlive());
    assertFalse(asyncAppenderBase.worker.isInterrupted());
  }

  private void verify(ListAppender<Integer> la, int atLeast) {
    assertFalse(la.isStarted());
    assertTrue(atLeast+ " <= "+la.list.size(), atLeast <= la.list.size());
    statusChecker.assertIsErrorFree();
    statusChecker.assertContainsMatch("Worker thread will flush remaining events before exiting.");
  }

  static class LossyAsyncAppender extends AsyncAppenderBase<Integer> {
    @Override
    protected boolean isDiscardable(Integer i) {
      return (i % 3 == 0);
    }
  }

  @Test
  public void checkThatStartMethodIsIdempotent() {
    asyncAppenderBase.addAppender(lossyAsyncAppender);
    asyncAppenderBase.start();

    // we don't need mockito for this test, but if we did here is how it would look
    //AsyncAppenderBase<Integer>  spied = Mockito.spy(asyncAppenderBase);
    //Mockito.doThrow(new IllegalStateException("non idempotent start")).when((UnsynchronizedAppenderBase<Integer>) spied).start();

    // a second invocation of start will cause a IllegalThreadStateException thrown by the asyncAppenderBase.worker thread
    asyncAppenderBase.start();
  }
}
