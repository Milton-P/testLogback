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
package ch.qos.logback.classic.joran.action;

import ch.qos.logback.classic.joran.ReconfigureOnChangeTask;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import org.xml.sax.Attributes;

import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.ReconfigureOnChangeFilter;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.ContextUtil;
import ch.qos.logback.core.util.Duration;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusListenerConfigHelper;

public class ConfigurationAction extends Action {
  static final String INTERNAL_DEBUG_ATTR = "debug";
  static final String PACKAGING_DATA_ATTR = "packagingData";
  static final String SCAN_ATTR = "scan";
  static final String SCAN_PERIOD_ATTR = "scanPeriod";
  static final String DEBUG_SYSTEM_PROPERTY_KEY = "logback.debug";
  static final Duration SCAN_PERIOD_DEFAULT = Duration.buildByMinutes(1);

  @Override
  public void begin(InterpretationContext ic, String name, Attributes attributes) {
    // See LBCLASSIC-225 (the system property is looked up first. Thus, it overrides
    // the equivalent property in the config file. This reversal of scope priority is justified
    // by the use case: the admin trying to chase rogue config file
    String debugAttrib = OptionHelper.getSystemProperty(DEBUG_SYSTEM_PROPERTY_KEY);
    if (debugAttrib == null) {
      debugAttrib = ic.subst(attributes.getValue(INTERNAL_DEBUG_ATTR));
    }

    if (OptionHelper.isEmpty(debugAttrib) || debugAttrib.equalsIgnoreCase("false")
            || debugAttrib.equalsIgnoreCase("null")) {
      addInfo(INTERNAL_DEBUG_ATTR + " attribute not set");
    } else {
      StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());
    }

    processScanAttrib(ic, attributes);
    ContextUtil contextUtil = new ContextUtil(context);
    contextUtil.addHostNameAsProperty();

    // the context is turbo filter attachable, so it is pushed on top of the
    // stack
    ic.pushObject(getContext());

    LoggerContext lc = (LoggerContext) context;
    boolean packagingData = OptionHelper.toBoolean(
            ic.subst(attributes.getValue(PACKAGING_DATA_ATTR)),
            LoggerContext.DEFAULT_PACKAGING_DATA);
    lc.setPackagingDataEnabled(packagingData);
  }

  String getSystemProperty(String name) {
    /*
     * LOGBACK-743: accessing a system property in the presence of a
     * SecurityManager (e.g. applet sandbox) can result in a SecurityException.
     */
    try {
      return System.getProperty(name);
    } catch (SecurityException ex) {
      return null;
    }
  }

  void processScanAttrib(InterpretationContext ic, Attributes attributes) {
    String scanAttrib = ic.subst(attributes.getValue(SCAN_ATTR));
    if (!OptionHelper.isEmpty(scanAttrib)
            && !"false".equalsIgnoreCase(scanAttrib)) {

      ScheduledExecutorService scheduledExecutorService = context.getScheduledExecutorService();
      URL mainURL = ConfigurationWatchListUtil.getMainWatchURL(context);
      if (mainURL == null) {
        addWarn("Due to missing top level configuration file, reconfiguration on change (configuration file scanning) cannot be done.");
        return;
      }
      ReconfigureOnChangeTask rocTask = new ReconfigureOnChangeTask();
      rocTask.setContext(context);

      context.putObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK, rocTask);

      String scanPeriodAttrib = ic.subst(attributes.getValue(SCAN_PERIOD_ATTR));
      Duration duration = getDurationOfScanPeriodAttribute(scanPeriodAttrib, SCAN_PERIOD_DEFAULT);

      addInfo("Will scan for changes in [" + mainURL + "] ");
      // Given that included files are encountered at a later phase, the complete list of files
      // to scan can only be determined when the configuration is loaded in full.
      // However, scan can be active if mainURL is set. Otherwise, when changes are detected
      // the top level config file cannot be accessed.
      addInfo("Setting ReconfigureOnChangeTask scanning period to " + duration);

      ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(rocTask, duration.getMilliseconds(), duration.getMilliseconds(),
              TimeUnit.MILLISECONDS);
      context.addScheduledFuture(scheduledFuture);
    }
  }

  private Duration getDurationOfScanPeriodAttribute(String scanPeriodAttrib, Duration defaultDuration) {
    Duration duration = null;
    if (!OptionHelper.isEmpty(scanPeriodAttrib)) {
      Exception ex = null;
      try {
        duration = Duration.valueOf(scanPeriodAttrib);
      } catch (IllegalArgumentException e) {
        ex = e;
      } catch (IllegalStateException e) {
        ex = e;
      }

      if (ex != null) {
        addWarn("Failed to parse 'scanPeriod' attribute ["+scanPeriodAttrib+"]", ex);
      }
    }

    if (duration == null) {
      addInfo("No 'scanPeriod' specified. Defaulting to " + defaultDuration.toString());
      duration = defaultDuration;
    }
    return duration;
  }

  @Override
  public void end(InterpretationContext ec, String name) {
    addInfo("End of configuration.");
    ec.popObject();
  }
}
