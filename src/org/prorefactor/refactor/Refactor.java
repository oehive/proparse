/**
 * Refactor.java
 * @author John Green
 * 15-Oct-2002
 * www.joanju.com
 * 
 * Copyright (c) 2002 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor;

import org.prorefactor.core.JPUtil;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.macrolevel.IncludeRef;
import org.prorefactor.macrolevel.ListingParser;

import com.joanju.ProparseLdr;


/**
 * A collection of functions to help with refactoring.
 * Mostly these are for working together with both an AST as well
 * as a scanner's token list. See also ScanLib for functions that
 * operate only on scanner token lists.
 * <p>
 * <h3>Integer return values</h3>
 * Integer return values are not like traditional C return values where 0
 * is success and any other number is an error number.
 * Instead, these are consistent with Proparse's return values: 
 * For many of these functions, where the return value is an int,
 * if the integer return value does not already have another meaning,
 * the function could be viewed as a logical "YES we got it / NO it doesn't exist"
 * <br>1 "success"
 * <br>0 "failure"
 * <br>-1 "warning"
 * <br>-2 "error"
 */
public class Refactor {

	private Refactor() {}

	private static Refactor instance = null;
	private ProparseLdr parser = ProparseLdr.getInstance();
	private JPUtil plus1 = JPUtil.getInstance();
	private ScanLib scanlib = ScanLib.getInstance();

	/**
	 * This class implements the "Singleton" design pattern.
	 */
	public static Refactor getInstance() {
		if (instance == null)
			instance = new Refactor();
		return instance;
	}


	/** Test if we have consecutive comments.
	 * We don't want to split two comments if they are one line after the other,
	 * because it is common for Progress programmers to slash-star and star-slash
	 * every line of a multi-line comment.
	 * When called, the current hidden token should be the first of the two
	 * being checked.
	 * @return 0 if not consec, 1 if two comments in a row, 2 if two
	 * comments seperated by whitespace with <2 '\n' characters.
	 */
	public int consecComments() {
		if (! parser.hiddenGetType().equals("COMMENT")) return 0;
		if (parser.hiddenGetNext() != 1) return 0;
		if (parser.hiddenGetType().equals("COMMENT")) {
			parser.hiddenGetPrevious();
			return 1;
		}
		if (! parser.hiddenGetType().equals("WS")) {
			parser.hiddenGetPrevious();
			return 0;
		}
		if (parser.hiddenGetText().split("\n").length > 2) {
			parser.hiddenGetPrevious();
			return 0;
		}
		if (parser.hiddenGetNext() != 1) {
			parser.hiddenGetPrevious();
			return 0;
		}
		int theRet = (parser.hiddenGetType().equals("COMMENT")) ? 2 : 0;
		parser.hiddenGetPrevious();
		parser.hiddenGetPrevious();
		return theRet;
	}



	/** Uses getSectionWithComments to find a token list segment from an AST node,
	 * copyAndMark the segment so it can be removed later with sweep(),
	 * put the copy of the segment onto a LooseChain.
	 * @param node The AST node.
	 * @param scanNum The scanner reference number.
	 * @param theChain The chain that we append the copied segment onto the end of.
	 * @throws RefactorException
	 * @return null on success, an error message otherwise.
	 */
	public void cutToChain(int node, int scanNum, LooseChain theChain)
			throws RefactorException {
		int begin = parser.getHandle();
		int end = parser.getHandle();
		parser.parseGetTop(scanNum, begin);
		parser.parseGetTop(scanNum, end);
		try {
			if (getSectionWithComments(node, begin, end) < 1)
				throw new RefactorException(node, "Failed to get section to cut/paste");
			scanlib.copyAndMark(begin, end);
			theChain.appendSegment(begin, end);
		} finally {
			parser.releaseHandle(begin);
			parser.releaseHandle(end);
		}
	} // cutToChain



	/** Find comments before the node which
	 * are not separated from the node by any blank lines.
	 * We also carry leading blank lines.
	 * @param astHandle Must be a handle to a <b>natural</b> node.
	 * @param tokenHandle Must be a valid handle to an early token in the scanner.
	 * On success, this handle is repositioned to the desired token.
	 * On fail, there is no guarantee as to where the handle points.
	 * @return True on success.
	 */
	public boolean findLeadingWhitespace(int astHandle, int tokenHandle) {
		if (parser.hiddenGetBefore(astHandle) < 1) return false;
		boolean noMoreComments = false;
		String filename = parser.getNodeFilename(astHandle);
		int line = 0;
		int col = 0;
		while (true) {
			if ( ! parser.hiddenGetFilename().equals(filename)) break;
			String type = parser.hiddenGetType();
			if (type.equals("COMMENT")) {
				if (noMoreComments) break;
				line = parser.hiddenGetLine();
				col = parser.hiddenGetColumn();
				if (parser.hiddenGetPrevious() < 1) break;
				continue;
			}
			if (type.equals("WS")) {
				if (!noMoreComments) {
					String theText = parser.hiddenGetText();
					int firstBreak = theText.indexOf('\n');
					if (firstBreak > -1) {
						int secondBreak = theText.lastIndexOf('\n');
						if (secondBreak!=firstBreak) noMoreComments = true;
					}
				}
				line = parser.hiddenGetLine();
				col = parser.hiddenGetColumn();
				if (parser.hiddenGetPrevious() < 1) break;
				continue;
			}
			break; // not WS or COMMENT - we're done
		} // while
		if (line < 1) return false;

		// We don't use a forward seek here, because we might run into whitespace
		// trailing another block/statement which has been cut from the scanner's
		// token list by a refactoring.
		if (scanlib.seek(astHandle, tokenHandle) < 1) return false;
		while (	parser.getNodeLine(tokenHandle)>line
				||	(parser.getNodeLine(tokenHandle)==line && parser.getNodeColumn(tokenHandle)>col)
				) {
			if (parser.nodePrevSibling(tokenHandle, tokenHandle).equals("")) break;
		}

		// We do not carry trailing newline from the previous statement/block.
		if (parser.getNodeTypeI(tokenHandle) == TokenTypes.NEWLINE) {
			parser.nodeNextSiblingI(tokenHandle, tokenHandle);
			return true;
		}

		// Oh oh, it wasn't a newline.
		// We probably have whitespace (without newline) followed by comment.
		if (parser.hiddenGetNext() < 1) return false;
		if ( ! parser.hiddenGetType().equals("COMMENT")) return false;
		if (parser.hiddenGetNext() < 1) return false;
		if ( ! parser.hiddenGetType().equals("WS")) return false;
		if (scanlib.seek(parser.hiddenGetLine(), parser.hiddenGetColumn(), tokenHandle) < 1)
			return false;

		return true;
	} // findLeadingWhitespace



	/** Find comments on the same line as the end of the AST branch, as well as one newline.
	 * @param astHandle A handle to the <b>top</b> of the AST branch.
	 * This function deals with finding the end of the branch.
	 * @param tokenHandle Must be a valid handle to an early token in the scan list.
	 * On success, this handle is repositioned to the desired token.
	 * On fail, there is no guarantee as to where the handle points.
	 * @return True on success.
	 */
	public boolean findTrailingWhitespace(int astHandle, int tokenHandle) {
		if (plus1.findFirstHiddenAfterLastDescendant(astHandle) < 1) return false;
		String filename = plus1.getFilename(astHandle);
		int line = 0;
		int col = 0;
		boolean gotEnd = false;
		while (true) {
			if ( ! parser.hiddenGetFilename().equals(filename)) break;
			String type = parser.hiddenGetType();
			if (type.equals("COMMENT") || type.equals("WS")) {
				if (	type.equals("WS")
					&&	parser.hiddenGetText().indexOf('\n') > -1
					) {
					line = parser.hiddenGetLine();
					col = parser.hiddenGetColumn();
					if (line > 0) {
						if (scanlib.seek(line, col, tokenHandle) < 1) return false;
						int nodeType = parser.getNodeTypeI(tokenHandle);
						while (nodeType>0 && nodeType!=TokenTypes.NEWLINE) {
							nodeType = parser.nodeNextSiblingI(tokenHandle, tokenHandle);
						}
						if (nodeType==TokenTypes.NEWLINE) gotEnd = true;
					}
					break;
				}
				if (parser.hiddenGetNext() < 1) break;
			} else {
				break; // not WS or COMMENT - we're done
			}
		} // while
		return gotEnd;
	} // findTrailingWhitespace



	/**
	 * Find an "insert point" in a specific source file, to come in front of
	 * a given AST node. (Node does not have to be natural.)
	 * Assumes we want a newline (i.e. Use for inserting between statements/blocks.)
	 * Makes assumptions about include files, only safe for DEFINE STATEMENTS,
	 * see TODO in source comments.
	 * @param astHandle The handle to find the insert point in front of.
	 * @param filename The file name that we want an insertion point for.
	 * (Currently no support for file index numbers with hidden tokens)
	 * @param scanNum A number for an existing scan for the file we want.
	 * @param tokenHandle The token handle to be repositioned. 
	 * Does not have to be pre-positioned.
	 * @return True on success.
	 * It's very possible for this to fail to find an insert point - the client
	 * should be prepared for a return value of false.
	 */
	public boolean getInsertPointInFile(
			int astHandle, String filename, int scanNum, int tokenHandle, ListingParser listingParser
			) {
		int h = parser.getHandle();
		try {

			// If this is the end of the syntax tree, the insert point is the end of the scan.
			if (parser.getNodeTypeI(astHandle) == TokenTypes.Program_tail) {
				parser.parseGetTop(scanNum, tokenHandle);
				// Hmm, no efficient way to get the end of a scan.  :(
				while (parser.nodeNextSiblingI(tokenHandle, tokenHandle) > 0) { }
				return true;
			}

			if (plus1.firstNaturalChild(astHandle, h) < 1) return false;

			// If this is the top of the file, then the node *is* the insert point.
			if (	parser.getNodeLine(h)==1
				&&	parser.getNodeColumn(h)==1
				&&	parser.getNodeFilename(h).equals(filename)
				) {
				parser.parseGetTop(scanNum, tokenHandle);
				// We don't want the Scanner_head, we want the first token.
				parser.nodeNextSibling(tokenHandle, tokenHandle);
				return true;
			}

			// If there's no hidden tokens then it's not a valid insert point.
			if (parser.hiddenGetFirst(h) < 1) return false;

			// Insert as early in previous hidden tokens as possible.
			int numTokens = 0;
			do {
				if (! parser.hiddenGetFilename().equals(filename)) continue;
				if (parser.hiddenGetLine()==1 && parser.hiddenGetColumn()==1)
					return getInsertPointFromTop(filename, scanNum, tokenHandle);
				// We want a newline to avoid inserting between code and same-line comment.
				numTokens++;
				if (	numTokens == 1
					&&	parser.hiddenGetText().indexOf('\n') == -1
					) {
					if (parser.hiddenGetNext() < 1) break;
					continue;
				}
				if (scanlib.seekFromTop(
						parser.hiddenGetLine()
						, parser.hiddenGetColumn()
						, scanNum
						, tokenHandle
						) > 1 ) {
					// Move forward by one NEWLINE if we can.
					// The insertions will be done in front of the selected token.
					if (parser.getNodeTypeI(tokenHandle) == TokenTypes.NEWLINE)
						parser.nodeNextSiblingI(tokenHandle, tokenHandle);
					return true;
				}
			} while (parser.hiddenGetNext() > 0);

			// If the node is actually in the file that we want an insert point in...
			// (...it might not be - the node might be in an include, and we might want
			// an insert point in main.p)			
			if (parser.getNodeFilename(h).equals(filename)) {
				// If we got here, then the whitespace in front of the node may
				// have begun in another file. Get at least to the front of any
				// WS token in front of the node. (In the scanner, that should get
				// us to the beginning of a line.)
				if (scanlib.seekFromTop(
						parser.getNodeLine(h)
						, parser.getNodeColumn(h)
						, scanNum
						, tokenHandle
						) < 1
					) return false;
				if (! parser.nodePrevSibling(tokenHandle, tokenHandle).equals("WS")) {
					// No whitespace. Insert point is the node itself.
					parser.nodeNextSiblingI(tokenHandle, tokenHandle);
					return true;
				}
			}

			// Another possible situation is that we have {a.i} <whitespace> {b.i},
			// that the node is at the top of b.i, but its whitespace begins in a.i,
			// and continues through main.p, right up to our node in {b.i}. In that case,
			// we want to insert in front of the {b.i} reference in main.p, but we don't
			// have any indicator in the WS hidden token that the ws comes from three
			// different source files.
			// So, we find our .i reference. If it is in the file that we want, then we
			// insert right in front of it.
			// This tricky logic can go away if we do something about fully justifying
			// our hidden tokens to the source.
			int fileIndex = parser.getNodeFileIndex(h);
			if (fileIndex > 0) { // node is in an include file
				// TODO This assumes that the first reference is the one we want.
				// That's fine if we are working with DEFINE statements, but no good otherwise.
				IncludeRef ref = (IncludeRef) listingParser.getRoot().findIncludeReferences(fileIndex).get(0);
				// If the parent file is actually a macro, we throw in the towel.
				if (! (ref.parent instanceof IncludeRef)) return false; 
				IncludeRef parent = (IncludeRef) ref.parent;
				// If this include is nested, we throw in the towel.
				if (! parser.getIndexFilename(parent.fileIndex).equals(filename)) return false;
				int type = scanlib.seekFromTop(
					ref.refLine
					, ref.refColumn
					, scanNum
					, tokenHandle
					);
				if (type!=TokenTypes.LEFTCURLY) return false;
				return true;
			}

			return false;

		} finally {
			parser.releaseHandle(h);
		}
	} // getInsertPointInFile()



	private boolean getInsertPointFromTop(
			String filename, int scanNum, int tokenHandle
			) {
		while (true) {
			if (! parser.hiddenGetFilename().equals(filename)) {
				parser.hiddenGetPrevious();
				break;
			} 
			String type = parser.hiddenGetType();
			if (!type.equals("WS") && !type.equals("COMMENT")) break;
			if (parser.hiddenGetNext()<1) break;
		}
		if (scanlib.seekFromTop(
				parser.hiddenGetLine()
				, parser.hiddenGetColumn()
				, scanNum
				, tokenHandle
				) > 0 ) {
			return true;
		}
		return false;
	} // getInsertPointFromTop()



	/** Get a scanner section, based on an AST node/branch,
	 * with leading comments and whitespace, and any trailing
	 * comments before the line break.
	 * This does <b>not</b> attempt to do scanner/AST synchronization.
	 * All you get is a segment of text.
	 * This finds the first natural node for the starting point, but it
	 * <b>does not (currently) try to find the last natural node. If the
	 * last node is not natural, it fails.</b>
	 * Returns -1 if the AST branch begins and ends in different files, etc.
	 * @param ast The node handle from the syntax tree
	 * @param begin A scanner handle, on success will point at beginning of section
	 * @param end A scanner handle, on success will point at end of section
	 * @return Positive int on success
	 */
	public int getSectionWithComments(int ast, int begin, int end) {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int astBegin = parser.getHandle();
		int astEnd = parser.getHandle();
		try {
			if (plus1.firstNaturalChild(ast, astBegin) < 1) return -1;
			parser.copyHandle(ast, astEnd);
			while (plus1.lastChild(astEnd, astEnd) > 0) { }
			if (parser.getNodeFileIndex(astBegin) != parser.getNodeFileIndex(astEnd)) return -1;
			if ( ! findLeadingWhitespace(astBegin, begin)) {
				if (scanlib.seek(astBegin, begin) < 1) return -1;
			}
			parser.copyHandle(begin, end); // faster reposition to start point for seek
			if ( ! findTrailingWhitespace(astBegin, end)) {
				parser.copyHandle(begin, end);  // fast reposition to start point for seek
				if (scanlib.seek(astEnd, end) < 1) return -1;
			}
			return 1;
		} finally {
			parser.releaseHandle(astBegin);
			parser.releaseHandle(astEnd);
		}
	} // getSectionWithComments()



	/**
	 * Attempt to change scan nodes so they match AST hidden tokens.
	 * Synchronize the hidden tokens in front of h1 with a scanner node h2
	 * This cannot (and should not) be used as a test to see if there are hidden tokens.
	 * Requires that h2 is a handle in a scan of h1's file, and h2 must be positioned
	 * at or before a node in the scan with same line/column as h1.
	 * If no hidden tokens: returns 1, h2 is unchanged.
	 * If there are hidden tokens:
	 * - Returns 1 on success: h2 is set to match hiddenGetFirst(h1).
	 * - Returns 0 on fail, and h2 is unchanged.
	 */
	public int hiddenBeforeSync(int h1, int h2) {
		// Implementation notes:
		// - We aren't able to just step through starting at the first
		//   whitespace, because there is no column number available for
		//   hidden tokens. Instead, we start at the node, and work our
		//   way backwards.
		int hiddenRet = parser.hiddenGetBefore(h1);
		if (hiddenRet != 1)
			return 1;
		int tnode = parser.getHandle();
		int sBegin = parser.getHandle();
		int sEnd = parser.getHandle();
		String expectFilename = parser.getNodeFilename(h2);
		try {

			// Hidden tokens in front of Program_tail are a special case,
			// because synthetic nodes do not have file/line number.
			// (Program_tail should be the only synthetic node with hidden tokens.)
			// If we're at Program_tail, we'll seek the scanner to Scanner_tail.
			// Otherwise, we just seek to the (presumably) natural node.
			parser.copyHandle(h2, tnode);
			if (parser.getNodeLine(h1) == 0 && parser.getNodeType(h1).equals("Program_tail")) {
				while (parser.nodeNextSiblingI(tnode, tnode) > 0) { }
			} else {
				if (scanlib.seek(h1, tnode) == 0)
					return 0;
			}

			String tokenType = parser.getNodeType(tnode);
			while (hiddenRet == 1) {
				if (! expectFilename.equals(parser.hiddenGetFilename()))
					return 0;
				String hiddenText = parser.hiddenGetText();
				String scanText = "";
				parser.copyHandle(tnode, sBegin);
				parser.copyHandle(tnode, sEnd);
				int numToMatch = 0;
				while (scanText.length() < hiddenText.length()) {
					tokenType = parser.nodePrevSibling(sBegin, sBegin);
					if (tokenType.equals(""))
						break;
					scanText = parser.getNodeText(sBegin) + scanText;
					numToMatch++;
				}
				if (scanText.equals(hiddenText)) {
					if (numToMatch > 1) {
						parser.setNodeNextSibling(sBegin, sEnd);
						parser.setNodeText(sBegin, hiddenText);
					}
					parser.setNodeType(sBegin, parser.hiddenGetType());
					parser.copyHandle(sBegin, tnode);
				} else {
					return 0;
				}
				hiddenRet = parser.hiddenGetPrevious();
			}
			parser.copyHandle(tnode, h2);
			return 1;
		} finally {
			parser.releaseHandle(tnode);
			parser.releaseHandle(sBegin);
			parser.releaseHandle(sEnd);
		}
	} // hiddenBeforeSync()



	/** Uses getSectionWithComments to find a token list segment from an AST node,
	 * markToBeCut the segment so it can be removed later with sweep().
	 * @param node The AST node
	 * @param scanNum The scanner for the branch's code.
	 */
	public void markToBeCut(int node, int scanNum) throws RefactorException {
		int begin = parser.getHandle();
		int end = parser.getHandle();
		parser.parseGetTop(scanNum, begin);
		parser.parseGetTop(scanNum, end);
		try {
			if (getSectionWithComments(node, begin, end) < 1)
				throw new RefactorException(node, "Failed to get section to cut/paste");
			scanlib.markToBeCut(begin, end);
		} finally {
			parser.releaseHandle(begin);
			parser.releaseHandle(end);
		}
	} // markToBeCut



	/**
	 * Multinode Sync (Branch) - synchronize an entire AST branch with a scanner list.
	 * @param inParseHandle is positioned at the first node we want in the parse tree
	 * @param inScanHandle1 is positioned in the scanner list *before* the expected node,
	 * probably just at the head of the scanner list.
	 * @param inScanHandle2 is positioned anywhere.
	 * @return 1 if the sync was successful, otherwise returns zero.
	 * <p>
	 * On success, inScanHandle1 is sync'd with the <b>first natural node</b>,
	 * and inScanHandle2 is at the end of the section of synchronized scan nodes.
	 * Does NOT attempt to synchronize whitespace until after the first natural node,
	 * because those may have nothing to do with this branch.
	 * Note that this will synchronize as far as it can before failing - so
	 * it may actually get some, but not all, of the relevent scan nodes changed
	 * so that they directly reflect the parse nodes. The two input scanHandles
	 * DO get moved around - don't count on their position for anything if the
	 * synchronization failed.
	 * @see #nsync
	 */
	public int msyncBranch(int inParseHandle, int inScanHandle1, int inScanHandle2) {
		int parseHandle = parser.getHandle();
		int scanHandle = parser.getHandle();
		try {
			// Since we're comparing to a flat scan list, it's easiest to
			// just work from a flattened AST section as well. Use an unfiltered query.
			int numResults = parser.queryCreate(inParseHandle, "refactor_msyncBranch", "");
			parser.copyHandle(inScanHandle1, scanHandle);
			boolean foundFirstNatural = false;
			String expectFilename = parser.getNodeFilename(inScanHandle1);
			nodes_loop:
			for (int resNum = 1; resNum <= numResults; resNum++ ) {
				parser.queryGetResult("refactor_msyncBranch", resNum, parseHandle);

				// Synchronize leading whitespace first.
				// hiddenBeforeSync() returns 1 on success and also if there's
				// no whitespace to sync. Returns 0 only if there is whitespace
				// but failed to sync.
				// We don't synchronize whitespace until after the first natural node.
				if (foundFirstNatural) {
					if (hiddenBeforeSync(parseHandle, scanHandle) == 0)
						return 0;
				}

				// Nothing to sync with synthetic nodes.
				if (parser.getNodeLine(parseHandle) == 0)
					continue nodes_loop;
				
				// Check that we're still getting nodes from the same file
				if (! expectFilename.equals(parser.getNodeFilename(parseHandle)))
					return 0;

				// Now synchronize the node.
				if (nsync(parseHandle, scanHandle) == 0)
					return 0;

				// If this is the first natural node, then this is where we want
				// inScanHandle1 to be pointing.
				if (! foundFirstNatural) {
					parser.copyHandle(scanHandle, inScanHandle1);
					foundFirstNatural = true;
				}

			} // nodes_loop:
			
			// We only got here on successful completion.
			// scanHandle is pointing to the last scanner node that
			// got synchronized. That's where we want inScanHandle2
			parser.copyHandle(scanHandle, inScanHandle2);
			return 1;
		} finally {
			parser.queryClear("refactor_msyncBranch");
			parser.releaseHandle(parseHandle);
			parser.releaseHandle(scanHandle);
		}
	} // msyncBranch()



	/**
	 * Node Sync - Synchronize a scanner node to a parser node.
	 * @param int h1 handle positioned at the node in the parse tree which we are interested in.
	 * @param int h2 handle positioned in the scanner list, somewhere *before* the expected node.
	 * @return 1 if h2 was successfully repositioned to a matching node,
	 * otherwise, returns 0, and leaves h2 unchanged.
	 * <p>   
	 * Causes one or more tokens in the scanner list to be changed in order for them
	 * to be synchronised with a token in the AST.
	 */
	public int nsync(int h1, int h2) {
		int tnode = parser.getHandle();
		int tnode2 = parser.getHandle();
		try {
			parser.copyHandle(h2, tnode);
		
			if (scanlib.seek(h1, tnode) == 0)
				return 0;
		
			// Check the text
			String theText = parser.getNodeText(h1);
			String haveText = parser.getNodeText(tnode);
			parser.copyHandle(tnode, tnode2);
			boolean identical = haveText.equals(theText);
			while (
				(! haveText.equals(theText))
				&& haveText.length() < theText.length()
				&& parser.nodeNextSiblingI(tnode2, tnode2) != 0
				) {
				haveText += parser.getNodeText(tnode2);
			}
		
			if (! haveText.equals(theText))
				return 0;
		
			// "Clump" scanner nodes into a single node matching the parser node
			// (i.e. extra, unnecessary nodes get discarded)
			if (!identical) {
				parser.nodeNextSibling(tnode2, tnode2);
				parser.setNodeNextSibling(tnode, tnode2);
				parser.setNodeText(tnode, theText);
				parser.setNodeTypeI(tnode, parser.getNodeTypeI(h1));
			}
		
			// One last check...
			if (parser.errorGetStatus() != 0)
				return 0;
		
			// All clear. Change h2 to point at the matching scanner node.
			parser.copyHandle(tnode, h2);
			return 1;

		} finally {
			parser.releaseHandle(tnode);
		}

	} // nsync()



	/**
	 * Replace Scanner Section
	 * Input scan number, scanner start handle, scanner end handle, and
	 * a handle to an AST branch to replace it with. Copies the nodes
	 * from the AST branch.
	 * Copying starts at the first node (not at the hidden tokens in
	 * front of the first node).
	 */
	public void replaceScannerSection(int scanHandle1, int scanHandle2, int parseHandle) {
		String qname = "refactor_repscansect";
		int currAST = parser.getHandle();
		int currScan = parser.getHandle();
		int tempHandle = parser.getHandle();
		try {

			// Work with a flat query. Gets us the nodes in the right order.
			int numResults = parser.queryCreate(parseHandle, qname, "");

			// We're going to drop scanHandle1 - we want to attach the newly
			// copied nodes to its previous sibling.
			parser.nodePrevSibling(scanHandle1, currScan);

			// Loop throught the query results
			for (int resNum = 1; resNum <= numResults; resNum++) {
				parser.queryGetResult(qname, resNum, currAST);

				// Copy leading hidden tokens (except on first node)
				int haveHidden = parser.hiddenGetFirst(currAST);
				while (resNum > 1 && haveHidden > 0) {
					parser.nodeCreate(tempHandle, parser.hiddenGetType(), parser.hiddenGetText());
					parser.setNodeNextSibling(currScan, tempHandle);
					parser.copyHandle(tempHandle, currScan);
					haveHidden = parser.hiddenGetNext();
				}

				// Now copy the AST node
				parser.nodeCreateI(tempHandle, parser.getNodeTypeI(currAST), parser.getNodeText(currAST));
				parser.setNodeNextSibling(currScan, tempHandle);
				parser.copyHandle(tempHandle, currScan);

			}

			// Attach the nextSibling of scanHandle2 to our newly created chain
			parser.nodeNextSiblingI(scanHandle2, tempHandle);
			parser.setNodeNextSibling(currScan, tempHandle);

		} finally {
			parser.queryClear(qname);
			parser.releaseHandle(currAST);
			parser.releaseHandle(currScan);
			parser.releaseHandle(tempHandle);
		}

	} // replaceScannerSection()



} // class Refactor


