/**
 * JPUtil.java
 * @author John Green
 * 15-Oct-2002
 * www.joanju.com
 * 
 * Copyright (c) 2002 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.core;

import com.joanju.ProparseLdr;
import com.joanju.proparse.ProToken;


/**
 * Joanju Proparse Utility Pack
 * <p>
 * <h3>Integer return values</h3>
 * Integer return values are not like traditional C return values where 0
 * is success and any other number is an error number.
 * Instead, these are consistent with Proparse's return values: 
 * For many of these functions, where the return value is an int,
 * if the integer return value does not already have another meaning,
 * the function could be viewed as a logical "YES we got it / NO it doesn't exist"
 * <br>1 "success"
 * <br>0 "failure"
 * <br>-1 "warning"
 * <br>-2 "error"
 */
public class JPUtil {

	private JPUtil() {}

	private ProparseLdr parser = ProparseLdr.getInstance();
	private static JPUtil instance = null;

	/**
	 * This class implements the "Singleton" design pattern.
	 */
	public static JPUtil getInstance() {
		if (instance == null)
			instance = new JPUtil();
		return instance;
	}



	/** Convenience method just to tell if a DEFINE node is DEFINE--SHARED.
	 * @param node The AST node handle.
	 * @return True if DEFINE [NEW [GLOBAL]] SHARED.
	 */
	public boolean isDefineShared(int node) {
		int temp = parser.getHandle();
		try {
			int firstChildType = parser.nodeFirstChildI(node, temp);
			return (firstChildType==TokenTypes.NEW || firstChildType==TokenTypes.SHARED);
		} finally {
			parser.releaseHandle(temp);
		}
	} // isDefineShared()



	/** Find the first direct child of a given node type
	 * @param parent The parent node handle
	 * @param intoHandle If found, this handle will point to the desired node
	 * @param nodeType Integer token type
	 * @return true if found, false otherwise
	 */
	public boolean findDirectChild(int parent, int intoHandle, int nodeType) {
		int tempHandle = parser.getHandle();
		try {
			int currNodeType = parser.nodeFirstChildI(parent, tempHandle);
			while (currNodeType!=0 && currNodeType!=nodeType) {
				currNodeType = parser.nodeNextSiblingI(tempHandle, tempHandle);
			}
			if (currNodeType==nodeType) {
				parser.copyHandle(tempHandle, intoHandle);
				return true;
			}
		} finally {
			parser.releaseHandle(tempHandle);
		}
		return false;
	}



	/** Find the ID node for a Field_ref
	 * @param parent
	 * @param intoHandle
	 * @return void
	 * Throws an Error if not found.
	 */
	public void findFieldRefIdNode(int parent, int idNode) {
		// We throw, rather than use assert, for Java 1.3 support.
		if ( ! findDirectChild(parent, idNode, TokenTypes.ID) )
			throw new Error("Failed to find ID child of Field_ref node");
	}



	/**
	 * Find the first descendant of a given type.
	 * Input root node handle, input handle to put pointer to resulting node
	 * into, input String token type name.
	 * Returns 0 on fail, 1 on success.
	 */
	public int findFirstDescendant(int rootNode, int intoHandle, String tokenTypeName) {
		// (jag - after next beta, rewrite this to get the integer token type,
		// and then make this a recursive function instead of using a query.
		String qname = "jputil_firstDescendant";
		try {
			int numResults = parser.queryCreate(rootNode, qname, tokenTypeName);
			if (numResults < 1)
				return 0;
			parser.queryGetResult(qname, 1, intoHandle);
			return 1;
		} finally {
			parser.queryClear(qname);
		}
	} // findFirstDescendant()



	/**
	 * Find the first hidden token after the current node's last descendant.
	 * @return The result from the hiddenGet... call.
	 * (i.e. 1 if found, 0 if not found, negative number on error.)
	 */
	public int findFirstHiddenAfterLastDescendant(int node) {
		// There's no direct way to get a "hidden after" token,
		// so to find the hidden tokens after the current node's last
		// descendant, we find the next sibling of the current node,
		// find the first "natural" descendant of it (if it is not
		// itself natural), and then get its first hidden token.
		int nextNatural = parser.getHandle();
		try {
			int nodeType = parser.nodeNextSiblingI(node, nextNatural);
			if (nodeType < 1) return 0;
			if (nodeType!=TokenTypes.Program_tail) {
				if (firstNaturalChild(nextNatural, nextNatural) < 1) return 0;
			}
			return parser.hiddenGetFirst(nextNatural);
		} finally {
			parser.releaseHandle(nextNatural);
		}
	}


	/** Find the first hidden token after the current node's last descendant. */
	public ProToken findFirstHiddenAfterLastDescendant(JPNode node) {
		// There's no direct way to get a "hidden after" token,
		// so to find the hidden tokens after the current node's last
		// descendant, we find the next sibling of the current node,
		// find the first "natural" descendant of it (if it is not
		// itself natural), and then get its first hidden token.
		JPNode nextNatural = node.nextSibling();
		if (nextNatural == null)
			return null;
		if (nextNatural.getType() != TokenTypes.Program_tail) {
			nextNatural = nextNatural.firstNaturalChild();
			if (nextNatural == null)
				return null;
		}
		return nextNatural.getHiddenFirst();
	}


	/**
	 * First Natural Child is found by repeating firstChild() until a natural node is found.
	 * Input: start handle, target handle. Start handle is not changed.
	 * If the start handle is a natural node, then the target handle will
	 * be changed to be pointing at the same node as the start handle.
	 * Return value: integer node type of the first natural child found, if any,
	 * otherwise 0 is returned.
	 * The test for "natural" node is line number. Synthetic node line numbers == 0.
	 * Note: This is very different than Prolint's "NextNaturalNode" in lintsuper.p.
	 */
	public int firstNaturalChild(int h1, int h2) {
		int tempHandle = parser.getHandle();
		try {
			parser.copyHandle(h1, tempHandle);
			int nodeType = parser.getNodeTypeI(tempHandle);
			int lineNum = parser.getNodeLine(tempHandle);
			while (nodeType > 0 &&  lineNum < 1) {
				nodeType = parser.nodeFirstChildI(tempHandle, tempHandle);
				lineNum = parser.getNodeLine(tempHandle);
			}
			if (nodeType < 1 || lineNum < 1) return 0;
			parser.copyHandle(tempHandle, h2);
			return nodeType;
		} finally {
			parser.releaseHandle(tempHandle);
		}
	} // firstNaturalChild()



	/** Get a node's filename - even for a synthetic node.
	 * Uses firstNaturalChild() to ensure that we have a natural node
	 * to get the filename from.
	 * @param Handle to a node.
	 * @return Whatever was returned after firstNaturalChild() and parser.getNodeFilename().
	 */
	public String getFilename(int astHandle) {
		int h = parser.getHandle();
		firstNaturalChild(astHandle, h);
		String filename = parser.getNodeFilename(h);
		parser.releaseHandle(h);
		return filename;
	}



	/** Get a node's fileIndex/line/col position - even for a synthetic node.
	 * Uses firstNaturalChild() to ensure that we have a natural node
	 * to get the position from.
	 * @param Handle to a node.
	 * @return Array of integers for fileIndex/line/col. Value is {-1, -1, -1}
	 * if there's no natural child to the node.
	 */
	public int [] getPosition(int astHandle) {
		int [] ret = {-1, -1, -1};
		int h = parser.getHandle();
		try {
			if (firstNaturalChild(astHandle, h) < 1) return ret;
			ret[0] = parser.getNodeFileIndex(h);
			ret[1] = parser.getNodeLine(h);
			ret[2] = parser.getNodeColumn(h);
			return ret;
		} finally {
			parser.releaseHandle(h);
		}
	}



	/** Get a string representation of a node's position,
	 * ex: "mydir/myfile.p:12:13".
	 * Useful for throwing into an Exception's text.
	 */
	public String getPositionString(int node) {
		return getPositionString(getPosition(node));
	}



	/** Get a string representation of an int[3] position,
	 * ex: "mydir/myfile.p:12:13".
	 * Useful for throwing into an Exception's text.
	 */
	public String getPositionString(int[] pos) {
		StringBuilder buff = new StringBuilder("");
		buff.append(parser.getIndexFilename(pos[0]));
		buff.append(":");
		buff.append(pos[1]);
		buff.append(":");
		buff.append(pos[2]);
		return buff.toString();
	}



	/**
	 * Return a simple string representation of a node.
	 * (Usually for testing / debugging)
	 */
	public String handleToString(int h1) {
		return
			parser.getNodeType(h1)
			+ " "
			+ parser.getNodeText(h1)
			+ " "
			+ parser.getNodeFilename(h1)
			+ " line "
			+ parser.getNodeLine(h1)
			;
	}



	/**
	 * Finds the last immediate child of a node (no grandchildren).
	 * Input: start handle, target handle.
	 * If there are no children, the target handle is left unchanged,
	 * otherwise it is changed to point at the last immediate child.
	 * @return The last child's integer node type on success, a number less
	 * than 1 on fail or error.
	 */
	public int lastChild(int h1, int h2) {
		int tempHandle = parser.getHandle();
		try {
			int nodeType = parser.nodeFirstChildI(h1, tempHandle);
			int nextNodeType = nodeType;
			while (nextNodeType>0) {
				nodeType = nextNodeType;
				nextNodeType = parser.nodeNextSiblingI(tempHandle, tempHandle);
			}
			if (nodeType<1) return nodeType;
			parser.copyHandle(tempHandle, h2);
			return nodeType;
		} finally {
			parser.releaseHandle(tempHandle);
		}
	} // lastChild()



	/**
	 * Number of Siblings - Count the number of "next siblings".
	 */
	public int numSiblings(int theNode) {
		int tempNode = parser.getHandle();
		try {
			parser.copyHandle(theNode, tempNode);
			int nextNode = 1;
			int theCount = -1;
			while (nextNode != 0) {
				theCount++;
				nextNode = parser.nodeNextSiblingI(tempNode, tempNode);
			}
			return theCount;
		} finally {
			parser.releaseHandle(tempNode);
		}
	}



} // class


