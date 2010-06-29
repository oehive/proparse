/* Token.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A token is a handle to a bunch of source file IAtom objects, which
 * contain the actual text/line/column.
 * A macro or .i reference is also a token.
 * Tokens may be nested, in the case where there are macro or .i references
 * embedded in a token. We don't have to look to the embedded tokens to find
 * all of the text though - the outer token points to all of the atoms.
 * There is one special token type: AMPIF_CONDITIONAL. In that case, it only
 * points to an "&ENDIF" atom, and there are two or more "expansions" which
 * must be traversed to find the "&IF" and all code between. This special
 * exception allows us to plug a tree structure for &IF branches into an
 * otherwise flat token list. 
 */
public class Token implements TETNode {

	/** @see #getParent() */
	public Token(TETNode parent) {
		this.parent = parent;
	}

	/** Copy constructor.
	 * Copies the pointers to other structures - does not clone.
	 * Sets type, firstAtom, terminatorAtom, parent.
	 * Does not set: expansions, nestedTokens, link, next, prev.
	 */
	public Token(Token orig) {
		this.type = orig.type;
		this.firstAtom = orig.firstAtom;
		this.terminatorAtom = orig.terminatorAtom;
		this.parent = orig.parent;
	}

	/** There is one special token type: AMPIF_CONDITIONAL. In that case, it only
	* points to an "&ENDIF" atom, and there are two or more "expansions" which
	* must be traversed to find the "&IF" and all code between. This special
	* exception allows us to plug a tree structure for &IF branches into an
	* otherwise flat token list. 
	*/
	public static final int AMPIF_CONDITIONAL = -100;

	private int type;

	/** @see #getExpansions(Object) */
	private ArrayList expansions;

	/** @see #addNestedToken(Token) */
	private ArrayList nestedTokens;

	/** @see #getFirstAtom() */
	private IAtom firstAtom = null;

	/** @see #getTerminatorAtom() */
	private IAtom terminatorAtom = null;

	/** @see #setLink(Object) */
	private Object link;

	/** @see #Token(TETNode) */
	private TETNode parent;

	private Token next;
	private Token prev;



	/** @see #getExpansions() */
	public void addExpansion(Object obj) {
		if (expansions==null) expansions = new ArrayList();
		expansions.add(obj);
	}



	/** @see #getNestedTokens() */
	public void addNestedToken(Token nestedToken) {
		if (nestedTokens == null) nestedTokens = new ArrayList();
		nestedTokens.add(nestedToken);
	}



	/** The expansions list is a list of Expansion objects, in the case
	 * of a curly expansion. 
	 * It is used (abused) as a list of ConditionalExpansion objects
	 * objects in the case of &IF..&ENDIF.
	 * It is also used (abused) for a list of Declaration objects, in the case
	 * of an &GLOBAL or an &SCOPED node.
	 * A Token may have expansions as well as nested tokens, which themselves
	 * have expansions.
	 * The list is null if no expansions have been added.
	 */
	public List getExpansions() { return expansions; }



	IAtom getFirstAtom() { return firstAtom; }



	/** There are a couple of potential special links from a Token.
	 * It may be a link to an AST node.
	 * In the case of any &DEFINE, it is a link to the Declaration object.
	 */
	Object getLink() { return link; }



	/** Our model keeps embedded curlies as embedded (nested) Token objects.
	 * For a Token which is a curly reference, but contains nested
	 * curly references, the Token has both Expansions and nested Tokens.
	 * The list is null if no nestedTokens have been added.
	 */
	public List getNestedTokens() { return nestedTokens; }



	Token getNext() { return next; }



	/** There are different object types that may be the parent of a Token.
	 * In the case of nested tokens, the parent is a Token.
	 * The rest of the time it is an Expansion object.
	 */
	TETNode getParent() { return parent; }



	Token getPrev() { return prev; }



	/** We use the "one-past-end" idiom, for easier iteration. */
	IAtom getTerminatorAtom() { return terminatorAtom; }



	int getType() { return type; }



	/** @see org.prorefactor.refactor.source.TETNode#nearestEnclosingFile() */
	public File nearestEnclosingFile() {
		return getParent().nearestEnclosingFile();
	}



	public int numExpansions() {
		if (expansions==null) return 0;
		return expansions.size();
	}



	public int numNestedTokens() {
		if (nestedTokens==null) return 0;
		return nestedTokens.size();
	}



	/** @see #getLink() */
	void setLink(Object link) { this.link = link; }



	void setNext(Token next) { this.next = next; }



	void setPrev(Token prev) { this.prev = prev; }



	void setType(int type) { this.type = type; }



	void setFirstAtom(IAtom firstAtom) { this.firstAtom = firstAtom; }



	/** @see #getTerminatorAtom() */
	void setTerminatorAtom(IAtom terminatorAtom) {
		this.terminatorAtom = terminatorAtom;
	}



	public String toString() {
		StringBuilder buff = new StringBuilder("<Token ");
		buff.append("type=\"");
		buff.append(type);
		buff.append("\"");
		buff.append(">");
		buff.append(
			"<![CDATA[" 
			+ Atom.getText(firstAtom, terminatorAtom) 
			+ "]]>" );
		if (expansions!=null) {
			buff.append("\n<ExpansionSet>\n");
			for (Iterator it = expansions.iterator(); it.hasNext();) {
				Expansion exp = (Expansion) it.next();
				buff.append(exp.toString());
			}
			buff.append("</ExpansionSet>\n");
		}
		buff.append("</Token>\n");
		return buff.toString();
	} // toString



} // class
