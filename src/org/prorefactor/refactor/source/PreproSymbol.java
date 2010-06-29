/* PreproSymbol.java
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

import java.util.ArrayList;


/** These objects only exist long enough to construct the Token tree.
 * They are the references to a list of possible declarations for a
 * given preprocessor symbol. They are created and dropped by the
 * pseudo processor, and are stored in maps in PreproScope objects.
 */
public class PreproSymbol {

	/** It is possible to conditionally define or undefine a preprocessor symbol.
	 * We have to know if this symbol is perhaps not defined (expands to empty string).
	 */
	boolean maybeUndefined = false;

	/** All of the possible declarations for this preprocessor symbol.
	 * There would normally only be one, unless different
	 * declarations come from different branches.
	 */
	ArrayList declarations = new ArrayList();

} // class
