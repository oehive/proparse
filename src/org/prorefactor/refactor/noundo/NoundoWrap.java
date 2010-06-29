///**
// * TempDirWrap.java
// * @author John Green
// * 17-Jul-2003
// * www.joanju.com
// * 
// * Copyright (c) 2003 Joanju Limited.
// * All rights reserved. This program and the accompanying materials 
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// * 
// */
//
//package org.prorefactor.refactor.noundo;
//
//import java.io.File;
//import java.util.Iterator;
//
//import org.prorefactor.core.JPNode;
//import org.prorefactor.refactor.FileStuff;
//import org.prorefactor.treeparser.TreeParserWrapper;
//import org.prorefactor.treeparser01.TreeParser01;
//import org.prorefactor.treeparser03.TP03Support;
//import org.prorefactor.treeparser03.TreeParser03;
//
//import com.joanju.ProparseLdr;
//
//
///**
// * Wraps the various NO-UNDO refactor pieces into a single function call.
// */
//public class NoundoWrap {
//
//	/** Overwrite files that already exist? Default is "true".
//	 */
//	public boolean overwrite = true;
//
//
//	/**
//	 * Constructor requires the name of the output directory.
//	 */
//	public NoundoWrap(String outputDir) {
//		this.outputDir = outputDir;
//	}
//
//
//	private String outputDir;
//	private ProparseLdr parser = ProparseLdr.getInstance();
//	private int scanNum = 0;
//	private int scanTopNode = 0;
//
//
//	/**
//	 * Run the NO-UNDO refactor.
//	 * @param filename The compile unit to parse and refactor all parts of.
//	 * @return Empty string on success, otherwise an error message.
//	 */
//	public String run(String filename) {
//		parser.parse(filename);
//		if (parser.errorGetStatus() != 0) return parser.errorGetText();
//		int topNode = parser.getHandle();
//		parser.nodeTop(topNode);
//		return run(topNode);
//	}
//
//
//	/**
//	 * Run the NO-UNDO refactor.
//	 * @param topnode The handle to the syntax tree top node.
//	 * @return Empty string on success, otherwise an error message.
//	 */
//	public String run(int topHandle) {
//
//		String tempString;
//		TP03Support tps = new TP03Support();
//
//		try {
//			JPNode topNode = JPNode.getTree(topHandle);
//			TreeParser01 tp01 = new TreeParser01();
//			tempString = TreeParserWrapper.run(tp01, topNode);
//			if (tempString.length()!=0) return tempString;
//			TreeParser03 tp = new TreeParser03();
//			tp.setSupport(tps);
//			tempString = TreeParserWrapper.run(tp, topNode);
//			if (tempString.length()!=0) return tempString;
//			if (tps.targetSet.size() == 0) return "";
//
//			String currSource = "";
//			boolean writeIt = false;
//			lint_results_loop:
//			for (Iterator it = tps.targetSet.iterator(); it.hasNext(); ) {
//				Object tempTar = it.next();
//				if (!(tempTar instanceof NoundoTarget))
//					continue;
//				NoundoTarget target = (NoundoTarget) tempTar;
//				if (! target.filename.equals(currSource)) {
//					if (writeIt) writeTargetSource(scanNum, currSource);
//					writeIt = false;
//					cleanupScanner();
//					currSource = target.filename;
//					scanNum = parser.parseCreate("scan", target.filename);
//				}
//				NoundoRefactor refactor = new NoundoRefactor();
//				int retVal = refactor.run(target, scanNum);
//				if (retVal==1) writeIt = true;
//			} // lint_results_loop
//			if (writeIt) writeTargetSource(scanNum, currSource);
//	
//			return "";
//		
//		} finally {
//			tps.cleanUp();
//			cleanupScanner();
//		}
//
//	} // run()
//
//
//
//	// Cleanup scan results as well as scan handles
//	private void cleanupScanner() {
//		if (scanTopNode > 0) {
//			parser.releaseHandle(scanTopNode);
//			scanTopNode = 0;
//		}
//		if (scanNum > 0) {
//			parser.parseDelete(scanNum);
//			scanNum = 0;
//		}
//	} // cleanupScanner()
//
//
//
//	// Write the scanner countents out to the target file.
//	private void writeTargetSource(int scanNum, String sourceFile) {
//		if (scanNum < 1) return;
//		String targetFile = FileStuff.prepareTarget(outputDir, sourceFile);
//		if (!overwrite) {
//			File outFile = new File(targetFile);
//			if (outFile.exists()) return;
//		}
//		scanTopNode = parser.getHandle();
//		parser.parseGetTop(scanNum, scanTopNode);
//		parser.writeNode(scanTopNode, targetFile);
//		parser.releaseHandle(scanTopNode);
//	}
//
//
//
//} // class TempDirWrap
//
