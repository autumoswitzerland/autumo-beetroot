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
import ch.autumo.beetroot.utils.Utils;

/**
 * Properties view handler. 
 */
public class PropertiesViewHandler extends DefaultViewHandler {
	
	private Property property = null;
	
	public PropertiesViewHandler(String entity) {
		super(entity);
	}

	@Override
	public String extractSingleTableData(BeetRootHTTPSession session, ResultSet set, String columnName, int idx, Entity entity) throws Exception {
		
		// in case you want to use a bean
		property = (Property) entity;
		
		switch (columnName) {
			// Note: Return a UI presentable value for each field.
			// The class 'Utils' provides some helper methods for this.
			// PS: Customize style for <td> if necessary.
			case "created": return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "name": return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "modified": return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "id": return "<td>" + Utils.getValue(set, columnName) + "</td>";
			case "value": return "<td>" + Utils.getValue(set, columnName) + "</td>";
			default: return "<td>"+ Utils.getValue(set, columnName) +"</td>";
		}
	}

	/**
	 * This works because we only have one record in the 'view.html' template,
	 * otherwise we would have to overwrite the prepare-method. Within the 'index.html'
	 * this would be necessary (1-n records); the method used would be the following:
	 * {@link ch.autumo.beetroot.handler.DefaultIndexHandler#prepare(BeetRootHTTPSession, Entity)
	 */
	@Override
	public String replaceTemplateVariables(String text, BeetRootHTTPSession session) {
		
		if (text.contains("{$propName}"))
			text = text.replaceAll("\\{\\$propName\\}", property.getName());
		
		return text;
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
