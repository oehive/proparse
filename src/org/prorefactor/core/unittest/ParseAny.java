/** ParseAny.java
 * June 2010 by John Green
 * 
 * Copyright (c) 2010 Joanju Software.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.prorefactor.core.unittest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.treeparser.ParseUnit;
import org.prorefactor.treeparserbase.JPTreeParser;


/** Reads settings from ./parseany.properties.
 * Ex:<pre>
 * projectPropsDir = /work/myproject/proparsesettings
 * topParseDir = /work/myproject
 * extensions = p w cls
 * </pre>
 */
public class ParseAny extends TestCase {
	
	File topDir;
	String [] extensions;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		loadProperties();
	}

	private void loadProperties() throws Exception {
		File propsfile = new File("parseany.properties");
		if (! propsfile.exists())
			throw new IOException("The file 'parseany.properties' must be in your working directory.");
		Properties props = new Properties();
		props.load(new FileInputStream(propsfile));
		
		String projectPropsDirName = props.getProperty("projectPropsDir");
		if (StringUtils.isEmpty(projectPropsDirName))
			throw new Exception("projectPropsDir must be defined");
		RefactorSession.getInstance().loadProjectPropertiesFromDirectory(projectPropsDirName);
		
		String extensionsProp = props.getProperty("extensions");
		if (StringUtils.isEmpty(extensionsProp))
			throw new Exception("extensions must be defined");
		extensions = StringUtils.split(extensionsProp);

		String topDirProp = props.getProperty("topParseDir");
		if (StringUtils.isEmpty(topDirProp))
			throw new Exception("topParseDir must be defined");
		topDir = new File(topDirProp);
		if (! (topDir.exists() && topDir.isDirectory()))
			throw new Exception(topDirProp + " is not a directory");

	}

	public void test01() throws Exception {
		Collection files = FileUtils.listFiles(topDir, extensions, true);
		for (Iterator it = files.iterator(); it.hasNext();) {
			File file = (File) it.next();
			System.out.println(file.getPath());
			ParseUnit pu = new ParseUnit(file);
			pu.treeParser(new JPTreeParser());
			pu.treeParser01();
		}
		System.out.println("ParseAny completed OK");
	}

}
