/**
 * Generated by PLANT - beetRoot CRUD Generator.
 */
package planted.beetroot.handler.##entitynameplural##;

import ch.autumo.beetroot.handler.DefaultEditHandler;

/**
 * ##Entitynameplural## edit handler. 
 */
public class ##Entitynameplural##EditHandler extends DefaultEditHandler {
	
	public ##Entitynameplural##EditHandler(String entity) {
		super(entity);
	}

	public ##Entitynameplural##EditHandler(String entity, String errMsg) {
		super(entity, errMsg);
	}
	
	@Override
	public Class<?> getRedirectHandler() {
		return ##Entitynameplural##IndexHandler.class;
	}

	@Override
	public Class<?> getBeanClass() {
		return ##Entityname##.class;
	}
	
}