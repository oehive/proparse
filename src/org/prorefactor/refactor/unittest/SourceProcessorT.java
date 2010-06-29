/**
 * SourceProcessorT.java
 * @author John Green
 * Feb 2004
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



import java.io.File;

import org.prorefactor.core.unittest.UnitTestBase2;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.refactor.source.CompileUnit;
import org.prorefactor.refactor.source.SourceFilePool;


/**
 * Tester for org.prorefactor.refactor.source
 */
public class SourceProcessorT extends UnitTestBase2 {

	public SourceProcessorT(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(SourceProcessorT.class);
	}

	private RefactorSession refpack = RefactorSession.getInstance();


	
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
		File origDir = new File("data/processor/t01/orig");
		File testDir = new File("data/processor/t01/test");
		testDir.mkdirs();

		org.prorefactor.core.Util.wipeDirectory(testDir, false);
		org.prorefactor.core.Util.copyAllFiles(origDir, testDir);
		File parseFile = new File("data/processor/t01/test/t01.p");

		CompileUnit cu = new CompileUnit(parseFile, new SourceFilePool(), CompileUnit.DEFAULT);
		cu.fullMonty();
		System.out.println(cu.getTokenTree().toString());
	}



} // class
