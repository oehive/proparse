/* PreproScope.java
 * Created on Jan 27, 2004
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
import java.util.HashMap;
import java.util.List;

import org.prorefactor.core.TokenTypes;



/** Preprocessor scopes are stacked, temporary objects,
 * pushed and popped off the stack by the pseudo processor.
 */
public class PreproScope {

	public PreproScope(PreproScope parent) {
		this.parent = parent;
		if (parent==null) globalSymbols = new HashMap();
	}

	/** The globals can't just be stored in the same place as the scoped
	 * from the topmost scope - try it with def global and scoped and undefine.
	 * This map is null unless this is the topmost scope.
	 */
	private HashMap globalSymbols = null;

	private HashMap intKeys = new HashMap();
	private HashMap stringKeys = new HashMap();
	private PreproScope parent;



/*
	* TODO If local scope are maybeDefined(), also return values from
	* parent scopes?
*/



	/** Add a symbol to the symbol tables.
	 * @param type The type can be either AMPGLOBALDEFINE or AMPSCOPEDDEFINE.
	 * @param name The symbol name.
	 * @param value The text for the newly defined macro.
	 * @param altDef Are there multiple possible values for this name?
	 * @param maybeDef Is this symbol conditionally defined?
	 */
	void addSymbol(int type, String name, String value, boolean altDef, boolean maybeDef) {
		if (type==TokenTypes.AMPGLOBALDEFINE) {
			if (parent!=null) {
				parent.addSymbol(type, name, value, altDef, maybeDef);
				return;
			}
			globalSymbols.put(name, value);
			return;
		}
		stringKeys.put(name, value);
	} // addSymbol



	List getArgsForAmpStar() {
		// TODO not started
		return new ArrayList();
	}



	List getArgsForNum(int argNum) {
		// TODO not started
		return new ArrayList();
	}



	List getArgsForStar() {
		// TODO not started
		return new ArrayList();
	}



	private List getDefsForName(String refName) {
		// TODO not started
		return new ArrayList();
	}



	/** Get the next scope down on the stack.
	 */
	public PreproScope pop() {
		assert this.parent != null;
		return this.parent;
	}



	/** Push a new scope onto the stack.
	 * Returns the new scope. It is the client's
	 * responsibility to maintain a reference to the
	 * new scope, because nothing else does.
	 */
	public PreproScope push() {
		PreproScope child = new PreproScope(this);
		return child;
	}



} // class
