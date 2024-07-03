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
package ch.autumo.beetroot.utils;

import java.util.ArrayList;
import java.util.List;


/**
 * Special list for roles and permissions that ignores
 * the case.
 */
public final class LowerCaseList extends ArrayList<String> {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Ignore case list
	 * @param initialCapacity initial capacity
	 */
    public LowerCaseList(int initialCapacity) {
    	super(initialCapacity);
    }
    
	@Override
    public boolean contains(Object o) {
        return indexOf(o.toString().toLowerCase()) >= 0;
    }

	/**
	 * Create a lower case list out of array.
	 * 
	 * @param array string elements
	 * @return lower case list
	 */
	public static List<String> asList(String array[]) {
		final LowerCaseList list = new LowerCaseList(array.length);
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
		return list;
	}
	
}
