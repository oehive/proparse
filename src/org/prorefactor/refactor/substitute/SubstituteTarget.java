/**
 * SubstituteTarget.java
 * @author John Green
 * 1-Nov-2002
 * www.joanju.com
 * 
 * Copyright (c) 2002-2003 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.substitute;

import org.prorefactor.refactor.*;


/**
 * Represents a (potential) target for SUBSTITUTE refactoring.
 */
public class SubstituteTarget extends RefactorTarget {


	/**
	 * Do the string attributes mismatch on this target?
	 */
	public int attributesMismatch;


	/**
	 * Number of translatable strings in the expression.
	 */
	public int numTranslatable;


	/**
	 * Do the quotation types mismatch in the target?
	 * (i.e. Is the expression a mix of single-quote strings
	 * and double-quote strings.)
	 */
	public int quoteTypeMismatch;


} // class SubstituteTarget

