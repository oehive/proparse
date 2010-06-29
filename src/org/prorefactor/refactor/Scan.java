/* Scan.java
 * Created on Dec 11, 2003
 * John Green
 *
 * Copyright (C) 2003 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor;

import com.joanju.ProparseLdr;

import java.io.IOException;

import org.prorefactor.core.TokenTypes;


/** Scan objects are managed by a ScanManager.
 * By having scan objects, scans can come and go, attributes
 * can be set, etc. These are for working with scans that are
 * related to an AST, and not intended for use with "standalone
 * scans".
 */
public class Scan {

	public Scan(int fileIndex) throws IOException {
		this.fileIndex = fileIndex;
		scanNum = parser.parseCreate("scan", parser.getIndexFilename(fileIndex));
		if (scanNum<0) throw new IOException(parser.errorGetText());
		indentString = rpack.getIndentString();
	}

	private boolean wasQStringSync = false;
	public int fileIndex;
	public int scanNum;
	private int topNode = 0;
	private ProparseLdr parser = ProparseLdr.getInstance();
	private Refactor reflib = Refactor.getInstance();
	private RefactorSession rpack = RefactorSession.getInstance();
	private ScanLib scanlib = ScanLib.getInstance();
	private String indentString;



	/** Assuming that the top node will never change (it shouldn't)
	 * then it's safe to hang on to a handle to that top node. It's
	 * something we need to refer to quite often, this is just a
	 * convenience function.
	 */
	public int getTopNode() {
		if (topNode==0) {
			topNode = parser.getHandle();
			parser.parseGetTop(scanNum, topNode);
		}
		return topNode;
	} // getTopNode



	/** Indent the code, using the current project's "indent" settings.
	 * Synchronizes as many QSTRING nodes from the AST as possible first,
	 * so that multi-line quoted strings don't get text added to them.
	 * @param begin first line to be indented.
	 * @param end last line to be indented. Use 0 to indent to end.
	 */
	public void indent(int begin, int end) {
		syncAllQStrings();
		int h = parser.getHandle();
		try {
			parser.parseGetTop(scanNum, h); // Scanner_head
			int type = parser.nodeNextSiblingI(h, h); // first token
			if (begin<=1) {
				parser.setNodeText(h, indentString + parser.getNodeText(h));
			}
			while (type > 0) {
				if (type == TokenTypes.NEWLINE) {
					int line = parser.getNodeLine(h) + 1;
					if (	line >= begin
						&&	(	end < 1
							||	line <= end
							)
						)
						parser.setNodeText(h, parser.getNodeText(h) + indentString);
				}
				type = parser.nodeNextSiblingI(h, h);
			}
		} finally {
			parser.releaseHandle(h);
		}
	} // indent



	/** One reason for synchronizing QSTRING nodes into a scanner
	 * is for the indenting functions: We sync all QSTRING nodes 
	 * so that we don't add spaces (or tab) into the middle of a quoted string;
	 * (the spaces/tabs are only added to NEWLINE tokens).
	 * @param queryName The query for all QSTRING nodes must have been created
	 * by the client.
	 * @param numResults The number of results from the query.
	 */
	public void syncAllQStrings() {
		if (wasQStringSync) return;
		wasQStringSync = true;
		int node = parser.getHandle();
		int token = parser.getHandle();
		int topNode = parser.getHandle();
		try {
			final String QUERYNAME = "org.prorefactor.refactor.Scan.qstrings";
			parser.nodeTop(topNode);
			int numResults = parser.queryCreate(topNode, QUERYNAME, "QSTRING");
			for (int i = 1; i <= numResults; i++) {
				parser.queryGetResult(QUERYNAME, i, node);
				if (parser.getNodeFileIndex(node) != fileIndex) continue;
				scanlib.seekFromTop(
					parser.getNodeLine(node)
					, parser.getNodeColumn(node)
					, scanNum
					, token
					);
				reflib.nsync(node, token);
			}
		} finally {
			parser.releaseHandle(node);
			parser.releaseHandle(token);
			parser.releaseHandle(topNode);
		}
	} // syncAllQStrings



} // class Scan
