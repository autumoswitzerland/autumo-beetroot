/**
 * Generated by PLANT - beetRoot CRUD Generator.
 */
package planted.beetroot.handler.properties;

import java.sql.ResultSet;

//import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultViewHandler;
import ch.autumo.beetroot.utils.DB;

/**
 * Properties view handler. 
 */
public class PropertiesViewHandler extends DefaultViewHandler {
	
	public PropertiesViewHandler(String entity) {
		super(entity);
	}

	@Override
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		// in case you want to use a bean
		final Property property = (Property) entity;
		
		switch (columnName) {
			// Note: Return a UI presentable value for each field.
			// The class 'DB' provides some helper methods for this.
			// PS: Customize style for <td> if necessary.
			case "created": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "name": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "modified": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "id": return "<td>" + DB.getValue(set, columnName) + "</td>";
			case "value": return "<td>" + DB.getValue(set, columnName) + "</td>";
			default: return "<td>"+ DB.getValue(set, columnName) +"</td>";
		}
	}

	@Override
	public Class<?> getBeanClass() {
		return Property.class;
	}

	@Override
	public boolean hasAccess(Session userSession) {
		return userSession.getUserRole().equalsIgnoreCase("Administrator");
	}

	@Override
	public String getTitle(Session userSession) {
		return "Settings";
	}
	
}
