/**
 * WrapProcT.java
 * @author John Green
 * 06-Jan-2004
 * www.joanju.com
 * 
 * Copyright (c) 2004 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.unittest;


import com.joanju.ProparseLdr;

import java.io.File;

import org.prorefactor.core.unittest.UnitTestBase2;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.refactor.Rollback;
import org.prorefactor.refactor.wrapproc.WrapProcedure;


/**
 * Tester for org.prorefactor.refactor.wrapproc
 */
public class WrapProcT extends UnitTestBase2 {

	public WrapProcT(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(WrapProcT.class);
	}

	private ProparseLdr parser = ProparseLdr.getInstance();
	private RefactorSession refpack = RefactorSession.getInstance();

	private void parserErrCheck() {
		if (parser.errorGetStatus() != 0)
			fail(parser.errorGetText());
	}



	private void refactor(String filename) throws Exception {
		parser.parse(filename);
		parserErrCheck();
		int topNode = parser.getHandle();
		parser.nodeTop(topNode);
		WrapProcedure refactor = new WrapProcedure(topNode, new Rollback());
		refactor.run();
		parserErrCheck();
	} // refactor



	protected void setUp() throws Exception {
		super.setUp();
		refpack.loadProject("sports2000");
		refpack.enableParserListing();
	}



	protected void tearDown() throws Exception {
		super.tearDown();
		refpack.disableParserListing();
	}



	public void test1() throws Exception {
		File expectDir = new File("data/wrapprocedure/t01/expect");
		File origDir = new File("data/wrapprocedure/t01/orig");
		File testDir = new File("data/wrapprocedure/t01/test");
		testDir.mkdirs();

		org.prorefactor.core.Util.wipeDirectory(testDir, false);
		org.prorefactor.core.Util.copyAllFiles(origDir, testDir);

		refactor("data/wrapprocedure/t01/test/t01.p");
		assertEquals(null, super.testCompareFiles(expectDir, testDir));
	}



} // class NoUndoT
