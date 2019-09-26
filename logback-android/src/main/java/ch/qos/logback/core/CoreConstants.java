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

import ch.qos.logback.core.util.EnvUtil;

public class CoreConstants {

  final public static String STATUS_LISTENER_CLASS = "logback.statusListenerClass";

  /**
   * Number of idle threads to retain in a context's executor service.
   */
  // CORE_POOL_SIZE must be 1 for JDK 1.5. For JDK 1.6 or higher it's set to 0
  // so that there are no idle threads
  public static final int CORE_POOL_SIZE = EnvUtil.isJDK5() ? 1 : 0;

  // Apparently ScheduledThreadPoolExecutor has limitation where a task cannot be submitted from
  // within a running task unless the pool has worker threads already available. ThreadPoolExecutor
  // does not have this limitation.
  // This causes tests failures in SocketReceiverTest.testDispatchEventForEnabledLevel and
  // ServerSocketReceiverFunctionalTest.testLogEventFromClient.
  // We thus set a pool size > 0 for tests to pass.
  public static final int SCHEDULED_EXECUTOR_POOL_SIZE = 2;

  /**
   * Maximum number of threads to allow in a context's executor service.
   */
  // if you need a different MAX_POOL_SIZE, please file create a jira issue
  // asking to make MAX_POOL_SIZE a parameter.
  public static final int MAX_POOL_SIZE = 32;

  // Note that the line.separator property can be looked up even by
  // applets.
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  public static final int LINE_SEPARATOR_LEN = LINE_SEPARATOR.length();


  public static final String CODES_URL = "http://logback.qos.ch/codes.html";
  public static final String MANUAL_URL_PREFIX = "http://logback.qos.ch/manual/";

  /**
   * The default context name.
   */
  public static final String DEFAULT_CONTEXT_NAME = "default";
  /**
   * Customized pattern conversion rules are stored under this key in the
   * {@link Context} object store.
   */
  public static final String PATTERN_RULE_REGISTRY = "PATTERN_RULE_REGISTRY";

  public static final String ISO8601_STR = "ISO8601";
  public static final String ISO8601_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";
  public static final String DAILY_DATE_PATTERN = "yyyy-MM-dd";

  /**
   * Time format used in Common Log Format
   */
  public static final String CLF_DATE_PATTERN = "dd/MMM/yyyy:HH:mm:ss Z";

  /**
   * The key used in locating the evaluator map in context's object map.
   */
  public static final String EVALUATOR_MAP = "EVALUATOR_MAP";

  /**
   * Key used to locate a collision map for FileAppender instances in context's object map.
   * 
   * The collision map consists of enties of the type (appender name, File option) 
   */
  public static final String FA_FILENAME_COLLISION_MAP = "FA_FILENAME_COLLISION_MAP";

  /**
   * Key used to locate a collision map for RollingFileAppender instances in context's object map.
   * 
   * The collision map consists of entities of the type (appender name, FileNamePattern option)
   */
  public static final String RFA_FILENAME_PATTERN_COLLISION_MAP = "RFA_FILENAME_PATTERN_COLLISION_MAP";

  /**
   * By convention, we assume that the static method named "valueOf" taking
   * a string argument can restore a given object from its string
   * representation.
   */
  public static final String VALUE_OF = "valueOf";

  /**
   * An empty string.
   */
  public static final String EMPTY_STRING = "";

  public static final String CAUSED_BY = "Caused by: ";
  public static final String SUPPRESSED = "Suppressed: ";
  public static final String WRAPPED_BY = "Wrapped by: ";

  public static final char PERCENT_CHAR = '%';
  public static final char LEFT_PARENTHESIS_CHAR = '(';
  public static final char RIGHT_PARENTHESIS_CHAR = ')';

  public static final char ESCAPE_CHAR = '\\';
  public static final char CURLY_LEFT = '{';
  public static final char CURLY_RIGHT = '}';
  public static final char COMMA_CHAR = ',';
  public static final char DOUBLE_QUOTE_CHAR = '"';
  public static final char SINGLE_QUOTE_CHAR = '\'';
  public static final char COLON_CHAR = ':';
  public static final char DASH_CHAR = '-';
  public static final String DEFAULT_VALUE_SEPARATOR = ":-";


  /**
   * Number of rows before in an HTML table before,
   * we close the table and create a new one
   */
  public static final int TABLE_ROW_LIMIT = 10000;


  // reset the ObjectOutputStream every OOS_RESET_FREQUENCY calls
  // this avoid serious memory leaks
  public static final int OOS_RESET_FREQUENCY = 70;

  // the max number of times an error should be reported
  public static final int MAX_ERROR_COUNT = 4;


  public static final char DOT = '.';
  public static final char TAB = '\t';
  public static final char DOLLAR = '$';

  public static final String SEE_FNP_NOT_SET = "See also "+CODES_URL+"#tbr_fnp_not_set";
  public static final String SEE_MISSING_INTEGER_TOKEN = "See also "+CODES_URL+"#sat_missing_integer_token";

  public static final String CONFIGURATION_WATCH_LIST = "CONFIGURATION_WATCH_LIST";
  public static final String CONFIGURATION_WATCH_LIST_RESET_X = "CONFIGURATION_WATCH_LIST_RESET";

  public static final String SAFE_JORAN_CONFIGURATION = "SAFE_JORAN_CONFIGURATION";
  public static final String XML_PARSING = "XML_PARSING";

  // Context Object name for the shutdown hook
  public static final String SHUTDOWN_HOOK_THREAD = "SHUTDOWN_HOOK";

  /**
   * The key under which the local host name is registered in the logger
   * context.
   */
  public static final String HOSTNAME_KEY = "HOSTNAME";

  /**
   * The key under which the current context name is registered in the logger
   * context.
   */
  public static final String CONTEXT_NAME_KEY = "CONTEXT_NAME";

  /**
   * The key under which the application files directory is registered in
   * the logger context. The value is typically something like:
   * "/data/data/com.example/files"
   */
  public static final String DATA_DIR_KEY = "DATA_DIR";

  /**
   * The key under which the Android external storage directory is
   * registered in the logger context. The value is null if the external
   * storage is not mounted.
   */
  public static final String EXT_DIR_KEY = "EXT_DIR";

  /**
   * The key under which the application package name is registered
   * in the logger context.
   */
  public static final String PACKAGE_NAME_KEY = "PACKAGE_NAME";

  /**
   * The key under which the application version name is registered
   * in the logger context.
   */
  public static final String VERSION_NAME_KEY = "VERSION_NAME";

  /**
   * The key under which the application version code is registered
   * in the logger context.
   */
  public static final String VERSION_CODE_KEY = "VERSION_CODE";

  public static final int BYTES_PER_INT = 4;
  public static final long MILLIS_IN_ONE_SECOND = 1000;
  public static final long MILLIS_IN_ONE_MINUTE = MILLIS_IN_ONE_SECOND*60;
  public static final long MILLIS_IN_ONE_HOUR = MILLIS_IN_ONE_MINUTE*60;
  public static final long MILLIS_IN_ONE_DAY = MILLIS_IN_ONE_HOUR*24;
  public static final long MILLIS_IN_ONE_WEEK = MILLIS_IN_ONE_DAY*7;

  /**
   * The number of seconds to wait for compression jobs to finish.
   */
  public static final int SECONDS_TO_WAIT_FOR_COMPRESSION_JOBS = 30;

  public static final String CONTEXT_SCOPE_VALUE = "context";

  public static final String RESET_MSG_PREFIX = "Will reset and reconfigure context ";

  public static final String UNDEFINED_PROPERTY_SUFFIX = "_IS_UNDEFINED";

  public static final String LEFT_ACCOLADE = new String(new char[] {CURLY_LEFT});
  public static final String RIGHT_ACCOLADE = new String(new char[] {CURLY_RIGHT});
  public static final long UNBOUNDED_TOTAL_SIZE_CAP = 0;
  public static final int UNBOUND_HISTORY = 0;

  public static final String RECONFIGURE_ON_CHANGE_TASK = "RECONFIGURE_ON_CHANGE_TASK";
  public static final String SIZE_AND_TIME_BASED_FNATP_IS_DEPRECATED = "SizeAndTimeBasedFNATP is deprecated. Use SizeAndTimeBasedRollingPolicy instead";
}
