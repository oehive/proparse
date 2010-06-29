///* QualifyFieldsRefactor.java
// * Created on Feb 16, 2004
// * John Green
// *
// * Copyright (C) 2004 Joanju Limited
// * All rights reserved. This program and the accompanying materials 
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// */
//package org.prorefactor.refactor.action;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Iterator;
//
//import org.prorefactor.core.ICallback;
//import org.prorefactor.core.IConstants;
//import org.prorefactor.core.JPNode;
//import org.prorefactor.core.PRCException;
//import org.prorefactor.nodetypes.FieldRefNode;
//import org.prorefactor.refactor.FileStuff;
//import org.prorefactor.refactor.RefactorException;
//import org.prorefactor.refactor.ScanLib;
//import org.prorefactor.treeparser.FieldBuffer;
//import org.prorefactor.treeparser.TreeParserWrapper;
//import org.prorefactor.treeparser01.TreeParser01;
//
//import com.joanju.ProparseLdr;
//
//
///** Add the table name to unqualified fields. */
//public class QualifyFieldsRefactor {
//
//	public QualifyFieldsRefactor() { }
//
//	protected boolean useCaps = false;
//	protected int sourceFileIndex;
//	protected ArrayList nodeList = new ArrayList();
//	private File sourceFile;
//	
//	protected class UnqualNode {
//		UnqualNode(JPNode idNode, FieldRefNode refNode) {
//			this.idNode = idNode;
//			this.refNode = refNode;
//		}
//		JPNode idNode;
//		FieldRefNode refNode;
//	}
//	
//
//
//	public ICallback callback = new ICallback() {
//		public Object run(Object obj) {
//			if (! (obj instanceof FieldRefNode)) return null;
//			FieldRefNode refNode = (FieldRefNode) obj;
//			JPNode idNode = refNode.getIdNode();
//			if (idNode.getFileIndex() != sourceFileIndex) return null;
//			if (refNode.attrGet(IConstants.UNQUALIFIED_FIELD) == IConstants.TRUE)
//				nodeList.add(new UnqualNode(idNode, refNode));
//			return null;
//		}
//	};
//
//
//
//	public String generateNewSource() throws IOException {
//		ProparseLdr parser = ProparseLdr.getInstance();
//		int scanNum = parser.parseCreate("scan", sourceFile.getCanonicalPath());
//		if (parser.errorGetStatus() < 0) throw new IOException(parser.errorGetText());
//		int handle = parser.getHandle();
//		ScanLib scanLib = ScanLib.getInstance();
//		for (Iterator it = nodeList.iterator(); it.hasNext();) {
//			UnqualNode uqn = (UnqualNode) it.next();
//			scanLib.seekFromTop(uqn.idNode.getLine(), uqn.idNode.getColumn(), scanNum, handle);
//			// We don't mess with any preprocessor junk.
//			String fieldname = parser.getNodeText(handle);
//			if (fieldname.compareToIgnoreCase(uqn.idNode.getText()) != 0) continue;
//			// Can't be unqualified unless it's a Field - which must have a BufferSymbol.
//			FieldBuffer fieldBuff = (FieldBuffer) uqn.refNode.getSymbol();
//			assert fieldBuff != null;
//			parser.setNodeText(
//					handle
//					, fieldBuff.getBuffer().getName() + "." + fieldname
//					);
//		}
//		StringBuilder buff = new StringBuilder();
//		parser.parseGetTop(scanNum, handle);
//		while(parser.nodeNextSiblingI(handle, handle) > 0) {
//			buff.append(parser.getNodeText(handle));
//		}
//		return buff.toString();
//	} // generateNewSource
//
//
//
//	/** Run this refactor.
//	 * @param sourceFile The source file to be refactored. 
//	 * @param compileFile The source file for the compile unit for building
//	 * an AST. Is different than the sourceFile if the sourceFile is not
//	 * compilable.
//	 * @return The number of unqualified field nodes.
//	 */
//	public int run(File sourceFile, File compileFile)
//			throws RefactorException, IOException, PRCException {
//		JPNode theTree = TreeParserWrapper.run(compileFile.getPath(), new TreeParser01());
//		this.sourceFile = sourceFile;
//		sourceFileIndex = FileStuff.getFileIndex(sourceFile);
//		theTree.walk(callback);
//		return nodeList.size();
//	} // run
//
//
//
//}
