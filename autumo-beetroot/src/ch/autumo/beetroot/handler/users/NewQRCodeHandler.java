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
package ch.autumo.beetroot.handler.users;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.handler.HandlerResponse;

/**
 * New QR Code handler.
 */
public class NewQRCodeHandler extends BaseHandler {

	private int uid = -1;
	
	public NewQRCodeHandler(String entity) {
		this.entity = entity;
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final String newSecretUserKey = Utils.createSecretUserKey();
		Utils.updateSecretUserKey(id, newSecretUserKey);
		
		uid = id;
		
		return null;
	}
	
	@Override
	protected String isNoContentResponseButRoute(Session userSession) {
		return "users/view?id="+userSession.getModifyId(uid, entity);
	}
	
	@Override
	public String getResource() {
		// since no output is created, no resource is necessary
		return null;
	}

	@Override
	public String getEntity() {
		return this.entity;
	}

}
