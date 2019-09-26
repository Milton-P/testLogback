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
package ch.qos.logback.classic.spi;

import ch.qos.logback.core.CoreConstants;

import java.util.List;

/**
 * This class computes caller data returning the result in the form
 * of a StackTraceElement array.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class CallerData {


  /**
   * When caller information is not available this constant is used for file
   * name, method name, etc.
   */
  public static final String NA = "?";

  // All logger call's in log4j-over-slf4j use the Category class
  private static final String LOG4J_CATEGORY = "org.apache.log4j.Category";
  private static final String SLF4J_BOUNDARY = "org.slf4j.Logger";

  /**
   * When caller information is not available this constant is used for the line
   * number.
   */
  public static final int LINE_NA = -1;

  public static final String CALLER_DATA_NA = "?#?:?" + CoreConstants.LINE_SEPARATOR;

  /**
   * This value is returned in case no caller data could be extracted.
   */
  public static final StackTraceElement[] EMPTY_CALLER_DATA_ARRAY = new StackTraceElement[0];


  /**
   * Extract caller data information as an array based on a Throwable passed as
   * parameter
   *
   * @param t Throwable to extract data from
   * @param fqnOfInvokingClass Fully qualified name of invoking class
   * @param maxDepth maximum stack depth
   * @param frameworkPackageList list of framework package names to search
   * @return caller stack trace array
   */
  public static StackTraceElement[] extract(Throwable t,
                                            String fqnOfInvokingClass, final int maxDepth,
                                            List<String> frameworkPackageList) {
    if (t == null) {
      return null;
    }

    StackTraceElement[] steArray = t.getStackTrace();
    StackTraceElement[] callerDataArray;

    int found = LINE_NA;
    for (int i = 0; i < steArray.length; i++) {
      if (isInFrameworkSpace(steArray[i].getClassName(),
              fqnOfInvokingClass, frameworkPackageList)) {
        // the caller is assumed to be the next stack frame, hence the +1.
        found = i + 1;
      } else {
        if (found != LINE_NA) {
          break;
        }
      }
    }

    // we failed to extract caller data
    if (found == LINE_NA) {
      return EMPTY_CALLER_DATA_ARRAY;
    }

    int availableDepth = steArray.length - found;
    int desiredDepth = maxDepth < (availableDepth) ? maxDepth : availableDepth;

    callerDataArray = new StackTraceElement[desiredDepth];
    for (int i = 0; i < desiredDepth; i++) {
      callerDataArray[i] = steArray[found + i];
    }
    return callerDataArray;
  }

  static boolean isInFrameworkSpace(String currentClass,
                                    String fqnOfInvokingClass, List<String> frameworkPackageList) {
    // the check for org.apache.log4j.Category class is intended to support
    // log4j-over-slf4j. it solves http://bugzilla.slf4j.org/show_bug.cgi?id=66
    if (currentClass.equals(fqnOfInvokingClass) || currentClass.equals(LOG4J_CATEGORY)
            || currentClass.startsWith(SLF4J_BOUNDARY) || isInFrameworkSpaceList(currentClass, frameworkPackageList)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Is currentClass present in the list of packages considered part of the logging framework?
   */
  private static boolean isInFrameworkSpaceList(String currentClass, List<String> frameworkPackageList) {
    if (frameworkPackageList == null)
      return false;

    for (String s : frameworkPackageList) {
      if (currentClass.startsWith(s))
        return true;
    }
    return false;
  }

  /**
   * Returns a StackTraceElement where all string fields are set to {@link #NA} and line number is set to {@link #LINE_NA}.
   *
   * @return StackTraceElement with values set to NA constants.
   * @since 1.0.10
   */
  public static StackTraceElement naInstance() {
    return new StackTraceElement(NA, NA, NA, LINE_NA);
  }

}
