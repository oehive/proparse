/**
 * BubbleDecsT.java
 * @author John Green
 * 19-Nov-2003
 * www.joanju.com
 * 
 * Copyright (c) 2003 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.unittest;


import com.joanju.ProparseLdr;

import java.io.File;
import java.util.Iterator;

import org.prorefactor.core.unittest.UnitTestBase2;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.refactor.Rollback;
import org.prorefactor.refactor.bubbledecs.BubbleDecsRefactor;

/**
 * Tester for org.prorefactor.refactor.bubbledecs
 */
public class BubbleDecsT extends UnitTestBase2 {

	public BubbleDecsT(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(BubbleDecsT.class);
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
		BubbleDecsRefactor refactor = new BubbleDecsRefactor(new Rollback());
		assertEquals(null, refactor.run(topNode));
		assertEquals("", parser.errorGetText());
		for (Iterator it = refactor.messages.iterator(); it.hasNext(); ) {
			assertTrue((String)it.next(), false);
		}
	} // refactor



	protected void setUp() throws Exception {
		super.setUp();
		parser.configSet("show-proparse-directives", "true");
		refpack.loadProject("sports2000");
		refpack.enableParserListing();
	}



	protected void tearDown() throws Exception {
		super.tearDown();
		refpack.disableParserListing();
	}



	public void test1() throws Exception {
		File expectDir = new File("data/bubble/expect");
		File origDir = new File("data/bubble/orig");
		File testDir = new File("data/bubble/test");
		testDir.mkdirs();

		org.prorefactor.core.Util.wipeDirectory(testDir, false);
		org.prorefactor.core.Util.copyAllFiles(origDir, testDir);

		refactor("data/bubble/test/bubbledecs.p");
		refactor("data/bubble/test/test2.p");
		assertEquals(null, super.testCompareFiles(expectDir, testDir));
	} // test1()



	public void test2() throws Exception {
		File expectDir = new File("data/bubble/expect2");
		File origDir = new File("data/bubble/orig2");
		File testDir = new File("data/bubble/test2");
		testDir.mkdirs();

		org.prorefactor.core.Util.wipeDirectory(testDir, false);
		org.prorefactor.core.Util.copyAllFiles(origDir, testDir);

		refactor("data/bubble/test2/bubb2.p");
		assertEquals(null, super.testCompareFiles(expectDir, testDir));
	} // test2()



	public void test3() throws Exception {
		File expectDir = new File("data/bubble/x03_expect");
		File origDir = new File("data/bubble/x03_orig");
		File testDir = new File("data/bubble/x03_test");
		testDir.mkdirs();

		org.prorefactor.core.Util.wipeDirectory(testDir, false);
		org.prorefactor.core.Util.copyAllFiles(origDir, testDir);

		refactor("data/bubble/x03_test/x03.p");
		assertEquals(null, super.testCompareFiles(expectDir, testDir));
	}



	public void test4() throws Exception {
		File expectDir = new File("data/bubble/x04/expect");
		File origDir = new File("data/bubble/x04/orig");
		File testDir = new File("data/bubble/x04/test");
		testDir.mkdirs();

		org.prorefactor.core.Util.wipeDirectory(testDir, false);
		org.prorefactor.core.Util.copyAllFiles(origDir, testDir);

		refactor("data/bubble/x04/test/x04.p");
		assertEquals(null, super.testCompareFiles(expectDir, testDir));
	}



	public void test5() throws Exception {
		File expectDir = new File("data/bubble/x05/expect");
		File origDir = new File("data/bubble/x05/orig");
		File testDir = new File("data/bubble/x05/test");
		testDir.mkdirs();

		org.prorefactor.core.Util.wipeDirectory(testDir, false);
		org.prorefactor.core.Util.copyAllFiles(origDir, testDir);

		refactor("data/bubble/x05/test/x05.p");
		assertEquals(null, super.testCompareFiles(expectDir, testDir));
	}



} // class NoUndoT
