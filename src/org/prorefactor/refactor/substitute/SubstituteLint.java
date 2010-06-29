/**
 * SubstituteLint.java
 * @author John Green
 * 15-Oct-2002
 * www.joanju.com
 *
 *  
 * With credit to Jurjen Dijskstra - Prolint's "substitute.p" was
 * written long before this Java implementation was done.
 *
 * 
 * Copyright (c) 2002 Joanju Limited.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */

package org.prorefactor.refactor.substitute;


import com.joanju.ProparseLdr;

import java.util.*;

import org.prorefactor.core.HandleCollection;
import org.prorefactor.core.Pstring;



/**
 * Find PLUS nodes which join two or more QSTRING nodes with expressions.
 * Those should be using SUBSTITUTE instead of concatenation, in order
 * for easier translation.
 */
public class SubstituteLint {

	private ProparseLdr parser;

	public TreeSet targetSet;

	public SubstituteLint() {
		parser = ProparseLdr.getInstance();
		targetSet = new TreeSet();
	}



	/**
	 * Run this lint routine.
	 * @param topNode The topmost node in the AST to start the lint from.
	 * @return A TreeSet of unique SubstituteTarget records.
	 * @see org.prorefactor.refactor.substitute.SubstituteTarget
	 * The client becomes the "owner" of the handles referred to in the
	 * SubstituteTarget records, and is responsible for releasing those when done.
	 */
	public TreeSet run(int topNode) {

		// We need lots of handles for this
		HandleCollection handler = new HandleCollection();

		// Query for PLUS nodes
		String qplusnodes = "jpsl_plusnodes";

		try {

			int currPlus       = handler.gimme();
			int firstChild     = handler.gimme();
			int plusNode       = handler.gimme();
			int secondChild    = handler.gimme();
			int tempHandle     = handler.gimme();
			
			// Create a query for all PLUS nodes
			int numPlusResults = parser.queryCreate(topNode, qplusnodes, "PLUS");

			// Loop through the query results	
			plus_results_loop:
			for (int count = 1; count <= numPlusResults; count++) {

				// Get current result
			    parser.queryGetResult(qplusnodes, count, plusNode);
			    
			    // If this PLUS node's parent is another PLUS node, then we
			    // have already examined it.
			    parser.nodeParent(plusNode, tempHandle);
			    if (parser.getNodeType(tempHandle).equals("PLUS"))
			    	continue plus_results_loop;

				// Create a "target" record. This gets added to our
				// targetSet if this PLUS node is interesting, it gets
				// dropped otherwise.
				SubstituteTarget target = new SubstituteTarget();

				// Initialize some state variables for this PLUS node
			    parser.copyHandle(plusNode, currPlus);
			    boolean firstTrans = true;
				char quoteType = 0;
				int numPlus = 0;
				int numStrings = 0;
				String attributesToMatch = "";
	
				// Find the child nodes
				String firstChildType = parser.nodeFirstChild(plusNode, firstChild);
				parser.nodeNextSibling(firstChild, secondChild);
	
				// Iteratively step down through this PLUS node's children,
				// to figure out if it is one that should be refactored or not.
				boolean done = false;
				examine_loop:
				while (! done) {
	
					// Count the number of PLUS nodes in this expression
					numPlus++;

					// Examine the two child nodes
					int currChild = firstChild;
					each_of_two_children:
					while (true) {
						if (parser.getNodeType(currChild).equals("QSTRING")) {
							numStrings++;
							Pstring pstring = new Pstring(parser.getNodeText(currChild));

							// Watch out for mixed double/single quotes in the expression;
							// currently our refactor routine doesn't handle those.
							if (quoteType == 0)
								quoteType = pstring.getQuote();
							if (quoteType != pstring.getQuote())
								target.quoteTypeMismatch = 1;

							// Watch for translatable strings. If this expression doesn't
							// contain any, then maybe the client application won't be
							// concerned about this expression.
							boolean isTrans = pstring.isTrans();
							if (isTrans)
								target.numTranslatable++;
							
							// Watch for mismatched translatable string attributes
							if (isTrans) {
								String attr = pstring.getAttributes();
								// First one?
								if (firstTrans) {
									firstTrans = false;
									attributesToMatch = attr;
								} else {
									if (! attr.equals(attributesToMatch))
										target.attributesMismatch ++;
								}
							}
							
						}
						if (currChild == secondChild)
							break each_of_two_children;
						currChild = secondChild;
					} // each_of_two_children

	
					// More PLUS nodes to examine?
					if (firstChildType.equals("PLUS")) {
						firstChildType = parser.nodeFirstChild(firstChild, firstChild);
						parser.nodeNextSibling(firstChild, secondChild);
						parser.copyHandle(firstChild, currPlus);
					} else {
						done = true;
					}
	
				} // examine_loop:
	
				if (numStrings < 2 || numPlus < 2)
					continue plus_results_loop;

				// If we got here, then we're going to register this node.
				// Grab a new (lasting) handle to this node.
				int storePlusHandle = parser.getHandle();
				parser.copyHandle(plusNode, storePlusHandle);

				// Add to our target set.
				target.changedLines[0]
					= target.changedLines[1]
					= target.changedLines[2]
					= target.changedLines[3]
					= target.linenum
					= parser.getNodeLine(storePlusHandle)
					;
				target.filename = parser.getNodeFilename(storePlusHandle);
				target.nodeHandle = storePlusHandle;

				// SubstituteTarget equality check is based on filename & line number.
				if (! targetSet.contains(target))
					targetSet.add(target);
	
			} // plus_results_loop
	
			return targetSet;
		
		} finally {
			parser.queryClear(qplusnodes);
			handler.releaseAll();
		}

	} // run()


} // class SubstituteLint

