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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.status.StatusManager;

/**
 * A context is the main anchorage point of all logback components.
 *
 * @author Ceki Gulcu
 *
 */
public interface Context extends PropertyContainer {

  /**
   * Return the StatusManager instance in use.
   *
   * @return the {@link StatusManager} instance in use.
   */
  StatusManager getStatusManager();

  /**
   * A Context can act as a store for various objects used by logback
   * components.
   *
   * @param key the key of the object
   * @return The object stored under 'key'.
   */
  Object getObject(String key);

  /**
   * Store an object under 'key'. If no object can be found, null is returned.
   *
   * @param key the key of the object
   * @param value the value to associate with the key
   */
  void putObject(String key, Object value);

  /**
   * Get the property of this context.
   * @param key the key of the property
   * @return the associated string value
   */
  String getProperty(String key);

  /**
   * Set a property of this context.
   * @param key the property's key
   * @param value the value associated with the key
   */
  void putProperty(String key, String value);


  /**
   * Get a copy of the property map
   * @return the property map copy
   * @since 0.9.20
   */
  Map<String, String> getCopyOfPropertyMap();

  /**
   * Contexts are named objects.
   *
   * @return the name for this context
   */
  String getName();

  /**
   * The name of the context. This can be set only once.
   *
   * @param name the desired context name
   */
  void setName(String name);

  /**
   * The time at which this context was created, expressed in
   * millisecond elapsed since the epoch (1.1.1970).
   *
   * @return The time as measured when this class was created.
   */
  long getBirthTime();

  /**
   * Object used for synchronization purposes.
   * INTENDED FOR INTERNAL USAGE.
   * @return the configuration lock
   */
  Object getConfigurationLock();

  /**
   * Returns the ScheduledExecutorService for this context.
   * @return the ScheduledExecutorService for this context.
   * @since 1.1.7
   */
  // Apparently ScheduledThreadPoolExecutor has limitation where a task cannot be submitted from
  // within a running task. ThreadPoolExecutor does not have this limitation.
  // This causes tests failutes in SocketReceiverTest.testDispatchEventForEnabledLevel and
  // ServerSocketReceiverFunctionalTest.testLogEventFromClient.
  ScheduledExecutorService getScheduledExecutorService();

  /**
   * Every context has an ExecutorService which be invoked to execute certain
   * tasks in a separate thread.
   *
   * @return the executor for this context.
   * @since 1.0.0
   * @deprecated use {@link #getScheduledExecutorService()} instead
   */
  ExecutorService getExecutorService();

  /**
   * Register a component that participates in the context's life cycle.
   * <p>
   * All components registered via this method will be stopped and removed
   * from the context when the context is reset.
   *
   * @param component the subject component
   */
  void register(LifeCycle component);

  void addScheduledFuture(ScheduledFuture<?> scheduledFuture);
}
