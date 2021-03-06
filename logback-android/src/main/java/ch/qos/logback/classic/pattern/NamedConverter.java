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
package ch.qos.logback.classic.pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;

public abstract class NamedConverter extends ClassicConverter {

  Abbreviator abbreviator = null;

  /**
   * Gets fully qualified name from event.
   * 
   * @param event
   *          The LoggingEvent to process, cannot not be null.
   * @return name, must not be null.
   */
  protected abstract String getFullyQualifiedName(final ILoggingEvent event);

  public void start() {
    String optStr = getFirstOption();
    if (optStr != null) {
      try {
        int targetLen = Integer.parseInt(optStr);
        if (targetLen == 0) {
          abbreviator = new ClassNameOnlyAbbreviator();
        } else if (targetLen > 0) {
          abbreviator = new TargetLengthBasedClassNameAbbreviator(targetLen);
        }
      } catch (NumberFormatException nfe) {
        // FIXME: better error reporting
      }
    }
  }

  public String convert(ILoggingEvent event) {
    String fqn = getFullyQualifiedName(event);

    if (abbreviator == null) {
      return fqn;
    } else {
      return abbreviator.abbreviate(fqn);
    }
  }
}
