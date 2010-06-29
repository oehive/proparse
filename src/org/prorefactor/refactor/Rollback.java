/* Rollback.java
 * Created on Dec 16, 2003
 * John Green
 *
 * Copyright (C) 2003 Joanju Limited
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.prorefactor.refactor;

import com.joanju.ProparseLdr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.prorefactor.core.ICallback;


/** A refactoring "roll back" restores files which were preserved
 * before a refactoring was run.
 */
public class Rollback {
	
	/** We only support one Rollback at a time (no real plans
	 * to support multiple rollbacks either.)
	 * Creating a new Rollback wipes out the previous one.
	 * If you want to find the current Rollback, use
	 * getCurrent().
	 */
	public Rollback() {
		try {
			// Ensure this rollback's directory is clear.
			this.clearRollback();
			// Redundant, but if we ever have seperate rollback directories for each
			// rollback, then this will be necessary.
			if (currentRollback!=null) currentRollback.clearRollback();
		} catch (IOException e) {}
		currentRollback = this;
	}

	/** This callback allows an external package to be notivied
	 * when an existing file has been preserved before rollback,
	 * in other words, it's about to be modified or deleted.
	 */
	public static ICallback externPreModify = null;

	/** For now, we really only support one Rollback at a time. */
	private static Rollback currentRollback = null;

	private ArrayList fileChanges = new ArrayList(); 



	/** Clears the list of rollback changes, clears the rollback directory.
	 * Calling this is not necessary - just crate a new Rollback. That will
	 * cause this to be called.
	 * @throws IOException
	 */
	public void clearRollback() throws IOException {
		try {
			org.prorefactor.core.Util.wipeDirectory(RefactorSession.getInstance().getRollbackDir(), true);
		} catch (IOException e) {
			throw new IOException("Error clearing the rollback directory: " + e.getMessage());
		}
	}



	/** Get a String description of the file changes.
	 */
	public String getChangeList() {
		String ret = "";
		for (Iterator it = fileChanges.iterator(); it.hasNext(); ) {
			try {
				FileChange change = (FileChange)it.next();
				String fullpath = change.sourceFile.getCanonicalPath();
				ret +=
					FileChange.whatLabel[change.whatHappened]
					+ " "
					+ fullpath
					+ "\n"
					;
			} catch (Exception e) {}
		}
		return ret;
	} // getChangeList



	/** Get the current rollback.
	 * @return null if no rollback exists for the session.
	 */
	public static Rollback getCurrent() {
		return currentRollback;
	}



	/** Returns the list of FileChange objects which was added to during
	 * the last refactoring.
	 */
	public ArrayList getFileChanges() {
		return fileChanges;
	}



	/** Preserve (copy) a file to the "rollback" directory.
	 * Does not re-copy a file that has already been preserved.
	 * @param refname The name that the file was referred to with.
	 * @param fullpath The full path to the file.
	 * @throws IOException
	 */
	public void preserve(String refname, String fullpath) throws IOException {
		String preserveName = FileStuff.prepareTarget(RefactorSession.getRollbackDirName(), fullpath);
		File sourceFile = new File(fullpath);
		File savedFile = new File(preserveName);
		if (! sourceFile.exists()) return;
		if (savedFile.exists()) return;
		if (externPreModify!=null) externPreModify.run(fullpath);
		org.prorefactor.core.Util.fileCopy(fullpath, preserveName);
		FileChange change = new FileChange(FileChange.MODIFIED, sourceFile, savedFile, refname);
		fileChanges.add(change);
	} // preserve



	/** Preserve (move) a file to the "rollback" directory.
	 * Does not re-move a file that has already been preserved.
	 * @param refname The name that the file was referred to with.
	 * @param fullpath The full path to the file.
	 * @throws IOException
	 */
	public void preserveMove(String refname, String fullpath) {
		String preserveName = FileStuff.prepareTarget(RefactorSession.getRollbackDirName(), fullpath);
		File sourceFile = new File(fullpath);
		File savedFile = new File(preserveName);
		if (! sourceFile.exists()) return;
		if (savedFile.exists()) return;
		if (externPreModify!=null) externPreModify.run(fullpath);
		sourceFile.renameTo(savedFile);
		FileChange change = new FileChange(FileChange.MODIFIED, sourceFile, savedFile, refname);
		fileChanges.add(change);
	} // preserveMove



	/** Preserve a file in the "rollback" directory, and then write to it.
	 * Does not change the original file if there were no differences between old & new.
	 * Also use this to write a new file - nothing to rollback, but a record of the
	 * newly created file will be kept.
	 * If the file has already been preserved, it is not re-copied.
	 * @param handle Handle to the top/first node/token.
	 * @param refname The name that was used for referring to this file.
	 * @param fullpath The full path to the file.
	 * @throws IOException
	 */
	public void preserveAndWrite(
			int handle, String refname, String fullpath
			) throws IOException {
		String preserveName = FileStuff.prepareTarget(RefactorSession.getRollbackDirName(), fullpath);
		File sourceFile = new File(fullpath);
		File savedFile = new File(preserveName);
		ProparseLdr parser = ProparseLdr.getInstance();
		
		// If this file has already been saved once, then we just do the
		// write and we are finished.
		if (savedFile.exists()) {
			if (parser.writeNode(handle, fullpath) < 1) {
				throw new IOException(parser.errorGetText());
			}
			return;
		}

		boolean newFile = true;
		if (sourceFile.exists()) {
			newFile = false;
			if (externPreModify!=null) externPreModify.run(fullpath);
			sourceFile.renameTo(savedFile);
		}
		if (parser.writeNode(handle, fullpath) < 1) {
			savedFile.renameTo(sourceFile);
			throw new IOException(parser.errorGetText());
		}
		if (! newFile) {
			if (parser.diff(fullpath, preserveName).equals("")) {
				// No change to the file. Move the preserved file back.
				savedFile.renameTo(sourceFile);
				return;
			}
		}
		FileChange change = new FileChange(
			newFile ? FileChange.NEW : FileChange.MODIFIED
			, sourceFile
			, savedFile
			, refname
			);
		fileChanges.add(change);
	} // preserveAndWrite



	/**
	 * Keep track of files which were newly generated by the refactoring.
	 * @param refname Relative filename for use with PROPATH. Probably not
	 * relevent for this type of a FileChange object.
	 * @param fullpath Fully qualified path to the file.
	 */
	public void registerNewFile(String refname, String fullpath) {
		File sourceFile = new File(fullpath);
		FileChange change = new FileChange(
			FileChange.NEW
			, sourceFile
			, null
			, refname
			);
		fileChanges.add(change);
	} // registerNewFile



	/** Roll back the file changes that were made with the previous
	 * refactoring. Note that if the user modifies one of the modified
	 * files in their workspace, their changes will be lost when they
	 * perform the rollback.
	 * @return A String error message, null if no errors.
	 */
	public String rollback() {
		String errors = "";
		for (Iterator it = fileChanges.iterator(); it.hasNext(); ) {
			FileChange change = (FileChange)it.next();
			switch (change.whatHappened) {
			case FileChange.MODIFIED :
			case FileChange.DELETED :
				change.sourceFile.delete();
				if (! change.preservedFile.renameTo(change.sourceFile))
					errors += "Failed to restore " + FileStuff.fullpath(change.sourceFile) + "\n";
				break;
			case FileChange.NEW :
				if (! change.sourceFile.delete())
					errors += "Failed to delete " + FileStuff.fullpath(change.sourceFile) + "\n";
			}
		}
		fileChanges = new ArrayList();
		if (errors.length()==0) return null;
		return errors;
	} // rollback


}
