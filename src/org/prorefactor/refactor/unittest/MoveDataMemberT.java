/**
 * MoveDataMember.java
 * @author John Green
 * 15-Jan-2004
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
/* TODO
import org.prorefactor.refactor.movedata.MoveData;
import org.prorefactor.refactor.movedata.MoveDataItem;
*/

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.prorefactor.core.JPNode;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.core.unittest.UnitTestBase2;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.refactor.Rollback;
import org.prorefactor.refactor.source.CompileUnit;
import org.prorefactor.refactor.source.SourceFilePool;


/**
 * Tester for org.prorefactor.refactor.wrapproc
 */
public class MoveDataMemberT extends UnitTestBase2 {

	public MoveDataMemberT(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(MoveDataMemberT.class);
	}

	private ProparseLdr parser = ProparseLdr.getInstance();
	private RefactorSession refpack = RefactorSession.getInstance();

	private void parserErrCheck() {
		if (parser.errorGetStatus() != 0)
			fail(parser.errorGetText());
	}



	private List findMoveNodes(CompileUnit cu) {
		List retList = new ArrayList();
		for (JPNode node=cu.getTopNode().firstChild(); node!=null; node=node.nextSibling()) {
			if (node.getType() != TokenTypes.DEFINE) continue;
			if (node.getState2() != TokenTypes.VARIABLE) continue;
/* TODO
			if (! ASTNav.getDefineID(node).getText().startsWith("move_")) continue;
			retList.add(new MoveDataItem(node));
*/
		}
		return null;
	}



	protected void setUp() throws Exception {
		super.setUp();
		refpack.loadProject("sports2000");
	}



	protected void tearDown() throws Exception {
		super.tearDown();
	}



	public void test1() throws Exception {
		File expectDir = new File("data/movedatamember/t01/expect");
		File origDir = new File("data/movedatamember/t01/orig");
		File testDir = new File("data/movedatamember/t01/test");
		testDir.mkdirs();

		org.prorefactor.core.Util.wipeDirectory(testDir, false);
		org.prorefactor.core.Util.copyAllFiles(origDir, testDir);
		
		File source = new File("data/movedatamember/t01/test/t01a.p");
		File target = new File("data/movedatamember/t01/test/t01b.p");

		// "cu1" is *connected*, so we parse it last.
		SourceFilePool sourcePool = new SourceFilePool();
		CompileUnit cu2 = new CompileUnit(target, sourcePool, CompileUnit.DISCONNECTED);
		cu2.fullMonty();
		CompileUnit cu1 = new CompileUnit(source, sourcePool, CompileUnit.CONNECTED);
		cu1.fullMonty();

		List moveList = findMoveNodes(cu1);

/* TODO
		MoveData refactor = new MoveData(new Rollback());
		refactor.run(cu1, cu2, moveList);

		assertEquals(null, super.testCompareFiles(expectDir, testDir));
		assertEquals(0, refactor.getMessageList().size());
*/
	}



} // class
