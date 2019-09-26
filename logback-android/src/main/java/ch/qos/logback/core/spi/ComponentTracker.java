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
package ch.qos.logback.core.spi;


import ch.qos.logback.core.CoreConstants;

import java.util.Collection;
import java.util.Set;

/**
 * Interface for tracking various components by key. Components which have not
 * been accessed for more than a user-specified duration are deemed stale and
 * removed. Components can also be explicitly marked as having reached their
 * {@link #endOfLife(String)} in which case they will linger for a few seconds
 * and then be removed.
 *
 * @author Tommy Becker
 * @author Ceki Gulcu
 * @author David Roussel
 *
 * @since 1.0.12
 */
public interface ComponentTracker<C> {

  /**
   * The default timeout duration is 30 minutes
   */
  public final long DEFAULT_TIMEOUT = 30 * 60 * CoreConstants.MILLIS_IN_ONE_SECOND; // 30 minutes

  /**
   * By default an unlimited number of elements can be tracked.
   */
  int DEFAULT_MAX_COMPONENTS = Integer.MAX_VALUE;

  /**
   * Returns the number of components tracked.
   * @return number of components
   */
  int getComponentCount();


  /**
   * Find the component identified by 'key', without updating the timestamp. Returns null if no
   * corresponding component could be found.
   *
   * @param key key of component
   * @return corresponding component, may be null
   */
  C find(String key);

  /**
   * Get the component identified by 'key', updating its timestamp in the
   * process. If the corresponding component could not be found, it is created.
   *
   * @param key key of component
   * @param timestamp timestamp in ms
   * @return the component
   */
  C getOrCreate(String key, long timestamp);


  /**
   * Remove components which are deemed stale. Components which have not
   * been accessed for more than a user-specified duration are deemed stale.
   *
   * <p>If the number of components exceeds, {@link #getComponentCount()},
   * components in excess will be removed.</p>
   *
   * <p>Depending on the component type, components will be cleared or stopped
   * (as appropriate) right before removal.</p>
   *
   * @param now  current time in milliseconds
   */
  void removeStaleComponents(long now);


  /**
   * Mark component identified by 'key' as having reached its end-of-life. End-of-lifed
   * components will linger for a few more seconds before being removed.
   *
   * @param key
   */
  void endOfLife(String key);

  /**
   * Returns the collection of all components tracked by this instance.
   * @return  collection of components
   */
  Collection<C> allComponents();


  /**
   * Set of all keys in this tracker in no particular order.
   *
   * @return the keys
   */
  Set<String> allKeys();
}
