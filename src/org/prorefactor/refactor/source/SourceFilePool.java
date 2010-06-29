/* SourceFilePool.java
 * Created on Jan 26, 2004
 * John Green
 *
 * Copyright (C) 2004 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.source;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/** We may work with two or more CompileUnit objects at the
 * same time, and those may reference some of the same files.
 * We only want to load those "shared" files into memory once.
 */
public class SourceFilePool {

	public SourceFilePool() {}

	private HashMap sourceFiles = new HashMap();



	/** Fetch a SourceFile object from a File object.
	 * You should work with canonicalpath when reasonable.
	 */
	public SourceFile getSourceFile(File file) throws IOException {
		if (sourceFiles.containsKey(file))
			return (SourceFile) sourceFiles.get(file);
		SourceFile ret = new SourceFile(file);
		sourceFiles.put(file, ret);
		return ret;
	}



} // class
