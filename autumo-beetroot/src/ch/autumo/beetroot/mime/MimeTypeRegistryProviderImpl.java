/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package ch.autumo.beetroot.mime;

import java.io.IOException;
import java.io.InputStream;

import jakarta.activation.MimeTypeRegistry;
import jakarta.activation.spi.MimeTypeRegistryProvider;

/**
 * Mime type registry provider implementation.
 * Patched: autumo GmbH, Michael Gasche
 */
public class MimeTypeRegistryProviderImpl implements MimeTypeRegistryProvider {

	/**
	 * Default constructor
	 */
	public MimeTypeRegistryProviderImpl() {
	}

	@Override
	public MimeTypeRegistry getByFileName(String name) throws IOException {
		return new MimeTypeFile(name);
	}

	@Override
	public MimeTypeRegistry getByInputStream(InputStream inputStream) throws IOException {
		return new MimeTypeFile(inputStream);
	}

	@Override
	public MimeTypeRegistry getInMemory() {
		return new MimeTypeFile();
	}

}
