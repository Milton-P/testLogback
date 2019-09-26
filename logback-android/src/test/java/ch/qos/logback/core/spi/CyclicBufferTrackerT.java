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
import ch.qos.logback.core.helpers.CyclicBuffer;

import java.util.*;

/**
 * Another tracker implementtion for testing purposes only.
 *
 * @author Ceki G&uuml;c&uuml;
 */
public class CyclicBufferTrackerT<E> implements ComponentTracker<CyclicBuffer<E>> {

  int bufferSize = CyclicBufferTracker.DEFAULT_BUFFER_SIZE;
  int maxComponents = CyclicBufferTracker.DEFAULT_NUMBER_OF_BUFFERS;

  List<TEntry<E>> liveList = new LinkedList<TEntry<E>>();
  List<TEntry<E>> lingererList = new LinkedList<TEntry<E>>();

  long lastCheck = 0;


  private TEntry<E> getEntry(List<TEntry<E>> list,String k) {
    for (int i = 0; i < list.size(); i++) {
      TEntry<E> te = list.get(i);
      if (te.key.equals(k)) {
        return te;
      }
    }
    return null;
  }

  private TEntry<E> getFromEitherList(String key) {
    TEntry<E> entry = getEntry(liveList, key);
    if(entry != null)
      return entry;
    else {
      return getEntry(lingererList, key);
    }
  }

  private List<String> keysAsOrderedList(List<TEntry<E>> list) {
    Collections.sort(list);
    List<String> result = new LinkedList<String>();
    for (int i = 0; i < list.size(); i++) {
      TEntry<E> te = list.get(i);
      result.add(te.key);
    }
    return result;
  }

  List<String> liveKeysAsOrderedList() {
    return keysAsOrderedList(liveList);
  }
  List<String> lingererKeysAsOrderedList() {
    return keysAsOrderedList(lingererList);
  }


  public Set<String> allKeys() {
    HashSet<String> allKeys = new HashSet<String>();
    for (TEntry<E> e : liveList)
      allKeys.add(e.key);
    for (TEntry<E> e : lingererList)
      allKeys.add(e.key);
    return allKeys;
  }


  public Collection<CyclicBuffer<E>> allComponents() {
    List<CyclicBuffer<E>> allComponents = new ArrayList<CyclicBuffer<E>>();
    for (TEntry<E> e : liveList)
      allComponents.add(e.value);
    for (TEntry<E> e : lingererList)
      allComponents.add(e.value);

    return allComponents;
  }

  public CyclicBuffer<E> find(String key) {
    TEntry<E> te = getFromEitherList(key);
    if(te == null) return  null;
    else return te.value;
  }

  public CyclicBuffer<E> getOrCreate(String key, long timestamp) {
    TEntry<E> te = getFromEitherList(key);
    if (te == null) {
      CyclicBuffer<E> cb = new CyclicBuffer<E>(bufferSize);
      te = new TEntry<E>(key, cb, timestamp);
      liveList.add(te);
      if (liveList.size() > maxComponents) {
        Collections.sort(liveList);
        liveList.remove(0);
      }
    } else {
      te.timestamp = timestamp;
      Collections.sort(liveList);
    }
    return te.value;
  }

  public void endOfLife(String k) {
    TEntry<E> te = null;
    boolean found = false;
    for (int i = 0; i < liveList.size(); i++) {
      te = liveList.get(i);
      if (te.key.equals(k)) {
        liveList.remove(i);
        found = true;
        break;
      }
    }
    if(found) {
      lingererList.add(te);
    }
  }

  private boolean isEntryStale(TEntry<E> entry, long now) {
    return ((entry.timestamp + DEFAULT_TIMEOUT) < now);
  }
  private boolean isEntryDoneLingering(TEntry<E> tEntry, long now) {
    return ((tEntry.timestamp + AbstractComponentTracker.LINGERING_TIMEOUT) < now);
  }

  public void removeStaleComponents(long now) {
    if (isTooSoonForRemovalIteration(now)) return;
    // both list should be sorted before removal attempts
    Collections.sort(liveList);
    Collections.sort(lingererList);
    removeComponentsInExcessFromMainList();
    removeStaleComponentsFromMainList(now);
    removeStaleComponentsFromLingerersList(now);
  }

  private void removeComponentsInExcessFromMainList() {
    while (liveList.size() > maxComponents) {
      liveList.remove(0);
    }
  }

  private void removeStaleComponentsFromMainList(long now) {
    while (liveList.size() != 0 && isEntryStale(liveList.get(0), now)) {
      liveList.remove(0);
    }
  }

  private void removeStaleComponentsFromLingerersList(long now) {
    while (lingererList.size() != 0 && isEntryDoneLingering(lingererList.get(0), now)) {
      lingererList.remove(0);
    }
  }

  private boolean isTooSoonForRemovalIteration(long now) {
    if (lastCheck + CoreConstants.MILLIS_IN_ONE_SECOND > now) {
      return true;
    }
    lastCheck = now;
    return false;
  }

  public int getComponentCount() {
    return liveList.size() + lingererList.size();
  }


  // ==================================================================

  private class TEntry<X> implements Comparable<TEntry<?>> {

    String key;
    CyclicBuffer<E> value;
    long timestamp;

    TEntry(String k, CyclicBuffer<E> v, long timestamp) {
      this.key = k;
      this.value = v;
      this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      return result;
    }

    public int compareTo(TEntry<?> o) {
      if (!(o instanceof TEntry)) {
        throw new IllegalArgumentException("arguments must be of type " + TEntry.class);
      }

      TEntry<?> other = (TEntry<?>) o;
      if (timestamp > other.timestamp) {
        return 1;
      }
      if (timestamp == other.timestamp) {
        return 0;
      }
      return -1;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      @SuppressWarnings("unchecked")
      final TEntry<?> other = (TEntry<?>) obj;
      if (key == null) {
        if (other.key != null)
          return false;
      } else if (!key.equals(other.key))
        return false;
      if (value == null) {
        if (other.value != null)
          return false;
      } else if (!value.equals(other.value))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "(" + key + ", " + value + ")";
    }
  }
}
