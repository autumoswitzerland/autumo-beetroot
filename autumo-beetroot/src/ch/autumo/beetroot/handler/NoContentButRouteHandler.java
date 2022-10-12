/**
 * Copyright 2022 autumo GmbH, Michael Gasche.
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains
 * the property of autumo GmbH The intellectual and technical
 * concepts contained herein are proprietary to autumo GmbH
 * and are protected by trade secret or copyright law.
 * 
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from autumo GmbH.
 * 
 */
package ch.autumo.beetroot.handler;

import ch.autumo.beetroot.Session;

/**
 * Handler that has no content output, no web resource.
 * 
 * The index will be loaded as specified.
 */
public abstract class NoContentButRouteHandler extends BaseHandler {

	public NoContentButRouteHandler(String entity) {
		super(entity);
	}	
		
	@Override
	protected abstract String isNoContentResponseButRoute(Session userSession);
	
	@Override
	public final String getResource() {
		return null;
	}

}