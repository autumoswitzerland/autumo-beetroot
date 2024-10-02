/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
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
 * 
 */
package ch.autumo.beetroot.plant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Field-set does some typical ordering for fields shown in an UI.
 */
public class FieldSet<K> implements Set<K> {
	
    // Define the priority list with the desired order
    final List<String> priorityList = Arrays.asList("id", "name", "description", "type", "category", "created", "modified");
	
    private final Set<K> keySet;

    public FieldSet(Set<K> keySet) {
        this.keySet = keySet;
    }

    @Override
    public Iterator<K> iterator() {
        // Create a list of keys to be sorted
        List<K> sortedKeys = new ArrayList<>(keySet);

        // Sort using a custom comparator with a generic priority list and placeholder for other fields
        Collections.sort(sortedKeys, new Comparator<K>() {
            @Override
            public int compare(K o1, K o2) {
                String s1 = o1.toString();
                String s2 = o2.toString();

                // Check the index of both keys in the priority list
                int index1 = priorityList.indexOf(s1);
                int index2 = priorityList.indexOf(s2);

                // If both are in the priority list, compare by their order in the list
                if (index1 != -1 && index2 != -1) {
                    return Integer.compare(index1, index2);
                }

                // If only one is in the priority list, determine its position
                if (index1 != -1) {
                    // If s1 is "created" or "modified", any non-predefined field should come before them
                    if (index1 >= priorityList.indexOf("created")) {
                        return 1; // s1 should come after any non-predefined field
                    }
                    return -1; // s1 should come before s2
                }
                if (index2 != -1) {
                    // If s2 is "created" or "modified", any non-predefined field should come before them
                    if (index2 >= priorityList.indexOf("created")) {
                        return -1; // s2 should come after any non-predefined field
                    }
                    return 1; // s2 should come before s1
                }

                // If both are non-predefined fields, sort them alphanumerically
                return s1.compareTo(s2);
            }
        });

        // Return an iterator over the sorted keys
        return sortedKeys.iterator();
    }
    
    // Delegate all other Set methods to the underlying keySet
    @Override public int size() { return keySet.size(); }
    @Override public boolean isEmpty() { return keySet.isEmpty(); }
    @Override public boolean contains(Object o) { return keySet.contains(o); }
    @Override public Object[] toArray() { return keySet.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return keySet.toArray(a); }
    @Override public boolean add(K k) { throw new UnsupportedOperationException(); } // keySet is read-only
    @Override public boolean remove(Object o) { return keySet.remove(o); }
    @Override public boolean containsAll(Collection<?> c) { return keySet.containsAll(c); }
    @Override public boolean addAll(Collection<? extends K> c) { throw new UnsupportedOperationException(); } // read-only
    @Override public boolean retainAll(Collection<?> c) { return keySet.retainAll(c); }
    @Override public boolean removeAll(Collection<?> c) { return keySet.removeAll(c); }
    @Override public void clear() { keySet.clear(); }
    
}
