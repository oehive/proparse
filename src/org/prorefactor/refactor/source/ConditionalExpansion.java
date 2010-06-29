/* ConditionalExpansion.java
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


/** Represents an &IF..&THEN and its code, an &ELSEIF..&THEN and its code,
 * or an &ELSEIF and its code.
 * This might seem backwards, but the "parent" token is the &ENDIF.
 */
public class ConditionalExpansion extends Expansion {

	ConditionalExpansion(Token parent) {
		super(parent);
	}

	/** @see #getFirstCondToken() */
	private Token firstCondToken;

	/** @see #getFirstProperToken() */
	private Token firstProperToken;



	/* @see org.prorefactor.refactor.source.TETNode#nearestEnclosingFile() */
	public File nearestEnclosingFile() {
		return getParent().nearestEnclosingFile();
	}

	

	/* @see org.prorefactor.refactor.source.Expansion#toStringSub() */
	public String toStringSub() {
		StringBuilder buff = new StringBuilder("<ConditionalExpansion>\n");
		buff.append("<ConditionalExpression>\n");
		for (Token tok = firstCondToken; tok!=null; tok = tok.getNext()) {
			buff.append(tok.toString());
		}
		buff.append("</ConditionalExpression>\n");
		buff.append("<ConditionalContents>\n");
		for (Token tok = firstProperToken; tok!=null; tok = tok.getNext()) {
			buff.append(tok.toString());
		}
		buff.append("</ConditionalContents>\n");
		buff.append("</ConditionalExpansion>\n");
		return buff.toString();
	} // toStringSub



	/** The first "condition" token: &IF, &ELSEIF, or &ELSE.
	 * The rest of the tokens in this chain are the rest of the conditions'
	 * text.
	 */
	public Token getFirstCondToken() { return firstCondToken; }

	/** The first "proper" token - the first token to come after the condition. */
	public Token getFirstProperToken() { return firstProperToken; }



	/** @see #getFirstCondToken() */
	public void setFirstCondToken(Token token) { firstCondToken = token; }

	/** @see #getFirstProperToken() */
	public void setFirstProperToken(Token token) { firstProperToken = token; }



} // class
