/* WrapProcedure.java
 * Created on Dec 11, 2003
 * John Green
 *
 * Copyright (C) 2003-2004 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.wrapproc;

import java.io.File;
import java.io.IOException;

import org.prorefactor.core.HandleCollection;
import org.prorefactor.core.JPUtil;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.macrolevel.ListingParser;
import org.prorefactor.refactor.FileStuff;
import org.prorefactor.refactor.Refactor;
import org.prorefactor.refactor.RefactorException;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.refactor.Rollback;
import org.prorefactor.refactor.Scan;
import org.prorefactor.refactor.ScanLib;
import org.prorefactor.refactor.ScanManager;

import com.joanju.ProparseLdr;

/** Wrap the procedural (non declarative) code of a .p
 * into an internal PROCEDURE. 
 */
public class WrapProcedure {

	public WrapProcedure(int topNode, Rollback rollback) throws IOException {
		this.topNode = topNode;
		scan = scanManager.getScanObjectFromIndex(0);
		filename = parser.getIndexFilename(0);
		if (rollback!=null) this.rollback = rollback;
		else this.rollback = new Rollback();
	}

	private int topNode;
	private HandleCollection handler;
	private JPUtil plus1 = JPUtil.getInstance();
	private ListingParser listingParser;
	private ProparseLdr parser = ProparseLdr.getInstance();
	private Refactor refactorlib = Refactor.getInstance();
	private RefactorSession rpack = RefactorSession.getInstance();
	private Rollback rollback = null;
	private Scan scan;
	private ScanLib scanlib = ScanLib.getInstance();
	private ScanManager scanManager = new ScanManager();
	private String filename;
	private String parameters = FileStuff.LINESEP;
	private StringBuffer signature = new StringBuffer("");



	private void addParameter(int node) throws RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int begin = parser.getHandle();
		int end = parser.getHandle();
		parser.parseGetTop(scan.scanNum, begin);
		parser.parseGetTop(scan.scanNum, end);
		try {
			if (refactorlib.getSectionWithComments(node, begin, end) < 1)
				throw new RefactorException(node, "Failed to get section to copy");
			parameters = parameters + scanlib.copyToText(begin, end) + FileStuff.LINESEP;
		} finally {
			parser.releaseHandle(begin);
			parser.releaseHandle(end);
		}
	} // addParameter



	private void addSignature(int h1, int h2, int h3) {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		if (signature.length() > 0) signature.append(", ");
		signature.append(parser.getNodeText(h1));
		signature.append(" ");
		signature.append(parser.getNodeText(h2));
		if (h3>0) {
			signature.append(" ");
			signature.append(parser.getNodeText(h3));
		}
	} // addSignature



	private void findProcedureInsert(int intoHandle) throws RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int h = handler.gimme();
		int type = parser.nodeFirstChildI(topNode, h);
		main_loop:
		while (type>0) {
			switch (type) {
			case TokenTypes.FUNCTION :
			case TokenTypes.PROCEDURE :
				break;
			case TokenTypes.DEFINE :
				if (! plus1.isDefineShared(h)) break main_loop;
			}
			type = parser.nodeNextSiblingI(h, h);
			// If we are in a program with nothing but declarations, we're done.
			if (type == TokenTypes.Program_tail)
				throw new RefactorException("No procedural code in program");
		}
		assert type>0;
		if (! refactorlib.getInsertPointInFile(h, filename, scan.scanNum, intoHandle, listingParser) )
			throw new RefactorException(h, "Could not set PROCEDURE statement insert point.");
	} // findProcedureInsert



	private void findProcedureEndInsert(int intoHandle) throws RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int h = handler.gimme();
		plus1.lastChild(topNode, h); // Program_tail
		String type = parser.nodePrevSibling(h, h);
		while (type.length()>0) {
			if (! (type.equals("FUNCTION") || type.equals("PROCEDURE")) ) break;
			type = parser.nodePrevSibling(h, h);
		}
		parser.nodeNextSiblingI(h, h);
		if (! refactorlib.getInsertPointInFile(h, filename, scan.scanNum, intoHandle, listingParser) )
			throw new RefactorException("Could not find PROCEDURE END insert point.");
	} // findProcedureEndInsert



	private void findProgramParameterInsert(int intoHandle) throws RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int h = handler.gimme();
		parser.nodeFirstChildI(topNode, h);
		if (! refactorlib.getInsertPointInFile(h, filename, scan.scanNum, intoHandle, listingParser) )
			throw new RefactorException("Could not find PARAMETERs insert point.");
	}



	private void getParameters() throws RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		int hHead = handler.gimme();
		int h1 =  handler.gimme();
		int h2 =  handler.gimme();
		int h3 =  handler.gimme();
		parser.nodeTop(hHead);
		int type = parser.nodeFirstChildI(hHead, hHead);
		head_loop:
		for ( ; type>0; type = parser.nodeNextSiblingI(hHead, hHead)) {
			if (type!=TokenTypes.DEFINE) continue;
			if ( ! parser.attrGetS(hHead, "state2").equals("PARAMETER") ) continue head_loop;
			addParameter(hHead);
			switch (parser.nodeFirstChildI(hHead, h1)) {
			case TokenTypes.PARAMETER :
				// Expect: DEFINE PARAMETER BUFFER ID
				if (parser.nodeNextSiblingI(h1, h1) != TokenTypes.BUFFER) break;
				if (parser.nodeNextSiblingI(h1, h2) != TokenTypes.ID) break;
				addSignature(h1, h2, 0); // BUFFER ID
				break;
			case TokenTypes.INPUT :
			case TokenTypes.INPUTOUTPUT :
			case TokenTypes.OUTPUT :
				// Expect: DEFINE (i|o|io) PARAMETER
				if (parser.nodeNextSiblingI(h1, h2) != TokenTypes.PARAMETER) break;
				switch (parser.nodeNextSiblingI(h2, h2)) {
				case TokenTypes.ID :
					addSignature(h1, h2, 0); // (i|o|io) ID
					break;
				case TokenTypes.TABLE :
					// Expect: DEFINE (i|o|io) TABLE FOR RECORD_NAME
					if (parser.nodeNextSiblingI(h2, h3) != TokenTypes.FOR) break;
					if (parser.nodeNextSiblingI(h3, h3) != TokenTypes.RECORD_NAME) break;
					addSignature(h1, h2, h3); // (i|o|io) TABLE RECORD_NAME
					break;
				case TokenTypes.TABLEHANDLE :
					// Expect: DEFINE (i|o|io) TABLEHANDLE (FOR)? ID
					int t;
					if ( (t = parser.nodeNextSiblingI(h2, h3)) == TokenTypes.FOR)
						t = parser.nodeNextSiblingI(h3, h3);
					if (t != TokenTypes.ID) break;
					addSignature(h1, h2, h3); // (i|o|io) TABLEHANDLE ID
				}
			}
		}
	} // getParameters



	private String getProcedureName() {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		File file = new File(parser.getIndexFilename(0));
		String name = file.getName();
		int dotpos = name.lastIndexOf('.');
		return name.substring(0, dotpos) + "_main";
	} // getProcedureName



	private String getProcedureStatement(String procedureName) {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		StringBuffer s = new StringBuffer();
		if (rpack.getProparseSettings().capKeyword)
			s.append("PROCEDURE ");
		else
			s.append("procedure ");
		s.append(procedureName);
		s.append(":");
		return s.toString();
	} // getProcedureStatement



	private String getProcedureEndStatement(String procedureName) {
		StringBuffer s = new StringBuffer();
		if (rpack.getProparseSettings().capKeyword)
			s.append("END PROCEDURE. /* ");
		else
			s.append("end procedure. /* ");
		s.append(procedureName);
		s.append(" */");
		return s.toString();
	} // getProcedureEndStatement



	private String getRunStatement(String procedureName) {
		StringBuffer s = new StringBuffer();
		if (rpack.getProparseSettings().capKeyword)
			s.append("RUN ");
		else
			s.append("run ");
		s.append(procedureName);
		if (signature!=null && signature.length()>0) {
			s.append(" (");
			s.append(signature);
			s.append(")");
		}
		s.append(".");
		return s.toString();
	} // getRunStatement



	/** Launch this refactoring.
	 * IMPORTANT: RefactorSession.enableParserListing() had to be in effect
	 * at the time that the call to parse() was made.
	 */
	public void run() throws IOException, RefactorException {
		assert parser.errorGetStatus()==0 : parser.errorGetText();
		handler = new HandleCollection();
		int procedureInsertPoint = handler.gimme();
		int procedureEndInsertPoint = handler.gimme();
		int programParameterInsertPoint = handler.gimme();

		// Parse the prepo listing
		listingParser = new ListingParser(RefactorSession.getListingFileName());
		listingParser.parse();

		try {
			findProcedureInsert(procedureInsertPoint);
			findProcedureEndInsert(procedureEndInsertPoint);
			findProgramParameterInsert(programParameterInsertPoint);
			getParameters();
			String procedureName = getProcedureName();
			int indentBegin = parser.getNodeLine(procedureInsertPoint);
			if (parser.getNodeColumn(procedureInsertPoint) !=1 ) indentBegin++;
			scan.indent(
				indentBegin
				, parser.getNodeLine(procedureEndInsertPoint)
				);
			parser.setNodeText(
				procedureInsertPoint
				,	FileStuff.LINESEP + FileStuff.LINESEP 
					+ getRunStatement(procedureName)
					+ FileStuff.LINESEP + FileStuff.LINESEP
					+ getProcedureStatement(procedureName) + FileStuff.LINESEP
					+ parser.getNodeText(procedureInsertPoint)
				);
			parser.setNodeText(
				programParameterInsertPoint
				,	FileStuff.LINESEP + parameters + FileStuff.LINESEP
					+ parser.getNodeText(programParameterInsertPoint)
				);
			parser.setNodeText(
				procedureEndInsertPoint
				,	FileStuff.LINESEP + FileStuff.LINESEP 
					+ getProcedureEndStatement(procedureName) + FileStuff.LINESEP
					+ parser.getNodeText(procedureEndInsertPoint)
				);

			int scanTop = handler.gimme();
			parser.parseGetTop(scan.scanNum, scanTop);
			String refname = parser.getIndexFilename(0);
			String fullpath = FileStuff.findFile(refname).getCanonicalPath();
			rollback.preserveAndWrite(scanTop, refname, fullpath);

		} finally {
			handler.releaseAll();
		}
	} // run()



} // class WrapProcedure
