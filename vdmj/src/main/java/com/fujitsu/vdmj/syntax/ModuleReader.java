/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.syntax;

import java.io.File;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotationList;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.definitions.ASTTypeDefinition;
import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.modules.ASTExportAll;
import com.fujitsu.vdmj.ast.modules.ASTExportList;
import com.fujitsu.vdmj.ast.modules.ASTExportedFunction;
import com.fujitsu.vdmj.ast.modules.ASTExportedOperation;
import com.fujitsu.vdmj.ast.modules.ASTExportedType;
import com.fujitsu.vdmj.ast.modules.ASTExportedValue;
import com.fujitsu.vdmj.ast.modules.ASTImportAll;
import com.fujitsu.vdmj.ast.modules.ASTImportFromModule;
import com.fujitsu.vdmj.ast.modules.ASTImportFromModuleList;
import com.fujitsu.vdmj.ast.modules.ASTImportList;
import com.fujitsu.vdmj.ast.modules.ASTImportedFunction;
import com.fujitsu.vdmj.ast.modules.ASTImportedOperation;
import com.fujitsu.vdmj.ast.modules.ASTImportedType;
import com.fujitsu.vdmj.ast.modules.ASTImportedValue;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.modules.ASTModuleExports;
import com.fujitsu.vdmj.ast.modules.ASTModuleImports;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.LocatedException;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCImportAll;
import com.fujitsu.vdmj.tc.modules.TCImportFromModule;
import com.fujitsu.vdmj.tc.modules.TCImportList;


/**
 * A syntax analyser to parse executableModules.
 */
public class ModuleReader extends SyntaxReader
{
	public ModuleReader(LexTokenReader reader)
	{
		super(reader);
	}

	public ASTModuleList readModules()
	{
		ASTModuleList modules = new ASTModuleList();

		try
		{
			if (lastToken().is(Token.EOF))
			{
				return modules;		// The file is empty
			}

    		if (lastToken().isNot(Token.MODULE) &&
    			!DefinitionReader.newSection(lastToken()))
    		{
    			warning(5015,
    				"LaTeX source should start with %comment, \\document, \\section or \\subsection",
    				lastToken().location);
    		}

    		while (lastToken().isNot(Token.EOF) && lastToken().isNot(Token.END))
    		{
    			switch (lastToken().type)
    			{
    				case MODULE:
    					LexCommentList comments = getComments();
    					ASTAnnotationList annotations = readAnnotations(comments);
    					annotations.astBefore(this);
    					ASTModule module = readModule();
    					modules.add(module);
    					annotations.astAfter(this, module);
    					module.setAnnotations(annotations);
    					module.setComments(comments);
    					break;

    				case IDENTIFIER:
    					LexIdentifierToken id = lastIdToken();

    					if (id.name.equals("class"))
    					{
    						throwMessage(2260, "Module starts with 'class' instead of 'module'");
    					}
    					// else fall through to a flat definition...

    				default:
    					modules.add(readFlatModule());
    					break;
    			}
    		}
    		
    		trailingAnnotationCheck();
    	}
    	catch (LocatedException e)
    	{
    		Token[] end = new Token[0];
    		report(e, end, end);
    	}

		return modules;
	}

	public static TCImportFromModule importAll(TCIdentifierToken name)
	{
		TCImportList types = new TCImportList();
		TCNameToken all = new TCNameToken(name.getLocation(), name.getName(), "all", false, false);
		types.add(new TCImportAll(all, true));
		return new TCImportFromModule(name, types);
	}

	private ASTModule readFlatModule() throws ParserException, LexException
	{
		File file = lastToken().location.file;
		setCurrentModule("DEFAULT");
		ASTDefinitionList definitions = getDefinitionReader().readDefinitions();
		checkFor(Token.EOF, 2318, "Unexpected token after flat definitions");
		return new ASTModule(file, definitions);
	}

	private ASTModule readModule() throws ParserException, LexException
	{
		LexIdentifierToken name = new LexIdentifierToken("?", false, lastToken().location);
		ASTModuleImports imports = null;
		ASTModuleExports exports = null;

		try
		{
			setCurrentModule("");
			checkFor(Token.MODULE, 2170, "Expecting 'module' at module start");
			name = readIdToken("Expecting identifier after 'module'");
			setCurrentModule(name.name);
			
			if (Settings.strict && name.name.equals("DEFAULT"))
			{
				warning(5043, "Strict: Should not use 'DEFAULT' as a module name", name.location);
			}

			if (lastToken().is(Token.IMPORTS))
			{
				imports = readImports(name);
			}

			if (lastToken().is(Token.EXPORTS))
			{
				exports = readExports();
			}

			// Be forgiving about the ordering...

			if (imports == null && lastToken().is(Token.IMPORTS))
			{
				if (Settings.strict)
				{
					warning(5024, "Strict: order should be imports then exports", lastToken().location);
				}
				
				imports = readImports(name);
			}
			
			if (Settings.strict && exports == null)
			{
				warning(5025, "Strict: expecting 'exports all' clause", lastToken().location);
			}
		}
		catch (LocatedException e)
		{
			Token[] after = { Token.DEFINITIONS };
			Token[] upto = { Token.END };
			report(e, after, upto);
		}

		ASTDefinitionList defs = null;

		if (lastToken().is(Token.DEFINITIONS))
		{
			nextToken();
			defs = getDefinitionReader().readDefinitions();
		}
		else
		{
			defs = new ASTDefinitionList();
		}

		checkFor(Token.END, 2171, "Expecting 'end' after module definitions");
		LexIdentifierToken endname =
			readIdToken("Expecting 'end <name>' after module definitions");

		if (name != null &&	!name.equals(endname))
		{
			throwMessage(2049, "Expecting 'end " + name.name + "'");
		}

		LexLocation.addSpan(idToName(name), lastToken());
		return new ASTModule(name, imports, exports, defs);
	}

	private ASTModuleExports readExports() throws ParserException, LexException
	{
		checkFor(Token.EXPORTS, 2174, "Malformed imports? Expecting 'exports' section");
		return new ASTModuleExports(readExportsFromModule());
	}

	private ASTExportList readExportsFromModule() throws ParserException, LexException
	{
		ASTExportList types = new ASTExportList();

		if (lastToken().is(Token.ALL))
		{
			LexNameToken all = new LexNameToken(getCurrentModule(), "all", lastToken().location);
			types.add(new ASTExportAll(all.location));
			nextToken();
			return types;
		}

		types.addAll(readExportsOfOneType());

		while (newType())
		{
			types.addAll(readExportsOfOneType());
		}

		return types;
	}

	private ASTExportList readExportsOfOneType()
		throws ParserException, LexException
	{
		switch (lastToken().type)
		{
			case TYPES:
				nextToken();
				return readExportedTypes();

			case VALUES:
				nextToken();
				return readExportedValues();

			case FUNCTIONS:
				nextToken();
				return readExportedFunctions();

			case OPERATIONS:
				nextToken();
				return readExportedOperations();
				
			default:
				throwMessage(2052, "Expecting 'all', 'types', 'values', 'functions' or 'operations'");
				return null;
		}
	}

	private ASTExportList readExportedTypes()
		throws ParserException, LexException
	{
		ASTExportList list = new ASTExportList();
		list.add(readExportedType());
		boolean semi = ignore(Token.SEMICOLON);

		while (lastToken().isNot(Token.DEFINITIONS) &&
			   lastToken().isNot(Token.USELIB) &&
			   lastToken().isNot(Token.IMPORTS) && !newType())
		{
			if (!semi && Settings.strict)
			{
				warning(5022, "Strict: expecting semi-colon between exports", lastToken().location);
			}

			list.add(readExportedType());
			semi = ignore(Token.SEMICOLON);
		}

		return list;
	}

	private ASTExportedType readExportedType()
		throws ParserException, LexException
	{
		boolean struct = lastToken().is(Token.STRUCT);
		if (struct) nextToken();
		LexNameToken name = readNameToken("Expecting exported type name");
		return new ASTExportedType(name, struct);
	}

	private ASTExportList readExportedValues()
		throws ParserException, LexException
	{
		ASTExportList list = new ASTExportList();
		list.add(readExportedValue());
		boolean semi = ignore(Token.SEMICOLON);

		while (lastToken().isNot(Token.DEFINITIONS) &&
			   lastToken().isNot(Token.USELIB) &&
			   lastToken().isNot(Token.IMPORTS) && !newType())
		{
			if (!semi && Settings.strict)
			{
				warning(5022, "Strict: expecting semi-colon between exports", lastToken().location);
			}

			list.add(readExportedValue());
			semi = ignore(Token.SEMICOLON);
		}

		return list;
	}

	private ASTExportedValue readExportedValue()
		throws ParserException, LexException
	{
		LexToken token = lastToken();
		LexNameList nameList = readIdList(false);
		checkFor(Token.COLON, 2175, "Expecting ':' after export name");
		ASTType type = getTypeReader().readType();
		return new ASTExportedValue(token.location, nameList, type);
	}

	private ASTExportList readExportedFunctions()
		throws ParserException, LexException
	{
		ASTExportList list = new ASTExportList();
		list.add(readExportedFunction());
		boolean semi = ignore(Token.SEMICOLON);

		while (lastToken().is(Token.IDENTIFIER) || lastToken().is(Token.NAME))
		{
			if (!semi && Settings.strict)
			{
				warning(5022, "Strict: expecting semi-colon between exports", lastToken().location);
			}

			list.add(readExportedFunction());
			semi = ignore(Token.SEMICOLON);
		}

		return list;
	}

	private ASTExportedFunction readExportedFunction()
		throws ParserException, LexException
	{
		LexToken token = lastToken();
		LexNameList nameList = readIdList(true);
		ASTTypeList typeParams = ignoreTypeParams();
		checkFor(Token.COLON, 2176, "Expecting ':' after export name");
		
		// Allow maximal types for inv_T exports
		boolean saved = Properties.parser_maximal_types;
		
		try
		{
			Properties.parser_maximal_types = true;
			ASTType type = getTypeReader().readType();
			return new ASTExportedFunction(token.location, nameList, type, typeParams);
		}
		finally
		{
			Properties.parser_maximal_types = saved;
		}
	}

	private ASTExportList readExportedOperations()
		throws ParserException, LexException
	{
		ASTExportList list = new ASTExportList();
		list.add(readExportedOperation());
		boolean semi = ignore(Token.SEMICOLON);

		while (lastToken().is(Token.IDENTIFIER) || lastToken().is(Token.NAME))
		{
			if (!semi && Settings.strict)
			{
				warning(5022, "Strict: expecting semi-colon between exports", lastToken().location);
			}

			list.add(readExportedOperation());
			semi = ignore(Token.SEMICOLON);
		}

		return list;
	}

	private ASTExportedOperation readExportedOperation()
		throws ParserException, LexException
	{
		LexToken token = lastToken();
		LexNameList nameList = readIdList(false);
		checkFor(Token.COLON, 2177, "Expecting ':' after export name");
		ASTType type = getTypeReader().readOperationType();
		return new ASTExportedOperation(token.location, nameList, type);
	}

	private LexNameList readIdList(boolean reservedOK)
		throws ParserException, LexException
	{
		LexNameList list = new LexNameList();
		list.add(readNameToken("Expecting name list", reservedOK));

		while (ignore(Token.COMMA))
		{
			list.add(readNameToken("Expecting name list", reservedOK));
		}

		return list;
	}

	private ASTModuleImports readImports(LexIdentifierToken name)
		throws ParserException, LexException
	{
		checkFor(Token.IMPORTS, 2178, "Expecting 'imports'");
		ASTImportFromModuleList imports = new ASTImportFromModuleList();
		imports.add(readImportDefinition());

		while (ignore(Token.COMMA))
		{
			imports.add(readImportDefinition());
		}

		return new ASTModuleImports(name, imports);
	}

	private ASTImportFromModule readImportDefinition()
		throws ParserException, LexException
	{
		checkFor(Token.FROM, 2179, "Expecting 'from' in import definition");
		LexIdentifierToken from = readIdToken("Expecting module identifier after 'from'");
		return new ASTImportFromModule(from, readImportsFromModule(from));
	}

	private ASTImportList readImportsFromModule(LexIdentifierToken from)
		throws ParserException, LexException
	{
		ASTImportList types = new ASTImportList();

		if (lastToken().is(Token.ALL))
		{
			LexNameToken all = new LexNameToken(getCurrentModule(), "all", lastToken().location);
			types.add(new ASTImportAll(all));
			nextToken();
			return types;
		}

		types.addAll(readImportsOfOneType(from));

		while (newType())
		{
			types.addAll(readImportsOfOneType(from));
		}

		return types;
	}

	private ASTImportList readImportsOfOneType(LexIdentifierToken from)
		throws ParserException, LexException
	{
		switch (lastToken().type)
		{
			case TYPES:
				nextToken();
				return readImportedTypes(from);

			case VALUES:
				nextToken();
				return readImportedValues(from);

			case FUNCTIONS:
				nextToken();
				return readImportedFunctions(from);

			case OPERATIONS:
				nextToken();
				return readImportedOperations(from);
				
			default:
				throwMessage(2054, "Expecting types, values, functions or operations");
				return null;
		}
	}

	private ASTImportList readImportedTypes(LexIdentifierToken from)
		throws ParserException, LexException
	{
		ASTImportList list = new ASTImportList();
		list.add(readImportedType(from));
		boolean semi = ignore(Token.SEMICOLON);

		while (lastToken().is(Token.IDENTIFIER) || lastToken().is(Token.NAME))
		{
			if (!semi && Settings.strict)
			{
				warning(5023, "Strict: expecting semi-colon between imports", lastToken().location);
			}

			list.add(readImportedType(from));
			semi = ignore(Token.SEMICOLON);
		}

		return list;
	}

	private ASTImportedType readImportedType(LexIdentifierToken from)
		throws ParserException, LexException
	{
		String savedModule = getCurrentModule();

		try
		{
			reader.push();
			setCurrentModule(from.name);	// So names are from "from" in...
			ASTTypeDefinition def = getDefinitionReader().readTypeDefinition();
			setCurrentModule(savedModule);	// and restore
			reader.unpush();

			LexNameToken renamed = null;

			if (ignore(Token.RENAMED))
			{
				renamed = readNameToken("Expected renamed type name");
			}

			return new ASTImportedType(def, renamed);
		}
		catch (ParserException e)
		{
			reader.pop();
			setCurrentModule(savedModule);
		}

		LexNameToken name = readNameToken("Expecting imported type name");
		LexNameToken defname = getDefName(from, name);
		LexNameToken renamed = null;

		if (ignore(Token.RENAMED))
		{
			renamed = readNameToken("Expected renamed type name");
		}

		return new ASTImportedType(defname, renamed);
	}

	private ASTImportList readImportedValues(LexIdentifierToken from)
		throws ParserException, LexException
	{
		ASTImportList list = new ASTImportList();
		list.add(readImportedValue(from));
		boolean semi = ignore(Token.SEMICOLON);

		while (lastToken().is(Token.IDENTIFIER) || lastToken().is(Token.NAME))
		{
			if (!semi && Settings.strict)
			{
				warning(5023, "Strict: expecting semi-colon between imports", lastToken().location);
			}

			list.add(readImportedValue(from));
			semi = ignore(Token.SEMICOLON);
		}

		return list;
	}

	private ASTImportedValue readImportedValue(LexIdentifierToken from)
		throws ParserException, LexException
	{
		LexNameToken name = readNameToken("Expecting imported value name");
		LexNameToken defname = getDefName(from, name);
		ASTType type = null;

		if (lastToken().is(Token.COLON))
		{
			nextToken();
			type = getTypeReader().readType();
		}

		LexNameToken renamed = null;

		if (ignore(Token.RENAMED))
		{
			renamed = readNameToken("Expected renamed value name");
		}

		return new ASTImportedValue(defname, type, renamed);
	}

	private ASTImportList readImportedFunctions(LexIdentifierToken from)
		throws ParserException, LexException
	{
		ASTImportList list = new ASTImportList();
		list.add(readImportedFunction(from));
		boolean semi = ignore(Token.SEMICOLON);

		while (lastToken().is(Token.IDENTIFIER) || lastToken().is(Token.NAME))
		{
			if (!semi && Settings.strict)
			{
				warning(5023, "Strict: expecting semi-colon between imports", lastToken().location);
			}

			list.add(readImportedFunction(from));
			semi = ignore(Token.SEMICOLON);
		}

		return list;
	}

	private ASTImportedFunction readImportedFunction(LexIdentifierToken from)
		throws ParserException, LexException
	{
		LexNameToken name =	readNameToken("Expecting imported function name", true);
		LexNameToken defname = getDefName(from, name);
		ASTTypeList typeParams = ignoreTypeParams();

		ASTType type = null;

		if (lastToken().is(Token.COLON))
		{
			nextToken();
			
			// Allow maximal ! for inv_T functions
			boolean saved = Properties.parser_maximal_types;
			
			try
			{
				Properties.parser_maximal_types = true;
				type = getTypeReader().readType();
			}
			finally
			{
				Properties.parser_maximal_types = saved;
			}
		}

		LexNameToken renamed = null;

		if (ignore(Token.RENAMED))
		{
			renamed = readNameToken("Expected renamed function name", true);
		}

		return new ASTImportedFunction(defname, type, typeParams, renamed);
	}

	private ASTImportList readImportedOperations(LexIdentifierToken from)
		throws ParserException, LexException
	{
		ASTImportList list = new ASTImportList();
		list.add(readImportedOperation(from));
		boolean semi = ignore(Token.SEMICOLON);

		while (lastToken().is(Token.IDENTIFIER) || lastToken().is(Token.NAME))
		{
			if (!semi && Settings.strict)
			{
				warning(5023, "Strict: expecting semi-colon between imports", lastToken().location);
			}

			list.add(readImportedOperation(from));
			semi = ignore(Token.SEMICOLON);
		}

		return list;
	}

	private ASTImportedOperation readImportedOperation(LexIdentifierToken from)
		throws ParserException, LexException
	{
		LexNameToken name = readNameToken("Expecting imported operation name", false);
		LexNameToken defname = getDefName(from, name);
		ASTType type = null;

		if (lastToken().is(Token.COLON))
		{
			nextToken();
			type = getTypeReader().readOperationType();
		}

		LexNameToken renamed = null;

		if (ignore(Token.RENAMED))
		{
			renamed = readNameToken("Expected renamed operation name");
		}

		return new ASTImportedOperation(defname, type, renamed);
	}

	private boolean newType() throws LexException
	{
		switch (lastToken().type)
		{
			case TYPES:
			case VALUES:
			case FUNCTIONS:
			case OPERATIONS:
			case EOF:
				return true;
				
			default:
				return false;
		}
	}

	private LexNameToken getDefName(LexIdentifierToken impmod, LexNameToken name)
	{
    	if (name.module.equals(getCurrentModule()))		//ie. it was an id
    	{
    		return new LexNameToken(impmod.name, name.name, name.location);
    	}

    	return name;
	}

	private ASTTypeList ignoreTypeParams() throws LexException, ParserException
	{
		if (lastToken().is(Token.SEQ_OPEN))
		{
			return getDefinitionReader().readTypeParams();
		}
		else
		{
			return null;
		}
	}
	
	@Override
	public List<VDMError> getErrors()
	{
		List<VDMError> errs = new Vector<VDMError>(reader.getErrors());
		errs.addAll(super.getErrors());
		return errs;
	}
	
	@Override
	public int getErrorCount()
	{
		return reader.getErrorCount() + super.getErrorCount();
	}
}
