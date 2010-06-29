/* IAtom.java
 * Created on Feb 4, 2004
 * John Green
 *
 * Copyright (C) 2004 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.source;

/**
 */
public interface IAtom {

	///// Accessors /////
	public abstract int column();
	public abstract int line();
	public abstract IAtom next();
	public abstract IAtom prev();
	public abstract String text();
	public abstract int type();
	public abstract void setColumn(int column);
	public abstract void setLine(int line);
	public abstract void setNext(IAtom next);
	public abstract void setPrev(IAtom prev);
	public abstract void setType(int type);
	public abstract void setText(String text);

} // interface
