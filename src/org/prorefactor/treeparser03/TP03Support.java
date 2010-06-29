/**
 * TP03Support.java
 * @author John Green
 * 16-Jul-2003
 * www.joanju.com
 * 
 * Copyright (c) 2003-2004 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * ============
 * Design Notes
 * ============
 * I thought of making the Field data a part of the NoundoTarget class,
 * but a DEFINE statement might be {included} in two different PROCEDURE blocks...
 * 
 */


//TODO Add regression test that the "targetSet" can hold RefactorTargets
//     for two different class/types of refactoring but at the same
//     node (filename/line/column).


package org.prorefactor.treeparser03;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.prorefactor.core.CommentFinder;
import org.prorefactor.core.IConstants;
import org.prorefactor.core.JPNode;
import org.prorefactor.nodetypes.FieldRefNode;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.refactor.RefactorTarget;
import org.prorefactor.refactor.noundo.NoundoTarget;
import org.prorefactor.treeparser.Symbol;

import antlr.collections.AST;

import com.joanju.ProparseLdr;


/**
 * Provides all functions called by TreeParser03.
 * TreeParser03 does not, itself, define any actions.
 * Instead, it only makes calls to the functions defined
 * in this class.
 */
public class TP03Support implements IConstants {


	/**
	 * Construct TP03Support
	 */
	public TP03Support() {
	} // TP03Support()



	/** The result set - a sorted set of RefactorTarget objects.
	 */
	public TreeSet targetSet = new TreeSet();


	private boolean isUpdating = false;	
	private Define define = null;
	private Ignore ignore = null;
	private HashMap undoTargetsMap = new HashMap();
	private LinkedList blockList = new LinkedList();
	private Procedure procedure = null;
	protected ProparseLdr parser = ProparseLdr.getInstance();

	class Block {
		public String label = "";
		public LinkedList undoTargetsAssignedList = new LinkedList();
	}

	class Define {
		public Define(AST inDef, AST inId) {
			defNode = inDef;
			idNode = inId;
		}
		public AST defNode;
		public boolean gotNoundo = false;
		public boolean gotUndo = false;
		public boolean gotUndoComment = false;
		public AST idNode;
	}

	class Ignore {
		public boolean noundo = false;
	}

	class Procedure {
		public Procedure(AST inAST) {
			procedureNode = inAST;
		}
		public boolean external = false;
		public AST procedureNode;
	}



	/**
	 * Called when a Code_block begins.
	 */
	public void blockBegin() {
		blockList.addFirst(new Block());
	}



	/**
	 * Called when a Code_block ends.
	 */
	public void blockEnd() {
		blockList.removeFirst();
	}


	/**
	 * Called when a labelled block's BLOCK_LABEL node is encountered.
	 * @param theNode The BLOCK_LABEL node.
	 */
	public void blockWithLabel(AST theNode) {
		((Block)blockList.getFirst()).label = theNode.getText();
	}

	

	/**
	 * Release all Proparse node handles held in "targetSet".
	 */
	public void cleanUp() {
		for (Iterator it = targetSet.iterator(); it.hasNext(); ) {
			RefactorTarget target = (RefactorTarget) it.next();
			parser.releaseHandle(target.nodeHandle);
		}
	}



	/**
	 * Called when certain types of DEFINE nodes are found.
	 * @param defNode The DEFINE node.
	 * @param idNode The ID node.
	 */
	public void define(AST defNode, AST idNode) {
		define = new Define(defNode, idNode);
		CommentFinder commentFinder = new CommentFinder();
		commentFinder.setFindString("undo");
		if (commentFinder.search((JPNode)defNode) > 0)
			define.gotUndoComment = true;
	}



	/**
	 * Called at the end of a DEFINE statement.
	 */
	public void defineEnd() {
		lintCheckNoundo();
		define = null;
	}



	/**
	 * Called at a Field_ref node.
	 * @param refAST The Field_ref node.
	 * @param idNode The ID node.
	 */
	public void fieldRef(AST refAST, AST idNode) {
		if (!isUpdating) return;
		FieldRefNode refNode = (FieldRefNode) refAST;
		Symbol symbol = refNode.getSymbol();
		NoundoTarget target = (NoundoTarget) undoTargetsMap.get(symbol);
		if (target==null) return;
		((Block)blockList.getFirst()).undoTargetsAssignedList.add(target);
	}


	/**
	 * Lint check to see if we've got a DEFINE without NO-UNDO.
	 */
	private void lintCheckNoundo() {
		if (define==null||define.gotNoundo||define.gotUndo||define.gotUndoComment) return;
		if (procedure != null && procedure.external) return;
		if (ignore != null && ignore.noundo) return;
		JPNode defHandle = (JPNode) define.defNode;
		String defType = new String(defHandle.attrGetS("state2"));
		if (	!(	defType.equals("VARIABLE")
				||	defType.equals("TEMPTABLE")
				||	defType.equals("WORKTABLE")
				||	defType.equals("PARAMETER")
				)
			) return;
		// If we got here, then we're going to register this node.
		// Grab a new (lasting) handle to this node.
		JPNode storeHandle = defHandle;
		NoundoTarget target = new NoundoTarget();
		// We're not too worried about line numbers with this
		// particular refactor. Just get the line where the DEFINE is.
		target.changedLines[0]
			= target.changedLines[1]
			= target.changedLines[2]
			= target.changedLines[3]
			= target.linenum
			= storeHandle.getLine()
			;
		target.filename = storeHandle.getFilename();
		target.column = storeHandle.getColumn();

// TODO
//		target.nodeandle = storeHandle;
		// The check to see if it already exists isn't completely superfluous.
		// You might include the same DEFINE statement into two different PROCEDURE blocks.
		if (! targetSet.contains(target)) targetSet.add(target);
		Symbol symbol = (Symbol) ((JPNode)define.idNode).getLink(JPNode.SYMBOL);
		assert symbol != null;
		target.symbol = symbol;
		undoTargetsMap.put(symbol, target);
	} // lintNoundo



	/**
	 * Called when a NO-UNDO node is found.
	 */
	public void noundo() {
		if (define != null)
			define.gotNoundo = true;
	}



	/**
	 * Raise a warning for variables and parameters which are targetted for
	 * refactoring (add NO-UNDO) but which are also assigned within a block
	 * which has an UNDO statement after it was assigned.
	 * We remove the target from the targetSet.
	 * @param undoNode The UNDO node.
	 * @param theBlock The block which is the subject of the UNDO statement.
	 */
	public void noundoUndoCheck(AST undoNode, Block theBlock) {
		File outfile = null;
		FileWriter writer = null;
		try {
			outfile = new File(RefactorSession.getProrefactorDirName() + "noundo.messages");
			outfile.getParentFile().mkdirs();
			outfile.createNewFile();
			writer = new FileWriter(outfile, true);
			if (theBlock.undoTargetsAssignedList.isEmpty()) return;
//TODO
int undoHandle = 0;
// int undoHandle = h(undoNode);
			int line = parser.getNodeLine(undoHandle);
			String file = parser.getNodeFilename(undoHandle);
			for (Iterator i = theBlock.undoTargetsAssignedList.iterator(); i.hasNext(); ) {
				NoundoTarget target = (NoundoTarget) i.next();
				writer.write(
						"(Refactor/UNDO) Field: "
					+	target.symbol.getName()
					+	" assignment might be undone at file:line: "
					+	file + ":"
					+	Integer.toString(line)
					+	"\n"
					);
				// Remove the target from the targetSet.
				SortedSet lookupSet = targetSet.tailSet(target);
				if (! lookupSet.isEmpty())
					targetSet.remove(lookupSet.first());
			}
			writer.close();
		} catch (IOException e) {
			return;
		}
	}



	/**
	 * Called when a PROCEDURE statement is encountered.
	 * @param theNode is the PROCEDURE node.
	 */
	public void procedure(AST theNode) {
		procedure = new Procedure(theNode);
	}



	/**
	 * Called at the end of any PROCEDURE.
	 */
	public void procedureEnd() {
		procedure = null;
	}



	/**
	 * Called if the current procedure is EXTERNAL.
	 */
	public void procedureExternal() {
		if (procedure != null)
			procedure.external = true;
	}



	/**
	 * Deal with PROPARSEDIRECTIVE nodes
	 * Only throws antlr RecognitionException for benefit of working with
	 * Antlr generated tree parser.
	 * We parse prolint-nowarn directives the same as Prolint does.
	 */
	public void proparsedirective(AST ast) {
		JPNode theNode = (JPNode) ast;
		String theText = theNode.attrGetS("proparsedirective");
		int index = theText.indexOf("prolint-nowarn");
		if (index == -1) return;
		index = theText.indexOf("(", index);
		if (index == -1) return;
		int indexEnd = theText.indexOf(")", index);
		if (indexEnd == -1) return;
		theText = theText.substring(index + 1, indexEnd);
		theText = theText.replace(' ', ',');
		String [] noWarn = theText.split(",");
		ignore = new Ignore();
		for (int i = 0; i != noWarn.length; i++) {
			if (noWarn[i].equalsIgnoreCase("noundo")) {
				ignore.noundo = true;
			}
		}
	} // proparsedirective()



	/**
	 * Deal with end-of-statement.
	 */
	public void statementEnd() {
		// Prolint-nowarn directives are only in effect through to the next statement.
		ignore = null;
	} // statementEnd()



	/**
	 * Called when an UNDO node is found as an option to DEFINE TEMP-TABLE.
	 */
	public void undo() {
		if (define != null)
			define.gotUndo = true;
	}



	/**
	 * Called when an UNDO statement is found.
	 * @param undoNode The UNDO node.
	 * @param blockLabel The block label node - might be null.
	 */
	public void undoState(AST undoNode, AST blockLabel) {
		if (blockList.size() < 1) return; // shouldn't happen
		if (blockLabel == null) {
			noundoUndoCheck(undoNode, (Block)blockList.getFirst());
		} else {
//			String labelText = parser.getNodeText(h(blockLabel));
//			for (Iterator i = blockList.iterator(); i.hasNext(); ) {
//				Block tempBlock = (Block) i.next();
//				noundoUndoCheck(undoNode, tempBlock);
//				if (tempBlock.label.equals(labelText)) break;
//			}
		}
	} // undoState()



	/**
	 * Called when in an ASSIGN, SET, IMPORT etc, where field assignment is done.
	 * @param inIsUpdating True at start of updating, false at end of updating.
	 */
	public void updating(boolean inIsUpdating) {
		isUpdating = inIsUpdating;
	}



} // class TP03Support

