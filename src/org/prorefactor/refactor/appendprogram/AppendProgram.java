///* AppendProgram.java
// * Created on Dec 15, 2003
// * John Green
// *
// * Copyright (C) 2003 Joanju Limited
// * All rights reserved. This program and the accompanying materials 
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// */
//package org.prorefactor.refactor.appendprogram;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.Iterator;
//
//import org.prorefactor.core.IConstants;
//import org.prorefactor.core.TokenTypes;
//import org.prorefactor.macrolevel.IncludeRef;
//import org.prorefactor.macrolevel.ListingParser;
//import org.prorefactor.refactor.FileStuff;
//import org.prorefactor.refactor.Refactor;
//import org.prorefactor.refactor.RefactorException;
//import org.prorefactor.refactor.RefactorSession;
//import org.prorefactor.refactor.Rollback;
//import org.prorefactor.refactor.Scan;
//import org.prorefactor.refactor.ScanIncludeRef;
//import org.prorefactor.refactor.ScanLib;
//import org.prorefactor.refactor.ScanManager;
//import org.prorefactor.refactor.bubbledecs.BubbleDecsRefactor;
//
//import com.joanju.ProparseLdr;
//
//
//
///** The "Append Program" Refactoring. Appends "second.p" to the end
// * of "first.p", and deals with removing duplicate includes, defines,
// * etc. Should be called after "Bubble Declarations" and "Wrap
// * Procedure Block" refactorings have been done to both the first and
// * second files.
// * This refactor spawns a BubbleDecsRefactor once complete - see
// * this object's "bubbler" member.
// */
//public class AppendProgram {
//
//	public AppendProgram(Rollback rollback) {
//		if (rollback!=null) this.rollback = rollback;
//		else this.rollback = new Rollback();
//		bubbler = new BubbleDecsRefactor(rollback);
//	}
//
//	public ArrayList messageList = new ArrayList();
//	public BubbleDecsRefactor bubbler;
//	private HashSet defineStrings = new HashSet();
//	private HashSet includeStrings = new HashSet();
//	private ListingParser listingParser = new ListingParser(RefactorSession.getListingFileName());
//	private ProparseLdr parser = ProparseLdr.getInstance();
//	private Refactor refactorlib = Refactor.getInstance();
//	private RefactorSession refpack = RefactorSession.getInstance();
//	private Rollback rollback = null;
//	private Scan mainScan;
//	private ScanLib scanlib = ScanLib.getInstance();
//	private ScanManager scanManager = new ScanManager();
//
//
//
//	public void appendFile(File first, File second) throws IOException {
//		BufferedWriter writer = new BufferedWriter(new FileWriter(first, true));
//		BufferedReader reader = new BufferedReader(new FileReader(second));
//		try {
//			writer.write(FileStuff.LINESEP + FileStuff.LINESEP + FileStuff.LINESEP);
//			int c;
//			while ((c = reader.read()) != -1) writer.write(c);
//		} finally {
//			reader.close();
//			writer.close();
//		}
//	} // appendFile
//
//
//
//	/** Once this refactoring has been completed, we (re-) apply
//	 * the "Bubble Declarations" refactoring.
//	 */
//	private String callBubbleDeclarations(File file)
//			throws IOException, RefactorException {
//		try {
//			refpack.enableParserListing();
//			if (parser.parse(file.toString()) < 1)
//				throw new IOException(parser.errorGetText());
//			int top = parser.getHandle();
//			parser.nodeTop(top);
//			return bubbler.run(top);
//		} finally {
//			refpack.disableParserListing();
//		}
//
//		
//	} // callBubbleDeclarations
//
//
//
//	/** Get an extremely simplistic signature of a statement.
//	 * We use this to determine if we are going to drop a duplicate.
//	 * This creates a string of space separated tokens. For all
//	 * nodes in the statement which are "node-type-keyword", we
//	 * append the string node type. For all others, we append the
//	 * lowercase value of the node text. It's easily fooled by
//	 * re-arranging keywords. We're not too worried about that for
//	 * this refactoring. 
//	 */
//	private String getStatementSignature(int node) {
//		final String QNAME = "getstatementsignature.appendprogram.joanju.com";
//		int h = parser.getHandle();
//		int numResults = parser.queryCreate(node, QNAME, "");
//		try {
//			StringBuffer buff = new StringBuffer("");
//			for (int i = 1; i <= numResults; i++) {
//				parser.queryGetResult(QNAME, i, h);
//				if (parser.attrGetI(h, IConstants.NODE_TYPE_KEYWORD) == IConstants.TRUE)
//					buff.append(parser.getNodeType(h));
//				else
//					buff.append(parser.getNodeText(h).toLowerCase());
//				buff.append(" ");
//			}
//			return buff.toString();
//		} finally {
//			parser.releaseHandle(h);
//			parser.queryClear(QNAME);
//		}
//	} // getStatementSignature
//
//
//
//	private void removeDuplicateIncludes() throws RefactorException {
//		IncludeRef listingRoot = listingParser.getRoot();
//		for (Iterator it = listingRoot.macroEventList.iterator(); it.hasNext();) {
//			Object obj = it.next();
//			if (! (obj instanceof IncludeRef)) continue;
//			IncludeRef liref = (IncludeRef) obj;
//			ScanIncludeRef siref = new ScanIncludeRef(liref.getPosition(), mainScan.scanNum);
//			if (includeStrings.contains(siref.entireReference)) {
//				siref.markToBeCut();
//			} else {
//				includeStrings.add(siref.entireReference);
//			}
//		}
//	} // removeDuplicateIncludes
//
//
//
//	/** Remove duplicate DEFINE SHARED statements.
//	 * Note that we do NOT remove duplicate NEW SHARED statements.
//	 * For those, we want to programmer to review the duplicate, because
//	 * it might be a problem - each of the appended procedures might have
//	 * a reason for defining its own new one.
//	 */
//	private void removeDuplicateShared() throws RefactorException {
//		int h1 = parser.getHandle();
//		int h2 = parser.getHandle();
//		try {
//			parser.nodeTop(h1);
//			for (	int type = parser.nodeFirstChildI(h1, h1);
//					type > 1;
//					type = parser.nodeNextSiblingI(h1, h1) ) {
//				if (type != TokenTypes.DEFINE) continue;
//				if (parser.nodeFirstChildI(h1, h2) != TokenTypes.SHARED) continue;
//				String sig = getStatementSignature(h1);
//				if (defineStrings.contains(sig)) {
//					refactorlib.markToBeCut(h1, mainScan.scanNum);
//				} else {
//					defineStrings.add(sig);
//				}
//			}
//		} finally {
//			parser.releaseHandle(h1);
//			parser.releaseHandle(h2);
//		}
//	} // removeDuplicateShared
//		
//
//
//	/** Remove external "bits" from second.p - the parameter statements
//	 * for the .p as well as the RUN statement which ran second_main if
//	 * it had one.
//	 */
//	private void removeSecondExternals(int firstNumLines, File second)
//			throws RefactorException {
//		assert parser.errorGetStatus()==0 : parser.errorGetText();
//		String name = second.getName();
//		int dotpos = name.lastIndexOf('.');
//		name = name.substring(0, dotpos) + "_main";
//		int hHead = parser.getHandle();
//		int h1 =  parser.getHandle();
//		try {
//			parser.nodeTop(hHead);
//			int type = parser.nodeFirstChildI(hHead, hHead);
//			head_loop:
//			for ( ; type>0; type = parser.nodeNextSiblingI(hHead, hHead)) {
//				if (parser.getNodeLine(hHead) <= firstNumLines) continue;
//				if (parser.getNodeFileIndex(hHead) != 0) continue;
//				if (	type == TokenTypes.DEFINE
//					&&	parser.attrGetS(hHead, "state2").equals("PARAMETER") ) {
//					refactorlib.markToBeCut(hHead, mainScan.scanNum);
//				}
//				if (type == TokenTypes.RUN) {
//					int type2 = parser.nodeFirstChildI(hHead, h1);
//					if (	type2 == TokenTypes.FILENAME
//						&&	parser.getNodeText(h1).equals(name) )
//						refactorlib.markToBeCut(hHead, mainScan.scanNum); 
//				}
//			}
//		} finally {
//			parser.releaseHandle(hHead);
//			parser.releaseHandle(h1);
//		}
//	} // removeSecondExternals
//
//
//
//	/** Run this refactoring.
//	 * IMPORTANT: After running, you should examine this object's "messageList"
//	 * member, as well as examine "bubbler" for any extra details from that
//	 * refactoring.
//	 * @param first The file append "second" to.
//	 * @param second The file to append to "first".
//	 * @return The return (error) value from BubbleDecsRefactor, if any.
//	 * @throws IOException
//	 * @throws RefactorException
//	 */
//	public String run(File first, File second) throws IOException, RefactorException {
//		assert parser.errorGetStatus()==0 : parser.errorGetText();
//
//		rollback.preserve(first.toString(), first.getCanonicalPath());
//
//		int firstNumLines = FileStuff.countLines(first);
//
//		appendFile(first, second);
//
//		try {
//			refpack.enableParserListing();
//			if (parser.parse(first.toString()) < 1)
//				throw new IOException(parser.errorGetText());
//		} finally {
//			refpack.disableParserListing();
//		}
//		listingParser.parse();
//		mainScan = scanManager.getScanObjectFromIndex(0);
//
//		removeDuplicateIncludes();
//		removeDuplicateShared();
//		removeSecondExternals(firstNumLines, second);
//
//		scanlib.sweep(mainScan.scanNum);
//		if (parser.writeNode(mainScan.getTopNode(), first.getCanonicalPath()) < 1) {
//			throw new IOException(parser.errorGetText());
//		}
//
//		// callBubbleDeclarations will do a new parse. The old parse is no longer valid.
//		scanManager = null;
//		mainScan = null;
//
//		String retString = callBubbleDeclarations(first);
//
//		// Line numbers will have changed, so *after* all refactoring
//		// is done and files are written, do a case-insensitive search
//		// through the compile-unit's files for "second.p".
//		messageList.addAll(
//			FileStuff.searchFilesInCompileUnit(
//				second.getName(), "Found text '" + second.getName() + "'" ) );
//
//		return retString;
//
//	} // run
//
//
//
//} // class AppendProgram
