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

import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;

/**
 * Given a classname locate associated PackageInfo (jar name, version name).
 *
 * @author James Strachan
 * @author Ceki G&uuml;lc&uuml;
 */
public class PackagingDataCalculator {

  final static StackTraceElementProxy[] STEP_ARRAY_TEMPLATE = new StackTraceElementProxy[0];

  HashMap<String, ClassPackagingData> cache = new HashMap<String, ClassPackagingData>();

  public void calculate(IThrowableProxy tp) {
    while (tp != null) {
      populateFrames(tp.getStackTraceElementProxyArray());
      IThrowableProxy[] suppressed = tp.getSuppressed();
      if(suppressed != null) {
        for(IThrowableProxy current:suppressed) {
          populateFrames(current.getStackTraceElementProxyArray());
        }
      }
      tp = tp.getCause();
    }
  }

  private void populateFrames(StackTraceElementProxy[] stepArray) {
    // in the initial part of this method we populate package information for
    // common stack frames
    final Throwable t = new Throwable("local stack reference");
    final StackTraceElement[] localteSTEArray = t.getStackTrace();
    final int commonFrames = STEUtil.findNumberOfCommonFrames(localteSTEArray,
            stepArray);
    final int stepFirstCommon = stepArray.length - commonFrames;

    ClassLoader lastExactClassLoader = null;
    ClassLoader firsExactClassLoader = null;

    for (int i = 0; i < commonFrames; i++) {
      StackTraceElementProxy step = stepArray[stepFirstCommon + i];
      ClassPackagingData pi = computeBySTEP(step, lastExactClassLoader);
      step.setClassPackagingData(pi);
    }
    populateUncommonFrames(commonFrames, stepArray, firsExactClassLoader);
  }

  private void populateUncommonFrames(int commonFrames,
                              StackTraceElementProxy[] stepArray, ClassLoader firstExactClassLoader) {
    int uncommonFrames = stepArray.length - commonFrames;
    for (int i = 0; i < uncommonFrames; i++) {
      StackTraceElementProxy step = stepArray[i];
      ClassPackagingData pi = computeBySTEP(step, firstExactClassLoader);
      step.setClassPackagingData(pi);
    }
  }

  private ClassPackagingData computeBySTEP(StackTraceElementProxy step,
                                           ClassLoader lastExactClassLoader) {
    String className = step.ste.getClassName();
    ClassPackagingData cpd = cache.get(className);
    if (cpd != null) {
      return cpd;
    }
    Class<?> type = bestEffortLoadClass(lastExactClassLoader, className);
    String version = getImplementationVersion(type);
    String codeLocation = getCodeLocation(type);
    cpd = new ClassPackagingData(codeLocation, version, false);
    cache.put(className, cpd);
    return cpd;
  }

  private String getImplementationVersion(Class<?> type) {
    if (type == null) {
      return "na";
    }
    Package aPackage = type.getPackage();
    if (aPackage != null) {
      String v = aPackage.getImplementationVersion();
      if (v == null) {
        return "na";
      } else {
        return v;
      }
    }
    return "na";

  }

  private String getCodeLocation(Class<?> type) {
    try {
      if (type != null) {
        // file:/C:/java/maven-2.0.8/repo/com/icegreen/greenmail/1.3/greenmail-1.3.jar
        CodeSource codeSource = type.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
          URL resource = codeSource.getLocation();
          if (resource != null) {
            String locationStr = resource.toString();
            // now lets remove all but the file name
            String result = getCodeLocation(locationStr, '/');
            if (result != null) {
              return result;
            }
            return getCodeLocation(locationStr, '\\');
          }
        }
      }
    } catch (Exception e) {
      // ignore
    }
    return "na";
  }

  private String getCodeLocation(String locationStr, char separator) {
    int idx = locationStr.lastIndexOf(separator);
    if (isFolder(idx, locationStr)) {
      idx = locationStr.lastIndexOf(separator, idx - 1);
      return locationStr.substring(idx + 1);
    } else if (idx > 0) {
      return locationStr.substring(idx + 1);
    }
    return null;
  }

  private boolean isFolder(int idx, String text) {
    return (idx != -1 && idx + 1 == text.length());
  }

  private Class<?> loadClass(ClassLoader cl, String className) {
    if (cl == null) {
      return null;
    }
    try {
      return cl.loadClass(className);
    } catch (ClassNotFoundException e1) {
      return null;
    } catch (NoClassDefFoundError e1) {
      return null;
    } catch (Exception e) {
      e.printStackTrace(); // this is unexpected
      return null;
    }

  }

  /**
   * @param lastGuaranteedClassLoader may be null
   * @param className
   * @return
   */
  private Class<?> bestEffortLoadClass(ClassLoader lastGuaranteedClassLoader,
                                    String className) {
    Class<?> result = loadClass(lastGuaranteedClassLoader, className);
    if (result != null) {
      return result;
    }
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    if (tccl != lastGuaranteedClassLoader) {
      result = loadClass(tccl, className);
    }
    if (result != null) {
      return result;
    }

    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e1) {
      return null;
    } catch (NoClassDefFoundError e1) {
      return null;
    } catch (Exception e) {
      e.printStackTrace(); // this is unexpected
      return null;
    }
  }

}
