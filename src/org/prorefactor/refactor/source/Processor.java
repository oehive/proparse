/* Processor.java
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.prorefactor.core.TokenTypes;
import org.prorefactor.refactor.FileStuff;
import org.prorefactor.refactor.RefactorException;

import com.joanju.ProparseLdr;


/** This is a "pseudo processor", inspired by the C refactoring work
 * done by Ralph Johnson and Alejandra Garrido.
 * <p>
 * It generates a complex tree of tokens, complete with all possible
 * branches and expansions for all possible compile-time configurations.
 * <p>
 * We call our tree a Token Expansion Tree (TET). The model for the tree
 * is made up of Token objects and Expansion objects.
 */
public class Processor {

	public Processor(File file, SourceFilePool sourceFilePool) {
		this.file = file;
		this.sourceFilePool = sourceFilePool;
		isUnix = parser.configGet("opsys").equalsIgnoreCase("unix");
	}

	private final boolean isUnix;
	private int symbolSuspend;
	private ArrayList ampIfStack = new ArrayList();
	private File file;
	private PreproScope symbolTable = null;
	private ProparseLdr parser = ProparseLdr.getInstance();
	private SourceFilePool sourceFilePool;

	private void addSymbols(IncludeExpansion include) {
		// TODO Auto-generated method stub
	}



	private List calculatePermsList(Token[] nestedTokens) {
		List permsList;
		ArrayList nestedList = new ArrayList();
		for (int i = 0; i < nestedTokens.length; i++) {
			nestedList.add(nestedTokens[i].getExpansions());
		}
		permsList = org.prorefactor.core.Util.permsList(nestedList);
		return permsList;
	}



	/** For a Token, return a list of TokenContent objects.
	 * Will return only one, unless the token has multiple
	 * possible values due to nested tokens and conditional
	 * compilation.
	 */
	public List calculateTokenContents(Token token) {
		ArrayList retList = new ArrayList();
		if (token.numNestedTokens() == 0) {
			TokenContent content = new TokenContent();
			content.firstAtom = token.getFirstAtom();
			content.terminatorAtom = token.getTerminatorAtom();
			retList.add(content);
			return retList;
		}
		Token[] nestedTokens = (Token[]) token.getNestedTokens().toArray(new Token[0]);
		List permsList = calculatePermsList(nestedTokens);
		for (Iterator it = permsList.iterator(); it.hasNext();) {
			Expansion[] expArray = (Expansion[]) ((List)it.next()).toArray(new Expansion[0]);
			TokenContent content = new TokenContent();
			content.derivation = expArray;
			IAtom mainAtom = token.getFirstAtom();
			IAtom atomRef = null;
			for (int i = 0; i < expArray.length; i++) {
				Token currNested = nestedTokens[i];
				while (	mainAtom!=null
						&&	mainAtom!=token.getTerminatorAtom()
						&&	mainAtom!=currNested.getFirstAtom() ) {
					atomRef = new AtomRef(mainAtom, atomRef);
					mainAtom = mainAtom.next();
				}
				while (	mainAtom!=null
						&&	mainAtom!=token.getTerminatorAtom()
						&& mainAtom!=currNested.getTerminatorAtom() ) {
					mainAtom = mainAtom.next();
				}
				atomRef = collectExpansionAtoms(expArray[i], atomRef);
			}
			while (	mainAtom!=null
					&&	mainAtom!=token.getTerminatorAtom() ) {
				atomRef = new AtomRef(mainAtom, atomRef);
			}
			content.firstAtom = Atom.getFirst(atomRef);
			content.terminatorAtom = null;
			retList.add(content);
		}
		return retList;
	} // calculateTokenContents



	IAtom collectExpansionAtoms(Expansion expansion, IAtom prevRef) {
		// TODO WIP
		return prevRef;
	} // collectExpansionAtoms



	private void collectToClosingCurly(Token token) throws RefactorException, IOException {
		IAtom last = token.getFirstAtom().next();
		for ( ; last != null; last = last.next()) {
			if (isCurlyRef(last)) {
				Token nestedToken = new Token(token);
				processToken(nestedToken, last);
				token.addNestedToken(nestedToken);
				last = nestedToken.getTerminatorAtom();
			}
			if (last.type()==TokenTypes.RIGHTCURLY && !isEscaped(last)) break;
		}
		if (last==null) throw new RefactorException(
			token.nearestEnclosingFile(), token.getFirstAtom().line(), token.getFirstAtom().column()
			, "Failed to find closing curlybrace" );
		token.setTerminatorAtom(last.next());
	} // tokenLeftCurly



	/** Generate the Token Expansion Tree, returns the top node,
	 * which is an IncludeExpansion object.
	 */
	public IncludeExpansion generateTree(CompileUnit compileUnit) throws IOException, RefactorException {
		IncludeExpansion exp = new IncludeExpansion(
			sourceFilePool.getSourceFile(file), compileUnit);
		symbolTable = new PreproScope(null);
		exp.setFirstToken(makeTokenList(exp, exp.getSourceFile().firstAtom)[0]);
		symbolTable = null;
		return exp;
	} // generateTree



	/** Is the Atom a curly reference? Checks for escapes. */
	boolean isCurlyRef(IAtom atom) {
		switch (atom.type()) {
		case TokenTypes.CURLYAMP :
		case TokenTypes.CURLYNUMBER :
		case TokenTypes.CURLYSTAR :
		case TokenTypes.LEFTCURLY :
			if (! isEscaped(atom)) return true;
		default :
			return false;
		}
	} // isCurlyRef



	/** Is the Atom before this one an escape character?
	 * TILDE, or BACKSLASH on unix only.
	 */
	boolean isEscaped(IAtom atom) {
		if (atom.prev() == null) return false;
		switch (atom.prev().type()) {
			case TokenTypes.TILDE :
				return true;
			case TokenTypes.BACKSLASH :
				if (isUnix) return true;
		}
		return false;
	} // isEscaped



	/** Add Expansion objects to a Token object, given an input TokenContents.
	 * May be more than one expansion for a single reference, due to multiple
	 * possible & declarations in conditional compilation.
	 */
	private void makeExpansions(Token token, TokenContent contents)
		throws RefactorException, IOException {

		String refText = Atom.getText(contents.firstAtom, contents.terminatorAtom );
		String textBetween = stripEscapes(refText).substring(1, refText.length() - 1);
		
		// {*}  --  all arguments
		if (refText == "{*}") {
			// TODO symbolTable.getArgsForStar();
			// TODO expansion.setFirstToken(makeTokenList(expansion, decl.getFirstAtom()));
			// TODO expansion.setDerivation(contents.derivation);
			// TODO Newly created expansions have to refer to Declaration.
			return;
		}

		// {&*  --  all named arguments
		if (refText.startsWith("{&*")) {
			// TODO symbolTable.getArgsForAmpStar();
			return;
		}

		// {(0..9)+}  --  a numbered argument
		{
			boolean isInt = true;
			int argNum = -1;
			try {
				argNum = Integer.parseInt(textBetween);
			} catch (NumberFormatException e) {
				isInt = false;
			}
			if (isInt && argNum >= 0) {
				// TODO symbolTable.getArgsForNum(argNum);
				return;
			} 
		}

		// { }  --  empty curlies - ignored
		if (textBetween.trim().length() == 0) {
			return;
		}

		// {& -- named argument or macro expansion
		// Note that you can reference "{&}" to get an
		// undefined named include argument.
		// In that case, argName remains blank.
		// Whitespace following the name of the argument is ignored
		if (refText.startsWith("{&")) {
			// TODO String refName = textBetween.trim().substring(1);
			// TODO symbolTable.getDefsForName(refName);
			return;
		}

		// If we got here, it's an include file reference
		// FUTURE Add support for multiple PROPATH alternatives.
		{
			IncludeExpansion include = new IncludeExpansion(token);
			include.init(textBetween);
			File file = FileStuff.findFile(include.getFilenameRefText());
			if (! file.exists()) throw new RefactorException(
				"Could not find file on PROPATH: " + include.getFilenameRefText());
			include.setSourceFile(sourceFilePool.getSourceFile(file));
			token.addExpansion(include);
			return;
		}

	} // makeExpansions



	/** Given a parent and a firstAtom, generate a token list.
	 * Terminates at &ELSEIF, &ELSE, and &ENDIF - those are
	 * never the last item in the list, but one may be the first
	 * in a list.
	 * Also terminates at one past &THEN (i.e. &THEN might be the
	 * last item in the list.)
	 * @return An array with the first and last Token objects.
	 */
	Token [] makeTokenList(Expansion parent, IAtom firstAtom)
		throws RefactorException, IOException {
		Token [] ret = new Token[2];
		if (firstAtom == null) return ret;
		Token currToken = new Token(parent);
		ret[0] = currToken;
		processToken(currToken, firstAtom);
		main_loop: for (
			IAtom nextAtom = currToken.getTerminatorAtom()
			; nextAtom != null
			; nextAtom = currToken.getTerminatorAtom()
			) {
			switch (nextAtom.type()) {
				case TokenTypes.AMPELSEIF :
				case TokenTypes.AMPELSE :
				case TokenTypes.AMPENDIF :
					if (ampIfStack.size() < 1) throw new RefactorException(
						currToken.nearestEnclosingFile(), currToken.getFirstAtom().line(), currToken.getFirstAtom().column()
						, "Unexpected " + parser.getTokenTypeName(nextAtom.type()) );
					break main_loop;
			}
			Token nextToken = new Token(parent);
			Token token = nextToken;
			Token prev = currToken;
			processToken(token, nextAtom);
			token.setPrev(prev);
			prev.setNext(token);
			currToken = nextToken;
			if (currToken.getType()==TokenTypes.AMPTHEN) {
				if (ampIfStack.size() < 1) throw new RefactorException(
					currToken.nearestEnclosingFile(), currToken.getFirstAtom().line(), currToken.getFirstAtom().column()
					, "Unexpected &THEN" );
				break main_loop;
			} 
		} // main_loop
		if (currToken != ret[0]) ret[1] = currToken;
		return ret;
	} // makeTokenList



	/** Fill a new Token given an Atom starting position */
	void processToken(Token token, IAtom atom) throws RefactorException, IOException {
		if (atom == null) return;
		token.setFirstAtom(atom);
		token.setType(atom.type());
		switch (atom.type()) {
		case TokenTypes.AMPANALYZERESUME :
			tokenAmpAnalyzeResume(token);
			break;
		case TokenTypes.AMPANALYZESUSPEND :
			tokenAmpAnalyzeSuspend(token);
			break;
		case TokenTypes.AMPGLOBALDEFINE :
		case TokenTypes.AMPSCOPEDDEFINE :
			tokenAmpDefine(token);
		case TokenTypes.AMPMESSAGE :
			tokenAmpMessage(token);
			break;
		case TokenTypes.AMPUNDEFINE :
			tokenAmpUndefine(token);
			break;
		case TokenTypes.AMPIF :
			tokenAmpIf(token);
			break;
		case TokenTypes.COMMENTSTART :
			tokenComment(token);
			break;
		case TokenTypes.CURLYAMP :
		case TokenTypes.CURLYNUMBER :
		case TokenTypes.CURLYSTAR :
		case TokenTypes.LEFTCURLY :
			tokenLeftCurly(token);
			break;
		case TokenTypes.SINGLEQUOTE :
		case TokenTypes.DOUBLEQUOTE :
			tokenQuote(token);
			break;
		default :
			// This Token maps to only one Atom
			token.setTerminatorAtom(atom.next());
			break;
		} // switch atom.type
	} // processToken(Token token, IAtom atom)



	/** For an & directive, this function is used to find the
	 * NEWLINE Atom which terminates that directive.
	 * You can have a comment in a directive, and that comment
	 * may contain a newline. It doesn't count.
	 * Also watches for escaped NEWLINE.
	 */
	IAtom seekDirectiveNewline(IAtom startAtom) throws RefactorException, IOException {
		IAtom curr = startAtom.next();
		for ( ; curr!=null; curr = curr.next()) {
			if (curr.type()==TokenTypes.COMMENTSTART) {
				// Create a temporary token just to find the end of the comment
				Token comment = new Token(null);
				processToken(comment, curr);
				curr = comment.getTerminatorAtom().prev();
				continue;
			}
			if (curr.type()==TokenTypes.NEWLINE && ! isEscaped(curr)) break;
		}
		return curr;
	} // seekDirectiveEOL



	public static String stripComments(String orig) {
		char[] chars = orig.toCharArray();
		StringBuilder buff = new StringBuilder();
		int commentLevel = 0;
		for (int pos=0; pos!=chars.length; pos++) {
			if (commentLevel>0 && chars[pos]=='/' && (chars[pos-1])=='*') {
				--commentLevel;
			} else if (chars[pos]=='/' && (chars[pos+1])=='*') {
				++commentLevel;
			} else if (commentLevel==0) {
				buff.append(chars[pos]);
			}
		}
		return buff.toString();
	} // stripComments



	String stripEscapes(String orig) {
		StringBuilder buff = new StringBuilder(orig);
		char curr;
		char_loop: for (int pos = 0; pos < buff.length(); pos++) {
			curr = buff.charAt(pos);
			if (curr!='~' && (!isUnix || curr!='\\')) continue;
			int escapePos = pos;
			pos++;
			main_switch: switch (buff.charAt(pos)) {
			case '\r' :
			case '\n' :
				// escaped '\n' is discarded, as are any '\r' that come before the '\n'
				while (buff.charAt(pos)=='\r') pos++;
				if (buff.charAt(pos)=='\n') {
					buff.delete(escapePos, pos + 1);
					break main_switch;
				} else {
					// '\r' is not dropped. Just drop the escape char.
					buff.deleteCharAt(escapePos);
					break main_switch;
				}
			case 'r' :
				// An escaped 'r' or an escaped 'n' gets *converted* to
				// a different character. We don't just drop chars.
				buff.setCharAt(pos, '\r');
				buff.deleteCharAt(escapePos);
				break main_switch;
			case 'n' :
				// An escaped 'r' or an escaped 'n' gets *converted* to
				// a different character. We don't just drop chars.
				buff.setCharAt(pos, '\n');
				buff.deleteCharAt(escapePos);
				break main_switch;
			default :
				buff.deleteCharAt(escapePos);
				break main_switch;
			} // main_switch
			pos = escapePos - 1;
		} // char_loop
		return buff.toString();
	} // stripEscapes



	/** When gathering atoms and nested tokens for some token types,
	 * such as Strings, we don't want to def/undef/redef any symbols
	 * if we happen to hit any nested &define or &undefine tokens.
	 */
	private void symbolDeclSuspend(boolean suspend) {
		if (suspend) symbolSuspend++;
		else symbolSuspend--;
		assert(symbolSuspend > -1);
	}



	/** Deal with a new Token that starts with an AMPANALYZERESUME Atom */
	private void tokenAmpAnalyzeResume(Token token) throws RefactorException, IOException {
		token.setTerminatorAtom(seekDirectiveNewline(token.getFirstAtom()).next());
		// token.type does not need to change.
	}



	/** Deal with a new Token that starts with an AMPANALYZESUSPEND Atom */
	private void tokenAmpAnalyzeSuspend(Token token) throws RefactorException, IOException {
		token.setTerminatorAtom(seekDirectiveNewline(token.getFirstAtom()).next());
		// token.type does not need to change.
	}



	/** Handle AMPGLOBALDEFINE and AMPSCOPEDDEFINE */
	private void tokenAmpDefine(Token token) throws RefactorException, IOException {
		// TODO wip
		// TODO replace tokenAmpGlobal/Scoped with this.
		// TODO for each token content... could be different name
		// TODO if multiple token contents, then altDefine()
		// TODO if multiple names, then maybeDefine()
		// TODO if both, then maybeAltDefine()
		// TODO if enclosed in &IF, then maybeDefine
		token.setTerminatorAtom(seekDirectiveNewline(token.getFirstAtom()).next());
		List contentList = calculateTokenContents(token);
		for (Iterator it = contentList.iterator(); it.hasNext();) {
			TokenContent content = (TokenContent) it.next();
			String defText = Atom.getText(content.firstAtom, content.terminatorAtom).trim();
			char[] chars = defText.toCharArray();
			int pos = 0;
			// &glob.. or &scoped..
			while (pos<chars.length && !Character.isWhitespace(chars[pos])) pos++;
			// whitespace
			while (pos<chars.length && Character.isWhitespace(chars[pos])) pos++;
			// name and text
			int start = pos;
			while (pos<chars.length && !Character.isWhitespace(chars[pos])) pos++;
			String name = new String(chars, start, pos-start).toLowerCase();
			String text = stripComments(defText.substring(pos)).trim();
			Declaration decl = new Declaration(token, content);
			token.addExpansion(decl);
			symbolTable.addSymbol(token.getType(), name, text, false, false);
		}
	} // tokenAmpDefine



	/** Deal with a new Token that starts with an AMPIF Atom.
	 * This works a little weird. We change the token type
	 * to Token.AMPIF_CONDITIONAL. The &IF itself actually becomes
	 * part of the first expansion, and we are only left with the
	 * &ENDIF atom within this topmost token.
	 * So, when printing, when we hit Token.AMPIF_CONDITIONAL,
	 * we have to print the expansions first, and then this token
	 * to get the &ENDIF, and then carry on to the next sibling.
	 */
	void tokenAmpIf(Token token) throws RefactorException, IOException {
		ampIfStack.add(token);
		IAtom currAtom = token.getFirstAtom();
		while (currAtom!=null && currAtom.type() != TokenTypes.AMPENDIF) {
			ConditionalExpansion exp = new ConditionalExpansion(token);
			int type = currAtom.type();
			Token[] chain;
			if (type==TokenTypes.AMPIF) {
				// Can't call makeTokenList with &IF - would be infinite recursion
				Token ifToken = new Token(token);
				ifToken.setTerminatorAtom(currAtom.next());
				chain = makeTokenList(exp, currAtom.next());
				ifToken.setNext(chain[0]);
				chain[0].setPrev(ifToken);
				chain[0] = ifToken;
			} else {
				chain = makeTokenList(exp, currAtom);
			}
			if (	(type==TokenTypes.AMPIF || type==TokenTypes.AMPELSEIF)
				&&	chain[1].getType() != TokenTypes.AMPTHEN ) 
				throw new RefactorException(
					chain[0].nearestEnclosingFile(), chain[0].getFirstAtom().line(), chain[0].getFirstAtom().column()
					, "Missing &THEN" );
			if (type == TokenTypes.AMPELSE) {
				exp.setFirstProperToken(chain[0].getNext());
				chain[0].setNext(null);
				exp.setFirstCondToken(chain[0]);
			} else {
				exp.setFirstCondToken(chain[0]);
				chain = makeTokenList(exp, chain[1].getTerminatorAtom());
				exp.setFirstProperToken(chain[0]);
			}
			token.addExpansion(exp);
			currAtom = chain[1].getTerminatorAtom();
		}
		if (currAtom==null || currAtom.type() != TokenTypes.AMPENDIF)
			throw new RefactorException(
				token.nearestEnclosingFile(), token.getFirstAtom().line(), token.getFirstAtom().column()
				, "Failed to find &ENDIF" );
		ampIfStack.remove(ampIfStack.size()-1);
		// The original input token becomes AMPIF_CONDITIONAL, and points to the &ENDIF atom
		token.setType(Token.AMPIF_CONDITIONAL);
		token.setFirstAtom(currAtom);
		token.setTerminatorAtom(currAtom.next());
	} // tokenAmpIf



	/** Deal with a new Token that starts with an AMPMESSAGE Atom */
	private void tokenAmpMessage(Token token) throws RefactorException, IOException {
		token.setTerminatorAtom(seekDirectiveNewline(token.getFirstAtom()).next());
		// token.type does not need to change.
	}



	/** Deal with a new Token that starts with an AMPUNDEFINE Atom */
	private void tokenAmpUndefine(Token token) {
		// TODO Auto-generated method stub
	}



	/** Deal with a new Token that starts at a COMMENTSTART Atom.
	 * You can end a comment with something dumb like: ~*~/  -- we don't
	 * support that nonsense, nor do we support a COMMENTSTART in a
	 * different file than the COMMENTEND. Macros are *not* expanded
	 * inside comments.
	 * Comments do not have nested tokens or alternative expansions.
	 */
	private void tokenComment(Token token) throws RefactorException, IOException {
		IAtom currAtom = token.getFirstAtom().next();
		for ( ; currAtom != null; currAtom = currAtom.next()) {
			if (currAtom.type() == TokenTypes.COMMENTSTART) {
				// Create a temporary token just to find the end of the embedded comment
				Token comment = new Token(null);
				processToken(comment, currAtom);
				currAtom = comment.getTerminatorAtom().prev();
			}
			if (currAtom.type() == TokenTypes.COMMENTEND) break;
		}
		if (currAtom==null) {
			throw new RefactorException(
				token.nearestEnclosingFile(), token.getFirstAtom().line(), token.getFirstAtom().column()
				, "Failed to find end of comment"
				);
		}
		token.setTerminatorAtom(currAtom.next());
		token.setType(TokenTypes.COMMENT);
	} // tokenComment



	/** Deal with a new token that starts at:
	 * LEFTCURLY, CURLYAMP, CURLYSTAR, or CURLYNUMBER.
	 */ 
	private void tokenLeftCurly(Token token) throws RefactorException, IOException {
		// TODO (TEST FIRST!)
		//		If the parent of this token is another token:
		//		then this is a nested token, and we require all full
		//		expansions, for the sake of finding all possible values
		//		of the parent token.
		//		Otherwise for include files, we just generate one regular include file
		//		expansion for each possible filename referenced.
		// TODO WIP
		token.setType(TokenTypes.LEFTCURLY);
		symbolDeclSuspend(true);
		collectToClosingCurly(token);
		symbolDeclSuspend(false);
		// It's possible to have multiple reference names.
		// That is because it is possible to have one or more nested tokens, and for
		// each of those nested tokens, it is possible for those to expand to different
		// text due to conditional compilation.
		List contentsList = calculateTokenContents(token);
		for (Iterator it = contentsList.iterator(); it.hasNext();) {
			TokenContent contents = (TokenContent) it.next();
			makeExpansions(token, contents);
		}


		// TODO If not nested then process expansions
		// TODO Might get multiple references to same include filename here (each with
		// different args. If not nested...)
		List exps = token.getExpansions();
		for (Iterator it = exps.iterator(); it.hasNext();) {
			Expansion exp = (Expansion) it.next();

			if (exp instanceof IncludeExpansion) {
				IncludeExpansion include = (IncludeExpansion) exp;
				if (token.getParent() instanceof Token) {
					// TODO
					// Is a nested token. Include files require immediate expansion.
				} else {
					// TODO
					// Watch for files that have already been expanded.
					// Just add any new symbol alternatives from this alt reference.
				}
				// TODO this will likely be part of the if block above.
				symbolTable = symbolTable.push();
				addSymbols(include);
				include.setFirstToken(makeTokenList(include, include.getSourceFile().firstAtom)[0]);
				symbolTable = symbolTable.pop();
			}


		}
		
	} // tokenLeftCurly



	/** Deal with a new Token that starts at a DOUBLEQUOTE or SINGLEQUOTE Atom.
	 * Progress's compiler and Proparse both allow a string to begin in one file
	 * and end in another. We don't put up with that nonsense here.
	 * Macros *are* expanded inside of a string - those become nested Token
	 * objects in our TET model.
	 * We watch for escaped quotes and quoted quotes.
	 */
	private void tokenQuote(Token token) throws RefactorException, IOException {
		token.setType(TokenTypes.QSTRING);
		symbolDeclSuspend(true);
		gather_atoms: {
			int quoteType = token.getFirstAtom().type();
			IAtom last = token.getFirstAtom().next();
			for ( ; last != null; last = last.next()) {
				if (last.type() == quoteType) {
					if (last.next().type() == quoteType) {
						last = last.next();
						continue;
					}
					if (! isEscaped(last)) break;
				}
				if (isCurlyRef(last)) {
					Token nestedToken = new Token(token);
					processToken(nestedToken, last);
					token.addNestedToken(nestedToken);
					last = nestedToken.getTerminatorAtom();
				}
			}
			if (last==null) throw new RefactorException(
				token.nearestEnclosingFile(), token.getFirstAtom().line(), token.getFirstAtom().column()
				, "Failed to find end of quoted string" );
			token.setTerminatorAtom(last.next());
		}
		symbolDeclSuspend(false);
	} // tokenQuote



} // class
