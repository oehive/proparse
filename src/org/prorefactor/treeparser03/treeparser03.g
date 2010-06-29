/*

treeparser03.g - Lint/Refactor Tree Parser.

This tree parser investigates the tree for possible Lint rules which
might have been broken. For each node or structure which requires
examination, it calls TP03Support.

To find actions taken within this grammar, search for "tpSupport",
which is the tree parser support object.

Copyright (c) 2003-2008 Joanju Software.
All rights reserved. This program and the accompanying materials 
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/epl-v10.html

*/


header {
	package org.prorefactor.treeparser03;

	import org.prorefactor.core.IJPNode;
	import org.prorefactor.treeparser.IJPTreeParser;
}


options {
        language = "Java";
}


// Class preamble - anything here gets inserted in front of the class definition.
{
} // Class preamble


// class definition options
class TreeParser03 extends JPTreeParser;
options {
	importVocab = ProParser;
	defaultErrorHandler = false;
	classHeaderSuffix = IJPTreeParser;
}



// This is added to top of the class definitions
{

	// --- The following are required in all tree parsers ---

	// Where did the tree parser leave off parsing -- might give us at least a bit
	// of an idea where things left off if an exception was thrown.
	// See antlr/TreeParser and the generated code.
	public AST get_retTree() {
		return _retTree;
	}

	// Func for grabbing the "state2" attribute from the node at LT(1) 
	private boolean state2(AST node, int match) {
		return ((IJPNode)node).getState2() == match;
	}


	// --- The above are for all tree parsers, below are for TreeParser03 ---


	TP03Support tpSupport = null;
	/**
	 * Set the support object for this tree parser.
	 * This tree parser does not instantiate its own support object.
	 */
	public void setSupport(TP03Support inSupport) {
		tpSupport = inSupport;
	}


} // end of what's added to the top of the class definition



///////////////////////////////////////////////////////////////////////////////////////////////////
// Begin grammar
///////////////////////////////////////////////////////////////////////////////////////////////////


program
	:	#(	Program_root
			{tpSupport.blockBegin();}  // procedure block
			(blockorstate)*
			{tpSupport.blockEnd();}
			Program_tail
		)
	;

code_block
	:	#(	Code_block {tpSupport.blockBegin();}
			(blockorstate)*
			{tpSupport.blockEnd();}
		)
	;

blockorstate
	:	(	labeled_block
		|	statement {tpSupport.statementEnd();}
		|	// Expr_statement has a "statehead" node attribute
			#(Expr_statement expression (NOERROR_KW)? state_end)
		|	pd:PROPARSEDIRECTIVE {tpSupport.proparsedirective(#pd);}
		|	PERIOD {tpSupport.statementEnd();}
		|	DOT_COMMENT
		|	#(ANNOTATION (.)* )
		)
	;

labeled_block
	:	#(	bl:BLOCK_LABEL {tpSupport.blockWithLabel(#bl);}
			LEXCOLON (dostate|forstate|repeatstate)
		)
	;

parameter
	:	#(BUFFER (RECORD_NAME | ID FOR RECORD_NAME ) )
	|	#(OUTPUT {tpSupport.updating(true);} parameter_arg {tpSupport.updating(false);} )
	|	#(INPUTOUTPUT {tpSupport.updating(true);} parameter_arg {tpSupport.updating(false);} )
	|	#(INPUT parameter_arg )
	;

field
	:	#(ref:Field_ref (INPUT)? (#(FRAME ID) | #(BROWSE ID))? id:ID (array_subscript)? )
		{tpSupport.fieldRef(#ref, #id);}
	;

assignment_list
	:	RECORD_NAME (#(EXCEPT (field)*))?
	|	(	assign_equal (#(WHEN expression))?
		|	#(	Assign_from_buffer
				{tpSupport.updating(true);} field {tpSupport.updating(false);}
			)
			(#(WHEN expression))?
		)*
	;
assign_equal
	:	#(	EQUAL
			(	pseudfn
			|	{tpSupport.updating(true);} field {tpSupport.updating(false);}
			)
			expression
		)
	;

defineparameterstate
	:	#(	def:DEFINE (def_shared)? (def_visib)?
			(	PARAMETER BUFFER
				ID FOR RECORD_NAME
				(PRESELECT)? (label_constant)? (#(FIELDS (field)* ))?
			|	(INPUT|OUTPUT|INPUTOUTPUT|RETURN) PARAMETER
				(	TABLE FOR RECORD_NAME (APPEND|BIND)*
				|	TABLEHANDLE (FOR)? id:ID (APPEND|BIND)*
				|	DATASET FOR ID (APPEND|BYVALUE|BIND)*
				|	DATASETHANDLE ID (APPEND|BYVALUE|BIND)*
				|	vid:ID {tpSupport.define(#def, #vid);}
					defineparam_var (triggerphrase)?
				)
			)
			state_end
			{tpSupport.defineEnd();}
		)
	;
defineparam_var
	:	(	#(	AS
				(	(HANDLE (TO)? datatype_dll)=> HANDLE (TO)? datatype_dll
				|	CLASS TYPE_NAME
				|	datatype_param
				)
			)
		)?
		(	options{greedy=true;}
		:	casesens_or_not | #(FORMAT expression) | #(DECIMALS expression ) | #(LIKE field (VALIDATE)?)
		|	initial_constant | label_constant | NOUNDO {tpSupport.noundo();}  | extentphrase
		)*
	;

definetemptablestate
	:	#(	def:DEFINE (def_shared)? (def_visib)? TEMPTABLE id:ID
			{tpSupport.define(#def, #id);}
			( UNDO {tpSupport.undo();} | NOUNDO {tpSupport.noundo();} )?
			(namespace_uri)? (namespace_prefix)?
			(REFERENCEONLY)?
			(def_table_like)?
			(label_constant)?
			(#(BEFORETABLE ID))?
			(RCODEINFORMATION)?
			(def_table_field)*
			(	#(	INDEX ID ( (AS|IS)? (UNIQUE|PRIMARY|WORDINDEX) )*
					( ID (ASCENDING|DESCENDING|CASESENSITIVE)* )+
				)
			)*
			state_end
			{tpSupport.defineEnd();}
		)
	;

defineworktablestate
	:	#(	def:DEFINE (def_shared)? (def_visib)? WORKTABLE id:ID
			{tpSupport.define(#def, #id);}
			(NOUNDO {tpSupport.noundo();} )?
			(def_table_like)? (label_constant)? (def_table_field)* state_end
			{tpSupport.defineEnd();}
		)
	;

definevariablestate
	:	#(	def:DEFINE (def_shared)? (def_visib)? VARIABLE id:ID
			{tpSupport.define(#def, #id);}
			(fieldoption)* (triggerphrase)? state_end
			{tpSupport.defineEnd();}
		)
	;

dostate
	:	#(	DO (block_for)? (block_preselect)? (block_opt)* block_colon 
			{tpSupport.statementEnd();}
			code_block block_end
		)
	;

fieldoption
	:	#(	AS
			(	CLASS TYPE_NAME
			|	datatype_field
			)
		)
	|	casesens_or_not
	|	color_expr
	|	#(COLUMNCODEPAGE expression )
	|	#(CONTEXTHELPID expression)
	|	#(DECIMALS expression )
	|	DROPTARGET
	|	extentphrase
	|	#(FONT expression)
	|	#(FORMAT expression)
	|	#(HELP constant)
	|	initial_constant
	|	label_constant
	|	#(LIKE field (VALIDATE)? )
	|	#(MOUSEPOINTER expression )
	|	NOUNDO {tpSupport.noundo();}
	|	viewasphrase
	|	TTCODEPAGE
	|	xml_data_type
	|	xml_node_type
	;

forstate
	:	#(	FOR for_record_spec (block_opt)* block_colon
			{tpSupport.statementEnd();}
			code_block block_end
		)
	;

importstate
	:	#(	IMPORT (stream_name_or_handle)?
			( #(DELIMITER constant) | UNFORMATTED )?
			(	RECORD_NAME (#(EXCEPT (field)*))?
			|	(	{tpSupport.updating(true);} field {tpSupport.updating(false);}
				|	CARET 
				)+
			)?
			(NOLOBS)? (NOERROR_KW)? state_end
		)
	;

procedurestate
	:	#(	p:PROCEDURE {tpSupport.procedure(p);}
			ID
			(	#(	EXTERNAL constant
					{tpSupport.procedureExternal();}
					(	CDECL_KW
					|	PASCAL_KW
					|	STDCALL_KW
					|	#(ORDINAL expression )
					|	PERSISTENT
					)*
				)
			|	PRIVATE
			|	IN_KW SUPER
			)?
			block_colon code_block (EOF | #(END (PROCEDURE)?) state_end)
			{tpSupport.procedureEnd();}
		)
	;

repeatstate
	:	#(	REPEAT (block_for)? (block_preselect)? (block_opt)* block_colon
			{tpSupport.statementEnd();}
			code_block block_end
		)
	;

setstate
	:	#(	SET
			(stream_name_or_handle)? (UNLESSHIDDEN)?
			{tpSupport.updating(true);} (form_item)* {tpSupport.updating(false);}
			(goonphrase)?  (#(EXCEPT (field)*))?  (#(IN_KW WINDOW expression))?
			(framephrase)?  (editingphrase)?  (NOERROR_KW)?
			state_end
		)
	;

undostate
	:	#(	u:UNDO (bl:BLOCK_LABEL)? {tpSupport.undoState(#u, #bl);}
			(	COMMA
				(	#(LEAVE (BLOCK_LABEL)? )
				|	#(NEXT (BLOCK_LABEL)? )
				|	#(RETRY (BLOCK_LABEL)? )
				|	#(RETURN (return_options)? )
				)
			)?
			state_end
		)
	;

updatestate
	:	#(	UPDATE
			(UNLESSHIDDEN)?
			{tpSupport.updating(true);} (form_item)* {tpSupport.updating(false);}
			(goonphrase)?
			(#(EXCEPT (field)*))?
			(#(IN_KW WINDOW expression))?
			(framephrase)?
			(editingphrase)?
			(NOERROR_KW)?
			state_end
		)
	;
