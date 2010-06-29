/* IncludeExpansion.java
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/** Represents a curly reference to an include file.
 * Note that for any one curly that expands to an include file, it is
 * possible (but uncommon) to have multiple expansions. That only
 * happens when the filename itself is a curly expansion with
 * multiple possible values.
 * <p>
 * Note that this model will also allow for multiple possible expansions
 * due to multiple possible PROPATH configurations. We have no short term
 * plans to implement support for this, but it is an important consideration
 * in the design of this model.
 */
public class IncludeExpansion extends Expansion {

	/** For creating the topmost file reference, ex: "main.p" */
	IncludeExpansion(SourceFile sourceFile, CompileUnit compileUnit) {
		super(null);
		this.sourceFile = sourceFile;
		this.compileUnit = compileUnit;
	}

	/** For creating any included files, but not the topmost "main.p" */
	IncludeExpansion(Token parent) {
		super(parent);
	}

	/** @see #getArgsByNumber() */
	private ArrayList argsByNumber = null;

	/** @see #getCompileUnit() */
	private CompileUnit compileUnit = null;

	/** @see #getArgsByName() */
	private HashMap argsByName = null;

	private SourceFile sourceFile;

	/** @see #getFilenameRefText() */
	private String filenameRefText;

	private static class IncludeRefArg {
		IncludeRefArg(int pos, String text) {
			this.pos = pos;
			this.text = text;
		}
		int pos;
		String text;
	}



	void addNamedArgument(String name, String val) {
		if (argsByName==null) argsByName = new HashMap();
		argsByName.put(name, val);
		addNumberedArgument(val);
	}



	void addNumberedArgument(String val) {
		if (argsByNumber==null) argsByNumber = new ArrayList();
		argsByNumber.add(val);
	}



	/** The map from an argument name to the argument Declaration object.
	 * Is null if there were no args or if numbered (not named) arguments were used.
	 * The key is a String, the value is a List of Declaration objects. Each named
	 * arg may have multiple possible values due to nested curlies.
	 */
	public Map getArgsByName() { return argsByName; }

	/** The list of argument Declaration objects that we can refer to by number.
	 * Is null if there were no arguments.
	 * Each entry is itself a List, because each argument may itself have multiple
	 * possible expansions due to nested curlies.
	 */
	public ArrayList getArgsByNumber() { return argsByNumber; }

	/** Mostly null, except for the main.p expansion.
	 * Only has a value when parentToken==null;
	 */
	public CompileUnit getCompileUnit() { return compileUnit; }

	/** The text used for refering to an include file,
	 * in other words, the first token that came after the opening curly.
	 */
	public String getFilenameRefText() { return filenameRefText; }

	public SourceFile getSourceFile() { return sourceFile; }



	/** How to process an include reference arg.
	 * Input an integer starting position in a string.
	 * A doublequote will start a string - 
	 * all this means is that we'll collect whitespace.
	 * A singlequote does not have this effect.
	 */
	private IncludeRefArg calculateIncludeRefArg(char[] text, int pos) {
		boolean gobbleWS = false;
		char curr;
		StringBuilder buff = new StringBuilder();
		main_loop: while (pos!=text.length) {
			curr = text[pos];
			switch (curr) {
			case '"':
				if (text[pos+1]=='"') { // quoted quote - does not open/close a string
					buff.append('"');
					++pos;
					++pos;
				} else {
					gobbleWS = !gobbleWS;
					++pos;
				}
				break;
			case ' ':
			case '\t':
			case '\f':
			case '\n':
			case '\r':
				if (gobbleWS) {
					buff.append(curr);
					++pos;
				} else {
					break main_loop;
				}
				break;
			default:
				buff.append(curr);
				++pos;
				break;
			} // switch
		} // main_loop
		return new IncludeRefArg(pos, buff.toString());
	} // calculateIncludeRefArg()



	/** The input text is expected to have been stripped of the enclosing curlies,
	 * trimmed, and processed for macro expansion as well as for escape sequences.
	 * This <b>only</b> populates the filename and args within the IncludeExpansion
	 * object. The client is responsible for filling symbol tables, generating token
	 * lists from the include file, etc.
	 */
	void init(String innerText) {
		char currChar;
		int pos;

		char [] chars = innerText.toCharArray();
		int innerLength = innerText.length();

		// filename
		IncludeRefArg name = calculateIncludeRefArg(chars, 0);
		pos = name.pos;
		filenameRefText = name.text;


		// no include args?
		if (pos == innerLength) {
			// do nothing
		}
		
		else if (chars[pos] == '&') { // include '&' named args
			while (pos!=innerLength && chars[pos]=='&') {
				++pos; // skip '&'

				// Arg name: Consume to '=' or end, discard all WS
				StringBuilder argName = new StringBuilder();
				while (pos!=innerLength) {
					currChar = chars[pos];
					if (currChar == '=') break;
					if (!(Character.isWhitespace(currChar))) argName.append(currChar);
					++pos;
				}

				String argVal = "";
				if (chars[pos] == '=') {
					// '=' with optional WS
					++pos;
					while (pos != innerLength && Character.isWhitespace(chars[pos])) ++pos;
					// Arg val
					if (pos != innerLength) {
						IncludeRefArg arg = calculateIncludeRefArg(chars, pos);
						pos = arg.pos;
						argVal = arg.text;
					}
				}


				addNamedArgument(argName.toString(), argVal);

				// Anything not beginning with & is discarded
				while (chars[pos]!='&' && pos!=innerLength) ++pos;

			} // while loop
		} // include '&' named args

		else { // include numbered args
			while (pos!=innerLength) {
				while (Character.isWhitespace(chars[pos])) pos++;
				if (pos == innerLength) break;
				IncludeRefArg arg = calculateIncludeRefArg(chars, pos);
				pos = arg.pos;
				addNumberedArgument(arg.text);
			}
		} // numbered args

	} // init



	/* @see org.prorefactor.refactor.source.TETNode#nearestEnclosingFile() */
	public File nearestEnclosingFile() { return sourceFile.file; }



	public void setSourceFile(SourceFile file) { sourceFile = file; }



	/* @see org.prorefactor.refactor.source.Expansion#toStringSub() */
	public String toStringSub() {
		StringBuilder buff = new StringBuilder("<ExpansionAttributes ");
		buff.append("subtype=\"include\" ");
		buff.append("file=\"" + sourceFile.file.toString() + "\"");
		buff.append("/>");
		return buff.toString();
	}



	boolean usesNamedArgs() { return argsByName != null; }



} // class
