/**
 * Generated by PLANT - beetRoot CRUD Generator.
 */
package planted.beetroot.handler.properties;

import java.util.HashMap;
import java.util.Map;

import ch.autumo.beetroot.handler.DefaultAddHandler;

/**
 * Properties add handler. 
 */
public class PropertiesAddHandler extends DefaultAddHandler {
	
	public PropertiesAddHandler(String entity) {
		super(entity);
	}

	public PropertiesAddHandler(String entity, String errMsg) {
		super(entity, errMsg);
	}
	
	@Override
	public Class<?> getRedirectHandler() {
		return PropertiesIndexHandler.class;
	}

	@Override
	public Map<String, Object> getAddMandatoryFields() {
		
		final Map<String, Object> fields = new HashMap<String, Object>();
		
		// NOTE: Provide default values for fields that are NOT
		// nullable and that aren't present in the add GUI!
		fields.put("name", "<DEFAULT-VALUE>");
		fields.put("id", "<DEFAULT-VALUE>");

		return fields;
	}

	@Override
	public Class<?> getBeanClass() {
		return Property.class;
	}
	
}
