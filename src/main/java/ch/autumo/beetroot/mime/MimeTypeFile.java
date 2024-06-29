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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.activation.MimeTypeEntry;
import jakarta.activation.MimeTypeRegistry;

/**
 * Mime type file.
 * Patched: autumo GmbH, Michael Gasche
 */
public class MimeTypeFile implements MimeTypeRegistry {

	protected final static Logger LOG = LoggerFactory.getLogger(MimeTypeFile.class.getName());
	
	private Hashtable<String, MimeTypeEntry> mimeTypeMap = new Hashtable<String, MimeTypeEntry>();

	/**
	 * Construct with mime types file ("mime.types" usually).
	 *
	 * @param mimeTypesfile The file name of the mime types file.
	 * @throws IOException IO exception
	 */
	public MimeTypeFile(String mimeTypesfile) throws IOException {
		
		File mimeFile = new File(mimeTypesfile);
		FileReader fr = new FileReader(mimeFile);
		this.parse(new BufferedReader(fr));
	}

	/**
	 * Construct with mime types input stream ( from"mime.types" usually).
	 * 
	 * @param is input stream
	 * @throws IOException IO exception
	 */
	public MimeTypeFile(InputStream is) throws IOException {
		parse(new BufferedReader(new InputStreamReader(is, "iso-8859-1")));
	}

	/**
	 * Creates an empty mime types registry.
	 */
	public MimeTypeFile() {
	}

	/**
	 * Get a mime type entry based on the file extension.
	 *
	 * @param ext the file extension
	 * @return one mime type entry
	 */
	@Override
	public MimeTypeEntry getMimeTypeEntry(String ext) {
		return (MimeTypeEntry) mimeTypeMap.get(ext);
	}

	/**
	 * Get a mime type entry based on the file extension.
	 *
	 * @param ext the file extension
	 * @return the MIME type string
	 */
	@Override
	public String getMIMETypeString(String ext) {
		MimeTypeEntry entry = this.getMimeTypeEntry(ext);
		if (entry != null)
			return entry.getMIMEType();
		else
			return null;
	}

	/**
	 * Appends string of entries to the types registry, must be valid mime.types
	 * format. A mime.types entry is one of two forms
	 *
	 * type/subtype ext1 ext2 ... or type=type/subtype desc="description of type"
	 * exts=ext1,ext2,...
	 *
	 * Example # this is a test audio/basic au text/plain txt text
	 * type=application/postscript exts=ps,eps
	 *
	 * @param mime_types the mime.types string
	 */
	@Override
	public void appendToRegistry(String mime_types) {
		try {
			parse(new BufferedReader(new StringReader(mime_types)));
		} catch (IOException ex) {
			// Well...
		}
	}

	/**
	 * Parse a stream of mime.types entries.
	 */
	private void parse(BufferedReader buf_reader) throws IOException {
		String line = null, prev = null;

		while ((line = buf_reader.readLine()) != null) {
			if (prev == null)
				prev = line;
			else
				prev += line;
			int end = prev.length();
			if (prev.length() > 0 && prev.charAt(end - 1) == '\\') {
				prev = prev.substring(0, end - 1);
				continue;
			}
			this.parseEntry(prev);
			prev = null;
		}
		if (prev != null)
			this.parseEntry(prev);
	}

	/**
	 * Parse single mime.types entry.
	 */
	private void parseEntry(String line) {
		
		String mimeType = null;
		String ext = null;
		line = line.trim();

		if (line.length() == 0) // empty line...
			return; // BAIL!

		// check to see if this is a comment line?
		if (line.charAt(0) == '#')
			return; // then we are done!

		// is it a new format line or old format?
		if (line.indexOf('=') > 0) {
			// new format
			LineTokenizer lt = new LineTokenizer(line);
			
			while (lt.hasMoreTokens()) {
				
				String name = lt.nextToken();
				String value = null;
				
				if (lt.hasMoreTokens() && lt.nextToken().equals("=") && lt.hasMoreTokens())
					value = lt.nextToken();
				
				if (value == null) {
					LOG.debug("Bad mime.types entry: " + line);
					return;
				}
				
				if (name.equals("type"))
					mimeType = value;
				
				else if (name.equals("exts")) {
					
					StringTokenizer st = new StringTokenizer(value, ",");
					while (st.hasMoreTokens()) {
						ext = st.nextToken();
						MimeTypeEntry entry = new MimeTypeEntry(mimeType, ext);
						this.mimeTypeMap.put(ext, entry);
						LOG.trace("Added: " + entry.toString());
					}
				}
			}
			
		} else {
			
			// old format, smaller and without descriptions and '=' assignments
			final StringTokenizer strtok = new StringTokenizer(line);
			int num_tok = strtok.countTokens();

			if (num_tok == 0) // empty line
				return;

			mimeType = strtok.nextToken(); // get the MIME type

			while (strtok.hasMoreTokens()) {
				
				MimeTypeEntry entry = null;

				ext = strtok.nextToken();
				entry = new MimeTypeEntry(mimeType, ext);
				this.mimeTypeMap.put(ext, entry);

				LOG.trace("Added: " + entry.toString());
			}
		}
	}

	/*
	 * public static void main(String[] args) throws Exception {
	 * 
	 * 		MimeTypeFile mf = new MimeTypeFile(argv[0]);
	 * 		System.out.println("ext " + args[1] + " type " + mf.getMIMETypeString(args[1]));
	 *		System.exit(0); 
	 * }
	 */
}

/**
 * Used for newer, longer format
 */
class LineTokenizer {
	
	private int currentPosition;
	private int maxPosition;
	private String str;
	private Vector<String> stack = new Vector<String>();
	private static final String singles = "="; // single character tokens

	/**
	 * Constructs a tokenizer for the specified string.
	 * <p>
	 *
	 * @param str a string to be parsed.
	 */
	public LineTokenizer(String str) {
		currentPosition = 0;
		this.str = str;
		maxPosition = str.length();
	}

	/**
	 * Skips white space.
	 */
	private void skipWhiteSpace() {
		while ((currentPosition < maxPosition) && Character.isWhitespace(str.charAt(currentPosition))) {
			currentPosition++;
		}
	}

	/**
	 * Tests if there are more tokens available from this tokenizer's string.
	 *
	 * @return <code>true</code> if there are more tokens available from this
	 *         tokenizer's string; <code>false</code> otherwise.
	 */
	public boolean hasMoreTokens() {
		if (stack.size() > 0)
			return true;
		skipWhiteSpace();
		return (currentPosition < maxPosition);
	}

	/**
	 * Returns the next token from this tokenizer.
	 *
	 * @return the next token from this tokenizer.
	 * @exception NoSuchElementException if there are no more tokens in this
	 *                                   tokenizer's string.
	 */
	public String nextToken() {
		int size = stack.size();
		if (size > 0) {
			String t = (String) stack.elementAt(size - 1);
			stack.removeElementAt(size - 1);
			return t;
		}
		skipWhiteSpace();

		if (currentPosition >= maxPosition) {
			throw new NoSuchElementException();
		}

		int start = currentPosition;
		char c = str.charAt(start);
		if (c == '"') {
			currentPosition++;
			boolean filter = false;
			while (currentPosition < maxPosition) {
				c = str.charAt(currentPosition++);
				if (c == '\\') {
					currentPosition++;
					filter = true;
				} else if (c == '"') {
					String s;

					if (filter) {
						StringBuffer sb = new StringBuffer();
						for (int i = start + 1; i < currentPosition - 1; i++) {
							c = str.charAt(i);
							if (c != '\\')
								sb.append(c);
						}
						s = sb.toString();
					} else
						s = str.substring(start + 1, currentPosition - 1);
					return s;
				}
			}
		} else if (singles.indexOf(c) >= 0) {
			currentPosition++;
		} else {
			while ((currentPosition < maxPosition) && singles.indexOf(str.charAt(currentPosition)) < 0
					&& !Character.isWhitespace(str.charAt(currentPosition))) {
				currentPosition++;
			}
		}
		return str.substring(start, currentPosition);
	}

	public void pushToken(String token) {
		stack.addElement(token);
	}
	
}
