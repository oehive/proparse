/* RefactorException.java
 * Created on Dec 6, 2003
 * John Green
 *
 * Copyright (C) 2003 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor;

import java.io.File;

import org.prorefactor.core.JPUtil;


/** Exception to be thrown only by the refactoring libraries,
 * especially ScanLib, Refactor, etc. These exceptions thrown
 * from the refactoring libraries are intended to help the
 * programmer (me!) more easily track down mistakes made when
 * writing new refactorings.
 */
public class RefactorException extends Exception {

	int [] filePos = null;
	File file = null;
	
	public RefactorException(String message) {
		super(message);
	}

	public RefactorException(Throwable cause) {
		super(cause);
	}

	public RefactorException(String message, Throwable cause) {
		super(message, cause);
	}

	/** Create an exception with "filename:line:col message" as the text.
	 * @param h A handle to the node (or scanner token) to get file:line:col from.
	 * @param message
	 */
	public RefactorException(int node, String inMessage) {
		super(JPUtil.getInstance().getPositionString(node) + " " + inMessage);
		filePos = JPUtil.getInstance().getPosition(node);
	}

	/** Create an exception with "filename:line:col message" as the text.
	 * @param pos An int[3] file/line/column position
	 * @param message
	 */
	public RefactorException(int [] pos, String inMessage) {
		super(JPUtil.getInstance().getPositionString(pos) + " " + inMessage);
		filePos = pos;
	}

	/** Create an exception where we don't have a file index.
	 */
	public RefactorException(File file, int line, int col, String message) {
		super(	file.toString()
				+ ":" + Integer.toString(line)
				+ ":" + Integer.toString(col)
				+ " " + message
				);
		this.file = file;
		this.filePos = new int [] { -1, line, col};
	}


} // class
