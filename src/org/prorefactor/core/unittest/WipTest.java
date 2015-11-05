package org.prorefactor.core.unittest;

import java.io.File;
import junit.framework.TestCase;
import org.prorefactor.core.JPNodeLister;
import org.prorefactor.core.TokenTypes;
import org.prorefactor.refactor.RefactorException;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.treeparser.ParseUnit;
import org.prorefactor.treeparserbase.JPTreeParser;

import com.joanju.proparse.DoParse;

// I launch this class for test parse etc against
// whatever work-in-process bug I'm currently investigating.
public class WipTest extends TestCase {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		RefactorSession refpack = RefactorSession.getInstance();
		refpack.loadProjectForced("unittest");
	}

	public void test01() throws Exception {
		File file = new File("data/bugsfixed/bug15.p");
		try {
			boolean treeparsers = true;
			if (treeparsers) {
				ParseUnit pu = new ParseUnit(file);
				pu.parse();
				pu.treeParser(new JPTreeParser());
				pu.treeParser01();
			}
			else {
				DoParse doParse = new DoParse(file.getPath());
				boolean justLex = false;
				doParse.setJustLex(justLex);
				doParse.doParse();
				if (!justLex) {
					JPNodeLister lister = new JPNodeLister(doParse.getTopNode(), "C:\\temp\\nodelister.txt", new TokenTypes());
					lister.print();
				}
			}
		} catch (Exception e) {
			throw new RefactorException(e);
		}
	}

}
