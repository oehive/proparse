/**
 * RefactorT.java
 * @author John Green
 * 20-Oct-2002
 * www.joanju.com
 * 
 * Copyright (c) 2002 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.unittest;


import org.prorefactor.core.JPUtil;
import org.prorefactor.core.unittest.UnitTestBase2;
import org.prorefactor.refactor.Refactor;
import org.prorefactor.refactor.ScanLib;

import com.joanju.ProparseLdr;



public class RefactorT extends UnitTestBase2 {

	public RefactorT(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(RefactorT.class);
	}

	private JPUtil plus1 = JPUtil.getInstance();
	private ProparseLdr parser = ProparseLdr.getInstance();
	private Refactor refactorlib = Refactor.getInstance();
	private ScanLib scanlib = ScanLib.getInstance();

	private void parserErrCheck() {
		if (parser.errorGetStatus() != 0)
			fail(parser.errorGetText());
	}



	/**
	 * Test findFirstDescendant()
	 */
	public void testFindFirstDescendant() {
		parser.parse("data/hello.p");
		parserErrCheck();
		int topNode = parser.getHandle();
		parser.nodeTop(topNode);
		int stringNode = parser.getHandle();
		if (plus1.findFirstDescendant(topNode, stringNode, "QSTRING") < 1)
			fail("findFirstDescendant failed");
		if (! parser.getNodeType(stringNode).equals("QSTRING"))
			fail("findFirstDescendant: did not get a QSTRING node");
	} // testFindFirstDescendant()



	/**
	 * Test firstNaturalChild()
	 */
	public void testFirstNaturalChild() {
		parser.parse("data/empty.p");
		parserErrCheck();
		int theHandle = parser.getHandle();
		parser.nodeTop(theHandle);
		if (plus1.firstNaturalChild(theHandle, theHandle) != 0)
			fail("testFirstNaturalChild: empty.p is supposed to be empty!");
		parser.parse("data/hello.p");
		parserErrCheck();
		theHandle = parser.getHandle();
		parser.nodeTop(theHandle);
		if (plus1.firstNaturalChild(theHandle, theHandle) == 0)
			fail("firstNaturalChild: Did not find any node");
		String nodeType = parser.getNodeType(theHandle);
		if (! nodeType.equals("DO"))
			fail("testFirstNaturalChild: expected DO node in hello.p, got: " + nodeType);
	}


	/**
	 * Test hiddenBeforeSync()
	 */
	public void testHiddenBeforeSync() {
		// First parse
		parser.parse("data/hello.p");
		parserErrCheck();
		int parseHandle = parser.getHandle();
		parser.nodeTop(parseHandle);
		plus1.firstNaturalChild(parseHandle, parseHandle);
		// Now scan
		int scanNum = parser.parseCreate("scan", "data/hello.p");
		int scanHandle = parser.getHandle();
		parser.parseGetTop(scanNum, scanHandle);
		parser.nodeNextSibling(scanHandle, scanHandle);
		if (! parser.getNodeType(scanHandle).equals("COMMENTSTART"))
			fail("testHiddenBeforeSync: Has hello.p been changed?");
		// Now sync
		parser.parseGetTop(scanNum, scanHandle);
		if (refactorlib.hiddenBeforeSync(parseHandle, scanHandle) == 0)
			fail("hiddenBeforeSync failed");
		if (! parser.getNodeType(scanHandle).equals("COMMENT"))
			fail(
				"hiddenBeforeSync: expected COMMENT, got: "
				+ parser.getNodeType(scanHandle)
				);
		parser.hiddenGetFirst(parseHandle);
		if (! parser.getNodeText(scanHandle).equals(parser.hiddenGetText()))
			fail("hiddenBeforeSync: text does not match");
	} // testHiddenBeforeSync()


	/**
	 * Test lastChild()
	 */
	public void testLastChild() {
		parser.parse("data/hello.p");
		parserErrCheck();
		int theHandle = parser.getHandle();
		parser.nodeTop(theHandle);
		plus1.firstNaturalChild(theHandle, theHandle); // gets the DO node
		if (
			plus1.lastChild(theHandle, theHandle) == 0
			|| ! parser.getNodeType(theHandle).equals("PERIOD")
			|| ! parser.nodePrevSibling(theHandle, theHandle).equals("END")
			)
			fail(
				"testLastChild expected END PERIOD, got: "
				+ parser.getNodeType(theHandle)
				+ " " + parser.nodeNextSibling(theHandle, theHandle)
				);
	} // testLastChild()


	/**
	 * Test scannerSeek() and nsync()
	 */
	public void testMatch() {

		// parse
		parser.parse("data/match.p");
		parserErrCheck();
		int parseNode = parser.getHandle();
		parser.nodeTop(parseNode);
		int numResults = parser.queryCreate(parseNode, "strings", "QSTRING");
		if (numResults < 1)
			fail("No strings in match.p?!");
		parser.queryGetResult("strings", 1, parseNode);

		// Now scan
		int scanNum = parser.parseCreate("scan", "data/match.p");
		int scanNode = parser.getHandle();
		parser.parseGetTop(scanNum, scanNode);
		parserErrCheck();

		// Seek our scanner handle to synch with the parser handle
		if (scanlib.seek(parseNode, scanNode) == 0)
			fail("scannerSeek failed");
		if (! parser.getNodeType(scanNode).equals("DOUBLEQUOTE"))
			fail(
				"testMatch() expected DOUBLEQUOTE got: "
				+ parser.getNodeType(scanNode)
				);

		// Now "clump" the scanner tokens into a single QSTRING token,
		// to match what the parser's node looks like.
		if (refactorlib.nsync(parseNode, scanNode) == 0)
			fail("nsync failed");
		if (! parser.getNodeType(scanNode).equals("QSTRING"))
			fail(
				"testMatch() expected QSTRING, got: "
				+ parser.getNodeType(scanNode)
				);

	} // testMatch()


	/**
	 * Test msyncBranch()
	 * We synchronize the entire hello.p AST against its scanner list.
	 * This test works because there are no include files references
	 * or preprocessing in hello.p.
	 */
	public void testMsyncBranch() {
		parser.parse("data/hello.p");
		parserErrCheck();
		int parseHandle = parser.getHandle();
		parser.nodeTop(parseHandle);
		int scanNum = parser.parseCreate("scan", "data/hello.p");
		int scanHandle1 = parser.getHandle();
		int scanHandle2 = parser.getHandle();
		parser.parseGetTop(scanNum, scanHandle1);

		// Although not really part of this test, we will do a little extra to sync
		// the hidden text that comes before the first natural node.
		// msyncBranch() specifically does *not* attempt to synchronize any
		// leading hidden tokens, because those may have nothing to do with the
		// branch that the client is interested in.
		plus1.firstNaturalChild(parseHandle, parseHandle);
		refactorlib.hiddenBeforeSync(parseHandle, scanHandle1);
		// Reposition back to the top again
		parser.nodeTop(parseHandle);
		parser.parseGetTop(scanNum, scanHandle1);

		// Now make the call
		if (refactorlib.msyncBranch(parseHandle, scanHandle1, scanHandle2) == 0)
			fail("msyncBranch failed");

		// Now we can check all AST nodes and all hidden tokens against the scanner.
		// At the end of the loop, we can check that the next node in the scan list
		// is "Scanner_tail".
		// msyncBranch() does not synchronize leading hidden tokens, so we have
		// to explicitely ignore those in this test.
		int numResults = parser.queryCreate(parseHandle, "all", "");
		parser.parseGetTop(scanNum, scanHandle1);
		int scanNodeType = parser.nodeNextSiblingI(scanHandle1, scanHandle1);
		nodes_loop:
		for (int resNum = 1; resNum <= numResults; resNum++ ) {
			parser.queryGetResult("all", resNum, parseHandle);
			int haveHidden = parser.hiddenGetFirst(parseHandle);
			while (haveHidden > 0) {
				if (! parser.getNodeType(scanHandle1).equals(parser.hiddenGetType()))
					fail(
						"testMsyncBranch - mismatched hidden token types. Parser: "
						+ parser.hiddenGetType()
						+ " Scanner: " + parser.getNodeType(scanHandle1)
						);
				if (! parser.getNodeText(scanHandle1).equals(parser.hiddenGetText()))
					fail(
						"testMsyncBranch - mismatched hidden token text. Parser: "
						+ parser.hiddenGetText()
						+ " Scanner: " + parser.getNodeText(scanHandle1)
						);
				haveHidden = parser.hiddenGetNext();
				scanNodeType = parser.nodeNextSiblingI(scanHandle1, scanHandle1);
			}
			// Synthetic nodes have no real-text counterparts in the scan list.
			if (parser.getNodeLine(parseHandle) == 0)
				continue;
			if (scanNodeType != parser.getNodeTypeI(parseHandle)
				|| ! parser.getNodeText(scanHandle1).equals(parser.getNodeText(parseHandle))
				)
				break nodes_loop;
			scanNodeType = parser.nodeNextSiblingI(scanHandle1, scanHandle1);
		} // nodes_loop:
		if (! parser.getNodeType(parseHandle).equals("Program_tail")
			|| ! parser.getNodeType(scanHandle1).equals("Scanner_tail")
			)
			fail(
				"msyncBranch failed. Scan node: "
				+ plus1.handleToString(scanHandle1)
				+ " Parse node: "
				+ plus1.handleToString(parseHandle)
				);
	} // testMsyncBranch()


} // class RefactorT

