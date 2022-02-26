/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ch.autumo.beetroot.handler.tasks;

import java.sql.ResultSet;

import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.handler.DefaultIndexHandler;

/**
 * Tasks index handler.
 */
public class TasksIndexHandler extends DefaultIndexHandler {

	public TasksIndexHandler(String entity) {
		super(entity);
	}

	public TasksIndexHandler(String entity, String msg) {

		super(entity);
		
		super.addSuccessMessage(msg);
		super.redirectedMarker(true);
	}
	
	@Override
	public String extractSingleTableData(ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		switch (columnName) {
		
			case "name"			: return "<td>" + set.getString(columnName) + "</td>";
			case "minute"		: return "<td>" + set.getString(columnName) + "</td>";
			case "hour"			: return "<td>" + set.getString(columnName) + "</td>";
			case "dayofmonth"	: return "<td>" + set.getString(columnName) + "</td>";
			case "monthofYear"	: return "<td>" + set.getString(columnName) + "</td>";
			case "dayofWeek"	: return "<td>" + set.getString(columnName) + "</td>";
			case "active"		: return set.getBoolean(columnName) ? "<td>Yes</td>" : "<td>No</td>";
			case "laststatus"	: return set.getBoolean(columnName) ? "<td class=\"redStatus\"></td>" : "<td class=\"greenStatus\"></td>";
			case "lastexecuted"	: return set.getTimestamp(columnName) == null ? "<td></td>" : "<td>"+Utils.getGUIDate(set.getTimestamp(columnName))+"</td>";
			case "modified"		: return set.getTimestamp(columnName) == null ? "<td></td>" : "<td>"+Utils.getGUIDate(set.getTimestamp(columnName))+"</td>";
			
			default				: return "<td>" + set.getObject(columnName) + "</td>";
		}
	}

	@Override
	public Class<?> getBeanClass() {
		return Task.class;
	}
	
}
