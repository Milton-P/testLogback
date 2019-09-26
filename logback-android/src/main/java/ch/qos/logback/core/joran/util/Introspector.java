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
package ch.qos.logback.core.joran.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Introspector class provides a standard way for tools to learn about the
 * properties, events, and methods supported by a target class.
 * 
 * @author Anthony K. Trinh
 */
public class Introspector {

  /**
   * Converts a name string's first letter to lowercase
   * 
   * @param name
   *          name string to evaluate
   * @return the name with its first letter in lowercase
   */
  static public String decapitalize(String name) {
    if (name == null || name.length() == 0) {
      return name;
    } else {
      String nm = name.substring(0, 1).toLowerCase(Locale.US);
      if (name.length() > 1) {
        nm += name.substring(1);
      }
      return nm;
    }
  }

  /**
   * Gets a class's method descriptors
   * 
   * @param clazz
   *          class to be evaluated
   * @return method descriptors
   */
  static public MethodDescriptor[] getMethodDescriptors(Class<?> clazz) {
    ArrayList<MethodDescriptor> methods = new ArrayList<MethodDescriptor>();
    for (Method m : clazz.getMethods()) {
      methods.add(new MethodDescriptor(m.getName(), m));
    }
    return methods.toArray(new MethodDescriptor[0]);
  }

  /**
   * Gets a class's property descriptors. All properties have methods whose name
   * begins with "set" or "get". The setters must have a single parameter and
   * getters must have none.
   * 
   * @param clazz
   *          class to be evaluated
   * @return property descriptors
   */
  static public PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
    final String SETTER_PREFIX = "set";
    final String GETTER_PREFIX = "get";
    final int LEN_PREFIX = SETTER_PREFIX.length();

    Map<String, PropertyDescriptor> map = new HashMap<String, PropertyDescriptor>();
    for (Method m : clazz.getMethods()) {
      PropertyDescriptor pd = null;
      String mName = m.getName();

      boolean isGet = mName.startsWith(GETTER_PREFIX) && (mName.length() > LEN_PREFIX);
      boolean isSet = mName.startsWith(SETTER_PREFIX) && (mName.length() > LEN_PREFIX);

      if (isGet || isSet) {
        String propName = decapitalize(mName.substring(LEN_PREFIX));
        
        pd = map.get(propName);
        if (pd == null) {
          pd = new PropertyDescriptor(propName);
          map.put(propName, pd);
        }

        Class<?>[] parmTypes = m.getParameterTypes();
        if (isSet) {
          if (parmTypes.length == 1) { // we only want the single-parm setter
            pd.setWriteMethod(m);
            pd.setPropertyType(parmTypes[0]);
          }
        } else if (isGet) {
          if (parmTypes.length == 0) { // we only want the zero-parm getter
            pd.setReadMethod(m);

            // let setter's type take priority
            if (pd.getPropertyType() == null) {
              pd.setPropertyType(m.getReturnType());
            }
          }
        }
      }
    }

    return map.values().toArray(new PropertyDescriptor[0]);
  }
}
