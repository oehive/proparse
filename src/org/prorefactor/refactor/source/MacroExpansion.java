/* MacroExpansion.java
 * Created on Jan 26, 2004
 * John Green
 *
 * Copyright (C) 2004 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.source;

import java.io.File;


/** A curly reference that is not an include file expansion.
 * In other words, it references a Declaration object: 
 * an include arg or an &DEFINE (or possibly multiple Declaration
 * objects with {*}, etc.).
 */
public class MacroExpansion extends Expansion {

	MacroExpansion(Token parent, Declaration declRefd) {
		super(parent);
		this.declRefd = declRefd;
	}

	private Declaration declRefd;



	public Declaration getDeclRefd() { return declRefd; }



	/* @see org.prorefactor.refactor.source.TETNode#nearestEnclosingFile() */
	public File nearestEnclosingFile() {
		return getParent().nearestEnclosingFile();
	}

	

	/* @see org.prorefactor.refactor.source.Expansion#toStringSub() */
	public String toStringSub() {
		return "";
	}



} // class
