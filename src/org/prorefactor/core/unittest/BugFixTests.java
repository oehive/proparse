/**
 * Authors: John Green
 * July 4, 2006.
 * 
 * Copyright (c) 2006 Joanju (www.joanju.com).
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.prorefactor.core.unittest;

import java.io.File;

import junit.framework.TestCase;

import org.prorefactor.core.schema.Schema;
import org.prorefactor.treeparser.ParseUnit;


/** Test bug fixes. */
public class BugFixTests extends TestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Schema schema = Schema.getInstance();
		schema.clear();
		schema.loadSchema("proparse.schema");
	}

	public void test01() throws Exception {
		File file = new File("data/bugsfixed/bug01.p");
		ParseUnit pu = new ParseUnit(file);
		pu.treeParser01();
		// No further tests needed. Passes if parses clean.
	}

	public void test02() throws Exception {
		File file = new File("data/bugsfixed/bug02.p");
		ParseUnit pu = new ParseUnit(file);
		pu.treeParser01();
		// No further tests needed. Passes if parses clean.
	}

	public void test03() throws Exception {
		File file = new File("data/bugsfixed/bug03.p");
		ParseUnit pu = new ParseUnit(file);
		pu.treeParser01();
		// No further tests needed. Passes if parses clean.
	}

	public void test04() throws Exception {
		File file = new File("data/bugsfixed/bug04.p");
		ParseUnit pu = new ParseUnit(file);
		pu.treeParser01();
		// No further tests needed. Passes if parses clean.
	}

	public void test05() throws Exception {
		File file = new File("data/bugsfixed/bug05.p");
		ParseUnit pu = new ParseUnit(file);
		pu.treeParser01();
		// No further tests needed. Passes if parses clean.
	}

}
