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
package ch.autumo.beetroot;

/**
 * Entity for transient obejcts.
 */
public abstract class TransientEntity implements Entity {

	private static final long serialVersionUID = 1L;
	
	@Override
	public int getId() {
		return -1;
	}

	@Override
	public void setId(int id) {
	}

}
