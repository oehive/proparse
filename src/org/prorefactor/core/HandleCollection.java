/**
 * HandleCollection.java
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
 * This class is a hack to get us around the fact that Java inhibits
 * implementation of any sane method of resource aquisition and release.
 * 
 */

package org.prorefactor.core;



import java.util.ArrayList;
import java.util.Iterator;

import com.joanju.*;


/**
 * Eases use of large numbers of Proparse node handles.
 */
public class HandleCollection {

	private ArrayList theList;
	private ProparseLdr parser;

	public HandleCollection() {
		parser = ProparseLdr.getInstance();
		theList = new ArrayList();
	}

	public int gimme() {
		int theHandle = parser.getHandle();
		theList.add(new Integer(theHandle));
		return theHandle;
	}

	public void releaseAll() {
		for (Iterator i = theList.iterator(); i.hasNext(); ) {
			parser.releaseHandle(((Integer)i.next()).intValue());
		}
	}

} // class HandleCollection
