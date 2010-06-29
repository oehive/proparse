/* Expansion.java
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


/** An expansion is just a bunch of Token objects.
 * Whether those came from a .p/.i, a macro expansion, or a
 * branch of an &IF block, depends on the Expansion type:
 * IncludeExpansion, MacroExpansion, ConditionalExpansion.
 * <p>
 * For any given curly expansion, there may be more than
 * one way to expand it, depending on if its declaration(s)
 * are found within conditional compile branches (&IF).
 * Usually there's just one.
 */
public abstract class Expansion implements TETNode {

	Expansion(Token parent) {
		setParent(parent);
	}

	/** @see #isParsed() */
	private boolean isParsed = false;

	/** @see #isPrimary() */
	private boolean isPrimary = false;

	/** @see #getDerivation() */
	private Expansion[] derivation;

	/** @see #getFirstToken() */
	private Token firstToken = new Token(this);

	/** @see #getParent() */
	private Token parent;



	/** Get the array of Expansion objects which were used
	 * for finding the text for nested tokens within the curly
	 * ref that this expansion is for. Null if no nested tokens.
	 */
	public Expansion[] getDerivation() { return derivation; }



	/** The first token in the chain of tokens which resulted
	 * from this expansion.
	 */
	public Token getFirstToken() { return firstToken; }



	/** The "parent" of an expansion is a Token object.
	 * It is where the expansion was reference from. 
	 * This is only null if this is an IncludeExpansion and
	 * compileUnit!=null.
	 * We can ascend Expansion objects until parentToken==null,
	 * and at that point, we know that the Expansion is the
	 * IncludeExpansion object for the main .p.
	 */
	Token getParent() { return parent; }



	/** Has this expansion (possibly an alternative branch)
	 * been parsed? (i.e. Have its nodes been mapped to an AST?)
	 */
	public boolean isParsed() {
		return isParsed;
	}



	/** When there's alternative expansions, we want to know
	 * whether this is the branch used by the configuration we 
	 * got when we did parser.parse().
	 */
	public boolean isPrimary() {
		return isPrimary;
	}



	/** @see #getDerivation() */
	void setDerivation(Expansion[] expansions) { derivation = expansions; }



	/** @see #getFirstToken() */
	void setFirstToken(Token firstToken) { this.firstToken = firstToken; }



	/** @see #getParent() */
	void setParent(Token parent) { this.parent = parent; }



	/** @see #isParsed() */
	public void setParsed(boolean isParsed) {
		this.isParsed = isParsed;
	}



	/** @see #isPrimary() */
	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}



	/** XML node text for the Expansion.
	 * @see #toStringSub()
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder("<Expansion>\n");
		buff.append(toStringSub());
		for (Token tok = firstToken; tok!=null; tok = tok.getNext()) {
			buff.append(tok.toString());
		}
		buff.append("</Expansion>\n");
		return buff.toString();
	}



	/** Use this method to display the subclass's attributes
	 * in XML subnode text (will be subnodes of an <Expansion> node).
	 */
	public abstract String toStringSub();



} // class
