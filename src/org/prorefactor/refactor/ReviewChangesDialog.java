/**
 * ReviewChangesDialog.java
 * @author John Green
 * 27-Oct-2002
 * www.joanju.com
 * 
 * To Do:
 *   - add support for multiple changed sections:
 *     - highlighting multiple sections
 *     - goto button should go to next change, with
 *       some indication that it has cycled round back to the
 *       first change
 * 
 * Copyright (c) 2002 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.prorefactor.refactor;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import java.io.*;



/**
 * Application modal dialog for previewing, changing, and then
 * either accepting or rejecting a change proposed by an automated
 * refactoring routine.
 */
public class ReviewChangesDialog {



	// Class data
	static final String windowTitle = "Review Changes";

	// Member data
	boolean displayResponsible = false;
	Button button;
	Color highlightColor;
	Display display;
	ExtendedModifyListener extendedModifyListener;
	Font font;
	Image windowIcon;
	int[] changedLines;
	int offsetOfChange;
	int userChoice = 0;
	SashForm sashForm;
	Shell shell;
	String filename1;
	String filename2;
	StyledText text1;
	StyledText text2;


	public ReviewChangesDialog(String inFilename1, String inFilename2) {
		filename1 = inFilename1;
		filename2 = inFilename2;
		changedLines = new int[4];
	}



	public void open() {
		if (Display.getCurrent() == null)
			displayResponsible = true;
		display = Display.getDefault();
		createShell(display);
		shell.open();
	}



	/**
	 * Wait for the user to close the dialog.
	 * Sets up the readAndDispatch loop.
	 * @return 1 on Accept, 0 on Reject.
	 */
	public int getUserInput() {
		gotoChangedLine();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
		if (displayResponsible)
			display.dispose();
		return userChoice;
	} // wait()



	void createShell(Display displayIn) {

		GridData griddata;

		shell = new Shell(displayIn, SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setLayout(new GridLayout(1, false));

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;

		sashForm = new SashForm(shell, SWT.NONE);
		sashForm.setOrientation(SWT.VERTICAL);

		griddata = new GridData(GridData.FILL_BOTH);
		griddata.horizontalSpan = 2;
		sashForm.setLayoutData(griddata);
		
		font = new Font(displayIn, "Courier New", 9, SWT.NORMAL);

		highlightColor = new Color(displayIn, 255, 255, 0);

		// The two text widgets
		text1 = createText(sashForm);
		text1.setEditable(false);
		text1.setFont(font);
		text2 = createText(sashForm);
		text2.setFont(font);
		text2.setFocus();

		// Group for the buttons to be in
		Group buttonGroup = new Group(shell, SWT.SHADOW_NONE);
		griddata = new GridData(GridData.HORIZONTAL_ALIGN_END);
		buttonGroup.setLayoutData(griddata);
		RowLayout rowLayout = new RowLayout();
		rowLayout.wrap = false;
		rowLayout.pack = false;
		buttonGroup.setLayout (rowLayout);

		// "Horizontal" button
		button = new Button (buttonGroup, SWT.PUSH);
		button.setText("&Horizontal");
		button.setToolTipText("Split Horizontal");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sashForm.setOrientation(SWT.VERTICAL);
			}
		});

		// "Vertical" button
		button = new Button (buttonGroup, SWT.PUSH);
		button.setText("&Vertical");
		button.setToolTipText("Split Vertical");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sashForm.setOrientation(SWT.HORIZONTAL);
			}
		});
		
		// "Go To" button
		button = new Button(buttonGroup, SWT.PUSH);
		button.setText("&Go To");
		button.setToolTipText("Go to the line with the changes");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				gotoChangedLine();
			}
		});
		
		// "Accept" button
		button = new Button(buttonGroup, SWT.PUSH);
		button.setText("&Accept");
		button.setToolTipText("Accept changes - save to file");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (writeFile() == 1)
					shell.close();
				userChoice = 1;
			}
		});

		// "Reject" button
		button = new Button(buttonGroup, SWT.PUSH);
		button.setText("&Reject");
		button.setToolTipText("Reject changes - do not save");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
				userChoice = 0;
			}
		});

		// "Cancel" button
		button = new Button(buttonGroup, SWT.PUSH);
		button.setText("&Cancel");
		button.setToolTipText("Stop refactoring");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
				userChoice = -1;
			}
		});

		// Window dressing - the icon
		windowIcon = new Image(displayIn,
			getClass().getClassLoader().getResourceAsStream("icons/joanju.gif"));

		// Shell settings
		int useWidth = (displayIn.getClientArea().width * 4) / 5;
		shell.setSize(useWidth, 700);
		// Use a fixed position.
		// It's annoying to have Windows pop it up in a different spot every time.
		shell.setLocation(85, 60);
		shell.setText(windowTitle + " - " + filename1);
		// shell.setLayout(gridLayout);
		shell.setImage(windowIcon);
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				windowIcon.dispose();
				font.dispose();
				highlightColor.dispose();
			}
		});

		// Load the files
		if (loadFiles() < 1)
			shell.close();

	} // createShell()



	StyledText createText(Composite parent) {
		StyledText text =
			new StyledText(
				parent,
				SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData spec = new GridData();
		spec.horizontalAlignment = GridData.FILL;
		spec.grabExcessHorizontalSpace = true;
		spec.verticalAlignment = GridData.FILL;
		spec.grabExcessVerticalSpace = true;
		text.setLayoutData(spec);
		return text;
	} // createText()



	void gotoChangedLine() {
		gotoChangedLineSub(text1);
		gotoChangedLineSub(text2);
		text2.setFocus();
	} // gotoChangedLine()



	void gotoChangedLineSub(StyledText text) {
		int textHeight = text.getClientArea().height;
		int lineHeight = text.getLineHeight();
		text.setTopPixel((changedLines[0] * lineHeight) - (textHeight / 2));
		text.setSelection(offsetOfChange);
		text.showSelection();
	} // gotoChangedLineSub()



	int loadFiles() {
		try {
			BufferedReader in;
			StringWriter out;
			int c = 0;

			// File1
			int offset = 0;
			int linenum = 1;
			in = new BufferedReader(new FileReader(filename1));
			out = new StringWriter();
			while ( (c=in.read()) != -1) {
				out.write(c);
				if (c == '\n') {
					linenum++;
					if (linenum == changedLines[0])
						offsetOfChange = offset + 1;
				}
				offset++;
			}
			in.close();
			text1.setText(out.toString());

			// File2
			in = new BufferedReader(new FileReader(filename2));
			out = new StringWriter();
			while ( (c=in.read()) != -1)
				out.write(c);
			in.close();
			text2.setText(out.toString());

			text1.setLineBackground(
				changedLines[0] - 1
				, changedLines[1] - changedLines[0] + 1
				, highlightColor
				);

			int begin = changedLines[2] - 1;
			int numlines = changedLines[3] - changedLines[2] + 1;
			if (begin + numlines > text2.getLineCount())
				numlines = text2.getLineCount() - begin;
			text2.setLineBackground(
				changedLines[2] - 1
				, numlines
				, highlightColor
				);

			return 1;
		} catch (IOException e) {
			showMessage(e.getMessage());
			return -1;
		}
	} // loadFiles()



	/**
	 * Set changed lines tells this dialog which lines have been
	 * changed for purposes of highlighting and "go to".
	 * @param inChangedLines Array of four integer line numbers: original file to/from
	 * and changed file to/from.
	 * Pass in line numbers with count starting at 1
	 * (converted internally for line count from zero where necessary).
	 */
	public void setChangedLines(int[] inChangedLines) {
		changedLines = inChangedLines;
	} // setChangedLines()



	public void showMessage(String theMessage) {
		MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION);
		dialog.setMessage(theMessage);
		dialog.setText(windowTitle);
		dialog.open();
	} // showMessage()



	int writeFile() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename1));
			StringReader in = new StringReader(text2.getText());
			int c = 0;
			while ( (c=in.read()) != -1)
				out.write(c);
			out.close();
		} catch (IOException e) {
			showMessage(e.getMessage());
			return -1;
		}
		return 1;
	} // writeFile()



} // class ReviewChangesDialog

