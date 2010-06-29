/**
 * NoundoTarget.java
 * @author John Green
 * 17-Jul-2003
 * www.joanju.com
 * 
 * Copyright (c) 2003 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.noundo;

import org.prorefactor.refactor.*;
import org.prorefactor.treeparser.Symbol;



/**
 * Represents a (potential) target for NO-UNDO refactoring.
 */
public class NoundoTarget extends RefactorTarget {

	/** The symbol that was defined without NO-UNDO */
	public Symbol symbol;

} // class NoundoTarget

