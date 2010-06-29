/* ScanLib.java
 * Created on Nov 24, 2003
 * John Green
 * 
 * Also see implementation notes at end of file.
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


/** Library of functions for working with a scanner and its token list.
 */
public class ScanLib {

	private ScanLib() {}

	private static ScanLib instance = null;
	private ProparseLdr parser = ProparseLdr.getInstance();

	/** This class implements the "Singleton" design pattern. */
	public static ScanLib getInstance() {
		if (instance == null)
			instance = new ScanLib();
		return instance;
	}



	/** Copy and mark a section for future removal.
	 * Could be thought of as a "soft cut" - the section cut out won't
	 * be removed until sweep() is called.
	 * @param begin Handle to first token to be cut out.
	 * On return, will point to the beginning of the "copied out" chain.
	 * @param end Handle to last token to be cut out.
	 * On return, will point to the end of the "copied out" chain.
	 */
	public void copyAndMark(int begin, int end) {
		int oldEnd = parser.getHandle();
		int currAddition = parser.getHandle();
		int currPosition = parser.getHandle();
		try {
			parser.copyHandle(end, oldEnd);
			parser.copyHandle(begin, currPosition);
			boolean first = true;
			boolean done = false;
			while (true) {
				done = parser.isSameNode(currPosition, oldEnd) != 0;
				parser.nodeCreateI(
					currAddition, parser.getNodeTypeI(currPosition), parser.getNodeText(currPosition));
				if (first) {
					first = false;
					parser.copyHandle(currAddition, begin);
					parser.copyHandle(currAddition, end);
				} else {
					parser.setNodeNextSibling(end, currAddition);
					parser.copyHandle(currAddition, end);
				}
				parser.attrSet(currPosition, IConstants.TO_BE_CUT, IConstants.TRUE);
				int currType = parser.nodeNextSiblingI(currPosition, currPosition);
				if (	currType < 1
					||	currType == TokenTypes.Scanner_tail
					||	done
					) break;
			}
		} finally {
			parser.releaseHandle(oldEnd);
			parser.releaseHandle(currAddition);
			parser.releaseHandle(currPosition);
		}
	} // copyAndMark()



	public String copyToText(int begin, int end) {
		int h = parser.getHandle();
		try {
			StringBuilder s = new StringBuilder();
			parser.copyHandle(begin, h);
			boolean done = false;
			while (!done) {
				s.append(parser.getNodeText(h));
				if (parser.nodeNextSiblingI(h, h) < 1) done = true;
				else done = (parser.isSameNode(h, end) != 0);
			}
			return s.toString();
		} finally {
			parser.releaseHandle(h);
		}
	} // copySection



	/** "Hard" cut a scanner section, from an input beginning through to and
	 * including an input end, from a scanner token list.
	 * Normally you want to use copyAndMark() instead, which copies a segment
	 * and marks the original as scheduled to be cut.
	 * @param begin Handle to first token to be cut out.
	 * @param end Handle to last token to be cut out.
	 */
	public void cutSectionHard(int begin, int end) {
		int before = parser.getHandle();
		int after = parser.getHandle();
		try {
			boolean havePrevSibling = parser.nodePrevSibling(begin, before).length() > 0;
			boolean haveNextSibling = parser.nodeNextSiblingI(end, after) > 0;
			if (havePrevSibling && haveNextSibling) {
				parser.setNodeNextSibling(before, after);
			} else {
				if (havePrevSibling) parser.setNodeNextSibling(before, 0);
				if (haveNextSibling) parser.setNodeNextSibling(end, 0);
			}
		} finally {
			parser.releaseHandle(before);
			parser.releaseHandle(after);
		}
	} // cutSectionHard()



	public void insertSection(int begin, int end, int insertBefore) {
		int prev = parser.getHandle();
		try {
			boolean havePrevSibling = parser.nodePrevSibling(insertBefore, prev).length() > 0;
			if (havePrevSibling) parser.setNodeNextSibling(prev, begin);
			parser.setNodeNextSibling(end, insertBefore);
		} finally {
			parser.releaseHandle(prev);
		}
	} // insertSection()



	/** Is the scanner list all whitespace? */
	public boolean isAllWhitespace(int scanNum) {
		int token = parser.getHandle();
		try {
			parser.parseGetTop(scanNum, token); // Scanner_head
			int tokenType = parser.nodeNextSiblingI(token, token);
			while (true) {
				if (tokenType<1 || tokenType==TokenTypes.Scanner_tail) break;
				switch (tokenType) {
				case TokenTypes.WS :
				case TokenTypes.NEWLINE :
					// do nothing
					break;
				default :
					if (parser.attrGetI(token, IConstants.TO_BE_CUT) == IConstants.FALSE)
						return false;
				}
				tokenType = parser.nodeNextSiblingI(token, token);
			}
			return true;
		} finally {
			parser.releaseHandle(token);
		}
	} // isAllWhitespace



	/** Mark a section for future removal.
	 * Could be thought of as a "soft cut" - the section cut out won't
	 * be removed until sweep() is called.
	 * @param begin Handle to first token to be cut out.
	 * @param end Handle to last token to be cut out.
	 */
	public void markToBeCut(int begin, int end) {
		int h = parser.getHandle();
		try {
			parser.copyHandle(begin, h);
			while (true) {
				parser.attrSet(h, IConstants.TO_BE_CUT, IConstants.TRUE);
				if (parser.isSameNode(h, end) != 0) break;
				if (parser.nodeNextSiblingI(h, h) < 1) break;
			}
		} finally {
			parser.releaseHandle(h);
		}
	} // markToBeCut()



	/**
	 * Seek to a token in a scan list.
	 * This variant seeks the scanner handle h2 to row/column of h1.
	 * Assumes scanner file matches filename for node h1.
	 * If finding a node at row/column fails, return 0.
	 * Does not reposition h2 if fail.
	 * This will fail if the node's text comes from preprocessing.
	 * @param The handle with the row/column we want to reposition to
	 * @param The handle that gets moved
	 * @return Positive int on success
	 */ 
	public int seek(int h1, int h2) {
		return seek(parser.getNodeLine(h1), parser.getNodeColumn(h1), h2);
	} // seek(int, int)



	/**
	 * Seek to a token in a scan list.
	 * This variant seeks the scanner to specified line and column number.
	 * Assumes you already know which file you want to seek in.
	 * If finding a node at row/column fails, return 0.
	 * Does not reposition h2 if fail.
	 * This will fail if the node's text comes from preprocessing, or if preprocessing
	 * earlier on the node's line has caused Proparse's column number to be inaccurrate.
	 */
	public int seek(int theLine, int theColumn, int h2) {
		int tnode = parser.getHandle();
		try {
			// Seek to line.
			parser.copyHandle(h2, tnode);
			int nodeType = parser.getNodeTypeI(tnode);
			while (nodeType > 0 && parser.getNodeLine(tnode) < theLine) {
				nodeType = parser.nodeNextSiblingI(tnode, tnode);
			}
			if (parser.getNodeLine(tnode) != theLine || parser.errorGetStatus() != 0)
				return 0;
			// Seek to column.
			boolean foundIt = false;
			while (parser.getNodeLine(tnode) == theLine && !foundIt) {
				if (parser.getNodeColumn(tnode) == theColumn) {
					foundIt = true;
				} else {
					if (parser.nodeNextSiblingI(tnode, tnode) == 0)
						break;
				}
			}
			if (!foundIt || parser.errorGetStatus() != 0)
				return 0;
			parser.copyHandle(tnode, h2);
			return 1;
		} finally {
			parser.releaseHandle(tnode);
		}
	} // seek(int, int, int)



	/**
	 * Seek a scan handle to a file position. Use's Proparse's query function
	 * for fast seek from the front of the scan list.
	 * @param line Line to seek to.
	 * @param col Column to seek to.
	 * @param scanNum The scanner number.
	 * @param tokenHandle Handle to reposition. Unlike other functions in
	 * this library, this does <b>not</b> already have to be pointing to an
	 * earlier token in the scan list.
	 * @return Integer token type if found, 0 if not found, negative int on error.
	 */
	public int seekFromTop(int line, int col, int scanNum, int tokenHandle) {
		parser.parseGetTop(scanNum, tokenHandle);
		final String queryName = "org.prorefactor.refactor.Refactor.scannerSeekFromTop";
		String queryString = "first_where_line=" + Integer.toString(line);
		try {
			int numResults = parser.queryCreate(tokenHandle, queryName, queryString);
			if (numResults < 1) return numResults;
			parser.queryGetResult(queryName, 1, tokenHandle);
			int type = parser.getNodeTypeI(tokenHandle);
			while (	parser.getNodeLine(tokenHandle)==line
					&&	parser.getNodeColumn(tokenHandle) < col ) {
				type = parser.nodeNextSiblingI(tokenHandle, tokenHandle);
				if (type<1) return type;
			}
			if (	parser.getNodeLine(tokenHandle)==line
				&&	parser.getNodeColumn(tokenHandle)==col )
				return type;
			return 0;
		} finally {
			parser.queryClear(queryName);
		}
	} // seekFromTop()



	/**
	 * Sweep out all tokens from a scanner's token list which have been
	 * marked as "cut".
	 * Note that the calling routines are expected to never cut Scanner_tail
	 * because this expects to setNodeNextSibling to *something*, even at the
	 * end of a file.
	 */
	public void sweep(int scanNum) throws RefactorException {
		int currToken = parser.getHandle();
		int prevToken = parser.getHandle();
		try {
			boolean cutting = false;
			boolean isMarked = false;
			parser.parseGetTop(scanNum, currToken); // Scanner_head
			int currType = parser.nodeNextSiblingI(currToken, currToken);
			while (currType > 1) {
				isMarked = parser.attrGetI(currToken, IConstants.TO_BE_CUT) == IConstants.TRUE;
				if (isMarked && ! cutting) {
					cutting = true;
					parser.nodePrevSibling(currToken, prevToken);
				}
				if (cutting && ! isMarked) {
					cutting = false;
					parser.setNodeNextSibling(prevToken, currToken);
				}
				currType = parser.nodeNextSiblingI(currToken, currToken);
			}
			if (cutting) throw new RefactorException("Cut past Scanner_tail");
		} finally {
			parser.releaseHandle(currToken);
		}
	} // sweep()



} // class ScanLib




/* Notes at end of file
 * 
 * Why do we use copy, mark, and sweep, rather than simply cutting chains?
 * 
 * - to avoid the situation where we are accidentally doing
 *   analysis of an AST which has gone out of sync with the scanner's token list.
 * - makes programming of "cut points" less critical - we don't have to be quite
 *   so careful that we don't try to cut the same newline into two different chains, etc.
 * - saves us from having trouble with our "insert points", which may have been 
 *   calculated such that it found an insert point which was demarcated by a "cut" token.
 *
 */
