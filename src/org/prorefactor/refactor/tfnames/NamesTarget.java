/* NamesTarget.java
 * Created on Oct 11, 2003
 * John Green
 *
 * Copyright (C) 2003 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.tfnames;

import org.prorefactor.refactor.RefactorTarget;

/** Refactor targets for table and table-field names which need cleanup.
 */
public class NamesTarget extends RefactorTarget {

	public boolean abbreviated = false;
	public boolean caseWrong = false;
	public boolean needsQualif = false;
	public String dbPart = null;
	public String tablePart = null;
	public String fieldPart = null;

	public NamesTarget() {
	}

	public String getFixedName() {
		String s = "";
		if (dbPart!=null) {
			s += dbPart;
			if (tablePart!=null) s += ".";
		}
		if (tablePart!=null) {
			s += tablePart;
			if (fieldPart!=null) s += ".";
		}
		if (fieldPart!=null) s += fieldPart;
		return s;
	}

}
