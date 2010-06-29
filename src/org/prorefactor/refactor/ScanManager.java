/* ScanManager.java
 * Created on Nov 18, 2003
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
import java.util.HashMap;

import org.prorefactor.core.JPUtil;

/**
 * Eases the use of one or more scanners in combination with the parser.
 * Is not intended for use with "standalone scans" - scans that are not
 * being dealt with in combination with the AST.
 */
public class ScanManager {

	public ScanManager() {}

	private HashMap map = new HashMap();
	private ProparseLdr parser = ProparseLdr.getInstance();
	private JPUtil plus1 = JPUtil.getInstance();


	/**
	 * Given a file index, find or create a scan for the source file.
	 */	
	public int getScanNumFromIndex(int index) throws IOException {
		return getScanObjectFromIndex(index).scanNum;
	}



	/**
	 * Given a node, find or create a scanner for the node's original source.
	 * The input node does not have to be "natural" - this function will find
	 * the first natural child in order to find the filename.
	 * @param node
	 * @return Less than zero on error, otherwise the scan number.
	 */	
	public int getScanNumFromNode(int node) throws IOException {
		int tempHandle = parser.getHandle();
		try {
			if (plus1.firstNaturalChild(node, tempHandle) < 1) return -1;
			return getScanNumFromIndex(parser.getNodeFileIndex(tempHandle));
		} finally {
			parser.releaseHandle(tempHandle);
		}
	} // getScanNumFromNode



	/**
	 * Given a file index, find or create a scan for the source file.
	 */	
	public Scan getScanObjectFromIndex(int index) throws IOException {
		Integer fileIndex = new Integer(index);
		if (map.containsKey(fileIndex)) return (Scan) map.get(fileIndex);
		Scan newScan = new Scan(index);
		map.put(fileIndex, newScan);
		return newScan;
	} // getScanObjectFromIndex



} // class ScanManager
