/* AtomRef.java
 * Created on Feb 4, 2004
 * John Green
 *
 * Copyright (C) 2004 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.source;

/** A reference to an Atom. When doing macro expansion, we often
 * need a chain of atoms which is a bunch of fragmented sections
 * from original source file(s). Rather than copy the atoms for macro
 * expansion, we create references to the original atoms.
 * To this end, we only need an object with a reference to the original
 * atom, and this object's own next/prev attributes. All other attributes
 * can come from the original Atom. Next and Prev must be other AtomRef
 * objects - they cannot be Atom objects.
 */
public class AtomRef implements IAtom {

	public AtomRef(IAtom atom) {
		sourceAtom = atom;
	}

	/** An AtomRef is usually created as part of a chain.
	 * Input the previous IAtom to link the newly created one onto.
	 */
	public AtomRef(IAtom refTo, IAtom prevInChain) {
		sourceAtom = refTo;
		if (prevInChain != null) {
			prevInChain.setNext(this);
			this.setPrev(prevInChain);
		}
	}

	private IAtom sourceAtom;
	private AtomRef next;
	private AtomRef prev;


	///// Accessors /////
	public int column() { return sourceAtom.column(); }
	public int line() { return sourceAtom.line(); }
	public IAtom next() { return next; }
	public IAtom prev() { return prev; }
	public String text() { return sourceAtom.text(); }
	public int type() { return sourceAtom.type(); }
	public void setColumn(int column) { sourceAtom.setColumn(column); }
	public void setLine(int line) { sourceAtom.setLine(line); }
	public void setNext(IAtom next) { this.next = (AtomRef) next; }
	public void setPrev(IAtom prev) { this.prev = (AtomRef) prev; }
	public void setType(int type) { sourceAtom.setType(type); }
	public void setText(String text) { sourceAtom.setText(text); }



} // class
