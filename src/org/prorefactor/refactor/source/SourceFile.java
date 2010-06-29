/* SourceFile.java
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
import java.io.IOException;
import java.util.ArrayList;

import org.prorefactor.refactor.FileStuff;

import com.joanju.ProparseLdr;

/** Represents a source file with a File (get "canonical name" from that)
 * and with a reference to the first Atom object for that file.
 * A SourceFile and its Atoms should be considered immutable - modify the
 * Tokens which reference the Atoms - not that Atoms themselves.
 * When working with multiple SourceFile objects (i.e. almost always),
 * fetch a SourceFile object by going through a SourceFilePool.
 */
public class SourceFile {

	public SourceFile(File file) throws IOException {
		this.file = FileStuff.findFile(file.toString());
		if (this.file == null) throw new IOException("Failed to find file " + file.toString());
		load();
	}

	ArrayList references = new ArrayList();
	Atom firstAtom = null;
	File file;



	/** Add a reference (an IncludeExpansion) to the list of known references
	 * to this source file.
	 * @param o The IncludeExpansion object which references this source file.
	 * This gives us a list of possible "{include.i}" references in the source
	 * code which can expand this file.
	 */
	public void addReference(Object o) {
		references.add(o);
	}



	/** Load a file into memory (chain of Atoms), via Proparse's "scanner".
	 * It is unlikely that a client would ever call this directly.
	 * It is called when the object is created.
	 */
	private void load() throws IOException {
		ProparseLdr parser = ProparseLdr.getInstance();
		int scanNum = parser.parseCreate("scan", FileStuff.fullpath(file));
		if (parser.errorGetStatus() < 0) throw new IOException(parser.errorGetText());
		int h = parser.getHandle();
		if (parser.parseGetTop(scanNum, h) < 1) return; // empty file
		firstAtom = new Atom(h);
		Atom prevAtom = firstAtom;
		for (int i = parser.nodeNextSiblingI(h, h); i > 0; i = parser.nodeNextSiblingI(h, h)) {
			Atom nextAtom = new Atom(h);
			prevAtom.setNext(nextAtom);
			nextAtom.setPrev(prevAtom);
			prevAtom = nextAtom;
		}
		parser.parseDelete(scanNum);
	}



} // class
