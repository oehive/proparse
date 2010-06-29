/**
 * SubstituteWrap.java
 * @author John Green
 * 29-Oct-2002
 * www.joanju.com
 * 
 * Copyright (c) 2002 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.substitute;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import org.prorefactor.refactor.ReviewChangesDialog;

import com.joanju.ProparseLdr;



/**
 * Wraps the SUBSTITUTE lint/refactor/review trio into
 * a single subroutine.
 */
public class SubstituteWrap {


	static ProparseLdr parser = null;


	/**
	 * Run the SUBSTITUTE lint/refactor/review trio.
	 * @param topNode The integer handle to the top node in the AST
	 * to perform the refactoring on.
	 * @return Empty string on successful completion, "cancel" if the user
	 * chose the cancel button, or else the text of an error message.
	 */
	public static String run(int topNode) {

		parser = ProparseLdr.getInstance();

		// lint
		SubstituteLint lint = new SubstituteLint();
		TreeSet targetSet = lint.run(topNode);

		try {

			if (targetSet.size() == 0)
				return "";
	
			// Iterate through the target set
			lint_results_loop:
			for (Iterator it = targetSet.iterator(); it.hasNext(); ) {
	
				SubstituteTarget target = (SubstituteTarget) it.next();
	
				// If there's less than two translatable strings, we don't refactor.
				if (target.numTranslatable < 2)
					continue lint_results_loop;
	
				String theReturn = processTarget(target);
				if (theReturn.length() > 0)
					return theReturn;
		
			} // lint_results_loop
	
			return "";
		
		} finally {
			// We are responsible for cleaning up handles in the resultSet.
			for (Iterator it = targetSet.iterator(); it.hasNext(); ) {
				SubstituteTarget target = (SubstituteTarget) it.next();
				parser.releaseHandle(target.nodeHandle);
			}
		}

	} // run()



	/**
	 * Process a single SubstituteTarget
	 * @param target The SubstituteTarget object to process
	 * @return Empty string on successful completion, "cancel" if the user
	 * chose the cancel button, or else the text of an error message.
	 */
	public static String processTarget(SubstituteTarget target) {

		int scanNum = 0;
		int scanTopNode = 0;
		File tempFile = null;

		if (parser==null) parser = ProparseLdr.getInstance();

		try {

			// Can we auto-transform?
			boolean doAutoTransform = true;
			if (target.quoteTypeMismatch > 0  ||  target.attributesMismatch > 0)
				doAutoTransform = false;

			// Get the temp filename
			tempFile = File.createTempFile("joanju", ".tmp");
			String tempFilename = tempFile.getAbsolutePath();

			// refactor
			int refactorResult = 0;
			SubstituteRefactor refactor = null;
			if (doAutoTransform) {

				// cleanup from previous loop
				if (scanTopNode > 0)
					parser.releaseHandle(scanTopNode);
				if (scanNum > 0)
					parser.parseDelete(scanNum);

				scanNum = parser.parseCreate("scan", target.filename);
				refactor = new SubstituteRefactor();
				refactorResult = refactor.run(target, scanNum);
				if (refactorResult == 1) {
					scanTopNode = parser.getHandle();
					parser.parseGetTop(scanNum, scanTopNode);
					parser.writeNode(scanTopNode, tempFilename);
				} else {
					doAutoTransform = false;
				}
			}

			// If we didn't auto-transform, then we'll just work from a copy of the original.
			if (! doAutoTransform)
				org.prorefactor.core.Util.fileCopy(target.filename, tempFilename);

			// Review the changes
			// setChangedLines must be called before open();
			ReviewChangesDialog reviewDialog = new ReviewChangesDialog(target.filename, tempFilename);
			reviewDialog.setChangedLines(target.changedLines);
			reviewDialog.open();
			if (refactorResult == -1)
				reviewDialog.showMessage("Unable to synchronize AST with source code - preprocessing or escape sequences?");
			if (target.quoteTypeMismatch > 0)
				reviewDialog.showMessage("Unable to auto-refactor due to different quote types used in strings.");
			if (target.attributesMismatch > 0)
				reviewDialog.showMessage("Unable to auto-refactor due to different attributes on translatable strings.");
			int theReturn = reviewDialog.getUserInput();

			if (theReturn == -1)
				return "cancel";
			return "";

		} catch (IOException e) {
			return e.getMessage();
		} finally {
			if (scanTopNode > 0)
				parser.releaseHandle(scanTopNode);
			if (scanNum > 0)
				parser.parseDelete(scanNum);
			if (tempFile != null)
				tempFile.delete();
		}

	} // processTarget()



} // class SubstituteWrap

