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

/**
 * Handler that has no content output, no web resource.
 * 
 * The default index will be loaded.
 */
public abstract class NoContentHandler extends BaseHandler {

	public NoContentHandler(String entity) {
		super(entity);
	}	
	
	@Override
	protected final boolean isNoContentResponse() {
		return true;
	}
	
	@Override
	public final String getResource() {
		return null;
	}

}
