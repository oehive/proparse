/**
 * SubstituteRefactor.java
 * @author John Green
 * 15-Oct-2002
 * www.joanju.com
 * 
 * To Do
 * - Currently, this changes the tree, whether the change is accepted or
 *   rejected. It should not commit any changes to the tree until the
 *   changes have been accepted.
 *   This will be easier with a node "clone" function in a future Proparse
 *   release, but for now we can use some other way around it.
 *   (Another option would be to use two passes. First pass would make the
 *   modifications on a copy of the branch, the second pass would make the
 *   modifications on the actual branch.)
 * 
 * Copyright (c) 2002-2003 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.substitute;


import org.prorefactor.core.HandleCollection;
import org.prorefactor.core.IConstants;
import org.prorefactor.core.Pstring;
import org.prorefactor.refactor.*;

import com.joanju.ProparseLdr;



/**
 * Given a handle to a PLUS node, where multiple PLUS and QSTRING
 * nodes are used, changes the AST to use SUBSTITUTE, and synchronizes
 * the scanner's token list.
 * <p>
 * Current implementation changes the AST, with no option to rollback
 * the changes. (i.e. If the changes are rejected by the client, then
 * the AST should be rebuilt (re-parse the compile unit) before using
 * it for anything else.) See notes at top of SubstituteRefactor.java.
 */
public class SubstituteRefactor implements IConstants {

	private ProparseLdr parser;
	private Refactor plus1;

	// Need some member fields for access from our recursive descend() function.
	private int currPos = 0;
	private String catString = "";
	private int numArgs = 0;
	private char quote = 0;
	private String attrib = "";


	public SubstituteRefactor() {
		parser = ProparseLdr.getInstance();
		plus1 = Refactor.getInstance();
	}


	/**
	 * Run this refactor for a given handle.
	 * @param target The SubstituteTarget record which contains details
	 * about the node which needs refactoring.
	 * @return 1 on success, or a negative number on fail:
	 * -1: Failed to synchronize, possibly due to preprocessing or escape sequences in source.
	 */
	public int run(SubstituteTarget target, int scanNum) {
		HandleCollection handler = new HandleCollection();
		try {
			return run(target, scanNum, handler);
		} finally {
			handler.releaseAll();
		}
	}



	private int run(SubstituteTarget target, int scanNum, HandleCollection handler) {
		int scanHandle1 = handler.gimme();
		int scanHandle2 = handler.gimme();

		parser.parseGetTop(scanNum, scanHandle1);
		parser.parseGetTop(scanNum, scanHandle2);

		// Make sure there's no preprocessing preventing the transform.
		if (plus1.msyncBranch(target.nodeHandle, scanHandle1, scanHandle2) < 1) {
			return -1;
		}

		// Keep track of lines changed: original from/to and changed file from/to
		target.changedLines[0] = parser.getNodeLine(scanHandle1);
		target.changedLines[1] = parser.getNodeLine(scanHandle2);
		target.changedLines[2] = target.changedLines[0];
		target.changedLines[3] = target.changedLines[1];

		// Grab a handle to the original first child, before we start messing
		// with this branch.
		int currChild = handler.gimme();
		parser.nodeFirstChild(target.nodeHandle, currChild);

		// Change the PLUS node to a SUBSTITUTE node.
		// Also - it is no longer an operator node. OPERATOR is from IConstants.
		parser.setNodeType(target.nodeHandle, "SUBSTITUTE");
		parser.setNodeText(target.nodeHandle, "SUBSTITUTE");
		parser.attrSet(target.nodeHandle, OPERATOR, 0);

		// Add the leftparen and the string
		int tempHandle = handler.gimme();
		parser.nodeCreate(tempHandle, "LEFTPAREN", "(");
		parser.setNodeFirstChild(target.nodeHandle, tempHandle);
		int stringNode = handler.gimme();
		parser.nodeCreate(stringNode, "QSTRING", "");
		parser.setNodeNextSibling(tempHandle, stringNode);

		// We need a handle to keep track of the end of the new SUBSTITUTE chain.
		currPos = handler.gimme();
		parser.copyHandle(stringNode, currPos);

		// Descend through the nodes in the expression, building the
		// string and the arguments for the new SUBSTITUTE expression.
		descend(currChild);

		// Finish off our new branch
		parser.setNodeText(stringNode, quote + catString + quote + attrib);
		parser.nodeCreate(tempHandle, "RIGHTPAREN", ")" );
		parser.setNodeNextSibling(currPos, tempHandle);

		// Modify the scanner
		plus1.replaceScannerSection(scanHandle1, scanHandle2, target.nodeHandle);

		return 1;
	} // run()



	/**
	 * Recursive function to descend the PLUS expression.
	 */
	private void descend(int currChild) {

		int tempHandle = parser.getHandle();

		try {

			String currChildType = parser.getNodeType(currChild);

			// If this is a QSTRING, we need to know more about it.
			boolean isTransString = false;
			if (currChildType.equals("QSTRING")) {
				Pstring pstring = new Pstring(parser.getNodeText(currChild));
				isTransString = pstring.isTrans();
			}

			if (isTransString) {
				// Just add this node's text
				Pstring pstring = new Pstring(parser.getNodeText(currChild));
				catString += pstring.justText();
				if (quote == 0) {
					quote = pstring.getQuote();
					attrib = pstring.getAttributes();
				}
			} else if (! currChildType.equals("PLUS")) {

				// Add a copy of this node as an argument in the SUBSTITUTE function
				numArgs++;
				catString += "&" + Integer.toString(numArgs);
	
				// Add a comma to the arguments list
				parser.nodeCreate(tempHandle, "COMMA", ",");
				parser.setNodeNextSibling(currPos, tempHandle);
				parser.copyHandle(tempHandle, currPos);
	
				// Move the node (and descendants) into the new SUBSTITUTE branch
				parser.setNodeNextSibling(currPos, currChild);
				parser.copyHandle(currChild, currPos);
			}

			if (currChildType.equals("PLUS")) {
				parser.nodeFirstChild(currChild, tempHandle);
				descend(tempHandle);
			}
			
			if (parser.nodeNextSiblingI(currChild, tempHandle) > 0)
				descend(tempHandle);

		} finally {
			parser.releaseHandle(tempHandle);
		}

	} // descend

}
