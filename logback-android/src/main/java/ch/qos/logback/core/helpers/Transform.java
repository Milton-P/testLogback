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
package ch.qos.logback.core.helpers;

import java.util.regex.Pattern;

/**
 * Utility class for transforming strings.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @author Michael A. McAngus
 */
public class Transform {
  private static final String CDATA_START = "<![CDATA[";
  private static final String CDATA_END = "]]>";
  private static final String CDATA_PSEUDO_END = "]]&gt;";
  private static final String CDATA_EMBEDED_END = CDATA_END + CDATA_PSEUDO_END
      + CDATA_START;
  private static final int CDATA_END_LEN = CDATA_END.length();
  private static final Pattern UNSAFE_XML_CHARS =
          Pattern.compile("[\u0000-\u0008\u000b\u000c\u000e-\u001f<>&'\"]");

  /**
   * This method takes a string which may contain HTML tags (ie, &lt;b&gt;,
   * &lt;table&gt;, etc) and replaces any '&lt;','&gt;' ... characters with
   * respective predefined entity references.
   * 
   * @param input
   *          The text to be converted.
   * @return the string with the tags escaped
   */
  public static String escapeTags(final String input) {
    if (input == null || input.length() == 0 || !UNSAFE_XML_CHARS.matcher(input).find()) {
      return input;
    }

    StringBuffer buf = new StringBuffer(input);
    return escapeTags(buf);
  }
  

  /**
   * This method takes a StringBuilder which may contain HTML tags (ie, &lt;b&gt;,
   * &lt;table&gt;, etc) and replaces any '&lt;' and '&gt;' characters with
   * respective predefined entity references.
   * @param buf StringBuffer to transform
   * @return the string with the tags escaped
   */
  public static String escapeTags(final StringBuffer buf) {
    for (int i = 0; i < buf.length(); i++) {
      char ch = buf.charAt(i);
      switch (ch) {
        case '\t':
        case '\n':
        case '\r':
          // These characters are below '\u0020' but are allowed:
          break;
        case '&':
          buf.replace(i, i + 1, "&amp;");
          break;
        case '<':
          buf.replace(i, i + 1, "&lt;");
          break;
        case '>':
          buf.replace(i, i + 1, "&gt;");
          break;
        case '"':
          buf.replace(i, i + 1, "&quot;");
          break;
        case '\'':
          buf.replace(i, i + 1, "&#39;");
          break;
        default:
          if (ch < '\u0020') {
            // These characters are not allowed,
            // replace them with "Object replacement character":
            buf.replace(i, i + 1, "\uFFFD");
          }
          break;
      }
    }
    return buf.toString();
  }
  

  /**
   * Ensures that embedded CDEnd strings (]]&gt;) are handled properly within
   * message, NDC and throwable tag text.
   * 
   * @param output
   *          Writer. The initial CDSutart (&lt;![CDATA[) and final CDEnd (]]&gt;) of
   *          the CDATA section are the responsibility of the calling method.
   * 
   * @param str
   *          The String that is inserted into an existing CDATA Section.
   */
  public static void appendEscapingCDATA(StringBuilder output, String str) {
    if (str == null) {
      return;
    }

    int end = str.indexOf(CDATA_END);

    if (end < 0) {
      output.append(str);

      return;
    }

    int start = 0;

    while (end > -1) {
      output.append(str.substring(start, end));
      output.append(CDATA_EMBEDED_END);
      start = end + CDATA_END_LEN;

      if (start < str.length()) {
        end = str.indexOf(CDATA_END, start);
      } else {
        return;
      }
    }

    output.append(str.substring(start));
  }
}
