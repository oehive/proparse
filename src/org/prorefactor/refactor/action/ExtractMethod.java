///* ExtractMethod.java
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
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//
//import org.prorefactor.core.ICallback;
//import org.prorefactor.core.IConstants;
//import org.prorefactor.core.JPNode;
//import org.prorefactor.nodetypes.FieldRefNode;
//import org.prorefactor.refactor.FileStuff;
//import org.prorefactor.refactor.RefactorException;
//import org.prorefactor.refactor.RefactorSession;
//import org.prorefactor.refactor.Rollback;
//import org.prorefactor.refactor.source.CompileUnit;
//import org.prorefactor.refactor.util.Strings;
//import org.prorefactor.treeparser.CQ;
//import org.prorefactor.treeparser.FieldBuffer;
//import org.prorefactor.treeparser.Symbol;
//import org.prorefactor.treeparser.SymbolScopeRoot;
//import org.prorefactor.treeparser.Variable;
//
//
//
///** Provides the Extract Method Refactoring.
// * Extracts a selected segment of source, and moves
// * it to a new method, replacing the original segment
// * with a call to the new method. Calculates the
// * parameters necessary for data elements in the selection.
// * Places the new method at the end of the file.
// */
//public class ExtractMethod {
//
//	public ExtractMethod(Rollback rollback) {
//		// If you start using a rollback, remember to create if null:
//		// if (rollback!=null) this.rollback = rollback;
//		// else this.rollback = new Rollback();
//		useCaps = refPack.getProparseSettings().capKeyword;
//	}
//
//	protected boolean useCaps = false;
//	private int selectionFile;
//	private int [] selectionBegin;
//	private int [] selectionEnd;
//	private ArrayList symbolList = new ArrayList();
//	private CompileUnit cu;
//	private Map symbolMap = new HashMap();
//	private RefactorSession refPack = RefactorSession.getInstance();
//	private String methodName;
//	private String selectedText;
//
//	private class RefAttr {
//		RefAttr(Symbol symbol, JPNode firstRef, JPNode idNode) {
//			this.symbol = symbol;
//			this.firstId = idNode;
//			this.firstRef = firstRef;
//		}
//		boolean referencing = false;
//		boolean updating = false;
//		int refsInRange = 0;
//		JPNode firstId;
//		JPNode firstRef;
//		Symbol symbol;
//		public String getRefdWith() {
//			if (firstRef.attrGet(IConstants.UNQUALIFIED_FIELD) == IConstants.FALSE)
//				return firstId.getText();
//			FieldBuffer field = (FieldBuffer) symbol;
//			// TODO deal with case
//			return field.getBuffer().getName() + "." + field.getName();
//		}
//		public String getArgName() {
//			String refdName = firstId.getText();
//			return refdName.substring(refdName.lastIndexOf('.') + 1);
//		}
//		public String getDirectionText() {
//			if (referencing && updating) return useCaps ? "INPUT-OUTPUT" : "input-output";
//			else if (updating) return useCaps ? "OUTPUT" : "output";
//			else return useCaps ? "INPUT" : "input";
//		}
//		public String toString() {
//			StringBuffer buff = new StringBuffer(useCaps ? "DEFINE " : "define ");
//			buff.append(getDirectionText());
//			buff.append(useCaps ? " PARAMETER " : " parameter ");
//			buff.append(getArgName() + " ");
//			if (symbol.getAsNode()!=null)
//				buff.append(twoNodesText(symbol.getAsNode()));
//			else if (symbol.getLikeNode()!=null)
//				buff.append(twoNodesText(symbol.getLikeNode()));
//			else
//				buff.append((useCaps ? "LIKE " : "like ") + getRefdWith());
//			buff.append(useCaps ? " NO-UNDO." : " no-undo.");
//			return buff.toString();
//		}
//	} // class RefAttr
//
//	
//	
//	/** Check if a Field_ref is in the selection range.
//	 * Update our reference list if so.
//	 */
//	protected void checkFieldRef(FieldRefNode refNode) {
//		JPNode idNode = refNode.getIdNode();
//		if (idNode.getFileIndex() != selectionFile) return;
//		if (! org.prorefactor.core.Util.isInRange(
//			idNode.getLine(), idNode.getColumn(), selectionBegin, selectionEnd ) )
//			return;
//		Symbol symbol = refNode.getSymbol();
//		assert symbol != null;
//		if (symbol instanceof Variable) {
//			// If the variable is scoped to the program block, parameter is not necessary.
//			if (symbol.getScope() instanceof SymbolScopeRoot) return;
//		}
//		RefAttr refAttr;
//		if (symbolMap.containsKey(symbol)) {
//			refAttr = (RefAttr) symbolMap.get(symbol);
//		} else {
//			refAttr = new RefAttr(symbol, refNode, idNode);
//			symbolMap.put(symbol, refAttr);
//			// We also store a vector of the RefAttr objects, so that we can access
//			// them in consistent order.
//			symbolList.add(refAttr);
//		}
//		int cq = refNode.attrGet(IConstants.CONTEXT_QUALIFIER);
//		if (CQ.isRead(cq)) refAttr.referencing = true;
//		if (CQ.isWrite(cq)) refAttr.updating = true;
//		refAttr.refsInRange++;
//	} // checkFieldRef
//
//
//
//	public String generateCallText() {
//		StringBuffer buff = new StringBuffer();
//		// If the selection begins at the beginning of a line, then we need
//		// to set the indent. (Otherwise, the RUN just goes wherever on the line
//		// the selection started.) We use the indent of the first line of the selection.
//		if (selectionBegin[1] < 2) {
//			buff.append(Strings.getIndentString(selectedText));
//		}
//		buff.append(useCaps ? "RUN " : "run ");
//		buff.append(methodName);
//		if (symbolList.size() > 0) {
//			buff.append(" (");
//			int argNum = 0;
//			for (Iterator it = symbolList.iterator(); it.hasNext();) {
//				if (++argNum > 1) buff.append(", ");
//				RefAttr param = (RefAttr) it.next();
//				buff.append(param.getDirectionText() + " " + param.getRefdWith());
//			}
//			buff.append(")");
//		}
//		buff.append(".");
//		return buff.toString();
//	} // generateCallText
//
//
//
//	public String generateMethodText() {
//		String indent = refPack.getIndentString();
//		StringBuffer buff = new StringBuffer();
//		buff.append(FileStuff.LINESEP);
//		buff.append(FileStuff.LINESEP);
//		buff.append(useCaps ? "PROCEDURE " : "procedure ");
//		buff.append(methodName + ":");
//		buff.append(FileStuff.LINESEP);
//		for (Iterator it = symbolList.iterator(); it.hasNext();) {
//			RefAttr param = (RefAttr) it.next();
//			buff.append(indent);
//			buff.append(param.toString());
//			buff.append(FileStuff.LINESEP);
//		}
//		buff.append(FileStuff.LINESEP);
//		buff.append(Strings.changeIndent(selectedText, indent, FileStuff.LINESEP));
//		buff.append(FileStuff.LINESEP);
//		buff.append(useCaps ? "END PROCEDURE." : "end procedure.");
//		buff.append(" /* " + methodName + " */");
//		buff.append(FileStuff.LINESEP);
//		return buff.toString();
//	} // generateMethodText
//
//
//
//	public String getMethodName() { return methodName; }
//
//	public String getSelectedText() { return selectedText; }
//
//
//
//	/** Run this refactor for a file and an integer array [4]
//	 * with the start line/column and end line/column of the code
//	 * section to be extracted.
//	 * @param sourceFile The source file containing the segment to be extracted. 
//	 * @param selectionBegin line/column
//	 * @param selectionEnd line/column end
//	 * @param compileFile The source file for the compile unit for building
//	 * an AST. Is different than the sourceFile if the sourceFile is not
//	 * compilable.
//	 */
//	public void run(File sourceFile, int[] selectionBegin, int[] selectionEnd, File compileFile)
//		throws RefactorException, IOException {
//		// TODO need functions to test that the selection is either a valid
//		// expression or else one or more statements/block.
//		this.selectionBegin = selectionBegin;
//		this.selectionEnd = selectionEnd;
//		cu = new CompileUnit(compileFile, null, CompileUnit.DEFAULT);
//		cu.treeParser01();
//		selectionFile = FileStuff.getFileIndex(sourceFile);
//		ICallback callback = new ICallback() {
//			public Object run(Object obj) {
//				if (obj instanceof FieldRefNode) checkFieldRef((FieldRefNode)obj);
//				return null;
//			}
//		};
//		cu.getTopNode().walk(callback);
//	} // run
//
//
//
//	public void setMethodName(String string) { methodName = string; }
//
//	public void setSelectedText(String string) { selectedText = string; }
//
//
//
//	/** This method returns the text of the input node as well as the
//	 * text of the following node, regardless of whether the next node is
//	 * a child node or the next sibling.
//	 */
//	String twoNodesText(JPNode node) {
//		if (node.getFirstChild()!=null)
//			return node.getText() + " " + node.getFirstChild().getText();
//		return node.getText() + " " + node.getNextSibling().getText();
//	} // twoNodesText
//
//
//
//	/** This tracing/debugging method gets a string representation of the arguments list.
//	 */
//	public String zztrace01() {
//		StringBuffer buff = new StringBuffer();
//		for (Iterator it = symbolList.iterator(); it.hasNext();) {
//			RefAttr param = (RefAttr) it.next();
//			buff.append(param.toString() + "\n");
//		}
//		return buff.toString();
//	} // zztrace01
//
//
//
//} // class
