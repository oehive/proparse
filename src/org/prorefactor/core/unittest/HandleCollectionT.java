/**
 * HandleCollectionT.java
 * @author John Green
 * 10-Oct-2002
 * www.joanju.com
 * 
 * Copyright (c) 2002 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.core.unittest;


import org.prorefactor.core.HandleCollection;


public class HandleCollectionT extends UnitTestBase2 {

	public HandleCollectionT(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(HandleCollectionT.class);
	}

	public static void test1() {
		HandleCollection it = new HandleCollection();
		HandleCollection it2 = new HandleCollection();
		try {
			int gotHandle = 0;	
			for (int i = 1; i < 1500; i++) {
				gotHandle = it.gimme();
			}
			assertEquals(1499, gotHandle);
			for (int i = 1; i < 300; i++) {
				gotHandle = it2.gimme();
			}
			assertEquals(1798, gotHandle);
		} finally {
			it.releaseAll();
			it2.releaseAll();
		}
		HandleCollection it3 = new HandleCollection();
		try {
			int gotHandle = 0;	
			for (int i = 1; i < 400; i++) {
				gotHandle = it3.gimme();
			}
			assertEquals(399, gotHandle);
		} finally {
			it3.releaseAll();
		}

	} // test1()

} // class HandleCollectionT
