/* TFNamesRefactor.java
 * Created on Oct 10, 2003
 * John Green
 *
 * Copyright (C) 2003-2004 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.tfnames;

import org.prorefactor.core.JPUtil;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.refactor.IRefactor;
import org.prorefactor.refactor.Refactor;
import org.prorefactor.refactor.RefactorTarget;

import com.joanju.ProparseLdr;




/** Table and Field Names Refactor.
 * An instance of this class stores the settings for the
 * refactoring, and also contains the "run" function which
 * applies the changes for a given Target to a given file scan.
 * Can be called/implemented via org.prorefactor.refactor.TempDirWrap.
 */
public class TFNamesRefactor implements IRefactor {

	public TFNamesRefactor() {}

	/** Refactor settings - use lowercase names? */
	public boolean useLowercase = true;
	/** Refactor settings - qualify unqualifed names? */
	public boolean qualify = true;
	/** Refactor settings - prefix names with DB qualifier? (Not currently used) */
	public boolean useDbQualifier = false;
	/** Refactor settings - unabbreviate abbreviated names? */
	public boolean unabbreviate = true;
	/** Refactor settings - fix case if case is wrong? */
	public boolean fixCase = true;
	/** Refactor settings - apply changes to work and temp table names? */
	public boolean workTempTables = true;

	private JPUtil plus = JPUtil.getInstance();
	private ProparseLdr parser = ProparseLdr.getInstance();
	private Refactor refactor = Refactor.getInstance();



	/**
	 * Run this refactor for a given target.
	 * @param tar The NamesTarget object which contains details
	 * about the node which needs refactoring.
	 * @param scanNum The scan number
	 * @return positive int on success, or a negative int on fail:
	 *  2: Wrong target type - nothing done.
	 *  1: Successful refactoring done.
	 * -1: Failed to synchronize, possibly due to preprocessing or escape sequences in source.
	 * -2: Something weird happened.
	 */
	public int run(RefactorTarget tar, int scanNum) {
		if (!(tar instanceof NamesTarget)) return 2;
		NamesTarget target = (NamesTarget)tar;
		int nameNode = parser.getHandle();
		int scanHandle = parser.getHandle();
		parser.parseGetTop(scanNum, scanHandle);
		try {

			// nameNode
			// For a Field_ref node, the field name is in a child ID node... fetch it
			// Otherwise it's just the current (RECORD_NAME) node
			if (parser.getNodeTypeI(target.nodeHandle)==TokenTypes.Field_ref)
				plus.findFieldRefIdNode(target.nodeHandle, nameNode);
			else
				parser.copyHandle(target.nodeHandle, nameNode);

			// Synchronize
			if (refactor.nsync(nameNode, scanHandle) == 0) return -1;

			parser.setNodeText(scanHandle, target.getFixedName());

		} finally {
			parser.releaseHandle(nameNode);
			parser.releaseHandle(scanHandle);
		}
		return 1;
	} // run



	/**
	 * Return a string representation of the settings stored in this Refactor object.
	 */
	public String toString() {
		String ret = super.toString();
		ret += "\nuseLowercase=" + useLowercase
			+ "\nqualify=" + qualify
			+ "\nuseDbQualifier=" + useDbQualifier
			+ "\nunabbreviate=" + unabbreviate
			+ "\nfixCase=" + fixCase
			+ "\nworkTempTables=" + workTempTables
			;
		return ret;
	}



}
