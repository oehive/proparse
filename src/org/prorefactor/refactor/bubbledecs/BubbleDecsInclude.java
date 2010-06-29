/* BubbleDecsInclude.java
 * Created on Nov 20, 2003
 * John Green
 * 
 * Copyright (C) 2003 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.bubbledecs;

import com.joanju.ProparseLdr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.prorefactor.core.ICallbacki;
import org.prorefactor.core.JPUtil;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.macrolevel.IncludeRef;
import org.prorefactor.macrolevel.MacroDef;
import org.prorefactor.macrolevel.NamedMacroRef;
import org.prorefactor.refactor.FileStuff;
import org.prorefactor.refactor.LooseChain;
import org.prorefactor.refactor.Refactor;
import org.prorefactor.refactor.RefactorException;
import org.prorefactor.refactor.ScanIncludeRef;
import org.prorefactor.refactor.ScanLib;


/**
 * Deals with include files for the Bubble Declarations refactoring.
 */
class BubbleDecsInclude {


	public BubbleDecsInclude(
			int fileIndex
			, int scanNum
			, BubbleDecsRefactor bdr
			) throws IOException, RefactorException {
		this.bubbler = bdr;
		this.fileIndex = fileIndex;
		this.scanNum = scanNum;

		// Find where this include file was referenced from,
		// create a ScanIncludeRef object for dealing with it.
		refList = bubbler.listingParser.getRoot().findIncludeReferences(fileIndex);
		int refHandle = parser.getHandle();
		thisListingIncludeRef = findIncludeReference(refHandle);
		thisScanIncludeRef = new ScanIncludeRef(refHandle);
		parser.releaseHandle(refHandle);

		// Check if this include was referenced from the top file,
		// or if it was referenced from another include file.
		// We want to be sure to add split references ({*-proc.i} etc.)
		// to the LooseChains for the correct file.
		if (thisListingIncludeRef.fileIndex == 0) {
			ParentMethodsChain = bubbler.methodChain;
			ParentSharedChain = bubbler.sharedChain;
			ParentUnsharedChain = bubbler.unsharedChain;
		} else {
			BubbleDecsInclude bdiParent = 
				bubbler.fetchBubbleDecsInclude(thisListingIncludeRef.fileIndex);
			ParentMethodsChain = bdiParent.methodsChain;
			ParentSharedChain = bdiParent.sharedChain;
			ParentUnsharedChain = bdiParent.unsharedChain;
		}

		// The value of the filename stored within Proparse will depend
		// on the values in the PROPATH. Whatever entry was used from the
		// PROPATH is prepended onto the front of the filename, so you
		// end up with "./whatever" or "/fullpath/whatever".
		// Neither of these is what we want to use for writing new include
		// file references. So, we use the original reference name. That
		// in itself can be problematic, because the original source might
		// be using another macro reference in order to expand out the include
		// name, for example "{&whatever}.i". 
		origFilename = parser.getNodeText(thisScanIncludeRef.filenameHandle);

		// Because the PATH was prepended to the filename by the parser,
		// we should be able to count on finding the file OK, whether it
		// had a relative path "./" or a full path "/whatever" on PROPATH.
		// For some of the OS tools (moving, copying files) we require the
		// fully qualified path as well.
		fileOnPropath = parser.getIndexFilename(fileIndex);
		File f = new File(fileOnPropath);
		if (! f.exists() )
			throw new RefactorException("Could not find file: " + fileOnPropath);
		origFullpath = f.getCanonicalPath();

	}

	protected boolean isShared = false;
	private boolean isCohesive = false;
	/** Is the include file cohesive AND is it already positioned OK */
	private boolean isPositionOK = false;
	private boolean isTested = false;
	protected int fileIndex;
	protected int scanNum;
	private ArrayList refList;
	protected BubbleDecsRefactor bubbler;
	private IncludeRef thisListingIncludeRef;
	protected JPUtil plus1 = JPUtil.getInstance();
	private LooseChain methodsChain = new LooseChain();
	private LooseChain sharedChain = new LooseChain();
	private LooseChain unsharedChain = new LooseChain();
	private LooseChain ParentMethodsChain;
	private LooseChain ParentSharedChain;
	private LooseChain ParentUnsharedChain;
	private NoSplit noSplit = null;
	protected ProparseLdr parser = ProparseLdr.getInstance();
	private Refactor refactorlib = Refactor.getInstance();
	private ScanIncludeRef thisScanIncludeRef = null;
	private ScanLib scanlib = ScanLib.getInstance();
	private SplitFile localFile = null;
	private SplitFile procFile = null;
	private SplitFile sharedFile = null;
	private String fileOnPropath;
	private String origFilename;
	protected String origFullpath;



	/** If the include file is in the "ignore" list, it does not get split.
	 * But where do we move the reference to?
	 */
	private class NoSplit {
		static final int NO_MOVE = 10;
		static final int METHODS = 40;
		static final int SHARED = 50;
		int level = NO_MOVE;
		void setLevel(int newLevel) {
			level = Math.max(level, newLevel);
		}
	} // private class NoSplit



	private class SplitFile {
		SplitFile(String filename, String fullpath) throws IOException {
			this.filename = filename;
			this.fullpath = fullpath;
			file = new File(fullpath);
			isNew = file.createNewFile();
		}
		boolean isNew;
		String filename;
		String fullpath;
		File file;
		/** Write to the file - only if it doesn't already exist. */
		void write(int firstToken) throws IOException {
			assert parser.errorGetStatus()==0 : parser.errorGetText();
			if (! isNew) return;
			FileWriter writer = new FileWriter(file);
			bubbler.rollback.registerNewFile(filename, fullpath);
			int currToken = parser.getHandle();
			try {
				parser.copyHandle(firstToken, currToken);
				int tokenType = parser.getNodeTypeI(currToken);
				while (tokenType>0) {
					writer.write(parser.getNodeText(currToken));
					tokenType = parser.nodeNextSiblingI(currToken, currToken);
				}
			} finally {
				parser.releaseHandle(currToken);
				writer.close();
			}
		} // append
	} // private class SplitFile



	/** Call this if the include file is in the "ignore" list and should not be split. */
	public void doNotSplit() {
		if (noSplit==null) noSplit = new NoSplit(); 
	}



	protected IncludeRef findIncludeReference(int tokenHandle)
			throws RefactorException, IOException {
		if (refList.size() <= 0) throw new RefactorException("Didn't find include reference");
		// We assume here that we want to work with the first reference to the
		// include file. Since we found a declaration, it's probably a reasonable assumption.
		IncludeRef ref = (IncludeRef) refList.get(0);
		if (! (ref.parent instanceof IncludeRef))
			throw new RefactorException("Include parent is not a file");
		IncludeRef parent = (IncludeRef) ref.parent;
		int type = scanlib.seekFromTop(
			ref.refLine
			, ref.refColumn
			, bubbler.scanManager.getScanNumFromIndex(parent.fileIndex)
			, tokenHandle
			);
		if (type!=TokenTypes.LEFTCURLY)
			throw new RefactorException(
				tokenHandle
				, "Did not find left curly for include reference."
				);
		return parent;
	} // findIncludeReference



	/** Test whether an include file is "cohesive".
	 * The file is cohesive if:
	 *		- Set of homogenous statements, decided by a given test.
	 *		  (AST analysis)
	 *		- AST node before first state/block is from another file,
	 *		  and AST node after last state/block is from another file.
	 *		  (AST analysis)
	 *		- In that include file, there is no external macro reference
	 *		  before first state/block or after last state/block.
	 *		  (Analysis of Proparse's "preprocess listing")
	 * @param node The first node encountered in this include file.
	 * @param theTest The test which determines whether a statement or block
	 * fits with the current use's definition of "cohesive".
	 * (Ex: A set DEFINE SHARED statements might be considered cohesive.)
	 */
	private boolean isIncludeCohesive(int node, ICallbacki theTest) throws RefactorException {
		int h = parser.getHandle();
		try {
			int [] begin = new int[2]; // line/col
			int [] end = new int[2];   // line/col
			begin[0] = parser.getNodeLine(node);
			begin[1] = parser.getNodeColumn(node); 
			// Test that the node prior to this define is *not* from the same include
			if( ! parser.nodePrevSibling(node, h).equals("") ) {
				while (plus1.lastChild(h, h) > 0) {}
				if (parser.getNodeFileIndex(h) == fileIndex) return false;
			}
			parser.copyHandle(node, h);
			while (plus1.getPosition(h)[0] == fileIndex) {
				if (theTest.run(h) < 1) return false;
				parser.nodeNextSiblingI(h, h);
			}
			// Step back to the last block/statement node in the file. Find its last sibling.
			// Make sure it's in the same file. Get the line/col.
			parser.nodePrevSibling(h, h);
			while (plus1.lastChild(h, h) > 0) {}
			if (parser.getNodeFileIndex(h) != fileIndex)
				throw new RefactorException(
					parser.getNodeFilename(node)
					+ ": Last statement or block does not end in same source file."
					);
			end[0] = parser.getNodeLine(h);
			end[1] = parser.getNodeColumn(h);

			// Test all references to this include file.
			for (Iterator it = refList.iterator(); it.hasNext(); ) {
				IncludeRef ref = (IncludeRef)it.next();
				ArrayList list = ref.findExternalMacroReferences(null, begin);
				if (! isIncludeCohesive2(list)) return false;
				list = ref.findExternalMacroReferences(end, null);
				if (! isIncludeCohesive2(list)) return false;
			}

			// If we got here, it's cohesive
			return true;
		} finally {
			parser.releaseHandle(h);
		}
	} // isIncludeCohesive



	/** This part examines the list of external references returned
	 * by MacroRef.findExternalMacroReferences(). That returns references
	 * (direct and UNDEF) to include arguments, which we are not interested in.
	 */
	private boolean isIncludeCohesive2(ArrayList list) {
		check_loop:
		for (Iterator it = list.iterator(); it.hasNext(); ) {
			Object obj = it.next();
			if (obj instanceof MacroDef) { // must be an undefine
				MacroDef def = (MacroDef)obj;
				assert def.type==MacroDef.UNDEFINE && def.undefWhat!=null;
				if (def.undefWhat.type == MacroDef.NAMEDARG) continue check_loop;
			}
			if (obj instanceof NamedMacroRef) {
				NamedMacroRef ref = (NamedMacroRef)obj;
				if (	ref.macroDef.type == MacroDef.NAMEDARG
					||	ref.macroDef.type == MacroDef.NUMBEREDARG
					)
					continue check_loop;
			}
			// Those were the only two allowable external reference types.
			// If we got here, it's not cohesive.
			return false;
		}
		return true;
	} // isIncludeCohesive2



	private void moveCohesive(LooseChain theChain) throws RefactorException {
		int newtoken = parser.getHandle();
		try {
			String refText = FileStuff.LINESEP + thisScanIncludeRef.entireReference + FileStuff.LINESEP;
			parser.nodeCreateI(newtoken, TokenTypes.IMPOSSIBLE_TOKEN, refText);
			theChain.appendSegment(newtoken, newtoken);
			thisScanIncludeRef.markToBeCut();
		} finally {
			parser.releaseHandle(newtoken);
		}
	} // writeCohesive



	private SplitFile prepareSplitFile(LooseChain refChain, String nameSuffix) throws IOException {
		int newNode = parser.getHandle();
		try {
			SplitFile splitFile = new SplitFile(
				FileStuff.insertBeforeExtension(origFilename, nameSuffix)
				, FileStuff.insertBeforeExtension(origFullpath, nameSuffix)
				);
			String refText =
				FileStuff.LINESEP
				+ "{" + splitFile.filename + thisScanIncludeRef.argString + "}"
				+ FileStuff.LINESEP;
			parser.nodeCreateI(newNode, TokenTypes.IMPOSSIBLE_TOKEN, refText);
			refChain.appendSegment(newNode, newNode);
			return splitFile;
		} finally {
			parser.releaseHandle(newNode);
		}
	} // prepareSplitFile



	void preWrite() throws RefactorException, IOException {
		// If this include is in the "ignore" list, it didn't get split, but
		// we still want to move its reference to the right spot in the parent.
		// If it only contains local declarations and/or procedural code, it does
		// not get moved.
		if (noSplit!=null) {
			switch (noSplit.level) {
			case NoSplit.SHARED :
				moveCohesive(ParentSharedChain);
				break;
			case NoSplit.METHODS :
				moveCohesive(ParentMethodsChain);
				break;
			}
			return;
		}

		if (isPositionOK) return;
		if (isCohesive) return;

		// It's possible to have child includes write to this include's chains.
		// Check the chains.
		if (sharedFile==null && (! sharedChain.isEmpty() || ! methodsChain.isEmpty()) )
			sharedFile = prepareSplitFile(ParentSharedChain, "-shared");
		if (localFile==null && (! unsharedChain.isEmpty()) )
			localFile = prepareSplitFile(ParentUnsharedChain, "-local");

		// What do we do with the original include reference?
		if (scanlib.isAllWhitespace(scanNum)) {
			thisScanIncludeRef.markToBeCut();
			return;
		}

		// The include reference is changed to point at -proc.i
		String oldName = parser.getNodeText(thisScanIncludeRef.filenameHandle);
		parser.setNodeText(
			thisScanIncludeRef.filenameHandle
			, FileStuff.insertBeforeExtension(oldName, "-proc")
			);
		procFile = new SplitFile(
			FileStuff.insertBeforeExtension(origFilename, "-proc")
			, FileStuff.insertBeforeExtension(origFullpath, "-proc")
			);

	} // preWrite



	/** This is only called for DEFINE nodes which are *not* positioned OK.
	 * - Note that because we are dealing with outer-procedure DEFINE statements, we
	 *	 do not have to worry about redundantly processing the same included code.
	 */
	protected void processDefine(int node) throws IOException, RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		isShared = plus1.isDefineShared(node);
		if (noSplit!=null) {
			if (isShared) noSplit.setLevel(NoSplit.SHARED);
			return;
		}
		if (!isTested) {
			isTested = true;
			ICallbacki theTest = new ICallbacki() {
				public int run(int h) {
					if (	parser.getNodeTypeI(h) == TokenTypes.DEFINE
						&&	plus1.isDefineShared(h) == isShared
						)
						return 1;
					return 0;
				}
			};
			isCohesive = isIncludeCohesive(node, theTest);
			if (isCohesive) {
				// POSITION_OK does not count unless this include is referenced
				// from the top most file. If the parent is another include, then
				// we always cut the reference into a LooseChain. (The parent is
				// always split)
				isPositionOK = 
					(parser.attrGetI(node, BubbleDecsRefactor.POSITION_OK) == 1)
					&& thisListingIncludeRef.fileIndex == 0;
				if (!isPositionOK)
					moveCohesive(isShared ? ParentSharedChain : ParentUnsharedChain);
			}
		}
		if (isCohesive) return;
		if (isShared) {
			refactorlib.cutToChain(node, scanNum, sharedChain);
			// Prepare new include ref immediately, to ensure correct include order.
			if (sharedFile==null)
				sharedFile = prepareSplitFile(ParentSharedChain, "-shared");
		} else {
			refactorlib.cutToChain(node, scanNum, unsharedChain);
			// Prepare new include ref immediately, to ensure correct include order.
			if (localFile==null)
				localFile = prepareSplitFile(ParentUnsharedChain, "-local");
		}
	} // processDefine



	protected void processMethod(int node) throws IOException, RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		if (noSplit!=null) {
			if (isShared) noSplit.setLevel(NoSplit.METHODS);
			return;
		}
		if (!isTested) {
			isTested = true;
			ICallbacki theTest = new ICallbacki() {
				public int run(int h) {
					int type = parser.getNodeTypeI(h);
					if (type==TokenTypes.FUNCTION || type==TokenTypes.PROCEDURE) return 1;
					return 0;
				}
			};
			isCohesive = isIncludeCohesive(node, theTest);
			if (isCohesive) {
				// POSITION_OK does not count unless this include is referenced
				// from the top most file. If the parent is another include, then
				// we always cut the reference into a LooseChain. (The parent is
				// always split)
				isPositionOK = 
					(parser.attrGetI(node, BubbleDecsRefactor.POSITION_OK) == 1)
					&& thisListingIncludeRef.fileIndex == 0;
				if (!isPositionOK) thisScanIncludeRef.markToBeCut();
			}
		}
		if (isCohesive) return;
		refactorlib.cutToChain(node, scanNum, methodsChain);
		// Prepare new include ref immediately, to ensure correct include order.
		// Methods tack on to the end of the shared file.
		if (sharedFile==null)
			sharedFile = prepareSplitFile(ParentSharedChain, "-shared");
	} // processMethod



	protected void writeFiles() throws IOException, RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();

		if (isPositionOK) return;
		if (isCohesive) return;
		if (noSplit!=null) return;

		if (procFile != null) {
			scanlib.sweep(scanNum);
			int top = parser.getHandle();
			parser.parseGetTop(scanNum, top);
			procFile.write(top);
			parser.releaseHandle(top);
		}

		if (sharedFile != null) {
			if (! methodsChain.isEmpty()) {
				sharedChain.appendSegment(
					methodsChain.getStartHandle(), methodsChain.getEndHandle());
			}
			sharedFile.write(sharedChain.getStartHandle());
		} 
		if (localFile != null) {
			localFile.write(unsharedChain.getStartHandle());
		} 
		
	}



} // class
