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

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultIndexHandler;
import ch.autumo.beetroot.utils.Utils;

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
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		switch (columnName) {
		
			case "name"			: return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "minute"		: return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "hour"			: return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "dayofmonth"	: return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "monthofyear"	: return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "dayofweek"	: return "<td>" + Utils.getValue(set, columnName) + "</td>";
			
			case "active"		: return set.getBoolean(columnName) ? "<td>Yes</td>" : "<td>No</td>";
			case "laststatus"	: return set.getBoolean(columnName) ? "<td class=\"greenStatus\"></td>" : "<td class=\"redStatus\"></td>";
			case "lastexecuted"	: return set.getTimestamp(columnName) == null ? "<td></td>" : "<td>"+Utils.getGUIDate(set.getTimestamp(columnName))+"</td>";
			case "modified"		: return set.getTimestamp(columnName) == null ? "<td></td>" : "<td>"+Utils.getGUIDate(set.getTimestamp(columnName))+"</td>";
			
			default				: return "<td>" + set.getObject(columnName) + "</td>";
		}
	}

	@Override
	public boolean deleteAllowed(Session userSession) {
		return userSession.getUserRole().equalsIgnoreCase("Administrator") ||
			userSession.getUserRole().equalsIgnoreCase("Operator");
	}

	@Override
	public boolean changeAllowed(Session userSession) {
		return userSession.getUserRole().equalsIgnoreCase("Administrator") ||
			userSession.getUserRole().equalsIgnoreCase("Operator");
	}
	
	@Override
	public Class<?> getBeanClass() {
		return Task.class;
	}
	
}
