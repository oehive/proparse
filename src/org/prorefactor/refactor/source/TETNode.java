/* TETNode.java
 * Created on Jan 29, 2004
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


/** The Token Expansion Tree is made up of Token objects
 * and Expansion objects - those are the implementing clients.
 * <p>
 * This interface declares the methods navigating and searching within the
 * model for the Token Expansion Tree.
 */
public interface TETNode {


	/** Find the nearest enclosing File, ignoring that the *actual* text for the
	 * Token may have come from a Declaration some number of layers higher.
	 * Useful for reporting errors.
	 */
	public File nearestEnclosingFile();



} // interface
