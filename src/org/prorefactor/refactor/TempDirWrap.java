/**
 * TempDirWrap.java
 * @author John Green
 * 25-Oct-2003
 * www.joanju.com
 * 
 * Copyright (c) 2003-2004 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */
package org.prorefactor.refactor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import com.joanju.ProparseLdr;


/**
 * Generic wrapper for refactorings which are non-interactive and
 * write their output to an empty output directory.
 */
public class TempDirWrap {

	public TempDirWrap(String outputDir) {
		this.outputDir = outputDir;
	}

	private int scanNum = 0;
	private int scanTopNode = 0;
	private HashSet badFileSet = new HashSet();
	private ProparseLdr parser = ProparseLdr.getInstance();
	private RefactorSession refpack = RefactorSession.getInstance();
	private String outputDir;
	private StringBuilder messages = new StringBuilder();



	// Cleanup scan results as well as scan handles
	private void cleanupScanner() {
		if (scanTopNode > 0) {
			parser.releaseHandle(scanTopNode);
			scanTopNode = 0;
		}
		if (scanNum > 0) {
			parser.parseDelete(scanNum);
			scanNum = 0;
		}
	} // cleanupScanner()



	/** Append a message and return the value from logMessages().
	 * @see #logMessages()
	 */
	private String logMessage(String newMessage) throws IOException {
		messages.append(newMessage);
		messages.append(FileStuff.LINESEP);
		return logMessages();
	}



	/** Log the messages, return the messages in a String,
	 * which is appended with a note about where messages were logged to.
	 */
	private String logMessages() throws IOException {
		if (messages.length()==0) return null;
		File outfile = new File(RefactorSession.getMessagesFileName());
		outfile.getParentFile().mkdirs();
		outfile.createNewFile();
		FileWriter writer = new FileWriter(outfile, true);
		writer.write(messages.toString());
		writer.close();
		return messages.toString()
			+ "Messages logged to " + RefactorSession.getMessagesFileName() + " in your working directory.";
	}



	/** Run the refactor for a given root node.
	 * Resets "messages" on each call.
	 * @param topnode The handle to the syntax tree top node.
	 * @return Empty string on success, otherwise an error message.
	 */
	public String run(int topNode, ILint lint, IRefactor refactor) throws IOException {
		messages = new StringBuilder();
		String errString = lint.run(topNode);
		if (errString!=null && errString.length()!=0) return logMessage(errString);
		TreeSet targetSet = lint.getTargetSet();
		return run(targetSet, refactor);
	}
	
	
	
	/** Run the refactor for a given change set.
	 * Does not reset "messages", so a new TempDirWrap object should be used
	 * for each compile unit.
	 * @param targetSet The set of RefactorTarget objects.
	 * @param refactor An IRefactor object which will process the targetSet.
	 */
	public String run(TreeSet targetSet, IRefactor refactor) throws IOException {
		try {
			if (targetSet.size() == 0) return "";
			String currSource = "";
			boolean writeIt = false;
			for (Iterator it = targetSet.iterator(); it.hasNext(); ) {
				RefactorTarget target = (RefactorTarget) it.next();
				if (! target.filename.equals(currSource)) {
					if (writeIt) {
						writeTargetSource(scanNum, currSource);
						writeIt = false;
					} 
					cleanupScanner();
					currSource = target.filename;
					scanNum = parser.parseCreate("scan", target.filename);
				}
				// Some refactoring targets just don't "happen", for whatever reason.
				// Often, because preprocessing gets in the way.
				if (refactor.run(target, scanNum) == 1) writeIt = true;
			}
			if (writeIt) writeTargetSource(scanNum, currSource);
			return logMessages();
		} finally {
			cleanupScanner();
		}
	}



	// Write the scanner countents out to the target file.
	private void writeTargetSource(int scanNum, String sourceFile) throws IOException {
		if (scanNum < 1) return;
		File existingCopy = null;
		String targetName = FileStuff.prepareTarget(outputDir, sourceFile);
		File outFile = new File(targetName);
		if (badFileSet.contains(outFile)) return;
		if (outFile.exists()) {
			existingCopy = File.createTempFile("tdwrap", null, refpack.getTempDir());
			org.prorefactor.core.Util.fileCopy(targetName, existingCopy.getCanonicalPath());
		}
		scanTopNode = parser.getHandle();
		parser.parseGetTop(scanNum, scanTopNode);
		parser.writeNode(scanTopNode, targetName);
		parser.releaseHandle(scanTopNode);
		if (existingCopy!=null) {
			boolean different =
				parser.diff(targetName, existingCopy.getCanonicalPath()).length() != 0;
			existingCopy.delete();
			if (different) {
				messages.append(
					"(" + new Date(System.currentTimeMillis()) + ") "
					+ "Include file refactoring has multiple outcomes, file not written: "
					+ sourceFile
					+ FileStuff.LINESEP
					);
				badFileSet.add(outFile);
				outFile.delete();
			}
		}
	}



}
