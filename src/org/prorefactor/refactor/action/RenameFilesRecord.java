/** 11-May-07 by John Green
 * Copyright (C) 2007 Joanju Software
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.action;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;


public class RenameFilesRecord implements Serializable {
	RenameFilesRecord(String from, String to, int type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}
	String from;
	String to;
	/** I use an int, rather than Java enum, because I think it's probably smaller to store. */
	int type;
	private static final long serialVersionUID = 1L;
	static final int INCLUDE_REF = 1;
	static final int RUN_SIMPLE = 2;
	static final int RUN_VALUE = 3;
	@Override
	public String toString() {
			try {
				return new JSONObject().put("from", from).put("to", to).toString();
			} catch (JSONException e) {
				return e.toString();
			}
	}
}
