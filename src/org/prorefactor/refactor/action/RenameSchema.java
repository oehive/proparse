///* RenameSchema.java
// * Created on Sep 6, 2004
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
//import java.util.StringTokenizer;
//import java.util.TreeSet;
//
//import org.prorefactor.core.JPNode;
//import org.prorefactor.core.TokenTypes;
//import org.prorefactor.core.schema.Field;
//import org.prorefactor.core.schema.Schema;
//import org.prorefactor.core.schema.Table;
//import org.prorefactor.nodetypes.FieldRefNode;
//import org.prorefactor.nodetypes.RecordNameNode;
//import org.prorefactor.refactor.IRefactor;
//import org.prorefactor.refactor.PUB;
//import org.prorefactor.refactor.Refactor;
//import org.prorefactor.refactor.RefactorException;
//import org.prorefactor.refactor.RefactorTarget;
//import org.prorefactor.refactor.TempDirWrap;
//import org.prorefactor.treeparser.FieldBuffer;
//import org.prorefactor.treeparser.ParseUnit;
//import org.prorefactor.treeparser.Symbol;
//import org.prorefactor.treeparser.TableBuffer;
//
//import com.joanju.ProparseLdr;
//
//
///** Rename schema tables and fields from an input from/to name mapping.
// * Database name changes are not supported (logical, alias, or otherwise).
// * Buffer names are not changed.
// * Input names map is a string of old/new name pairs, all separated by
// * any sort of whitespace (space, tab, newline,...).
// * The "old" names must be either "db.table" or "db.table.field".
// * Any qualifier on the "new" name is completely ignored.
// * Any database qualifier from the original source code is retained.
// * For fields, any table qualifier is retained from the original source code.
// * Table and field names can be changed at the same time - any table 
// * qualifier on a field would be changed as long as it is not for a named buffer.
// */
//public class RenameSchema {
//
// 	/** See the class notes for details about the format of the input names map
// 	 * @throws RefactorException if there are any invalid "old" names in the map.
// 	 */
//	public RenameSchema(String namesMap, String outDir) throws RefactorException {
//		generateSchemaMap(namesMap);
//		this.outDir = outDir;
//	}
//
//	private Schema schema = Schema.getInstance();
//
//	private Changer changer = new Changer();
//	private HashMap schemaMap = new HashMap();
//	private String outDir;
//	private TreeSet targetSet = null; // initialized by run()
//
//	public class RenameTarget extends RefactorTarget {
//		public RenameTarget(JPNode node, String newName) {
//			this.column = node.getColumn();
//			this.linenum = node.getLine();
//			this.filename = node.getFilename();
//			this.nodeHandle = node.getHandle();
//			this.newName = newName;
//		}
//		public String newName;
//	}
//	
//	public class Changer implements IRefactor {
//		ProparseLdr parser = ProparseLdr.getInstance();
//		Refactor refactor = Refactor.getInstance();
//		public int run(RefactorTarget tar, int scanNum) {
//			if (!(tar instanceof RenameTarget)) return 2;
//			RenameTarget target = (RenameTarget)tar;
//			int nameNode = parser.getHandle();
//			int scanHandle = parser.getHandle();
//			parser.parseGetTop(scanNum, scanHandle);
//			try {
//				parser.copyHandle(target.nodeHandle, nameNode);
//				if (refactor.nsync(nameNode, scanHandle) == 0) return -1;
//				parser.setNodeText(scanHandle, target.newName);
//			} finally {
//				parser.releaseHandle(nameNode);
//				parser.releaseHandle(scanHandle);
//			}
//			return 1;
//		}
//	}
//
//
//
//	private void checkNode(JPNode node) {
//		int type = node.getType();
//		if (type == TokenTypes.Field_ref) checkNodeDuField((FieldRefNode)node);
//		else if (type == TokenTypes.RECORD_NAME) checkNodeDuTable((RecordNameNode)node);
//		return;
//	}
//	
//	
//	
//	private void checkNodeDuField(FieldRefNode refNode) {
//		Symbol symbol = refNode.getSymbol();
//		if (! (symbol instanceof FieldBuffer)) return;
//		FieldBuffer fieldBuff = (FieldBuffer) symbol;
//		assert fieldBuff != null;
//		TableBuffer tableBuff = fieldBuff.getBuffer();
//		String mapFieldValue = (String) schemaMap.get(fieldBuff.getField());
//		String mapTableValue = (String) schemaMap.get(tableBuff.getTable());
//		if (mapFieldValue==null && mapTableValue==null) return;
//		JPNode idNode = refNode.getIdNode();
//		String origText = idNode.getText();
//		Field.Name oldName = new Field.Name(origText);
//		Field.Name newName = new Field.Name(origText);
//		if (	mapTableValue != null
//			&&	tableBuff.isDefault()
//			&&	oldName.table!=null
//			&&	oldName.table.length()>0
//			) {
//			Table.Name n = new Table.Name(mapTableValue);
//			newName.table = n.table;
//		}
//		if (mapFieldValue!=null) {
//			Field.Name n = new Field.Name(mapFieldValue);
//			newName.field = n.field;
//		}
//		String newText = newName.generateName();
//		if (! newText.equalsIgnoreCase(origText))
//			targetSet.add(new RenameTarget(idNode, newText));
//	}
//	
//	
//	
//	private void checkNodeDuTable(RecordNameNode node) {
//		TableBuffer buffer = node.getTableBuffer();
//		assert buffer != null;
//		// Check that this is a "default" schema buffer - not a named buffer.
//		if (! buffer.isDefaultSchema()) return;
//		String mapValue = (String) schemaMap.get(buffer.getTable());
//		if (mapValue==null) return;
//		// The following use of Table.Name gets us the database qualifier, if present.
//		Table.Name oldName = new Table.Name(node.getText());
//		Table.Name mapName = new Table.Name(mapValue);
//		Table.Name newName = new Table.Name(oldName.db, mapName.table);
//		targetSet.add(new RenameTarget(node, newName.generateName()));
//	}
//	
//	
//	
//	private Object [] checkPUB(String relPath, File compileFile) throws IOException, RefactorException {
//		// In the future we will have an xref index that we can use, instead
//		// of checking the PUB file for matching tables. Change this once the xref
//		// index is available.
//		PUB pub = new PUB(compileFile.getCanonicalPath());
//		ParseUnit pu = null;
//		if (! pub.loadTo(PUB.SCHEMA)) pu = pub.build();
//		boolean containsChanges = false;
//		ArrayList tableNames = new ArrayList();
//		pub.copySchemaTableLowercaseNamesInto(tableNames);
//		for (Iterator it = tableNames.iterator(); it.hasNext();) {
//			String tableName = (String) it.next();
//			Table table = schema.lookupTable(tableName);
//			assert table != null;
//			if (schemaMap.containsKey(table)) {
//				containsChanges = true;
//				break;
//			}
//		}
//		Object [] ret = new Object[2];
//		ret[0] = new Boolean(containsChanges);
//		ret[1] = pu;
//		return ret;
//	}
//
//
//
//	/** Generate the schema objects map.
//	 * Takes a String of whitespace delimited from/to name pairs, and generates the
//	 * schemaMap of from/to, where from is either a Table or a Field, and
//	 * the "to" is the same string as the "to" from the original map.
//	 * @throws RefactorException if there are any invalid entries in the names list.
//	 */
//	private void generateSchemaMap(String namesList) throws RefactorException {
//		StringTokenizer tok = new StringTokenizer(namesList);
//		while (tok.hasMoreTokens()) {
//			String old = tok.nextToken();
//			if (! tok.hasMoreTokens()) throw new RefactorException("Odd number of names in input");
//			String [] parts = old.split("\\.");
//			if (parts.length==2) {
//				Table table = schema.lookupTable(parts[0], parts[1]);
//				if (table==null) throw new RefactorException("Unknown table " + old);
//				schemaMap.put(table, tok.nextToken());
//			} else if (parts.length==3) {
//				Field field = schema.lookupField(parts[0], parts[1], parts[2]);
//				if (field==null) throw new RefactorException("Unknown field " + old);
//				schemaMap.put(field, tok.nextToken());
//			} else {
//				throw new RefactorException("Bad schema 'from' name entry: " + old);
//			}
//		}
//	}
//
//
//
//	/** Run this refactor for an input compile unit.
//	 * @param relPath The path/filename, relative to the project directory.
//	 * @return the number of refactor targets attempted
//	 */
//	public int run(File compileFile, String relPath) throws RefactorException, IOException {
//		targetSet = new TreeSet();
//		Object [] pubRet = checkPUB(relPath, compileFile);
//		Boolean requiresChanges = (Boolean) pubRet[0];
//		if (! requiresChanges.booleanValue()) return 0;
//		ParseUnit pu = (ParseUnit) pubRet[1];
//		if (pu==null) {
//			pu = new ParseUnit(compileFile);
//			pu.treeParser01();
//		}
//		walkTree(pu.getTopNode());
//		if (targetSet.size()==0) return 0;
//		TempDirWrap wrapper = new TempDirWrap(outDir);
//		wrapper.run(targetSet, changer);
//		return targetSet.size();
//	}
//
//	
//	
//	private void walkTree(JPNode node) {
//		if (node==null) return;
//		checkNode(node);
//		walkTree(node.firstChild());
//		walkTree(node.nextSibling());
//	}
//
//	
//
//}
