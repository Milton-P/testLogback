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
package ch.qos.logback.classic.turbo;

import java.io.File;
import java.net.URL;
import java.util.List;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.android.AndroidContextUtil;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.status.StatusUtil;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterReply;

import static ch.qos.logback.core.CoreConstants.MILLIS_IN_ONE_SECOND;

/**
 * Reconfigure a LoggerContext when the configuration file changes.
 *
 * @author Ceki Gulcu
 */
public class ReconfigureOnChangeFilter extends TurboFilter {

  /**
   * Scan for changes in configuration file once every minute.
   */
  // 1 minute - value mentioned in documentation
  public final static long DEFAULT_REFRESH_PERIOD = 60 * MILLIS_IN_ONE_SECOND;

  long refreshPeriod = DEFAULT_REFRESH_PERIOD;
  URL mainConfigurationURL;
  protected volatile long nextCheck;

  ConfigurationWatchList configurationWatchList;

  @Override
  public void start() {
    configurationWatchList = ConfigurationWatchListUtil.getConfigurationWatchList(context);
    if (configurationWatchList != null) {
      mainConfigurationURL = configurationWatchList.getMainURL();
      if(mainConfigurationURL == null) {
        addWarn("Due to missing top level configuration file, automatic reconfiguration is impossible.");
        return;
      }
      List<File> watchList = configurationWatchList.getCopyOfFileWatchList();
      long inSeconds = refreshPeriod / 1000;
      addInfo("Will scan for changes in [" + watchList + "] every "
              + inSeconds + " seconds. ");
      synchronized (configurationWatchList) {
        updateNextCheck(System.currentTimeMillis());
      }
      super.start();
    } else {
      addWarn("Empty ConfigurationWatchList in context");
    }
  }

  @Override
  public String toString() {
    return "ReconfigureOnChangeFilter{" +
            "invocationCounter=" + invocationCounter +
            '}';
  }

  // The next fields counts the number of time the decide method is called
  //
  // IMPORTANT: This field can be updated by multiple threads. It follows that
  // its values may *not* be incremented sequentially. However, we don't care
  // about the actual value of the field except that from time to time the
  // expression (invocationCounter++ & mask) == mask) should be true.
  private long invocationCounter = 0;

  private volatile long mask = 0xF;
  private volatile long lastMaskCheck = System.currentTimeMillis();


  @Override
  public FilterReply decide(Marker marker, Logger logger, Level level,
                            String format, Object[] params, Throwable t) {
    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }

    // for performance reasons, skip change detection (MASK-1) times out of MASK.
    // Only once every MASK calls is change detection code executed
    // Note that MASK is a variable itself.
    if (((invocationCounter++) & mask) != mask) {
      return FilterReply.NEUTRAL;
    }

    long now = System.currentTimeMillis();

    synchronized (configurationWatchList) {
      updateMaskIfNecessary(now);
      if (changeDetected(now)) {
        // Even though reconfiguration involves resetting the loggerContext,
        // which clears the list of turbo filters including this instance, it is
        // still possible for this instance to be subsequently invoked by another
        // thread if it was already executing when the context was reset.
        disableSubsequentReconfiguration();
        detachReconfigurationToNewThread();
      }
    }

    return FilterReply.NEUTRAL;
  }

  // experiments indicate that even for CPU intensive applications with 200 or more threads MASK
  // values in the order of 0xFFFF is appropriate
  private static final int MAX_MASK = 0xFFFF;


  // if less  than MASK_INCREASE_THRESHOLD milliseconds elapse between invocations of updateMaskIfNecessary() method,
  // then the mask should be increased
  private static final long MASK_INCREASE_THRESHOLD = 100;

  // if more than MASK_DECREASE_THRESHOLD milliseconds elapse between invocations of updateMaskIfNecessary() method,
  // then the mask should be decreased
  private static final long MASK_DECREASE_THRESHOLD = MASK_INCREASE_THRESHOLD*8;

  // update the mask so as to execute change detection code about once every 100 to 8000 milliseconds.
  private void updateMaskIfNecessary(long now) {
    final long timeElapsedSinceLastMaskUpdateCheck = now - lastMaskCheck;
    lastMaskCheck = now;
    if (timeElapsedSinceLastMaskUpdateCheck < MASK_INCREASE_THRESHOLD && (mask < MAX_MASK)) {
        mask = (mask << 1) | 1;
    } else if (timeElapsedSinceLastMaskUpdateCheck > MASK_DECREASE_THRESHOLD) {
      mask = mask >>> 2;
    }
  }

  // by detaching reconfiguration to a new thread, we release the various
  // locks held by the current thread, in particular, the AppenderAttachable
  // reader lock.
  void detachReconfigurationToNewThread() {
    addInfo("Detected change in [" + configurationWatchList.getCopyOfFileWatchList() + "]");
    context.getScheduledExecutorService().submit(new ReconfiguringThread());
  }

  void updateNextCheck(long now) {
    nextCheck = now + refreshPeriod;
  }

  protected boolean changeDetected(long now) {
    if (now >= nextCheck) {
      updateNextCheck(now);
      return configurationWatchList.changeDetected();
    }
    return false;
  }

  void disableSubsequentReconfiguration() {
    nextCheck = Long.MAX_VALUE;
  }

  public long getRefreshPeriod() {
    return refreshPeriod;
  }

  public void setRefreshPeriod(long refreshPeriod) {
    this.refreshPeriod = refreshPeriod;
  }

  class ReconfiguringThread implements Runnable {
    public void run() {
      if (mainConfigurationURL == null) {
        addInfo("Due to missing top level configuration file, skipping reconfiguration");
        return;
      }
      LoggerContext lc = (LoggerContext) context;
      addInfo(CoreConstants.RESET_MSG_PREFIX + "named [" + context.getName() + "]");
      if (mainConfigurationURL.toString().endsWith("xml")) {
        performXMLConfiguration(lc);
      }
    }

    private void performXMLConfiguration(LoggerContext lc) {
      JoranConfigurator jc = new JoranConfigurator();
      jc.setContext(lc);
      StatusUtil statusUtil = new StatusUtil(lc);
      List<SaxEvent> eventList = jc.recallSafeConfiguration();
      URL mainURL = ConfigurationWatchListUtil.getMainWatchURL(lc);
      lc.reset();
      new AndroidContextUtil().setupProperties(lc);
      long threshold = System.currentTimeMillis();
      try {
        jc.doConfigure(mainConfigurationURL);
        if (statusUtil.hasXMLParsingErrors(threshold)) {
          fallbackConfiguration(lc, eventList, mainURL);
        }
      } catch (JoranException e) {
        fallbackConfiguration(lc, eventList, mainURL);
      }
    }

    private void fallbackConfiguration(LoggerContext lc, List<SaxEvent> eventList, URL mainURL) {
      JoranConfigurator joranConfigurator = new JoranConfigurator();
      joranConfigurator.setContext(lc);
      if (eventList != null) {
        addWarn("Falling back to previously registered safe configuration.");
        try {
          lc.reset();
          new AndroidContextUtil().setupProperties(lc);
          JoranConfigurator.informContextOfURLUsedForConfiguration(lc, mainURL);
          joranConfigurator.doConfigure(eventList);
          addInfo("Re-registering previous fallback configuration once more as a fallback configuration point");
          joranConfigurator.registerSafeConfiguration(eventList);
        } catch (JoranException e) {
          addError("Unexpected exception thrown by a configuration considered safe.", e);
        }
      } else {
        addWarn("No previous configuration to fall back on.");
      }
    }
  }
}
