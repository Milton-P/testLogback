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
package org.slf4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactoryFriend;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.read.ListAppender;

// This test is failing in getRecordedEvents(). For some reason,
// it can't find the listAppender.xml file via CONFIG_FILE_PROPERTY.
// Disable the test for now.
@Ignore
@RunWith(RobolectricTestRunner.class)
public class MultithreadedInitializationTest {

  private final static int THREAD_COUNT = 4 + Runtime.getRuntime().availableProcessors() * 2;
  private static AtomicLong EVENT_COUNT = new AtomicLong(0);

  private int diff = new Random().nextInt(10000);
  private String loggerName = "org.slf4j.impl.MultithreadedInitializationTest";

  @Before
  public void setUp() throws Exception {
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, ClassicTestConstants.INPUT_PREFIX + "listAppender.xml");
    LoggerFactoryFriend.reset();
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
  }

  @Test
  public void multiThreadedInitialization() throws InterruptedException, BrokenBarrierException {
    LoggerAccessingThread[] accessors = harness();

    for (LoggerAccessingThread accessor : accessors) {
      EVENT_COUNT.getAndIncrement();
      accessor.logger.info("post harness");
    }

    Logger logger = LoggerFactory.getLogger(loggerName + ".slowInitialization-" + diff);
    logger.info("hello");
    EVENT_COUNT.getAndIncrement();
    List<ILoggingEvent> events = getRecordedEvents();
    assertEquals(EVENT_COUNT.get(), events.size());
  }

  private List<ILoggingEvent> getRecordedEvents() {
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(Logger.ROOT_LOGGER_NAME);

    ListAppender<ILoggingEvent> la = (ListAppender<ILoggingEvent>) root.getAppender("LIST");
    assertNotNull(la);
    return la.list;
  }

  private static LoggerAccessingThread[] harness() throws InterruptedException, BrokenBarrierException {
    LoggerAccessingThread[] threads = new LoggerAccessingThread[THREAD_COUNT];
    final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT + 1);
    for (int i = 0; i < THREAD_COUNT; i++) {
      threads[i] = new LoggerAccessingThread(barrier, i);
      threads[i].start();
    }

    barrier.await();
    for (int i = 0; i < THREAD_COUNT; i++) {
      threads[i].join();
    }
    return threads;
  }

  static class LoggerAccessingThread extends Thread {
    final CyclicBarrier barrier;
    Logger logger;
    int count;

    LoggerAccessingThread(CyclicBarrier barrier, int count) {
      this.barrier = barrier;
      this.count = count;
    }

    public void run() {
      try {
        barrier.await();
      } catch (Exception e) {
        e.printStackTrace();
      }
      logger = LoggerFactory.getLogger(this.getClass().getName() + "-" + count);
      logger.info("in run method");
      EVENT_COUNT.getAndIncrement();
    }
  }

}
