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
 * Handler that has no columns configuration.
 */
public abstract class NoConfigHandler extends BaseHandler {

	public NoConfigHandler(String entity) {
		super(entity);
	}
	
	@Override
	protected final boolean hasNoColumnsConfig() {
		return true;
	}
	
}
