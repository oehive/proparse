/**
 * NoundoRefactor.java
 * @author John Green
 * 17-Jul-2003
 * www.joanju.com
 * 
 * Copyright (c) 2003 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.noundo;


import org.prorefactor.core.*;
import org.prorefactor.refactor.*;
import org.prorefactor.treeparserbase.JPTreeParserTokenTypes;

import com.joanju.ProparseLdr;



/**
 * Given a DEFINE statement that is missing NO-UNDO, we add the NO-UNDO
 * just before the statement close (period or colon).
 */
public class NoundoRefactor {


	/**
	 * Constructor - no args.
	 */
	public NoundoRefactor() {
		parser = ProparseLdr.getInstance();
		plus1 = JPUtil.getInstance();
		refactor = Refactor.getInstance();
		nuText = 
			RefactorSession.getInstance().getProparseSettings().capKeyword
			? "NO-UNDO" : "no-undo";
	}


	/**
	 * "NO-UNDO" or "no-undo", based on ProparseProjectSettings.capKeyword.
	 */
	public String nuText = "NO-UNDO";


	private HandleCollection handler = new HandleCollection();
	private ProparseLdr parser;
	private JPUtil plus1;
	private Refactor refactor;
	private int scanNum;
	private NoundoTarget target;


	/**
	 * Run this refactor for a given handle.
	 * @param target The NoundoTarget object which contains details
	 * about the node which needs refactoring.
	 * @return 1 on success, or a negative number on fail:
	 * 3: Temp or work table target - not processed.
	 * 1: success.
	 * -1: Failed to synchronize, possibly due to preprocessing or escape sequences in source.
	 * -2: Something weird happened. Review the DEFINE statement manually.
	 */
	public int run(NoundoTarget inTarget, int inScanNum) {
		try {
			target = inTarget;
			scanNum = inScanNum;
			String defType = new String(parser.attrGetS(target.nodeHandle, "state2"));
			if (defType.equals("VARIABLE") || defType.equals("PARAMETER")) {
				return addAtDot();
			} else if (defType.equals("TEMPTABLE") || defType.equals("WORKTABLE")) {
				return 3;
			} else
				return -2;
		} finally {
			handler.releaseAll();
		}
	} // run



	// Add NO-UNDO at the closing dot (or colon) for a variable or parameter definition.
	private int addAtDot() {
		int dotHandle = handler.gimme();
		if (plus1.lastChild(target.nodeHandle, dotHandle) == 0) return -2;
		if (	parser.getNodeTypeI(dotHandle) != JPTreeParserTokenTypes.PERIOD
			&&	parser.getNodeTypeI(dotHandle) != JPTreeParserTokenTypes.LEXCOLON
			) return -2;
		int scanHandle = handler.gimme();
		parser.parseGetTop(scanNum, scanHandle);
		if (refactor.nsync(dotHandle, scanHandle) == 0) return -1;
		parser.setNodeText(scanHandle, " " + nuText + parser.getNodeText(dotHandle)); 
		return 1;
	} // addAtDot



} // class NoundoRefactor
