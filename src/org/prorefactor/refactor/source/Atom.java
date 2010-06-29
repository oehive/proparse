/* Atom.java
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

import com.joanju.ProparseLdr;

/** One or more characters in a source file, which could never be
 * split between two tokens.
 * These are copied directly out of Proparse's "scanner".
 * Atoms and SourceFile objects should be considered "immutable".
 * You should only modify the Tokens which reference the Atoms.
 */
public class Atom implements IAtom {

	private Atom() {};

	/** Create an Atom from a handle to a Proparse "scanner token".
	 * @param h A Proparse "handle" number.
	 */
	Atom(int handle) {
		column = parser.getNodeColumn(handle);
		line = parser.getNodeLine(handle);
		type = parser.getNodeTypeI(handle);
		text = parser.getNodeText(handle);
	}

	/** You would create your own Atom if you were generating new code
	 * to be written to a source file. Usually in those cases, only the text
	 */
	public Atom(String text) {
		this.text = text;
	}

	private static ProparseLdr parser = ProparseLdr.getInstance();

	private int column;
	private int line;
	private int type;
	private Atom next;
	private Atom prev;
	private String text;


	///// Accessors /////
	public int column() { return column; }
	public int line() { return line; }
	public IAtom next() { return next; }
	public IAtom prev() { return prev; }
	public String text() { return text; }
	public int type() { return type; }
	public void setColumn(int column) { this.column = column; }
	public void setLine(int line) { this.line = line; }
	public void setNext(IAtom next) { this.next = (Atom) next; }
	public void setPrev(IAtom prev) { this.prev = (Atom) prev; }
	public void setType(int type) { this.type = type; }
	public void setText(String text) { this.text = text; }




	///// Class Methods /////
	// These methods can operate on any IAtom. Note that an
	// AtomRef is not a subclass of Atom - it is a reference object,
	// therefor inheritance for these methods is not an option.



	/** Given an IAtom, repeat through prev() until the first is found */
	public static IAtom getFirst(IAtom position) {
		if (position==null) return null;
		IAtom a = position;
		while (a.prev()!=null) a = a.prev();
		return a;
	}



	/** Find the text between two IAtoms, inclusive of begin,
	 * exclusive of the terminator.
	 */
	public static String getText(IAtom begin, IAtom terminator) {
		String retString = "";
		for (IAtom curr = begin; curr!=terminator; curr = curr.next()) {
			retString += curr.text();
		}
		return retString;
	}



} // class
