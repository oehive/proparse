/* LooseChain.java
 * Created on Nov 21, 2003
 * John Green
 *
 * Copyright (C) 2003 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor;

import com.joanju.ProparseLdr;


/**
 * A convenience class for storing loose token chains.
 */
public class LooseChain {

	public LooseChain() {}

	private boolean isEmpty = true;
	private ProparseLdr parser = ProparseLdr.getInstance();
	private int startHandle = 0;
	private int endHandle = 0;


	public void appendSegment(int begin, int end) {
		if (isEmpty) {
			isEmpty = false;
			startHandle = parser.getHandle();
			endHandle = parser.getHandle();
			parser.copyHandle(begin, startHandle);
			parser.copyHandle(end, endHandle);
		} else {
			assert parser.errorGetStatus()==0 : parser.errorGetText();
			parser.setNodeNextSibling(endHandle, begin);
			parser.copyHandle(end, endHandle);
		}
	} // appendSegment()



	public int getEndHandle() {
		return endHandle;
	}

	public int getStartHandle() {
		return startHandle;
	}

	public boolean isEmpty() {
		return isEmpty;
	}


} // class LooseChain