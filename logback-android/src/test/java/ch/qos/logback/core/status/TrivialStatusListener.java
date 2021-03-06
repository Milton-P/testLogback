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
package ch.qos.logback.core.status;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

public class TrivialStatusListener implements StatusListener, LifeCycle {

  public List<Status> list = new ArrayList<Status>();
  boolean start = false;

  public void addStatusEvent(Status status) {
    if(!isStarted())
      return;
    list.add(status);
  }

  public void start() {
    start = true;
  }

  public void stop() {
    start = false;
  }

  public boolean isStarted() {
    return start;
  }
}
