/* Declaration.java
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

import org.prorefactor.core.TokenTypes;


/** Represents the declaration of an include argument or
 * the declaration at an &GLOBAL-DEFINE or &SCOPED-DEFINE.
 */
public class Declaration {

	public Declaration(TETNode parent, TokenContent content) {
		this.parent = parent;
		this.tokenContent = content;
	}

	/** For include args, the number. Not used for &DEFINE declarations. */
	int number = -1;

	/** @see #getFirstAtom() */
	private IAtom firstAtom;

	/** The name to refer to this declaration with.
	 * Not used for numbered include args.
	 */
	String name;

	/** The value should never be null - it's an empty String instead. */
	String value = "";

	/** The parent of a Declaration is a Token or an IncludeExpansion */
	TETNode parent;

	/** The first Token of the Token chain that this declaration came from */
	Token firstToken;

	/** Marks one-past-end of the segment of the Token chain that this declaration came from. */
	Token terminatorToken;

	TokenContent tokenContent;



	/** The first atom in this declaration's "value" */
	public IAtom getFirstAtom() { return firstAtom; }



	/** The tokenContent contains the &glob.. or &scop.. atoms, whitespace,
	 * and potentially even some comments which we have to strip from this
	 * macro's "value".
	 * We don't hack the TokenContent, instead, we create a new chain of
	 * AtomRef objects.
	 */
	private void init() {
		IAtom at = tokenContent.firstAtom; // &glob.. or &scop..
		IAtom term = tokenContent.terminatorAtom;
		at = at.next();
// TODO 1. Strip escapes 2. Strip comments
		whitespace_loop: for (; at!=term; at=at.next()) {
			switch (at.type()) {
				case TokenTypes.WS :
				case TokenTypes.NEWLINE :
					break;
				default :
					break whitespace_loop;
			}
		}
		


//		String defText = Atom.getText(content.firstAtom, content.terminatorAtom).trim();
//		char[] chars = defText.toCharArray();
//		int pos = 0;
//		// &glob.. or &scoped..
//		while (pos<chars.length && !Character.isWhitespace(chars[pos])) pos++;
//		// whitespace
//		while (pos<chars.length && Character.isWhitespace(chars[pos])) pos++;
//		// name and text
//		int start = pos;
//		while (pos<chars.length && !Character.isWhitespace(chars[pos])) pos++;
//		String name = new String(chars, start, pos-start).toLowerCase();
//		String text = stripComments(defText.substring(pos)).trim();
	}



	/** @see #getFirstAtom() */
	void setFirstAtom(IAtom firstAtom) { this.firstAtom = firstAtom; }



} // class
