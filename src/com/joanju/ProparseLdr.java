/**
 * @author John Green
 * October, 2002
 * www.joanju.com
 * 
 * Copyright (c) 2002-2007 Joanju Software.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.joanju;


import java.io.File;
import java.util.ArrayList;


/**
 * Primary JNI interface to proparse.dll.
 * Is a "Singleton". Create a reference to this
 * in order to refer to the parser and its API.
 */
public class ProparseLdr {

	private static ProparseLdr instance = null;

	private ProparseLdr() {
		initLib();
	}

	private ProparseLdr(boolean noInit) {}

	/** This class is a "singleton" */
	public static ProparseLdr getInstance() {
		if (instance == null)
			instance = new ProparseLdr();
		return instance;
	}

	/**
	 * Try to load proparse.dll using a fully qualified path/filename.
	 * This method checks for exceptions, and returns a simple error
	 * string if the load failed.
	 */
	public static String loadUsing(String fullpath) {
		File inFile = new File(fullpath);
		if (! inFile.isFile())
			return "Invalid path to proparse.dll: " + fullpath;
		try {
			System.load(inFile.getAbsolutePath());
		} catch (Throwable e) {
			return "Failed to load " + fullpath;
		}
		// Now create the instance *without* calling initLib.
		instance = new ProparseLdr(false);
		return "";
	}

	private void initLib() {
		System.loadLibrary("proparse");
	}



	/////  Begin utility functions  /////



	/** Because the filenames are not actually stored, but an integer index is,
	 * it is common to need an array representation of the filenames.
	 * Important note: Sometimes Proparse will stick a blank "" entry into this list.
	 * @deprecated May,2006. The new "filename-list" node attribute is used when the tree is loaded
	 * into JPNode, and the filename array is stored in the ProgramRootNode object.
	 * @see org.prorefactor.treeparser.ParseUnit#getFileIndex()
	 */
	public String [] getFilenameArray() {
		ArrayList names = new ArrayList();
		for (int i = 0; errorGetStatus() >= 0 ; i++) {
			names.add(getIndexFilename(i));
		}
		// getIndexFilename raises a warning when we've gone past the last index
		errorClear();
		return (String[]) names.toArray(new String[names.size()]);
	}



	/////  Begin parser function calls /////


	/** 
	 * Returns the integer value of a node attribute.
	 * @param handle
	 * @param key The integer representation of the attribute key.
	 * @return zero if the node does not have that attribute
	 */
	public native int attrGetI(int handle, int key);


	/**
	 * Returns the string value of a node attribute. 
	 * @return An empty string "" if the node does not have that attribute.
	 */
	public String attrGetS(int handle, String key) {
		return new String(attrGetSJNI(handle, key.getBytes()));
	}
	private native byte [] attrGetSJNI(int handle, byte [] key);


	/**
	 * Sets a node's attribute. 
	 * @param handle The node's handle.
	 * @param key The attribute key.
	 * @param val The attribute value.
	 * @return positive int on success
	 */
	public native int attrSet(int handle, int key, int val);


	/**
	 * Returns the string value of an integer-keyed node attribute.  
	 * This function doesn't use the same internal table as the 
	 * parserAttrGet and parserAttrGetI functions, 
	 * and there is no cross reference between the two tables.
	 * @see parserAttrStringSet
	 * @return Returns an empty string "" if the node does not have that attribute. 
	 */
	public String attrStringGet(int handle, int key) {
		return new String(attrStringGetJNI(handle, key));
	}
	private native byte [] attrStringGetJNI(int handle, int key);


	/**
	 * Sets a string attribute on a node. 
	 * This function doesn't use the same internal table as the 
	 * parserAttrSet function, and there is no cross reference 
	 * between the two tables. 
	 * @see parserAttrStringGet
	 * @return Positive integer on success.
	 */
	public int attrStringSet(int handle, int key, String val) {
		return attrStringSetJNI(handle, key, val.getBytes());
	}
	private native int attrStringSetJNI(int handle, int key, byte [] val);


	/**
	 * Not currently used.
	 */
	public native int cleanup();


	/**
	 * Returns a configuration value. 
	 * If an invalid configuration flag parameter is used, 
	 * then an empty string "" is returned and a Proparse error condition is raised.
	 */
	public String configGet(String flag) {
		return new String(configGetJNI(flag.getBytes()));
	}
	private native byte [] configGetJNI(byte [] flag);


	/**
	 * Sets a configuration value.
	 * @return A positive integer on success.
	 */
	public int configSet(String flag, String val) {
		return configSetJNI(flag.getBytes(), val.getBytes());
	}
	private native int configSetJNI(byte [] flag, byte [] val);


	/**
	 * Copies a pointer to a node, the result being that toHandle 
	 * now points to the same node as fromHandle.
	 * @return A positive integer on success.
	 */
	public native int copyHandle(int fromHandle, int toHandle);


	/**
	 * Adds a user string literal to the user node types dictionary. 
	 * For example, parserDictAdd("define_const", "DEFINE") 
	 * would add "define_const" as a string literal to be treated 
	 * the same as "def" or "DEFINE". 
	 * @param theText The string literal to add.
	 * @param theType Must be a valid Proparse node type.
	 * @return A positive integer on success.
	 */
	public int dictAdd(String theText, String theType) {
		return dictAddJNI(theText.getBytes(), theType.getBytes());
	}
	private native int dictAddJNI(byte [] theText, byte [] theType);


	/**
	 * Removes a string literal from the user node types dictionary. 
	 * @param theText The string literal to remove.
	 * @return 1 even if theText did not exist as an entry in the user node types dictionary.
	 */
	public int dictDelete(String theText) {
		return dictDeleteJNI(theText.getBytes());
	}
	private native int dictDeleteJNI(byte [] theText);


	/**
	 * Trivially finds differences between two files. 
	 * Used by Joanju for testing the scanner.
	 * @return An empty string "" if no differences were found.
	 */
	public String diff(String file1, String file2) {
		return new String(diffJNI(file1.getBytes(), file2.getBytes()));
	}
	private native byte [] diffJNI(byte [] file1, byte [] file2);


	/**
	 * Resets Proparse's error status to zero and clears out the warning/error text.
	 * @return 1.
	 */
	public native int errorClear();


	/**
	 * Use this function to find out if the current error status is current. 
	 * If subsequent API function calls have been made since the call which 
	 * raised the error status, then this function returns 0.
	 * @return 1 if true, 0 if false.
	 */
	public native int errorGetIsCurrent();


	/**
	 * Get the parser's error status.
	 * If an API function call raises a warning or an error status, 
	 * that warning or error status remains in effect until parserErrorClear is called.
	 * @return
	 * Returns zero if there is no error or warning. 
	 * Returns -1 if there has been a warning. 
	 * Returns -2 if there has been an error. 
	 */
	public native int errorGetStatus();


	/**
	 * If there is an error condition in Proparse, 
	 * then this returns the error message. 
	 * Use this function to get the text of warnings as well as errors. 
	 * (i.e. Use this function whether the error status is -1 or -2.) 
	 * If an API function call raises a warning or an error status, 
	 * that warning or error message remains available until parserErrorClear is called.
	 * @return An empty string "" if there is no current error condition.
	 */
	public String errorGetText() {
		return new String(errorGetTextJNI());
	}
	private native byte [] errorGetTextJNI();


	/**
	 * Get a node handle.
	 * To use node handles, define an int variable and use parserGetHandle to set its value. 
	 * That variable can now be used in places where the parser's API requires a node handle. 
	 * If there is a free node pointer (one that has been released by parserReleaseHandle), 
	 * then the DLL will reuse it, rather than create a new pointer.
	 * @return An integer reference to a pointer to a node.
	 */
	public native int getHandle();

	/**
	 * Get the file name for a given file index.
	 * Not commonly used. Normally, you would want to just use getNodeFilename.
	 * File name indexes, rather than the entire filename, are stored in the syntax
	 * tree in Proparse. You can use this index number for efficient external
	 * storage of the syntax tree.
	 * Raises a warning condition if an invalid index was accessed.
	 * Use the warning condition (errorGetStatus) if iterating through index numbers
	 * to get all filenames. Do not check for a blank filename return - there is no
	 * guarantee that there will not be a blank entry in the filename list.
	 * @param index The file index
	 * @return The file name, as stored within Proparse.
	 * Raises warning condition if the index number is invalid.
	 * @deprecated See the new "filename-list" node attribute in Proparse.
	 * @see ProparseLdr#getNodeFileIndex(int)
	 * @see ProparseLdr#getNodeFilename(int)
	 */
	public String getIndexFilename(int index) {
		return new String(getIndexFilenameJNI(index));
	}
	private native byte [] getIndexFilenameJNI(int index);


	/**
	 * Get the column position of a node's original text.
	 * If the node represents an actual source code token, 
	 * then this function returns the column position within the source file 
	 * where the token was found. 
	 * A tab character counts as a single column. 
	 * @return Integer column position, starts at 1.
	 */
	public native int getNodeColumn(int handle);


	/**
	 * Get the file index for a given node.
	 * Not commonly used. Normally, you would want to just use getNodeFilename.
	 * File name indexes, rather than the entire filename, are stored in the syntax
	 * tree in Proparse. You can use this index number for efficient external
	 * storage of the syntax tree.
	 * @param handle The node handle
	 * @return The integer index to the file's name
	 * @see ProparseLdr#getNodeFilename(int)
	 */
	public native int getNodeFileIndex(int handle);


	/**
	 * If the node represents an actual source code token, then this function
	 * returns the name of the source file where the token was found.
	 */
	public String getNodeFilename(int handle) {
		return new String(getNodeFilenameJNI(handle));
	}
	private native byte [] getNodeFilenameJNI(int handle);


	/**
	 * If the node represents an actual source code token, then this function
	 * returns the line number within the source file where the token was found.
	 */
	public native int getNodeLine(int handle);


	/**
	 * Returns the original source code for the given node's token.
	 * For example, if the node type is "SUBSTRING", the original source
	 * code that the node's token is related to may have been "substr".
	 */
	public String getNodeText(int handle) {
	 	return new String(getNodeTextJNI(handle));
	}
	private native byte[] getNodeTextJNI(int handle);


	/**
	 * Returns the type of the given node's token.
	 * For example, if the original source code token was "substr",
	 * the node type is "SUBSTRING".
	 */
	public String getNodeType(int handle) {
		return new String(getNodeTypeJNI(handle));
	}
	private native byte [] getNodeTypeJNI(int handle);


	/**
	 * Returns the integer type of the given node's token.
	 * For example, if the original source code token was "substr", the node
	 * type would be "SUBSTRING" and the integer node type might be 766.
	 * @see parserSetNodeTypeI
	 */
	public native int getNodeTypeI(int handle);


	/**
	 * Returns the token type name of the given token type number.
	 * For example, Proparse's internal representation of token type number 766
	 * may be the SUBSTRING token type (these numbers can change).
	 * @see parserGetTokenTypeNumber
	 * @return An empty string "" if the tokenTypeNumber is not valid.
	 */
	public String getTokenTypeName(int tokenTypeNumber) {
		return new String(getTokenTypeNameJNI(tokenTypeNumber));
	}
	private native byte [] getTokenTypeNameJNI(int tokenTypeNumber);


	/**
	 * Returns the token type number of the given token type name. For example,
	 * Proparse's internal representation of the SUBSTRING token type may be
	 * token type number 766 (these numbers can change). Differs from the
	 * parserGetNodeTypeI function in that a specific node does not need to be supplied.
	 * @see parserGetTokenTypeName
	 */
	public int getTokenTypeNumber(String tokenTypeName) {
		return getTokenTypeNumberJNI(tokenTypeName.getBytes());
	}
	private native int getTokenTypeNumberJNI(byte [] tokenTypeName);


	/**
	 * Returns the version of proparse.dll.
	 */
	public String getVersion() {
		return new String(getVersionJNI());
	}
	private native byte [] getVersionJNI();


	/**
	 * Creates a new hidden token with type <code>newType</code> and text <code>newText</code>,
	 * and makes this token the first hidden token of <code>theHandle</code>.
	 * It also becomes the current hidden token.
	 * See also Modifying Hidden Tokens in the User Guide.
	 * @param theHandle
	 * @param newType
	 * @param newText
	 * @return Positive integer on success.
	 */
	public int hiddenAddToFront(int theHandle, String newType, String newText) {
		return hiddenAddToFrontJNI(theHandle, newType.getBytes(), newText.getBytes());
	}
	private native int hiddenAddToFrontJNI(int theHandle, byte [] newType, byte [] newText);


	/**
	 * Deletes the current hidden token of <code>theHandle</code>.
	 * If there is a "next" hidden token, it becomes the current hidden token.
	 * Otherwise, if there is a "previous" hidden token,
	 * it becomes the current hidden token.
	 * If this was the last hidden token of <code>theHandle</code>,
	 * there is no current hidden token.
	 * See also Modifying Hidden Tokens in the User's Guide.
	 * @return Positive integer on success.
	 */
	public native int hiddenDelete(int theHandle);


	/**
	 * Sets the current hidden token to be the hidden token that comes before
	 * the node referred to by the input handle.
	 * @return 1 if found, 0 if not found, negative number on error.
	 */
	public native int hiddenGetBefore(int handle);


	/**
	 * This function returns the column position within the source file
	 * where the current hidden token was found.
	 * A tab character counts as a single column.
	 */
	public native int hiddenGetColumn();


	/**
	 * Returns the name of the source file where the current hidden token came from.
	 */
	public String hiddenGetFilename() {
		return new String(hiddenGetFilenameJNI());
	}
	private native byte [] hiddenGetFilenameJNI();


	/**
	 * Sets the current hidden token to be the first hidden token that comes before
	 * the node referred to by the input handle.
	 * See Hidden Tokens in the User's Guide for a description of 
	 * what "first hidden token" means. 
	 * @return 1 if found, 0 if not found, negative number on error.
	 */
	public native int hiddenGetFirst(int handle);


	/**
	 * Returns the line number within the source file
	 * where the current hidden token came from.
	 */
	public native int hiddenGetLine();


	/**
	 * Finds the next hidden token after the current hidden token,
	 * then sets the current hidden token to be that newly found one.
	 * @return 1 if found, 0 if not found, negative number on error.
	 */
	public native int hiddenGetNext();


	/**
	 * Finds the hidden token previous to the current hidden token,
	 * then sets the current hidden token to be that newly found one.
	 * @return 1 if found, 0 if not found, negative number on error.
	 */
	public native int hiddenGetPrevious();


	/**
	 * Returns the text of the current hidden token.
	 */
	public String hiddenGetText() {
		return new String(hiddenGetTextJNI());
	}
	private native byte [] hiddenGetTextJNI();


	/**
	 * Returns the type of the current hidden token, for example
	 * "WS" for whitespace, or "COMMENT".
	 */
	public String hiddenGetType() {
		return new String(hiddenGetTypeJNI());
	}
	private native byte [] hiddenGetTypeJNI();


	/**
	 * Creates a new hidden token of the given type and text and inserts it after the
	 * current hidden token.
	 * The new token then becomes the current hidden token.
	 * See also Modifying Hidden Tokens in the User Guide.
	 * @return Positive integer on success, negative integer on error.
	 */
	public int hiddenInsertAfter(String newType, String newText) {
		return hiddenInsertAfterJNI(newType.getBytes(), newText.getBytes());
	}
	private native int hiddenInsertAfterJNI(byte [] newType, byte [] newText);


	/**
	 * Sets the text of the current hidden token to <code>newText</code>.
	 * See also Modifying Hidden Tokens.
	 * @return  Positive integer on success, negative integer on error.
	 */
	public int hiddenSetText(String newText) {
		return hiddenSetTextJNI(newText.getBytes());
	}
	private native int hiddenSetTextJNI(byte [] newText);


	/**
	 * Sets the type of the current hidden token to <code>newType</code>.
	 * Valid values are "WS" for whitespace, or "COMMENT". Returns true on success.
	 * See also parserHiddenGetType and Modifying Hidden Tokens in the User Guide.
	 * @return Positive integer on success, negative integer on error.
	 */
	public int hiddenSetType(String newType) {
		return hiddenSetTypeJNI(newType.getBytes());
	}
	private native int hiddenSetTypeJNI(byte [] newType);


	/**
	 * Not currently used.
	 * @return Always returns 1.
	 */
	public native int init();


	/**
	 * Compares the node for <code>handle1</code> with the node for <code>handle2</code>,
	 * and returns 1 if the handles refer to the same node or returns 0 if they don't.
	 * Returns negative number on error. 
	 */
	public native int isSameNode(int handle1, int handle2);


	/**
	 * Checks the node for <code>theHandle</code>, and returns true if the handle refers
	 * to a valid node or returns false if the node isn't valid. 
	 */
	public native int isValidNode(int handle);


	/**
	 * Used by <code>testrun2.p</code> to check that the output from compile with
	 * preprocess is the same as the output from the parser.
	 * Originally named as "Ignore Whitespace diff",
	 * it does less ignoring whitespace now,
	 * and does much more to compensate for the way that Progress's preprocessor
	 * writes out preprocessed code.
	 * @param file1 The COMPILE..PREPROCESS output
	 * @param file2 The output from Proparse (usually just from
	 * the <code>writenode</code> function)
	 * @return Empty string on success,
	 * otherwise a string showing the line numbers from the two files
	 * where a difference was found.
	 */
	public String iwdiff(String file1, String file2) {
		return new String(iwdiffJNI(file1.getBytes(), file2.getBytes()));
	}
	private native byte [] iwdiffJNI(byte [] file1, byte [] file2);


	/**
	 * Creates a new node of type <code>newType</code>, with text <code>newText</code>,
	 * and places the handle of this new node into the <code>intoHandle</code>.
	 * See also "Tree Manipulation and Code Refactoring"
	 * and its subsection
	 * "Creating Nodes" (in the User Guide)
	 * for further information, including how to find valid node types for this function.
	 * @return Positive integer on success.
	 */
	public int nodeCreate(int intoHandle, String newType, String newText) {
		return nodeCreateJNI(intoHandle, newType.getBytes(), newText.getBytes());
	}
	private native int nodeCreateJNI(int intoHandle, byte [] newType, byte [] newText);


	/**
	 * Creates a new node of integer type <code>newType</code> (which does not have to
	 * be a valid node type), with text <code>newText</code>, and places the handle
	 * of this new node into the <code>intoHandle</code>.
	 * See also "Tree Manipulation and Code Refactoring"
	 * and its subsection
	 * "Creating Nodes" (in the User Guide)
	 * for further information, including how to find valid node types for this function.
	 * @return Positive integer on success.
	 */
	public int nodeCreateI(int intoHandle, int newType, String newText) {
		return nodeCreateIJNI(intoHandle, newType, newText.getBytes());
	}
	private native int nodeCreateIJNI(int intoHandle, int newType, byte [] newText);


	/**
	 * A handle to the first child of <code>ofHandle</code> is stored in
	 * <code>intoHandle</code>.
	 * If there is no first child, <code>intoHandle</code> is not changed.
	 * The same handle may be used for
	 * both <code>ofHandle</code> and <code>intoHandle</code>, with the effect
	 * of moving the handle from one node to the next.  
	 * @return  Returns <code>""</code> if <code>ofHandle</code>
	 * has no first child, otherwise, the found node's type is returned.
	 */
	public String nodeFirstChild(int ofHandle, int intoHandle) {
		return new String(nodeFirstChildJNI(ofHandle, intoHandle));
	}
	private native byte [] nodeFirstChildJNI(int parent, int child);


	/**
	 * A handle to the first child of <code>ofHandle</code> is stored in
	 * <code>intoHandle</code>.
	 * If there is no first child, <code>intoHandle</code> is not changed.
	 * The same handle may be used for both <code>ofHandle</code> and
	 * <code>intoHandle</code>, with the effect of moving the handle from one node to the next.  
	 * @return Returns 0 if <code>ofHandle</code>
	 * has no first child, otherwise, the found node's integer node type is returned.
	 */
	public native int nodeFirstChildI(int ofHandle, int intoHandle);


	/**
	 * A handle to the next sibling of <code>ofHandle</code> is stored in
	 * <code>intoHandle</code>. 
	 * If there is no next sibling, <code>intoHandle</code> is not changed.
	 * The same handle may be used for
	 * both <code>ofHandle</code> and <code>intoHandle</code>, with the effect
	 * of moving the handle from one node to the next.  
	 * @return Returns <code>""</code> if <code>ofHandle</code>
	 * has no next sibling, otherwise, the found node's type is returned.
	 */
	public String nodeNextSibling(int ofHandle, int intoHandle) {
		return new String(nodeNextSiblingJNI(ofHandle, intoHandle));
	}
	private native byte [] nodeNextSiblingJNI(int current, int next);


	/**
	 * A handle to the next sibling of <code>ofHandle</code> is stored in
	 * <code>intoHandle</code>. 
	 * If there is no next sibling, <code>intoHandle</code> is not changed.
	 * The same handle may be used for
	 * both <code>ofHandle</code> and <code>intoHandle</code>, with the effect
	 * of moving the handle from one node to the next.  
	 * @return Returns 0 if <code>ofHandle</code>
	 * has no next sibling, otherwise, the found node's type is returned.
	 */
	public native int nodeNextSiblingI(int ofHandle, int intoHandle);


	/**
	 * A handle to the parent of <code>ofHandle</code> is stored in
	 * <code>intoHandle</code>. 
	 * If there is no parent, <code>intoHandle</code> is not changed.
	 * The same handle may be used for
	 * both <code>ofHandle</code> and <code>intoHandle</code>, with the effect
	 * of moving the handle from one node to the next.  
	 * @return Returns <code>""</code> if <code>ofHandle</code>
	 * has no parent, otherwise, the found node's type is returned.
	 */
	public String nodeParent(int ofHandle, int intoHandle) {
		return new String(nodeParentJNI(ofHandle, intoHandle));
	}
	private native byte [] nodeParentJNI(int child, int parent);


	/**
	 * A handle to the previous sibling of <code>ofHandle</code> is stored in
	 * <code>intoHandle</code>. 
	 * If there is no previous sibling, <code>intoHandle</code> is not changed.
	 * The same handle may be used for
	 * both <code>ofHandle</code> and <code>intoHandle</code>, with the effect
	 * of moving the handle from one node to the next.  
	 * @return Returns <code>""</code> if <code>ofHandle</code>
	 * has no previous sibling, otherwise, the found node's type is returned.
	 */
	public String nodePrevSibling(int ofHandle, int intoHandle) {
		return new String(nodePrevSiblingJNI(ofHandle, intoHandle));
	}
	private native byte [] nodePrevSiblingJNI(int current, int prev);


	/**
	 * A handle to the statement head node of <code>ofHandle</code> is stored in
	 * <code>intoHandle</code>. 
	 * If there is no statement head node, <code>intoHandle</code> is not changed.
	 * The same handle may be used for
	 * both <code>ofHandle</code> and <code>intoHandle</code>, with the effect
	 * of moving the handle from one node to the next.
	 * <br>&nbsp;&nbsp;&nbsp;
	 * A "statement head node" is a node with the attribute "statehead".
	 * See "statehead" in the Parameter Values Reference.
	 * <code>parserNodeStateHead</code> may be used iteratively to move up/out through
	 * nested blocks, all the way up to the outermost block.
	 * For example, given the program
	 * <code>DO: DISPLAY "Hi!". END.</code> and starting at the QSTRING "Hi!" node,
	 * the first call to <code>parserNodeStateHead</code> would find the DISPLAY node.
	 * The second call would find the DO node. A third call would not find another
	 * statement head - blank would be returned, and <code>intoHandle</code> would
	 * not be changed.
	 * @return Returns <code>""</code> if <code>ofHandle</code>
	 * has no statement head node, otherwise, the found node's type is returned.
	 */
	public String nodeStateHead(int ofHandle, int intoHandle) {
		return new String(nodeStateHeadJNI(ofHandle, intoHandle));
	}
	private native byte [] nodeStateHeadJNI(int child, int parent);


	/**
	 * Stores a reference to the topmost node in the node handle
	 * <code>intoHandle</code>.
	 * The topmost node is always a special node of type <code>Program_root</code>.
	 * Unlike most nodes, the <code>Program_root</code> node does not represent any
	 * actual token from your program's source code.
	 * @return Returns <code>"Program_root"</code> on success, or
	 * <code>""</code> on failure (i.e. if there is no syntax tree).
	 */
	public String nodeTop(int handle) {
		return new String(nodeTopJNI(handle));
	}
	private native byte [] nodeTopJNI(int handle);


	/**
	 * Parses the program file specified by <code>filename</code>.
	 * If the parse failed, there will not be any syntax tree available to work with.
	 * Your application is responsible for getting and dealing with the text of the
	 * error message.
	 * @param filename
	 * @return A positive integer on success, a negative integer if the parse failed.
	 */
	public int parse(String filename) {
		return parseJNI(filename.getBytes());
	}
	private native int parseJNI(byte [] filename);


	/**
	 * Causes Proparse to scan <code>filename</code> and create a scanner result set.
	 * @param parseType The only valid value for is <code>"scan"</code>".
	 * @param fileName The name of the source file to "scan".
	 * @return An integer "parse number", which is used as a handle to other
	 * functions like <code>parserParseGetTop</code> and <code>parserParseDelete</code>.
	 */
	public int parseCreate(String parseType, String fileName) {
		return parseCreateJNI(parseType.getBytes(), fileName.getBytes());
	}
	private native int parseCreateJNI(byte [] parseType, byte [] fileName);


	/**
	 * Deletes the parse number <code>parseNum</code> created by
	 * <code>parserParseCreate</code>.
	 * Also deletes all queries related to that scan result,
	 * as well as all handles to nodes in that scan result.
	 * @return Negative integer only if there was an error, 1 otherwise.
	 */
	public native int parseDelete(int parseNum);


	/**
	 * Finds the top (first) node from parse (or scan) result <code>parseNum</code>
	 * and causes handle <code>intoHandle</code> to refer to that node.
	 * @return Positive integer on success, negative integer on error.
	 */
	public native int parseGetTop(int parseNum, int nodeHandle);


	/**
	 * Clears the named query. Pass an empty string "" to clear all queries.
	 */
	public int queryClear(String queryName) {
		return queryClearJNI(queryName.getBytes());
	}
	private native int queryClearJNI(byte [] queryName);


	/**
	 * <br>&nbsp;&nbsp;&nbsp;
	 * Creates a named query. Any number of queries can be created, and referred to
	 * by name. The query starts at the <code>fromNode</code> node handle, evaluating
	 * that node and all of its child nodes. A list of node references is created within
	 * Proparse, one node reference for each node that is of the input
	 * <code>nodeType</code>. This function returns the number of results (the number
	 * of node references that were added to the list). You reference this result
	 * list with the function <code>parserQueryGetResult</code>.
	 * If a previous query of the same name exists, it is cleared out first.
	 * <br>&nbsp;&nbsp;&nbsp;
	 * You can pass an empty string "" as the node type to build an "unfiltered
	 * query". All nodes are added to the result set. This has the effect of
	 * flattening the tree down into a one dimensional vector of nodes. Operator
	 * nodes are placed in between their operands, so that this feature is especially
	 * useful for printing out code.
	 * <br>&nbsp;&nbsp;&nbsp;
	 * In order to make it possible to quickly find the first node of a given
	 * line number in a scan result, a built-in function was added,
	 * and it is accessed via the existing queries API. For example: <br>
	 * <code>
	 * parserQueryCreate(topNode, "myQuery", "first_where_line=" + STRING(myLine)).
	 * </code><br>
	 * ...where topNode is the first node in a scan result,
	 * and myLine is an integer line number.
	 * Queries with option <code>first_where_line=</code> are only sensible and only
	 * supported for scanner results.
	 * @param theNode The topmost node to start from.
	 * @param theQueryName Your unique name for the query.
	 * @param theType The type of node to search for,
	 * or "" for an "unfiltered" query (all nodes, flattened).
	 * @return The number of results (the number
	 * of node references that were added to the list).
	 */
	public int queryCreate(int theNode, String theQueryName, String theType) {
		return queryCreateJNI(theNode, theQueryName.getBytes(), theType.getBytes());
	}
	private native int queryCreateJNI(int theNode, byte [] theQueryName, byte [] theType);


	/**
	 * Sets the handle <code>intoHandle</code> to point to the node referred to
	 * by result number <code>resultNum</code> in the query <code>queryName</code>.
	 * @param theQueryName The name of a
	 * query created with the function <code>parserQueryCreate</code>.
	 * @param resultNum An integer between one and the number of results for that query.
	 * @param intoHandle A valid node handle, created with the function
	 * <code>parserGetHandle</code>.
	 * @return 1 on success, negative integer on failure.
	 */
	public int queryGetResult(String theQueryName, int resultNum, int intoHandle) {
		return queryGetResultJNI(theQueryName.getBytes(), resultNum, intoHandle);
	}
	private native int queryGetResultJNI(byte [] theQueryName, int resultNum, int intoNode);


	/**
	 * Releases a node handle for re-use.
	 * See Node Handles / Releasing Node Handles in the User Guide. 
	 */
	public native int releaseHandle(int theHandle);


	/**
	 * Adds a name to Proparse's list of database names.
	 */
	public int schemaAddDb(String name) {
		return schemaAddDbJNI(name.getBytes());
	}
	private native int schemaAddDbJNI(byte [] name);


	/**
	 * Adds a table name to the last added database name.
	 */
	public int schemaAddTable(String name) {
		return schemaAddTableJNI(name.getBytes());
	}
	private native int schemaAddTableJNI(byte [] name);


	/**
	 * If your program requires a database alias to exist before it will compile,
	 * then you must use this function to tell Proparse about your database
	 * alias before that program can be parsed. If the alias already exists, then
	 * it is replaced with the new value.
	 */
	public int schemaAliasCreate(String aliasname, String dbname) {
		return schemaAliasCreateJNI(aliasname.getBytes(), dbname.getBytes());
	}
	private native int schemaAliasCreateJNI(byte [] aliasname, byte [] dbname);


	/**
	 * Use this function to delete an alias that you created with
	 * <code>parserSchemaAliasCreate</code>.
	 * To delete all aliases, use an empty string as this function's argument (i.e.
	 * <code>parserSchemaAliasDelete("")</code> ).
	 */
	public int schemaAliasDelete(String aliasname) {
		return schemaAliasDeleteJNI(aliasname.getBytes());
	}
	private native int schemaAliasDeleteJNI(byte [] aliasname);


	/**
	 * Use this function if you want to clear the old schema names out of Proparse
	 * and load a new list of schema names into Proparse. For example, you might
	 * use this after making schema changes or after changing from one working
	 * environment to another.
	 */
	public native int schemaClear();


	/**
	 * Not used.
	 */
	public native int schemaOldLoad();


	/**
	 * Sets the <code>childHandle</code> node to be the first child node
	 * of the <code>parentHandle</code>, displacing the former
	 * first child node. Returns true on success.
	 * Use 0 for <code>childHandle</code> to indicate that there are no children.
	 * This function doesn't change the relationships between the child
	 * node and its own children, its siblings, or its
	 * hidden nodes.
	 * See also Tree Manipulation and Code Refactoring
	 * subsection Moving Nodes in the User Guide.
	 */
	public native int setNodeFirstChild(int parentHandle, int childHandle);


	/**
	 * Sets the <code>sibHandle</code> node to be the next sibling node 
	 * of the <code>ofHandle</code>, displacing the former next sibling node.
	 * Use 0 for <code>sibHandle</code> to indicate that there is no next sibling.
	 * This function doesn't change the relationships between the sibling 
	 * node and its own children, its own siblings, or its 
	 * hidden nodes. 
	 * See also Tree Manipulation and Code Refactoring
	 * subsection Moving Nodes in the User Guide.
	 */
	public native int setNodeNextSibling(int ofHandle, int sibHandle);


	/**
	 * Sets the <code>ofHandle</code> node's text in the tree to the <code>newText</code>. 
	 * For example, if the node type is "SUBSTRING", the 
	 * original source code that the node is related to may have been "substr". 
	 * Use of this function allows the value in the original source code 
	 * to be modified (when Proparse's tree is written out), for example to "SUBSTR".
	 */
	public int setNodeText(int theNode, String theText) {
		return setNodeTextJNI(theNode, theText.getBytes());
	}
	private native int setNodeTextJNI(int theNode, byte [] theText);


	/**
	 * Sets the node's type in the tree to <code>theType</code>. 
	 * For example, if the node type is "FIRST", 
	 * the node type may be changed to "LAST".
	 * See also Tree Manipulation and Code Refactoring in the User Guide.
	 */
	public int setNodeType(int theNode, String theType) {
		return setNodeTypeJNI(theNode, theType.getBytes());
	}
	private native int setNodeTypeJNI(int theNode, byte [] theType);


	/**
	 * Sets the node's type.
	 */
	public native int setNodeTypeI(int theNode, int theType);


	/**
	 * Given an input node handle, <code>parserWriteNode</code> 
	 * writes the text from that node and all of its
	 * child nodes, out to the text file <code>filename</code>.
	 * <br>&nbsp;&nbsp;&nbsp;
	 * <code>parserWriteNode</code> takes care of ensuring that operators
	 * are written out with infix notation
	 * rather than the operator prefix notation that you would normally see when
	 * walking a tree. (i.e.: displays: 1 + 2 instead of: + 1 2).
	 * See Syntax Trees for more
	 * information about operator prefix notation from syntax trees.
	 * <br>&nbsp;&nbsp;&nbsp;
	 * Also, <code>parserWriteNode</code> writes code that looks similar to the
	 * original source code by displaying the whitespace (hidden token) prior to
	 * each node. See Hidden Tokens.
	 * <br>This function is mostly used by Joanju as a very fast function
	 * to write out the results of
	 * a parse to a text file, which can then be compared to the output of
	 * a COMPILE...PREPROCESS. This allows us to test that programs are parsing
	 * correctly.
	 * <br>&nbsp;&nbsp;&nbsp;
	 * Additionally, <code>parserWriteNode</code> is a handy way to write out
	 * nodes found in ad hoc queries.
	 * For example, see examples/query_length.p.  
	 */
	public int writeNode(int theNode, String filename) {
		return writeNodeJNI(theNode, filename.getBytes());
	}
	private native int writeNodeJNI(int theNode, byte [] filename);


}
