/* April 2007 by John Green.
 *
 * Copyright (C) 2007 Joanju Software
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor.action;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.prorefactor.core.JPNode;
import org.prorefactor.core.Pstring;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.macrolevel.IncludeRef;
import org.prorefactor.macrolevel.MacroRef;
import org.prorefactor.refactor.FileStuff;
import org.prorefactor.refactor.FileTarget;
import org.prorefactor.refactor.FileTargetComparator;
import org.prorefactor.refactor.RefactorException;
import org.prorefactor.treeparser.ParseUnit;


/** Change file references (run and include) for an input from/to name mapping.
 * <p>
 * Input names map is a string of comma separated old/new name pairs, those separated by newlines.
 * </p><p>
 * Enhancements needed: Add support for quoted filenames with embedded spaces in the regular expression
 * search and replace part of this.
 * </p>
 */
public class RenameFilesRefactor {

 	/** See the class notes for details about the format of the input names map. */
	public RenameFilesRefactor(String namesMap) throws IOException {
		dbInit();
		logFile = File.createTempFile("prorefactor", "tmp");
		logFile.deleteOnExit();
		logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
		logWriter.println(new Date());
		buildNamesMap(namesMap);
	}

	private BTree targetTable;
	private File dbDir;
	private File logFile;
	private HashMap<String, String> namesMap = new HashMap<String, String>();
	private HashMap<String, String> canonicalMap = new HashMap<String, String>();
	PreparedStatement insertStatement = null;
	PreparedStatement lookupStatement = null;
	private PrintWriter logWriter;
	private RecordManager recordManager;
	private String [] canonicalIncludesIndex;

	
	
	
	private void buildCanonicalIndex(ParseUnit pu) throws IOException {
		String [] fileIndex = pu.getFileIndex();
		canonicalIncludesIndex = new String[fileIndex.length];
		for (int i = 0; i < fileIndex.length; i++) {
			String canonical = new File(fileIndex[i]).getCanonicalPath();
			canonicalIncludesIndex[i] = canonical;
		}
	}
	
	
	private void buildNamesMap(String namesList) throws IOException {
		String [] lines = namesList.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String [] pair = lines[i].split(",");
			if (pair.length != 2)
				continue;
			String fromName = normalize(pair[0]);
			namesMap.put(fromName, pair[1].trim());
			File foundFile = FileStuff.findFile(fromName);
			if (foundFile==null)
				logWriter.println("INPUT_NAMES_LIST Not found on PROPATH: " + fromName);
			else
				canonicalMap.put(foundFile.getCanonicalPath(), pair[1].trim());
		}
	}
	
	
	/** Release temp files and other resources. */
	public void close() throws IOException {
		dbClose();
		logWriter.println("End of logfile");
		logWriter.flush();
		logWriter.close();
	}


	private void dbClose() throws IOException {
		if (recordManager!=null)
			recordManager.close();
		if (dbDir!=null && dbDir.exists())
			FileUtils.deleteDirectory(dbDir);
	}
	
	
	private void dbInit() throws IOException {
		dbDir = File.createTempFile("prorefactor", null);
		dbDir.delete();
		dbDir.mkdir();
		String dbname = dbDir.getPath() + "/renamefiles";
		Properties props = new Properties();
        props.put(jdbm.RecordManagerOptions.CACHE_SIZE, "1000" );
		recordManager = jdbm.RecordManagerFactory.createRecordManager(dbname, props);
		targetTable = BTree.createInstance(recordManager, new FileTargetComparator());
		recordManager.commit();
	}
	
	
	/** Examine one parse unit, keeping a list of file changes to make. */
	public int examine(ParseUnit pu) throws RefactorException, IOException {
		buildCanonicalIndex(pu);
		walkTree(pu.getTopNode());
		processIncludeRefs(pu);
		int numEntries = targetTable.size();
		return numEntries;
	}
	
	
	/** Call close(), to flush the log file buffer, before looking at this file's contents. */
	public File getLogFile() {return logFile;}
	
	
	/** Trim, lowercase, backslashes to forward, dequote. */
	private String normalize(String orig) {
		return Pstring.dequote(orig.trim().toLowerCase().replace('\\', '/'));
	}
	
	
	private void processIncludeRefs(ParseUnit pu) throws RefactorException, IOException {
		HashMap<Integer, String> matches = new HashMap<Integer, String>();
		for (int i = 0; i < canonicalIncludesIndex.length; i++) {
			if (canonicalMap.containsKey(canonicalIncludesIndex[i]))
				matches.put(i, canonicalIncludesIndex[i]);
		}
		if (matches.size()==0)
			return;
		MacroRef [] macroRefs = pu.getMacroSourceArray();
		for (int i = 0; i < macroRefs.length; i++) {
			MacroRef macroRef = macroRefs[i];
			if (macroRef instanceof IncludeRef) {
				IncludeRef includeRef = (IncludeRef) macroRef;
				if (matches.containsKey(includeRef.getFileIndex())) {
					// We're not sure what form the file reference was, might have been:
					//   dir/file, or dir\file, or ./dir\file, or whatever
					// So we leave it to the filechanger to scan the include reference for the
					// filename, and look it up in the to/from namesMap.
					targetTable.insert(
							new FileTarget(
									canonicalIncludesIndex[includeRef.parent.getFileIndex()]
									, includeRef.refLine
									, includeRef.refColumn
									)
							, new RenameFilesRecord(null, null, RenameFilesRecord.INCLUDE_REF)
							, false
							);
					recordManager.commit();
				}
			}
		}
	}


	private void processRunNode(JPNode runNode) throws IOException {
		JPNode firstChild = runNode.firstChild();
		String runText;
		boolean isRunValue = firstChild.getType() == TokenTypes.VALUE;
		if (isRunValue) {
			// We want expression from #(VALUE LEFTPAREN expression ...
			// Watch for RUN VALUE("hard/coded/filename"). Why people do this is beyond me.
			JPNode expressionNode = firstChild.firstChild().nextSibling();
			if (expressionNode.getType() == TokenTypes.QSTRING) {
				runText = expressionNode.getText();
			} else {
				// It's a VALUE(expression) that we don't process.
				return;
			}
		} else {
			runText = firstChild.getText();
		}
		String newRefName = namesMap.get(normalize(runText));
		if (newRefName==null) {
			File foundFile = FileStuff.findFile(Pstring.dequote(runText));
			if (foundFile!=null)
				newRefName = canonicalMap.get(foundFile.getCanonicalPath());
		}
		if (newRefName!=null) {
			FileTarget target = new FileTarget(
					firstChild.getFilename()
					, firstChild.getLine()
					, firstChild.getColumn()
					);
			if (targetTable.find(target) == null) {
				RenameFilesRecord record = 
					new RenameFilesRecord(runText, newRefName, RenameFilesRecord.RUN_SIMPLE);
				if (isRunValue)
					record.type = RenameFilesRecord.RUN_VALUE;
				targetTable.insert(target, record, true);
				recordManager.commit();
			}
		}
	}
	
	
	private void walkTree(JPNode node) throws IOException {
		if (node==null) return;
		if (	node.getType()==TokenTypes.RUN
			&&	node.isStateHead()
			&&	node.getState2()==0
			) {
			processRunNode(node);
		}
		walkTree(node.firstChild());
		walkTree(node.nextSibling());
	}
	
	
	public void writeChanges(String outDir) throws IOException {
		// Process the file list backwards. That way, later lines will get processed before
		// earlier lines in the same file, and save us from the potential of messing up
		// line/column/offset numbers for subsequent changes.
		TupleBrowser browser = targetTable.browse(null);
		Tuple tuple = new Tuple();
		String prevFilename = "";
		String textString = null;
		StringBuilder textBuilder = null;
		while (browser.getPrevious(tuple)) {
			FileTarget target = (FileTarget) tuple.getKey();
			RenameFilesRecord record = (RenameFilesRecord) tuple.getValue();
			if (! prevFilename.equals(target.filename)) {
				if (textBuilder!=null) {
					String targetName = FileStuff.prepareTarget(outDir, prevFilename);
					FileUtils.writeStringToFile(new File(targetName), textBuilder.toString(), null);
				}
				prevFilename = target.filename;
				textString = FileUtils.readFileToString(new File(target.filename), null);
				textBuilder = new StringBuilder(textString);
			}
			int offset = StringUtils.ordinalIndexOf(textString, "\n", target.line-1) + target.column;
			switch (record.type) {
			case RenameFilesRecord.INCLUDE_REF:
				writeChangeToIncludeRef(target, record, textBuilder, offset);
				break;
			case RenameFilesRecord.RUN_SIMPLE:
				writeChangeToRunSimple(target, record, textBuilder, offset);
				break;
			case RenameFilesRecord.RUN_VALUE:
				writeChangeToRunValue(target, record, textBuilder, offset);
				break;
			}
		}
	}
	
	
	private void writeChangeToIncludeRef(FileTarget target, RenameFilesRecord record
			, StringBuilder text, int offset) throws IOException {
		Matcher matcher = Pattern.compile("^(\\{\\s*)([^\\s\\}]*)").matcher(text);
		matcher.region(offset, text.length());
		String replacement = null;
		if (matcher.lookingAt()) {
			File foundFile = FileStuff.findFile(Pstring.dequote(matcher.group(2)));
			if (foundFile!=null)
				replacement = canonicalMap.get(foundFile.getCanonicalPath());
		}
		if (replacement==null) {
			writeMatchFailure(target, text, offset);
			return;
		}
		text.replace(matcher.start(), matcher.end(), "{" + replacement);
	}


	private void writeChangeToRunSimple(FileTarget target, RenameFilesRecord record
			, StringBuilder text, int offset) {
		int replaceLength = record.from.length();
		if (! text.substring(offset, offset+replaceLength).equals(record.from)) {
			// This would happen if the program name is derived from a macro.
			writeMatchFailure(target, text, offset);
			return;
		}
		text.replace(offset, offset+replaceLength, record.to);
	}


	/** Replace the entire silly VALUE("hard/coded/string") */
	private void writeChangeToRunValue(FileTarget target, RenameFilesRecord record
			, StringBuilder text, int offset) {
		Matcher matcher = Pattern.compile("(?i)^value\\s*\\(\\s*([^\\s\\)]*)\\s*\\)").matcher(text);
		matcher.region(offset, text.length());
		if (	! matcher.lookingAt()
			||	! record.from.equals(matcher.group(1))
			) {
			// This would happen if any of VALUE("stringliteral") is derived from a macro.
			writeMatchFailure(target, text, offset);
			return;
		}
		text.replace(matcher.start(), matcher.end(), record.to);
	}


	private void writeMatchFailure(FileTarget target, StringBuilder text, int offset) {
		int snippetEnd = Math.min(offset+16, text.length());
		snippetEnd = Math.min(snippetEnd, text.indexOf("\n", offset)-2);
		String snippet = text.substring(offset, snippetEnd) + "...";
		logWriter.println(
				"MATCH_FAIL line:" 
				+ target.line + " col:" + target.column
				+ " " + snippet
				+ " " + target.filename
				);
	}


	public void zzDebugDbContents() throws IOException {
		TupleBrowser browser = targetTable.browse();
		Tuple target = new Tuple();
		while (browser.getNext(target)) {
			System.out.println(target.getKey().toString() + target.getValue().toString());
		}
	}
	

}
