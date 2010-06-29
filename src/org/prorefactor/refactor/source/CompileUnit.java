/* CompileUnit.java
 * Created on Jan 15, 2004
 * John Green
 *
 * Copyright (C) 2004,2006 Joanju Software
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.source;


import java.io.File;
import java.io.IOException;

import org.prorefactor.macrolevel.ListingParser;
import org.prorefactor.refactor.RefactorException;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.treeparser.ParseUnit;


/** A ParseUnit subclass, extended with source file features like the ListingParser.
 * @deprecated
 */
public class CompileUnit extends ParseUnit {
	
	/** Constructor with default values.
	 * @param file The compile unit's source file - usually a ".p" or ".w".
	 */
	public CompileUnit(File file) {
		super(file);
	}

	/** Constructor with specified SourceFilePool and connection style.
	 * @param file The compile unit's source file - usually a ".p" or ".w".
	 * @param pool Will create a new one if null.
	 * @param style A bitset, use bitwise OR to combine flags. ex: CompileUnit.DEFAULT
	 */
	public CompileUnit(File file, SourceFilePool pool, int style) {
		super(file);
		this.style = style;
		if (pool!=null) this.sourceFilePool = pool;
		this.file = file;
	}

	private IncludeExpansion tokenTree = null;
	private ListingParser listingParser;
	protected SourceFilePool sourceFilePool = new SourceFilePool();
	
	
	
	public void enableParserListing() {
		refpack.enableParserListing();
	}



	/** Just calls ParseUnit.treeParser01(), which now does all that this did. */
	public void fullMonty() throws RefactorException {
		treeParser01();
	}



	SourceFilePool getSourceFilePool() {
		return sourceFilePool;
	}



	void generateTokenTree() throws IOException, RefactorException {
		Processor processor = new Processor(file, sourceFilePool);
		tokenTree = processor.generateTree(this);
	}
	
	
	
	public ListingParser getListingParser() {
		return listingParser;
	}



	public IncludeExpansion getTokenTree() throws IOException, RefactorException {
		if (tokenTree == null) generateTokenTree();
		return tokenTree;
	}


	/** Runs the listing parser. */
	public void loadMacroTree() throws RefactorException, IOException {
		listingParser = new ListingParser(RefactorSession.getListingFileName());
		listingParser.parse();
	}
	
	
}
