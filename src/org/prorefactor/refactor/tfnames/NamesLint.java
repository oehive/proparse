///* NamesLint.java
// * Created on Oct 11, 2003
// * John Green
// *
// * Copyright (C) 2003-2004 Joanju Limited
// * All rights reserved. This program and the accompanying materials 
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// */
//package org.prorefactor.refactor.tfnames;
//
//import java.util.TreeSet;
//
//import org.prorefactor.core.ICallback;
//import org.prorefactor.core.IConstants;
//import org.prorefactor.core.JPNode;
//import org.prorefactor.core.JPUtil;
//import org.prorefactor.core.PRCException;
//import org.prorefactor.core.TokenTypes;
//import org.prorefactor.core.schema.Database;
//import org.prorefactor.core.schema.Table;
//import org.prorefactor.nodetypes.FieldRefNode;
//import org.prorefactor.nodetypes.RecordNameNode;
//import org.prorefactor.refactor.ILint;
//import org.prorefactor.treeparser.FieldBuffer;
//import org.prorefactor.treeparser.Symbol;
//import org.prorefactor.treeparser.TableBuffer;
//import org.prorefactor.treeparser.TreeParserWrapper;
//import org.prorefactor.treeparser01.TreeParser01;
//
//import com.joanju.ProparseLdr;
//
//
///** Find all table and table.field names, find those which need
// * to be refactored. For each node found which needs fixing,
// * create a Target, and store the "corrected" node text in that
// * Target.
// * Can be called/implemented via org.prorefactor.refactor.TempDirWrap.
// */
//public class NamesLint implements ILint {
//
//	public NamesLint(TFNamesRefactor namesSettings) {
//		this.settings = namesSettings;
//	}
//
//	private NamesTarget currTarget = null;
//	private TFNamesRefactor settings = null;
//	private ProparseLdr parser = ProparseLdr.getInstance();
//	private JPUtil plus = JPUtil.getInstance();
//	public TreeSet targetSet = new TreeSet();
//
//	ICallback callback = new ICallback() {
//		public Object run(Object obj) {
//			JPNode node = (JPNode) obj;
//			examineNode(node);
//			return null;
//		}
//	};
//
//
//
//	/** Check on the case of a name
//	 * @param The original name
//	 * @return null if case is OK, else the corrected text
//	 */
//	private String checkCase(String orig) {
//		String s = setCase(orig);
//		if (s.equals(orig)) return null;
//		return s;
//	}
//
//
//
//	protected void examineNode(JPNode node) {
//		switch (node.getType()) {
//			case TokenTypes.Field_ref :
//				currTarget = new NamesTarget();
//				lintFieldRef((FieldRefNode)node);
//				break;
//			case TokenTypes.RECORD_NAME :
//				currTarget = new NamesTarget();
//				lintRecordName((RecordNameNode)node);
//				break;
//		}
//	} // examineNode()
//
//
//
//	public TreeSet getTargetSet() {
//		return targetSet;
//	}
//
//
//
//	private boolean needsAbbrevFix(JPNode node) {
//		return
//			settings.unabbreviate
//			&&	node.attrGet(IConstants.ABBREVIATED) == 1
//			;
//	}
//
//
//
//	private boolean isTargetTarget() {
//		return (currTarget.abbreviated || currTarget.caseWrong || currTarget.needsQualif);
//	}
//
//
//
//	private void lintFieldRef(FieldRefNode node) {
//		int handle = node.getHandle();
//		Symbol symbol =  node.getSymbol();
//		FieldBuffer fieldBuff = null;
//		TableBuffer tableBuff = null;
//		if (symbol instanceof FieldBuffer) { 
//			fieldBuff = (FieldBuffer) symbol;
//			tableBuff = fieldBuff.getBuffer();
//		}
//
//		// Are we applying changes to work/temp tables?
//		if (	(! settings.workTempTables)
//			&&	(node.attrGet(IConstants.STORETYPE) != IConstants.ST_DBTABLE)
//			) return;
//
//		currTarget.abbreviated = needsAbbrevFix(node);
//
//		currTarget.needsQualif =
//			(	settings.qualify
//			&&	node.attrGet(IConstants.UNQUALIFIED_FIELD) == IConstants.TRUE
//			);
//
//		if (!isTargetTarget() && !settings.fixCase) return;
//
//		int idNode = parser.getHandle();
//		try {
//			plus.findFieldRefIdNode(handle, idNode);
//	
//			// Get the name parts
//			String [] parts = parser.getNodeText(idNode).split("\\.");
//			switch (parts.length) {
//				case 3 :
//					currTarget.dbPart = parts[0];
//					currTarget.tablePart = parts[1];
//					currTarget.fieldPart = parts[2];
//					break;
//				case 2 :
//					currTarget.tablePart = parts[0];
//					currTarget.fieldPart = parts[1];
//					break;
//				case 1 :
//					currTarget.fieldPart = parts[0];
//					break;
//			}
//	
//			if (currTarget.abbreviated) {
//				// If abbreviated, must be a Field. Variables cannot be abbreviated.
//				assert fieldBuff != null;
//				currTarget.fieldPart = setCase(fieldBuff.getName());
//			}
//
//			if (settings.fixCase) {
//				String s = checkCase(currTarget.fieldPart);
//				if (s!=null) {
//					currTarget.caseWrong = true;
//					currTarget.fieldPart = s;
//				}
//			}
//
//			if (currTarget.needsQualif) {
//				assert fieldBuff != null;
//				if (tableBuff.isDefault())
//					currTarget.tablePart = setCase(tableBuff.getName());
//				else
//					currTarget.tablePart = tableBuff.getName();
//			} else if (currTarget.tablePart!=null) {
//				lintRecordNameCommon(node, tableBuff);
//			}
//
//
//			if (isTargetTarget()) {
//				// Fetch a persisting handle for the Target
//				currTarget.nodeHandle = parser.getHandle();
//				parser.copyHandle(handle, currTarget.nodeHandle);
//				// Important to note that we use the line/col etc. of the idNode...
//				// the Field_ref node has line/col both equal zero.
//				// (Field_ref is a synthetic node)
//				currTarget.changedLines[0]
//					= currTarget.changedLines[1]
//					= currTarget.changedLines[2]
//					= currTarget.changedLines[3]
//					= currTarget.linenum
//					= parser.getNodeLine(idNode)
//					;
//				currTarget.filename = parser.getNodeFilename(idNode);
//				currTarget.column = parser.getNodeColumn(idNode);
//				targetSet.add(currTarget);
//			} 
//
//		} finally {
//			parser.releaseHandle(idNode);
//		}
//
//	} // lintFieldRef()
//
//
//
//	/*
//	 * We only examine database table names, and only if it is not a buffer name.
//	 * (Buffer names cannot be abbreviated, and we're not going to try to deal
//	 * with capitalization on buffer names. bCustomer might be very valid.)
//	 */
//	private void lintRecordName(RecordNameNode node) {
//		TableBuffer tableBuff = node.getTableBuffer();
//		Table table = tableBuff.getTable();
//
//		// Are we applying changes to work/temp tables?
//		if (	! settings.workTempTables
//			&&	table.getStoretype() != IConstants.ST_DBTABLE
//			) return;
//
//		// We don't mess with buffer names.
//		if (! tableBuff.isDefault()) return;
//
//		currTarget.abbreviated = needsAbbrevFix(node);
//
//		if (!currTarget.abbreviated && !settings.fixCase) return;
//
//		// Get the name parts
//		String [] parts = node.getText().split("\\.");
//		switch (parts.length) {
//			case 2 :
//				currTarget.dbPart = parts[0];
//				currTarget.tablePart = parts[1];
//				break;
//			case 1 :
//				currTarget.tablePart = parts[0];
//				break;
//		}
//
//		lintRecordNameCommon(node, tableBuff);
//
//		if (isTargetTarget()) {
//			// Fetch a persisting handle for the Target
//			currTarget.nodeHandle = parser.getHandle();
//			parser.copyHandle(node.getHandle(), currTarget.nodeHandle);
//			currTarget.changedLines[0]
//				= currTarget.changedLines[1]
//				= currTarget.changedLines[2]
//				= currTarget.changedLines[3]
//				= currTarget.linenum
//				= parser.getNodeLine(currTarget.nodeHandle)
//				;
//			currTarget.filename = parser.getNodeFilename(currTarget.nodeHandle);
//			currTarget.column = parser.getNodeColumn(currTarget.nodeHandle);
//			targetSet.add(currTarget);
//		} 
//
//	} // lintRecordName()
//
//
//
//	/* Common lint checks against the db and table name, whether we are
//	 * processing a RECORD_NAME node or a Field_ref node.
//	 * The caller is responsible for "setting up" the currTarget object.
//	 * We only examine database table names, and only if it is not a buffer name.
//	 * (Buffer names cannot be abbreviated, and we're not going to try to deal
//	 * with capitalization on buffer names. bCustomer might be very valid.)
//	 */
//	private void lintRecordNameCommon(JPNode node, TableBuffer tableBuff) {
//		if (! tableBuff.isDefaultSchema()) return;
//		Table table = tableBuff.getTable();
//
//		// If abbreviated, get the proper full table name
//		if (currTarget.abbreviated)
//			currTarget.tablePart = setCase(table.getName()); 
//
//		if (settings.fixCase) {
//
//			if (currTarget.dbPart!=null) {
//				Database db = table.getDatabase();
//				// If the node isn't using an alias, check db name case
//				if (db.getName().compareToIgnoreCase(currTarget.dbPart)==0) {
//					String s = checkCase(currTarget.dbPart);
//					if (s!=null) {
//						currTarget.caseWrong = true;
//						currTarget.dbPart = s;
//					}
//				}
//			} // dbPart!=null
//
//			String s = checkCase(currTarget.tablePart);
//			if (s!=null) {
//				currTarget.caseWrong = true;
//				currTarget.tablePart = s;
//			}
//
//		} // settings.fixCase
//
//	} // lintRecordNameCommon
//
//
//
//	/**
//	 * Run this lint routine.
//	 * @param topNode The topmost node in the AST to start the lint from.
//	 * @return A string representing any error message, null or empty if no error
//	 * @see org.prorefactor.refactor.tfnames.NamesTarget
//	 * The client becomes the "owner" of the handles referred to in the
//	 * NamesTarget records, and is responsible for releasing those when done.
//	 */
//	public String run(int topNode) {
//		TreeParser01 tp01 = new TreeParser01();
//		JPNode theAST;
//		try {
//			theAST = TreeParserWrapper.getTree(tp01, topNode);
//		} catch (PRCException e) {
//			return e.getMessage();
//		}
//		theAST.walk(callback);
//		return null;
//	} // run()
//
//
//
//	private String setCase(String s) {
//		return settings.useLowercase ? s.toLowerCase() : s.toUpperCase();
//	}
//
//
//
//}
//
