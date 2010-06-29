/**
 * SubstituteT.java
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

package org.prorefactor.refactor.unittest;


import com.joanju.*;


import java.io.*;

import org.prorefactor.core.unittest.UnitTestException;
import org.prorefactor.refactor.substitute.*;


public class SubstituteT {

	private ProparseLdr parser;


	public static void main(String[] args) {
		System.out.println("Test the Substitue refactoring.");
		try {
			SubstituteT tester = new SubstituteT();
			tester.printVersion();
			tester.test2();
			System.out.println("All tests passed.");
		} catch (UnitTestException e) {
			System.out.println("Substitute refactoring tests failed:");
			System.out.println(e.getMessage());
		}
	}

	SubstituteT() throws UnitTestException {
		loadParser();
	}

	public void loadParser() throws UnitTestException {
		try {
			parser = ProparseLdr.getInstance();
		} catch (Throwable e) {
			throw new UnitTestException("Failed to load proparse.dll");
		}
	}

	private void parserErrCheck() throws UnitTestException {
		if (parser.errorGetStatus() != 0)
			throw new UnitTestException(parser.errorGetText());
	}

	public void printVersion() {
		System.out.println("Proparse version: " + parser.getVersion());
	}



	public void runAllTests() throws UnitTestException {
		test2();
	} // runAllTests()




	/**
	 * This test uses refactor.SubstituteWrap
	 */
	public void test2() throws UnitTestException {

		// Restore our test file
		try {
			org.prorefactor.core.Util.fileCopy("data/substitute.orig.p", "data/substitute.p");
		} catch (IOException e) {
			throw new UnitTestException(e.getMessage());
		}

		// parse
		parser.parse("data/substitute.p");
		parserErrCheck();
		int topNode = parser.getHandle();
		parser.nodeTop(topNode);

		// Call the wrapper, which lints, refactors, and shows the review dialog.
		String retString = SubstituteWrap.run(topNode);
		if (retString.length() > 0)
			throw new UnitTestException(retString);

	} // test2()




} // class SubstituteT

