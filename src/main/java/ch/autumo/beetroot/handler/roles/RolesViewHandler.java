/**
 * Generated by PLANT - beetRoot CRUD Generator.
 */
package ch.autumo.beetroot.handler.roles;

import java.sql.ResultSet;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultViewHandler;
import ch.autumo.beetroot.utils.DB;

/**
 * Roles view handler. 
 */
public class RolesViewHandler extends DefaultViewHandler {
	
	public RolesViewHandler(String entity) {
		super(entity);
	}

	@Override
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		// in case you want to use a bean
		//final Role role = (Role) entity;

		switch (columnName) {
			// Note: Return a UI presentable value for each field.
			// The class 'DB' provides some helper methods for this.
			// PS: Customize style for <td> if necessary.
			case "permissions": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "created": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "name": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "description": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "modified": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "id": return "<td>" + DB.getValue(set, columnName) + "</td>";
			default: return "<td>" + set.getObject(columnName) + "</td>";
		}
	}

	@Override
	public boolean hasAccess(Session userSession) {
		return userSession.getUserRoles().contains("Administrator") ||
				userSession.getUserRoles().contains("Operator");
	}
	
	@Override
	public Class<?> getBeanClass() {
		return Role.class;
	}
	
}
