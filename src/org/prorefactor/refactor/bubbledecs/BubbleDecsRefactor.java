/**
 * BubbleDecsRefactor.java
 * @author John Green
 * 18-Nov-2003
 * www.joanju.com
 *
 * Copyright (c) 2003 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * 
 * IMPLEMENTATION NOTES
 * - We don't paste any cut segments back into the scanner while
 *   we are still working. That is because any segments that get
 *   pasted in will have the potential of messing up any subsequent
 *   seek by line number.
 * 
 */

package org.prorefactor.refactor.bubbledecs;


import com.joanju.ProparseLdr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.prorefactor.core.IConstants;
import org.prorefactor.core.JPUtil;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.macrolevel.IncludeRef;
import org.prorefactor.macrolevel.ListingParser;
import org.prorefactor.macrolevel.MacroRef;
import org.prorefactor.refactor.FileStuff;
import org.prorefactor.refactor.LooseChain;
import org.prorefactor.refactor.Refactor;
import org.prorefactor.refactor.RefactorException;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.refactor.Rollback;
import org.prorefactor.refactor.ScanLib;
import org.prorefactor.refactor.ScanManager;



/**
 * Find declarations which need to be "bubbled" to the top of the compile unit.
 */
public class BubbleDecsRefactor {


	/** You may create the object with an existing Rollback, or else pass
	 * in null to have the constructor create a new rollback.
	 */
	public BubbleDecsRefactor(Rollback rollback) {
		if (rollback!=null) this.rollback = rollback;
		else this.rollback = new Rollback();
	}

	public ArrayList messages = new ArrayList();

	ListingParser listingParser;
	// methodChain is for functions and procedures
	// At the end of processing, we simply write methodChain immediately after unsharedChain
	LooseChain methodChain = new LooseChain();
	LooseChain sharedChain = new LooseChain();
	LooseChain unsharedChain = new LooseChain();
	ScanManager scanManager = new ScanManager();

	protected static final int POSITION_OK = 49001;

	private int insertNonSharedPoint = 0;
	private int insertSharedPoint = 0;
	private int mainScanNum = 0;
	/** We watch for {&_proparse_ bdr_ignore_begin}
	 * and {&_proparse_ bdr_ignore_end}, which demarcate sections of code
	 * which should be ignored for bubbling.
	 */
	private int ignoreSectionDepth = 0;

	private HashSet ignoreIndexes;
	private JPUtil plus1 = JPUtil.getInstance();
	private ProparseLdr parser = ProparseLdr.getInstance();
	private Refactor refactorlib = Refactor.getInstance();
	protected Rollback rollback = null;
	private ScanLib scanlib = ScanLib.getInstance();
	private String mainFilename = null;

	// We keep both a map and an array of include files,
	// because we want both fast lookups as well as in-sequence processing.
	private ArrayList includeFilesList = new ArrayList();
	private HashMap includeFilesMap = new HashMap();



	private void examineNode(int node) throws RefactorException, IOException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int type = parser.getNodeTypeI(node);
		if (skipSection(node, type)) return;
		if (	type==TokenTypes.DEFINE
			&&	parser.attrGetI(node, IConstants.STATEHEAD) == IConstants.TRUE
			) {
			int index = parser.getNodeFileIndex(node);
			if (parser.attrGetI(node, POSITION_OK)==1 && index==0) return;
			if (index == 0) {
				refactorlib.cutToChain(
					node, mainScanNum
					, plus1.isDefineShared(node) ? sharedChain : unsharedChain
					);
			} else {
				fetchBubbleDecsInclude(index).processDefine(node);
			}
		}
	} // examineNode()



	/** Examine a node, processing method nodes as necessary.
	 */
	private void examineNodeForMethods(int node, int type)
			throws RefactorException, IOException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		if (skipSection(node, type)) return;
		if (type!=TokenTypes.FUNCTION && type!=TokenTypes.PROCEDURE) return;
		int index = parser.getNodeFileIndex(node);
		if (parser.attrGetI(node, POSITION_OK)==1 && parser.getNodeFileIndex(node)==0) return;
		if (index == 0) {
			refactorlib.cutToChain(node, mainScanNum, methodChain);
		} else {
			fetchBubbleDecsInclude(index).processMethod(node);
		}
	} // examineNodeForMethods



	protected BubbleDecsInclude fetchBubbleDecsInclude(int index)
			throws IOException, RefactorException {
		Integer fileIndex = new Integer(index);
		BubbleDecsInclude theInclude = null;
		if (includeFilesMap.containsKey(fileIndex)) {
			theInclude = (BubbleDecsInclude) includeFilesMap.get(fileIndex);
		} else {
			int scanNum = scanManager.getScanNumFromIndex(index);
			theInclude = new BubbleDecsInclude(fileIndex.intValue(), scanNum, this);
			includeFilesMap.put(fileIndex, theInclude);
			includeFilesList.add(theInclude);
		}
		return theInclude;
	} // fetchBubbleDecsInclude



	/** Check if a method node can be marked POSITION_OK.
	 * We don't bubble if the method or procedure is at the beginning or at the end
	 * of the primary program file.
	 */
	private void findOKMethods(int topNode) {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int h = parser.getHandle();
		try {
			int type = parser.nodeFirstChildI(topNode, h);
			while (true) {
				if (! findOKMethodsSub(h, type)) break;
				type = parser.nodeNextSiblingI(h, h);
				// If we are in a program with nothing but declarations, we're done.
				if (type == TokenTypes.Program_tail) return;
			}
			// Now search backward from the end
			plus1.lastChild(topNode, h); // Program_tail
			parser.nodePrevSibling(h, h);
			while (true) {
				if (! findOKMethodsSub(h, parser.getNodeTypeI(h))) break;
				String stype = parser.nodePrevSibling(h, h);
				assert stype.length() > 0;
			}
		} finally {
			parser.releaseHandle(h);
		}
	} // findOKMethods
	/** Set method POSITION_OK attribute.
	 * Returns true if the node is a declaration (method or DEFINE).
	 */
	private boolean findOKMethodsSub(int node, int type) {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		switch (type) {
		case TokenTypes.FUNCTION :
		case TokenTypes.PROCEDURE :
			parser.attrSet(node, POSITION_OK, 1);
			return true;
		case TokenTypes.DEFINE :
			return true;
		}
		return false;
	} // findOKMethodsSub



	/** Gather a list of include files to ignore.
	 * In prorefactor/projects/projectname/bdr_ignore.txt, there is a line
	 * delimited list of include file names which we skip. We have to find
	 * if any of those match files referenced by this compile unit, and then
	 * for those that match, we also have to ignore the nested include files.
	 */
	private HashSet gatherIgnoreIndexes() throws IOException {
		HashSet ignoreIndexesSet = new HashSet();

		// Gather the ignore set.
		File configFile = new File(RefactorSession.getProjectsDirName() + RefactorSession.getInstance().getProjectName() + "/bdr_ignore.txt");
		if (! configFile.exists()) return ignoreIndexesSet;
		BufferedReader in = new BufferedReader(new FileReader(configFile));
		String filename;
		HashSet ignoreNamesSet = new HashSet();
		while ((filename = in.readLine()) != null) {
			// Ignoring blank lines is a bugfix. If a blank name gets added
			// to the list, then it matches with the weird blank fileName/fileIndex
			// entry in Proparse's list, and then we try to scan ""...
			if (filename.trim().length()!=0) ignoreNamesSet.add(filename);
		}
		in.close();

		// Find file indexes which are in the ignore set.
		Set indexSet = listingParser.fileIndexes.entrySet();
		for (Iterator it = indexSet.iterator(); it.hasNext(); ) {
			Map.Entry entry = (Map.Entry) it.next();
			Integer index = (Integer) entry.getKey();
			String fullpath = (String) entry.getValue();
			File file = new File(fullpath);
			if (ignoreNamesSet.contains(file.getName()))
				ignoreIndexesSet.add(index);
		}

		// Find nested include file indexes to ignore
		HashSet returnSet = new HashSet();
		returnSet.addAll(ignoreIndexesSet);
		gatherIgnoreIndexesSub(listingParser.getRoot(), ignoreIndexesSet, returnSet, 0);

		return returnSet;
	} // gatherIgnoreIndexes

	private void gatherIgnoreIndexesSub(
			MacroRef ref, HashSet ignoreIndexesSet, HashSet returnSet, int skipDepth) {
		int newDepth = skipDepth;
		if (ref instanceof IncludeRef) {
			IncludeRef include = (IncludeRef) ref;
			if (skipDepth > 0) returnSet.add(new Integer(include.fileIndex));
			if (ignoreIndexesSet.contains(new Integer(include.fileIndex))) newDepth++;
		}
		for (Iterator it = ref.macroEventList.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof MacroRef) {
				gatherIgnoreIndexesSub((MacroRef)obj, ignoreIndexesSet, returnSet, newDepth);
			}
		}
	} // gatherIgnoreIndexesSub



	private void locateInsertPoints(int topNode) {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int headNode = parser.getHandle();
		insertNonSharedPoint = parser.getHandle();
		insertSharedPoint = parser.getHandle();

		// topNode is Program_root. Get first block/statement node.
		int headNodeType = parser.nodeFirstChildI(topNode, headNode);

		// Iterate through the siblings of the top level nodes.
		// These will all be block or statement head nodes.
		// Mark all consecutive SHARED DEFINE nodes as position OK,
		// as long as they are in the top source file. 
		while (headNodeType==TokenTypes.DEFINE) {
			if (!plus1.isDefineShared(headNode)) break;
			parser.attrSet(headNode, POSITION_OK, 1);
			headNodeType = parser.nodeNextSiblingI(headNode, headNode);
		}

		setInsertPoint(headNode, insertSharedPoint);

		// Iterate through the siblings of the top level nodes.
		// These will all be block or statement head nodes.
		// Mark all consecutive non-SHARED DEFINE nodes as position OK.
		while (headNodeType==TokenTypes.DEFINE) {
			if (! plus1.isDefineShared(headNode))
				parser.attrSet(headNode, POSITION_OK, 1);
			headNodeType = parser.nodeNextSiblingI(headNode, headNode);
		}

		setInsertPoint(headNode, insertNonSharedPoint);

		parser.releaseHandle(headNode);
	}



	/**
	 * Run this refactoring.
	 * IMPORTANT: RefactorSession.enableParserListing() had to be in effect
	 * at the time that the call to parse() was made.
	 * @param topNode The topmost node in the AST to start the lint from.
	 * @return A string representing any error message, null or empty if no error
	 * @see org.prorefactor.refactor.tfnames.NamesTarget
	 * The client becomes the "owner" of the handles referred to in the
	 * NamesTarget records, and is responsible for releasing those when done.
	 */
	public String run(int topNode) throws IOException, RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();

		// Create a scanner token list for the main program file.
		// The file index for the main program file is always 0.
		mainScanNum = scanManager.getScanNumFromIndex(0);
		mainFilename = parser.getIndexFilename(0);

		// Parse the prepo listing
		listingParser = new ListingParser(RefactorSession.getListingFileName());
		try {
			listingParser.parse();
		} catch (IOException e) {
			return "Error parsing the listing file: " + e.getMessage();
		}

		// Gather a set of include file indexes to ignore, generated from the list of
		// ignored include file names in "bdr_ignore.txt".
		ignoreIndexes = gatherIgnoreIndexes();
		for (Iterator it = ignoreIndexes.iterator(); it.hasNext();) {
			Integer index = (Integer) it.next();
			fetchBubbleDecsInclude(index.intValue()).doNotSplit();
		}

		locateInsertPoints(topNode);

		try {
			walker(topNode); // deals with DEFINE statements
			if (ignoreSectionDepth!=0) throw new RefactorException("Failed to match all proparse bdr_ignore begin and end directives");
			runForMethods(topNode); // deals with FUNCTION and PROCEDURE statements and blocks.
			// Last includefile changes before sweep and write
			for (Iterator it = includeFilesList.iterator(); it.hasNext(); ) {
				((BubbleDecsInclude)it.next()).preWrite();
			}
		} catch (RefactorException e) {
			return e.getMessage();
		}

		// Include files sweep and write, append new include refs
		for (Iterator it = includeFilesList.iterator(); it.hasNext(); ) {
			try {
				((BubbleDecsInclude)it.next()).writeFiles();
			} catch (IOException e) {
				messages.add(e.getMessage());
			}
		}

		// Insert new DEFINE chains
		if (! sharedChain.isEmpty()) {
			scanlib.insertSection(
				sharedChain.getStartHandle()
				, sharedChain.getEndHandle()
				, insertSharedPoint
				);
		}
		if (! methodChain.isEmpty()) {
			scanlib.insertSection( 
				methodChain.getStartHandle()
				, methodChain.getEndHandle()
				, insertSharedPoint
				);
		}
		if (! unsharedChain.isEmpty()) {
			scanlib.insertSection( 
				unsharedChain.getStartHandle()
				, unsharedChain.getEndHandle()
				, insertNonSharedPoint
				);
		}

		// Main file sweep and write
		scanlib.sweep(mainScanNum);
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int tempHandle = parser.getHandle();
		parser.parseGetTop(mainScanNum, tempHandle);
		String refname = parser.getNodeFilename(tempHandle);
		String fullpath = FileStuff.findFile(refname).getCanonicalPath();
		rollback.preserveAndWrite(tempHandle, refname, fullpath);

		return null;

	} // run()



	/** Find functions and method declarations which need to be bubbled.
	 */
	private void runForMethods(int topNode) throws RefactorException, IOException {
		int h = parser.getHandle();
		try {
			findOKMethods(topNode);
			int type = parser.nodeFirstChildI(topNode, h);
			while (true) {
				examineNodeForMethods(h, type);
				type = parser.nodeNextSiblingI(h, h);
				if (type < 1) break;
			}
		} finally {
			parser.releaseHandle(h);
		}
	} // runForMethods



	private void setInsertPoint(int astHandle, int scanHandle) {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int sibHandle = parser.getHandle();
		try {
			parser.copyHandle(astHandle, sibHandle);
			while (true) {
				// Here we step backwards (if necessary) until we are back in the
				// topmost file. We don't want to set an insert point in an include file!
				if (refactorlib.getInsertPointInFile(
						sibHandle, mainFilename, mainScanNum, scanHandle, listingParser) )
					return;
				if (parser.nodePrevSibling(sibHandle, sibHandle).length()==0)
					break;
			}
			// If we get to the first sibling and we are still in an include file,
			// then {include.i} was at the very top of the main file and our
			// insert point is the front of the main file.
			parser.parseGetTop(mainScanNum, scanHandle); // Scanner_head
			parser.nodeNextSiblingI(scanHandle, scanHandle);
		} finally {
			parser.releaseHandle(sibHandle);
		}
	} // setInsertPoint()



	/** Watch for sections to be ignored.
	 */
	private boolean skipSection(int h, int type) {
		if (type==TokenTypes.PROPARSEDIRECTIVE) {
			String directive = parser.attrGetS(h, "proparsedirective");
			if (directive.equals("bdr_ignore_begin")) {
				ignoreSectionDepth++;
			}
			if (directive.equals("bdr_ignore_end")) {
				ignoreSectionDepth--;
			}
		}
		if (ignoreSectionDepth > 0) return true;
		return false;
	} // skipSection



	/**
	 * This determines if a block can be skipped from the examination.
	 * The blocks that we are not interested in the contents of are
	 * PROCEDURE blocks, UDF blocks, and TRIGGER blocks.
	 * Those three have their own scoping for defined symbols.
	 * @param node
	 * @return True if the block is to be skipped.
	 */
	private boolean skipSub(int node) {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		switch (parser.getNodeTypeI(node)) {
		case TokenTypes.FUNCTION :
		case TokenTypes.ON :
		case TokenTypes.PROCEDURE :
			if (parser.attrGetI(node, IConstants.STATEHEAD) == IConstants.TRUE) return true;
			return false;
		default :
			return false;
		}
	} // skipSub()



	private void walker(int node) throws RefactorException, IOException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int nextNode = parser.getHandle();
		if (! skipSub(node)) {
			if (parser.nodeFirstChildI(node, nextNode)!=0) walker(nextNode);
			examineNode(node);
		}
		if (parser.nodeNextSiblingI(node, nextNode)!=0) walker(nextNode);
		parser.releaseHandle(nextNode);
	} // walker()


} // class BubbleDecsRefactor

