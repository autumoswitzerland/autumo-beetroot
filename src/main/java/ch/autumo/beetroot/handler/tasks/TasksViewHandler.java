/**
 * 
 * Copyright (c) 2023 autumo Ltd. Switzerland, Michael Gasche
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
package ch.autumo.beetroot.handler.tasks;

import java.sql.ResultSet;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.handler.DefaultViewHandler;
import ch.autumo.beetroot.utils.common.Time;
import ch.autumo.beetroot.utils.database.DB;

/**
 * Tasks view handler. 
 */
public class TasksViewHandler extends DefaultViewHandler {
	
	public TasksViewHandler(String entity) {
		super(entity);
	}

	@Override
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		switch (columnName) {
		
			case "name"			: return "<td>"+set.getString(columnName)+"</td>";
								  
			case "minute"		: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "hour"			: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "dayofmonth"	: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "monthofyear"	: return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "dayofweek"	: return "<td>" + DB.getValue(set, columnName) + "</td>";
			
			case "active"		: return set.getBoolean(columnName) ? 
									"<td class=\"yesStatus\"></td>" : 
									"<td class=\"noStatus\"></td>";
					
			case "laststatus"	: return set.getBoolean(columnName) ? "<td class=\"greenStatus\"></td>" : "<td class=\"redStatus\"></td>";
			case "lastexecuted"	: return set.getTimestamp(columnName) == null ? "<td></td>" : "<td>"+Time.getGUIDate(set.getTimestamp(columnName))+"</td>";
			case "created"		: return "<td>" +Time.getGUIDate(set.getTimestamp(columnName))+ "</td>";
			case "modified"		: return set.getTimestamp(columnName) == null ? "<td></td>" : "<td>"+Time.getGUIDate(set.getTimestamp(columnName))+"</td>";
			
			default				: return "<td>"+DB.getValue(set, columnName)+"</td>";
		}
	}
	
	@Override
	public Class<?> getBeanClass() {
		return Task.class;
	}

}
