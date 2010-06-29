/**
 * PUBTest.java
 * @author John Green
 * Sep 1, 2004
 * www.joanju.com
 *
 * Copyright (C) 2004 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.unittest;

import java.io.File;
import java.util.ArrayList;

import org.prorefactor.core.JPNode;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.core.unittest.UnitTestBase2;
import org.prorefactor.refactor.PUB;
import org.prorefactor.refactor.RefactorSession;



/** Test "Parse Unit Binary" files.
 */
public class PUBTest extends UnitTestBase2 {

	public PUBTest(String arg0) {
		super(arg0);
	}

	private RefactorSession refpack = RefactorSession.getInstance();
		
	public static void main(String[] args) {
		junit.textui.TestRunner.run(PUBTest.class);
	}


	
	public void test00() throws Exception {
		refpack.loadProject("sports2000");
	}

	public void test01() throws Exception {
		String relPath = "data/pub/test01.p";
		File parseFile = new File(relPath);
		PUB pub = new PUB(parseFile.getCanonicalPath());
		pub.build();
		pub = new PUB(parseFile.getCanonicalPath());
		assertTrue(pub.load());
		String [] fileIndex = pub.getTree().getFilenames();
		
		// Test that file at index 1 matches the include file name that we expect
		File iGet = new File(fileIndex[1]);
		File iBase = new File("data/pub/test01.i");
		assertTrue(iGet.getCanonicalPath().equals(iBase.getCanonicalPath()));

		// Test that the file timestamp checking works
		long origTime = iBase.lastModified();
		iBase.setLastModified(System.currentTimeMillis() + 10000);
		assertFalse(pub.load());
		iBase.setLastModified(origTime);
		assertTrue(pub.load());
		
		// Test that the schema load works
		ArrayList tables = new ArrayList();
		pub.copySchemaTableLowercaseNamesInto(tables);
		assertTrue(tables.size() == 1);
		assertTrue(tables.get(0).toString().equals("sports2000.customer"));
		ArrayList fields = new ArrayList();
		pub.copySchemaFieldLowercaseNamesInto(fields, "sports2000.customer");
		assertTrue(fields.size() == 1);
		assertTrue(fields.get(0).toString().equals("name"));

		// Test the import table.
		PUB.SymbolRef [] imports = pub.getImportTable();
		PUB.SymbolRef imp = imports[0];
		assertTrue(imp.progressType == TokenTypes.VARIABLE);
		assertTrue(imp.symbolName.equals("sharedChar"));
		
		// Test the export table.
		PUB.SymbolRef [] exports = pub.getExportTable();
		PUB.SymbolRef exp = exports[0];
		assertTrue(exp.progressType == TokenTypes.FRAME);
		assertTrue(exp.symbolName.equals("myFrame"));
		
		// Test that there are comments in front of the first real node
		JPNode topNode = pub.getTree();
		assertTrue(topNode.firstNaturalChild().getComments().length() > 2);
		
		// Test that the ID nodes have text.
		for (JPNode node : topNode.query(TokenTypes.ID)) {
			assertTrue(node.getText().length() > 0);
		}
		
	}

}
