/* ScanIncludeRef.java
 * Created on Dec 8, 2003
 * John Green
 *
 * Copyright (C) 2003 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor;


import org.prorefactor.core.IConstants;
import org.prorefactor.core.TokenTypes;

import com.joanju.ProparseLdr;


/** Scan an include reference gathering attributes like
 * the entire reference, the string used for the include name,
 * and a string containing all arguments.
 * This <b>does change the scanner's token list</b>.
 * It compacts the file reference string into a single token.
 * This is for working with the include reference's text,
 * not for macro level analysis.
 * For that, see org.prorefactor.refactor.macrolevel.*.
 */
public class ScanIncludeRef {

	/** Create a ScanIncludeRef object with the LEFTCURLY token.
	 * @param h a handle to the LEFTCURLY.
	 * @throws RefactorException if RIGHTCURLY not found.
	 */
	public ScanIncludeRef(int h) throws RefactorException {
		scanIncludeRef(h);
	}



	/** Construct given an int[3] file/line/col position and a scan number */
	public ScanIncludeRef(int[] pos, int scanNum) throws RefactorException {
		int h = parser.getHandle();
		try {
			int type = scanlib.seekFromTop(pos[1], pos[2], scanNum, h);
			if (type != TokenTypes.LEFTCURLY)
				throw new RefactorException(pos, "Failed to find left curly for include reference.");
			scanIncludeRef(h);
		} finally {
			parser.releaseHandle(h);
		}
	} // findIncludeReference





	private ProparseLdr parser = ProparseLdr.getInstance();
	private ScanLib scanlib = ScanLib.getInstance();


	/** Handle to the (new) FILENAME token */
	public int filenameHandle;
	/** Handle to the LEFTCURLY */
	public int leftCurly;
	/** Handle to the RIGHTCURLY */
	public int rightCurly;
	/** The reference arguments, including leading and trailing whitespace
	 * Might contain embedded macro references.
	 */
	public String argString;
	/** The entire reference "{...}"
	 * Might contain embedded macro references.
	 */
	public String entireReference;



	/** Compact an include reference filename into one token,
	 * set a handle to point at that token.
	 * A doublequote will start a string - 
	 * all this means is that we'll collect whitespace.
	 * A singlequote does not have this effect.
	 */
	protected void compactRefname(int leftcurly) throws RefactorException {
		int h = parser.getHandle();
		int nameHandle = parser.getHandle();
		try {
			StringBuilder filename = new StringBuilder("");
			int type = parser.nodeNextSiblingI(leftcurly, h);
			while (type==TokenTypes.WS || type==TokenTypes.NEWLINE) {
				type = parser.nodeNextSiblingI(h, h);  // whitespace
			}
			// We now have the first token of the filename.
			parser.copyHandle(h, nameHandle);
			parser.setNodeTypeI(nameHandle, TokenTypes.FILENAME);
			int numEmbedded = 0;
			boolean wasEscape = false;
			boolean gobbleWS = false;
			token_loop:
			while (true) {
				if (type<1) throw new RefactorException(leftcurly, "No matching curlybrace.");
				switch (type) {
				case TokenTypes.LEFTCURLY :
				case TokenTypes.CURLYAMP :
				case TokenTypes.CURLYNUMBER :
				case TokenTypes.CURLYSTAR :
					if (!wasEscape) numEmbedded++;
					break;
				case TokenTypes.RIGHTCURLY :
					if (!wasEscape) {
						if (numEmbedded>0) numEmbedded--;
						else break token_loop;
					}
					break;
				case TokenTypes.TILDE :
					wasEscape = true;
					break;
				case TokenTypes.DOUBLEQUOTE :
					filename.append(parser.getNodeText(h));
					type = parser.nodeNextSiblingI(h, h);
					if (type==TokenTypes.DOUBLEQUOTE) { // quoted quote
						type = parser.nodeNextSiblingI(h, h);
						continue token_loop;
					}
					gobbleWS = !gobbleWS;
					break;
				case TokenTypes.WS :
				case TokenTypes.NEWLINE :
					if (!gobbleWS) break token_loop;
					break;
				default :
					wasEscape = false;
				}
				filename.append(parser.getNodeText(h));
				type = parser.nodeNextSiblingI(h, h);
			}
			parser.setNodeNextSibling(nameHandle, h);
			parser.setNodeText(nameHandle, filename.toString());
		} finally {
			parser.releaseHandle(h);
			parser.releaseHandle(nameHandle);
		}
	} // compactRefname



	// Mark all tokens in the include reference as TO_BE_CUT
	public void markToBeCut() throws RefactorException {
		int h = parser.getHandle();
		parser.copyHandle(leftCurly, h);
		int type = parser.getNodeTypeI(h);
		while (type>1) {
			parser.attrSet(h, IConstants.TO_BE_CUT, IConstants.TRUE);
			if (parser.isSameNode(h, rightCurly) != 0) break;
			type = parser.nodeNextSiblingI(h, h);
		}
		if (type!=TokenTypes.RIGHTCURLY)
			throw new RefactorException(leftCurly, "Could not find matching curlybrace");
		parser.releaseHandle(h);
	} // markToBeCut



	private void scanIncludeRef(int h) throws RefactorException {
		int type;
		filenameHandle = parser.getHandle();
		leftCurly = parser.getHandle();
		rightCurly = parser.getHandle();
		parser.copyHandle(h, leftCurly);
		parser.copyHandle(h, rightCurly);
		StringBuilder arg = new StringBuilder("");
		StringBuilder entire = new StringBuilder("");
		compactRefname(h);

		entire.append(parser.getNodeText(h)); // leftcurly
		type = parser.nodeNextSiblingI(h, h);
		while (type==TokenTypes.WS || type==TokenTypes.NEWLINE) {
			entire.append(parser.getNodeText(h)); // whitespace
			type = parser.nodeNextSiblingI(h, h);
		}

		// file ref name
		entire.append(parser.getNodeText(h));
		parser.copyHandle(h, filenameHandle);

		boolean wasEscape = false;
		int numEmbeddedRefs = 0;
		type = parser.nodeNextSiblingI(h, h);
		token_loop:
		while (type>0) {
			entire.append(parser.getNodeText(h)); // entire gets closing curly
			switch (type) {
			case TokenTypes.LEFTCURLY :
			case TokenTypes.CURLYAMP :
			case TokenTypes.CURLYNUMBER :
			case TokenTypes.CURLYSTAR :
				if (!wasEscape) numEmbeddedRefs++;
				break;
			case TokenTypes.RIGHTCURLY :
				if (wasEscape) break;
				if (numEmbeddedRefs>0) numEmbeddedRefs--;
				else break token_loop;
			}
			arg.append(parser.getNodeText(h)); // arg doesn't get closing curly
			wasEscape = type==TokenTypes.TILDE;
			type = parser.nodeNextSiblingI(h, h);
		}

		argString = arg.toString();
		entireReference = entire.toString();
		parser.copyHandle(h, rightCurly);

		if (type!=TokenTypes.RIGHTCURLY)
			throw new RefactorException(leftCurly, "Could not find matching curlybrace");

	} // scanIncludeRef(h)


}
