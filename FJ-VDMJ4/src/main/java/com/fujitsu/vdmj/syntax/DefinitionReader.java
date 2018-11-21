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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.syntax;

import java.util.List;
import java.util.Vector;
import java.util.Arrays;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotationList;
import com.fujitsu.vdmj.ast.definitions.ASTAccessSpecifier;
import com.fujitsu.vdmj.ast.definitions.ASTAssignmentDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassInvariantDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.definitions.ASTEqualsDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTInstanceVariableDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTMutexSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTNamedTraceDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTPerSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTStateDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTThreadDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTTypeDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTEqualsExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.expressions.ASTNotYetSpecifiedExpression;
import com.fujitsu.vdmj.ast.expressions.ASTSubclassResponsibilityExpression;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTBind;
import com.fujitsu.vdmj.ast.patterns.ASTIdentifierPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.patterns.ASTPatternListList;
import com.fujitsu.vdmj.ast.patterns.ASTSeqBind;
import com.fujitsu.vdmj.ast.patterns.ASTSetBind;
import com.fujitsu.vdmj.ast.patterns.ASTTuplePattern;
import com.fujitsu.vdmj.ast.patterns.ASTTypeBind;
import com.fujitsu.vdmj.ast.statements.ASTCallObjectStatement;
import com.fujitsu.vdmj.ast.statements.ASTCallStatement;
import com.fujitsu.vdmj.ast.statements.ASTErrorCase;
import com.fujitsu.vdmj.ast.statements.ASTErrorCaseList;
import com.fujitsu.vdmj.ast.statements.ASTExternalClause;
import com.fujitsu.vdmj.ast.statements.ASTExternalClauseList;
import com.fujitsu.vdmj.ast.statements.ASTNotYetSpecifiedStatement;
import com.fujitsu.vdmj.ast.statements.ASTPeriodicStatement;
import com.fujitsu.vdmj.ast.statements.ASTSpecificationStatement;
import com.fujitsu.vdmj.ast.statements.ASTSporadicStatement;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.ast.statements.ASTSubclassResponsibilityStatement;
import com.fujitsu.vdmj.ast.traces.ASTTraceApplyExpression;
import com.fujitsu.vdmj.ast.traces.ASTTraceBracketedExpression;
import com.fujitsu.vdmj.ast.traces.ASTTraceConcurrentExpression;
import com.fujitsu.vdmj.ast.traces.ASTTraceCoreDefinition;
import com.fujitsu.vdmj.ast.traces.ASTTraceDefinition;
import com.fujitsu.vdmj.ast.traces.ASTTraceDefinitionList;
import com.fujitsu.vdmj.ast.traces.ASTTraceDefinitionTerm;
import com.fujitsu.vdmj.ast.traces.ASTTraceDefinitionTermList;
import com.fujitsu.vdmj.ast.traces.ASTTraceLetBeStBinding;
import com.fujitsu.vdmj.ast.traces.ASTTraceLetDefBinding;
import com.fujitsu.vdmj.ast.traces.ASTTraceRepeatDefinition;
import com.fujitsu.vdmj.ast.types.ASTFieldList;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.ast.types.ASTInvariantType;
import com.fujitsu.vdmj.ast.types.ASTNamedType;
import com.fujitsu.vdmj.ast.types.ASTOperationType;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePair;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePairList;
import com.fujitsu.vdmj.ast.types.ASTPatternTypePair;
import com.fujitsu.vdmj.ast.types.ASTProductType;
import com.fujitsu.vdmj.ast.types.ASTRecordType;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.ast.types.ASTUnresolvedType;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.LocatedException;

/**
 * A syntax analyser to parse definitions.
 */
public class DefinitionReader extends SyntaxReader
{
	public DefinitionReader(LexTokenReader reader)
	{
		super(reader);
	}

	private static Token[] sectionArray =
	{
		Token.TYPES,
		Token.FUNCTIONS,
		Token.STATE,
		Token.VALUES,
		Token.OPERATIONS,
		Token.INSTANCE,
		Token.THREAD,
		Token.SYNC,
		Token.TRACES,
		Token.END,
		Token.EOF
	};

	private static Token[] afterArray =
	{
		Token.SEMICOLON
	};

	private static List<Token> sectionList = Arrays.asList(sectionArray);

	private boolean newSection() throws LexException
	{
		return newSection(lastToken());
	}

	public static boolean newSection(LexToken tok)
	{
		return sectionList.contains(tok.type);
	}
	
	private boolean accessSpecifier() throws LexException
	{
		LexToken tok = lastToken();
		
		return tok.is(Token.PUBLIC) || tok.is(Token.PRIVATE) || tok.is(Token.PROTECTED) ||
			   tok.is(Token.PURE) || tok.is(Token.STATIC)|| tok.is(Token.ASYNC); 
	}

	public ASTDefinitionList readDefinitions() throws ParserException, LexException
	{
		ASTDefinitionList list = new ASTDefinitionList();
		boolean threadDone = false;

		while (lastToken().isNot(Token.EOF) && lastToken().isNot(Token.END))
		{
			switch (lastToken().type)
			{
				case TYPES:
					list.addAll(readTypes());
        			break;

				case FUNCTIONS:
					list.addAll(readFunctions());
        			break;

				case STATE:
					if (dialect != Dialect.VDM_SL)
					{
						throwMessage(2277, "Can't have state in VDM++");
					}

					try
    				{
    					nextToken();
        				list.add(readStateDefinition());

        				if (!newSection())
        				{
        					checkFor(Token.SEMICOLON,
        						2080, "Missing ';' after state definition");
        				}
    				}
    				catch (LocatedException e)
    				{
    					report(e, afterArray, sectionArray);
    				}
					break;

				case VALUES:
        			list.addAll(readValues());
					break;

				case OPERATIONS:
        			list.addAll(readOperations());
					break;

				case INSTANCE:
					if (dialect == Dialect.VDM_SL)
					{
						throwMessage(2009, "Can't have instance variables in VDM-SL");
					}

					list.addAll(readInstanceVariables());
					break;

				case TRACES:
					if (dialect == Dialect.VDM_SL &&
						Settings.release != Release.VDM_10)
					{
						throwMessage(2262, "Can't have traces in VDM-SL classic");
					}

					list.addAll(readTraces());
					break;

				case THREAD:
					if (dialect == Dialect.VDM_SL)
					{
						throwMessage(2010, "Can't have a thread clause in VDM-SL");
					}

					if (!threadDone)
					{
						threadDone = true;
					}
					else
					{
						throwMessage(2011, "Only one thread clause permitted per class");
					}

					try
    				{
    					nextToken();
        				list.add(readThreadDefinition());

        				if (!newSection())
        				{
        					checkFor(Token.SEMICOLON,
        						2085, "Missing ';' after thread definition");
        				}
    				}
    				catch (LocatedException e)
    				{
    					report(e, afterArray, sectionArray);
    				}
					break;

				case SYNC:
					if (dialect == Dialect.VDM_SL)
					{
						throwMessage(2012, "Can't have a sync clause in VDM-SL");
					}

					list.addAll(readSyncs());
					break;

				case EOF:
					break;

				default:
					try
					{
						throwMessage(2013, "Expected 'operations', 'state', 'functions', 'types' or 'values'");
					}
    				catch (LocatedException e)
    				{
    					report(e, afterArray, sectionArray);
    				}
			}
		}

		return list;
	}

	private ASTAccessSpecifier readAccessSpecifier(boolean asyncOK, boolean pureOK)
		throws LexException, ParserException
	{
		if (dialect == Dialect.VDM_SL)
		{
			if (lastToken().is(Token.PURE))
			{
				if (Settings.release == Release.CLASSIC)
				{
					throwMessage(2325, "Pure operations are not available in classic");
				}
				
				if (pureOK)
				{
					nextToken();
					return new ASTAccessSpecifier(false, false, Token.PRIVATE, true);
				}
				else
				{
					throwMessage(2324, "Pure only permitted for operations");
				}
			}
			else
			{
				return ASTAccessSpecifier.DEFAULT;
			}
		}

		// Keyword counts
		int numStatic = 0;
		int numAsync = 0;
		int numPure = 0;
		int numAccess = 0;

		// Defaults
		boolean isStatic = false;
		boolean isAsync = false;
		boolean isPure = false;
		Token access = Token.PRIVATE;
		
		boolean more = true;

		while (more)
		{
			switch (lastToken().type)
			{
				case ASYNC:
					if (asyncOK)
					{
						if (++numAsync > 1)
						{
							throwMessage(2329, "Duplicate async keyword");
						}
						
						isAsync = true;
						nextToken();
					}
					else
					{
						throwMessage(2278, "Async only permitted for operations");
						more = false;
					}
					break;

				case STATIC:
					if (++numStatic > 1)
					{
						throwMessage(2329, "Duplicate static keyword");
					}
					
					isStatic = true;
					nextToken();
					break;

				case PUBLIC:
				case PRIVATE:
				case PROTECTED:
					if (++numAccess > 1)
					{
						throwMessage(2329, "Duplicate access specifier keyword");
					}

					access = lastToken().type;
					nextToken();
					break;
					
				case PURE:
					if (Settings.release == Release.CLASSIC)
					{
						throwMessage(2325, "Pure operations are not available in classic");
					}

					if (pureOK)
					{
						if (++numPure > 1)
						{
							throwMessage(2329, "Duplicate pure keyword");
						}
						
						isPure = true;
						nextToken();
					}
					else
					{
						throwMessage(2324, "Pure only permitted for operations");
					}
					break;

				default:
					more = false;
					break;
			}
		}

		return new ASTAccessSpecifier(isStatic, isAsync, access, isPure);
	}
	
	public ASTTypeDefinition readTypeDefinition() throws ParserException, LexException
	{
		LexIdentifierToken id = readIdToken("Expecting new type identifier");
		TypeReader tr = getTypeReader();
		ASTInvariantType invtype = null;

		switch (lastToken().type)
		{
			case EQUALS:
				nextToken();
				ASTNamedType nt = new ASTNamedType(idToName(id), tr.readType());

				if (nt.type instanceof ASTUnresolvedType &&
					((ASTUnresolvedType)nt.type).typename.equals(nt.typename))
				{
					throwMessage(2014, "Recursive type declaration");
				}

				invtype = nt;
				break;

			case COLONCOLON:
				nextToken();
				invtype = new ASTRecordType(idToName(id), tr.readFieldList(), false);
				break;

			default:
				throwMessage(2015, "Expecting =<type> or ::<field list>");
		}

		ASTPattern invPattern = null;
		ASTExpression invExpression = null;
		ASTPattern eqPattern1 = null;
		ASTPattern eqPattern2 = null;
		ASTExpression eqExpression = null;
		ASTPattern ordPattern1 = null;
		ASTPattern ordPattern2 = null;
		ASTExpression ordExpression = null;

		while (lastToken().is(Token.INV) || lastToken().is(Token.EQ) || lastToken().is(Token.ORD))
		{
    		switch (lastToken().type)
    		{
    			case INV:
    				if (invPattern != null)
    				{
    					throwMessage(2332, "Duplicate inv clause");
    				}
    				
        			nextToken();
        			invPattern = getPatternReader().readPattern();
        			checkFor(Token.EQUALSEQUALS, 2087, "Expecting '==' after pattern in invariant");
        			invExpression = getExpressionReader().readExpression();
        			break;
        			
    			case EQ:
    				if (Settings.release == Release.CLASSIC)
    				{
    					throwMessage(2333, "Type eq/ord clauses not available in classic");
    				}

    				if (eqPattern1 != null)
    				{
    					throwMessage(2332, "Duplicate eq clause");
    				}
    				
        			nextToken();
        			eqPattern1 = getPatternReader().readPattern();
        			checkFor(Token.EQUALS, 2087, "Expecting '=' between patterns in eq clause");
        			eqPattern2 = getPatternReader().readPattern();
        			checkFor(Token.EQUALSEQUALS, 2087, "Expecting '==' after patterns in eq clause");
        			eqExpression = getExpressionReader().readExpression();
    				break;
    				
    			case ORD:
    				if (Settings.release == Release.CLASSIC)
    				{
    					throwMessage(2333, "Type eq/ord clauses not available in classic");
    				}

    				if (ordPattern1 != null)
    				{
    					throwMessage(2332, "Duplicate ord clause");
    				}
    				
        			nextToken();
        			ordPattern1 = getPatternReader().readPattern();
        			checkFor(Token.LT, 2087, "Expecting '<' between patterns in ord clause");
        			ordPattern2 = getPatternReader().readPattern();
        			checkFor(Token.EQUALSEQUALS, 2087, "Expecting '==' after patterns in ord clause");
        			ordExpression = getExpressionReader().readExpression();
    				break;

    			default:
    				throwMessage(2331, "Expecting inv, eq or ord clause");
    		}
		}

		return new ASTTypeDefinition(idToName(id), invtype, invPattern, invExpression,
			eqPattern1, eqPattern2, eqExpression, ordPattern1, ordPattern2, ordExpression);
	}

	private ASTDefinitionList readTypes() throws LexException, ParserException
	{
		checkFor(Token.TYPES, 2013, "Expected 'types'");
		ASTDefinitionList list = new ASTDefinitionList();

		while (!newSection())
		{
			try
			{
				ASTAnnotationList annotations = readAnnotations();
				annotations.before(this);
				ASTAccessSpecifier access = readAccessSpecifier(false, false);
				ASTTypeDefinition def = readTypeDefinition();
				annotations.after(this);
				def.setAnnotations(annotations);

				// Force all type defs (invs) to be static
				def.setAccessSpecifier(access.getStatic(true));
				list.add(def);

				if (!newSection())
				{
					checkFor(Token.SEMICOLON,
						2078, "Missing ';' after type definition");
				}
			}
			catch (LocatedException e)
			{
				report(e, afterArray, sectionArray);
			}
		}

		return list;
	}

	private ASTDefinitionList readValues() throws LexException, ParserException
	{
		checkFor(Token.VALUES, 2013, "Expected 'values'");
		ASTDefinitionList list = new ASTDefinitionList();

		while (!newSection())
		{
			try
			{
				ASTAnnotationList annotations = readAnnotations();
				annotations.before(this);
				ASTAccessSpecifier access = readAccessSpecifier(false, false);
				ASTDefinition def = readValueDefinition();
				annotations.after(this);
				def.setAnnotations(annotations);

				// Force all values to be static
				def.setAccessSpecifier(access.getStatic(true));
				list.add(def);

				if (!newSection())
				{
					checkFor(Token.SEMICOLON,
						2081, "Missing ';' after value definition");
				}
			}
			catch (LocatedException e)
			{
				report(e, afterArray, sectionArray);
			}
		}

		return list;
	}

	private ASTDefinitionList readFunctions() throws LexException, ParserException
	{
		checkFor(Token.FUNCTIONS, 2013, "Expected 'functions'");
		ASTDefinitionList list = new ASTDefinitionList();

		while (!newSection())
		{
			try
			{
				ASTAnnotationList annotations = readAnnotations();
				annotations.before(this);
				ASTAccessSpecifier access = readAccessSpecifier(false, false);
				ASTDefinition def = readFunctionDefinition();
				annotations.after(this);
				def.setAnnotations(annotations);

				if (Settings.release == Release.VDM_10)
				{
					// Force all functions to be static for VDM-10
					def.setAccessSpecifier(access.getStatic(true));
				}
				else
				{
					def.setAccessSpecifier(access);
				}

				list.add(def);

				if (!newSection())
				{
					checkFor(Token.SEMICOLON,
						2079, "Missing ';' after function definition");
				}
			}
			catch (LocatedException e)
			{
				report(e, afterArray, sectionArray);
			}
		}

		return list;
	}

	public ASTDefinitionList readOperations() throws LexException, ParserException
	{
		checkFor(Token.OPERATIONS, 2013, "Expected 'operations'");
		ASTDefinitionList list = new ASTDefinitionList();

		while (!newSection())
		{
			try
			{
				ASTAnnotationList annotations = readAnnotations();
				annotations.before(this);
				ASTAccessSpecifier access = readAccessSpecifier(dialect == Dialect.VDM_RT, true);
				ASTDefinition def = readOperationDefinition();
				annotations.after(this);
				def.setAccessSpecifier(access);
				def.setAnnotations(annotations);
				list.add(def);

				if (!newSection())
				{
					LexToken end = lastToken();
					
					checkFor(Token.SEMICOLON,
						2082, "Missing ';' after operation definition");
					
					if (lastToken().isNot(Token.IDENTIFIER) && !newSection() && !accessSpecifier())
					{
						throwMessage(2082, "Semi-colon is not allowed here", end);
					}
				}
			}
			catch (LocatedException e)
			{
				report(e, afterArray, sectionArray);
			}
		}

		return list;
	}

	public ASTDefinitionList readInstanceVariables() throws LexException, ParserException
	{
		checkFor(Token.INSTANCE, 2083, "Expected 'instance variables'");
		checkFor(Token.VARIABLES, 2083, "Expecting 'instance variables'");
		ASTDefinitionList list = new ASTDefinitionList();

		while (!newSection())
		{
			try
			{
				ASTAnnotationList annotations = readAnnotations();
				annotations.before(this);
				ASTDefinition def = readInstanceVariableDefinition();
				annotations.after(this);
				def.setAnnotations(annotations);
				list.add(def);

				if (!newSection())
				{
					checkFor(Token.SEMICOLON,
						2084, "Missing ';' after instance variable definition");
				}
			}
			catch (LocatedException e)
			{
				report(e, afterArray, sectionArray);
			}
		}

		return list;
	}

	private ASTDefinitionList readTraces() throws LexException, ParserException
	{
		checkFor(Token.TRACES, 2013, "Expected 'traces'");
		ASTDefinitionList list = new ASTDefinitionList();

		while (!newSection())
		{
			try
			{
				ASTAnnotationList annotations = readAnnotations();
				annotations.before(this);
				ASTDefinition def = readNamedTraceDefinition();
				annotations.after(this);
				def.setAnnotations(annotations);
				list.add(def);

				if (!newSection())
				{
					ignore(Token.SEMICOLON);	// Optional?
				}
			}
			catch (LocatedException e)
			{
				report(e, afterArray, sectionArray);
			}
		}

		return list;
	}

	private ASTDefinitionList readSyncs() throws LexException, ParserException
	{
		checkFor(Token.SYNC, 2013, "Expected 'sync'");
		ASTDefinitionList list = new ASTDefinitionList();

		while (!newSection())
		{
			try
			{
				ASTAnnotationList annotations = readAnnotations();
				annotations.before(this);
				ASTDefinition def = readPermissionPredicateDefinition();
				annotations.after(this);
				list.add(def);

				if (!newSection())
				{
					checkFor(Token.SEMICOLON,
						2086, "Missing ';' after sync definition");
				}
			}
			catch (LocatedException e)
			{
				report(e, afterArray, sectionArray);
			}
		}

		return list;
	}

	public LexNameList readTypeParams() throws LexException, ParserException
	{
		LexNameList typeParams = null;

		if (lastToken().is(Token.SEQ_OPEN))
		{
			typeParams = new LexNameList();
			nextToken();
			checkFor(Token.AT, 2088, "Expecting '@' before type parameter");
			LexIdentifierToken tid = readIdToken("Expecting '@identifier' in type parameter list");
			typeParams.add(idToName(tid));

			while (ignore(Token.COMMA))
			{
				checkFor(Token.AT, 2089, "Expecting '@' before type parameter");
				tid = readIdToken("Expecting '@identifier' in type parameter list");
				typeParams.add(idToName(tid));
			}

			checkFor(Token.SEQ_CLOSE, 2090, "Expecting ']' after type parameters");
		}

		return typeParams;
	}
	
	private void verifyName(String name) throws ParserException, LexException
	{
		if (name.startsWith("mk_"))
		{
			throwMessage(2016, "Name cannot start with 'mk_'");
		}
		else if (name.equals("mu"))
		{
			throwMessage(2016, "Name cannot be 'mu' (reserved)");
		}
		else if (name.equals("narrow_"))
		{
			throwMessage(2016, "Name cannot be 'narrow_' (reserved)");
		}
	}

	private ASTDefinition readFunctionDefinition() throws ParserException, LexException
	{
		ASTDefinition def = null;
		LexIdentifierToken funcName = readIdToken("Expecting new function identifier");
		verifyName(funcName.name);

		LexNameList typeParams = readTypeParams();

		if (lastToken().is(Token.COLON))
		{
			def = readExplicitFunctionDefinition(funcName, typeParams);
		}
		else if (lastToken().is(Token.BRA))
		{
			def = readImplicitFunctionDefinition(funcName, typeParams);
		}
		else
		{
			throwMessage(2017, "Expecting ':' or '(' after name in function definition");
		}

		LexLocation.addSpan(idToName(funcName), lastToken());
		return def;
	}

	private ASTDefinition readExplicitFunctionDefinition(LexIdentifierToken funcName, LexNameList typeParams)
		throws ParserException, LexException
	{
		// Explicit function definition, like "f: int->bool f(x) == true"

		nextToken();
		ASTType t = getTypeReader().readType();

		if (!(t instanceof ASTFunctionType))
		{
			throwMessage(2018, "Function type is not a -> or +> function");
		}

		ASTFunctionType type = (ASTFunctionType)t;

		LexIdentifierToken name =
			readIdToken("Expecting identifier after function type in definition");

		if (!name.equals(funcName))
		{
			throwMessage(2019, "Expecting identifier " + funcName.name + " after type in definition");
		}

		if (lastToken().isNot(Token.BRA))
		{
			throwMessage(2020, "Expecting '(' after function name");
		}

		ASTPatternListList parameters = new ASTPatternListList();

		while (lastToken().is(Token.BRA))
		{
			if (nextToken().isNot(Token.KET))
			{
    			parameters.add(getPatternReader().readPatternList());
    			checkFor(Token.KET, 2091, "Expecting ')' after function parameters");
    		}
    		else
    		{
    			parameters.add(new ASTPatternList());	// empty "()"
    			nextToken();
    		}
		}

		checkFor(Token.EQUALSEQUALS, 2092, "Expecting '==' after parameters");
		ExpressionReader expr = getExpressionReader();
		ASTExpression body = readFunctionBody();
		ASTExpression precondition = null;
		ASTExpression postcondition = null;
		ASTExpression measure = null;

		if (lastToken().is(Token.PRE))
		{
			nextToken();
			precondition = expr.readExpression();
		}

		if (lastToken().is(Token.POST))
		{
			nextToken();
			postcondition = expr.readExpression();
		}

		if (lastToken().is(Token.MEASURE))
		{
			nextToken();
			
			if (lastToken().is(Token.IS))
			{
				nextToken();
				checkFor(Token.NOT, 2125, "Expecting 'is not yet specified'");
				checkFor(Token.YET, 2125, "Expecting 'is not yet specified'");
				checkFor(Token.SPECIFIED, 2126, "Expecting 'is not yet specified'");
				measure = new ASTNotYetSpecifiedExpression(lastToken().location);
			}
			else
			{
				measure = getExpressionReader().readExpression();
			}
		}

		return new ASTExplicitFunctionDefinition(
			idToName(funcName), typeParams, type,
			parameters, body, precondition, postcondition, false,
			measure);
	}

	private ASTDefinition readImplicitFunctionDefinition(LexIdentifierToken funcName, LexNameList typeParams)
		throws ParserException, LexException
	{
		// Implicit, like g(x: int) y: bool pre exp post exp

		nextToken();

		PatternReader pr = getPatternReader();
		TypeReader tr = getTypeReader();
		ASTPatternListTypePairList parameterPatterns = new ASTPatternListTypePairList();

		if (lastToken().isNot(Token.KET))
		{
			ASTPatternList pl = pr.readPatternList();
			checkFor(Token.COLON, 2093, "Missing colon after pattern/type parameter");
			parameterPatterns.add(new ASTPatternListTypePair(pl, tr.readType()));

			while (ignore(Token.COMMA))
			{
				pl = pr.readPatternList();
				checkFor(Token.COLON, 2093, "Missing colon after pattern/type parameter");
				parameterPatterns.add(new ASTPatternListTypePair(pl, tr.readType()));
			}
		}

    	checkFor(Token.KET, 2124, "Expecting ')' after parameters");

		LexToken firstResult = lastToken();
   		ASTPatternList resultNames = new ASTPatternList();
   		ASTTypeList resultTypes = new ASTTypeList();

   		do
   		{
   			LexIdentifierToken rname = readIdToken("Expecting result identifier");
   	   		resultNames.add(new ASTIdentifierPattern(idToName(rname)));
   	   		checkFor(Token.COLON, 2094, "Missing colon in identifier/type return value");
   	   		resultTypes.add(tr.readType());
   		}
   		while (ignore(Token.COMMA));

   		if (lastToken().is(Token.IDENTIFIER))
		{
			throwMessage(2261, "Missing comma between return types?");
		}

   		ASTPatternTypePair resultPattern = null;

   		if (resultNames.size() > 1)
   		{
   			resultPattern = new ASTPatternTypePair(
   	   			new ASTTuplePattern(firstResult.location, resultNames),
 	   			new ASTProductType(firstResult.location, resultTypes));
   		}
   		else
   		{
   			resultPattern = new ASTPatternTypePair(
   	   			resultNames.get(0), resultTypes.get(0));
   		}

		ExpressionReader expr = getExpressionReader();
		ASTExpression body = null;
		ASTExpression precondition = null;
		ASTExpression postcondition = null;
		ASTExpression measure = null;

		if (lastToken().is(Token.EQUALSEQUALS))		// extended implicit function
		{
			nextToken();
			body = readFunctionBody();
		}

		if (lastToken().is(Token.PRE))
		{
			nextToken();
			precondition = expr.readExpression();
		}

		if (body == null)	// Mandatory for standard implicit functions
		{
			checkFor(Token.POST, 2095, "Implicit function must have post condition");
			postcondition = expr.readExpression();
		}
		else
		{
			if (lastToken().is(Token.POST))
			{
				nextToken();
				postcondition = expr.readExpression();
			}
		}

		if (lastToken().is(Token.MEASURE))
		{
			nextToken();
			
			if (lastToken().is(Token.IS))
			{
				nextToken();
				checkFor(Token.NOT, 2125, "Expecting 'is not yet specified'");
				checkFor(Token.YET, 2125, "Expecting 'is not yet specified'");
				checkFor(Token.SPECIFIED, 2126, "Expecting 'is not yet specified'");
				measure = new ASTNotYetSpecifiedExpression(lastToken().location);
			}
			else
			{
				measure = getExpressionReader().readExpression();
			}
		}

		return new ASTImplicitFunctionDefinition(
			idToName(funcName), typeParams, parameterPatterns, resultPattern, body,
			precondition, postcondition, measure);
	}

	public ASTDefinition readLocalDefinition() throws ParserException, LexException
	{
		ParserException funcDefError = null;

    	try
    	{
        	reader.push();
        	ASTDefinition def = readFunctionDefinition();
    		reader.unpush();
    		return def;
    	}
    	catch (ParserException e)		// Not a function then...
    	{
			e.adjustDepth(reader.getTokensRead());
    		reader.pop();
    		funcDefError = e;
    	}

		try
		{
        	reader.push();
        	ASTDefinition def = readValueDefinition();
    		reader.unpush();
    		return def;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
    		reader.pop();
			throw e.deeperThan(funcDefError) ? e : funcDefError;
		}
	}

	public ASTDefinition readValueDefinition() throws ParserException, LexException
	{
       	// Should be <pattern>[:<type>]=<expression>

    	ASTPattern p = getPatternReader().readPattern();
    	ASTType type = null;

    	if (lastToken().is(Token.COLON))
    	{
    		nextToken();
    		type = getTypeReader().readType();
    	}

 		checkFor(Token.EQUALS, 2096, "Expecting <pattern>[:<type>]=<exp>");
		return new ASTValueDefinition(
			p, type, getExpressionReader().readExpression());
	}

	private ASTDefinition readStateDefinition() throws ParserException, LexException
	{
		LexIdentifierToken name = readIdToken("Expecting identifier after 'state' definition");
		checkFor(Token.OF, 2097, "Expecting 'of' after state name");
		ASTFieldList fieldList = getTypeReader().readFieldList();

		ASTExpression invExpression = null;
		ASTExpression initExpression = null;
		ASTPattern invPattern = null;
		ASTPattern initPattern = null;

		if (lastToken().is(Token.INV))
		{
			nextToken();
			invPattern = getPatternReader().readPattern();
			checkFor(Token.EQUALSEQUALS, 2098, "Expecting '==' after pattern in invariant");
			invExpression = getExpressionReader().readExpression();
		}

		if (lastToken().is(Token.INIT))
		{
			nextToken();
			initPattern = getPatternReader().readPattern();
			checkFor(Token.EQUALSEQUALS, 2099, "Expecting '==' after pattern in initializer");
			initExpression = getExpressionReader().readExpression();
		}

		// Be forgiving about the inv/init order
		if (lastToken().is(Token.INV) && invExpression == null)
		{
			nextToken();
			invPattern = getPatternReader().readPattern();
			checkFor(Token.EQUALSEQUALS, 2098, "Expecting '==' after pattern in invariant");
			invExpression = getExpressionReader().readExpression();
		}

		checkFor(Token.END, 2100, "Expecting 'end' after state definition");
		return new ASTStateDefinition(idToName(name), fieldList,
			invPattern, invExpression, initPattern, initExpression);
	}

	private ASTDefinition readOperationDefinition()
		throws ParserException, LexException
	{
		ASTDefinition def = null;
		LexIdentifierToken opName = readIdToken("Expecting new operation identifier");
		verifyName(opName.name);

		if (lastToken().is(Token.COLON))
		{
			def = readExplicitOperationDefinition(opName);
		}
		else if (lastToken().is(Token.BRA))
		{
			def = readImplicitOperationDefinition(opName);
		}
		else if (lastToken().is(Token.SEQ_OPEN))
		{
			throwMessage(2059, "Operations cannot have [@T] type parameters");
		}
		else
		{
			throwMessage(2021, "Expecting ':' or '(' after name in operation definition");
		}

		LexLocation.addSpan(idToName(opName), lastToken());
		return def;
	}

	private ASTDefinition readExplicitOperationDefinition(LexIdentifierToken opName)
		throws ParserException, LexException
	{
		// Like "f: int ==> bool f(x) == <statement>"

		nextToken();
		ASTOperationType type = getTypeReader().readOperationType();

		LexIdentifierToken name =
			readIdToken("Expecting operation identifier after type in definition");

		if (!name.equals(opName))
		{
			throwMessage(2022, "Expecting name " + opName.name + " after type in definition");
		}

		if (lastToken().isNot(Token.BRA))
		{
			throwMessage(2023, "Expecting '(' after operation name");
		}

		ASTPatternList parameters = null;

		if (nextToken().isNot(Token.KET))
		{
			parameters = getPatternReader().readPatternList();
			checkFor(Token.KET, 2101, "Expecting ')' after operation parameters");
		}
		else
		{
			parameters = new ASTPatternList();		// empty "()"
			nextToken();
		}

		checkFor(Token.EQUALSEQUALS, 2102, "Expecting '==' after parameters");
		ASTStatement body = readOperationBody();
		ASTExpression precondition = null;
		ASTExpression postcondition = null;

		if (lastToken().is(Token.PRE))
		{
			nextToken();
			precondition = getExpressionReader().readExpression();
		}

		if (lastToken().is(Token.POST))
		{
			nextToken();
			postcondition = getExpressionReader().readExpression();
		}

		ASTExplicitOperationDefinition def = new ASTExplicitOperationDefinition(
			idToName(opName), type,
			parameters, precondition, postcondition, body);

		return def;
	}

	private ASTDefinition readImplicitOperationDefinition(LexIdentifierToken opName)
		throws ParserException, LexException
	{
		// Like g(x: int) [y: bool]? ext rd fred[:int] pre exp post exp

		nextToken();
		PatternReader pr = getPatternReader();
		TypeReader tr = getTypeReader();
		ASTPatternListTypePairList parameterPatterns = new ASTPatternListTypePairList();

		if (lastToken().isNot(Token.KET))
		{
			ASTPatternList pl = pr.readPatternList();
			checkFor(Token.COLON, 2103, "Missing colon after pattern/type parameter");
			parameterPatterns.add(new ASTPatternListTypePair(pl, tr.readType()));

			while (ignore(Token.COMMA))
			{
				pl = pr.readPatternList();
				checkFor(Token.COLON, 2103, "Missing colon after pattern/type parameter");
				parameterPatterns.add(new ASTPatternListTypePair(pl, tr.readType()));
			}
		}

    	checkFor(Token.KET, 2124, "Expecting ')' after args");

		LexToken firstResult = lastToken();
   		ASTPatternTypePair resultPattern = null;

		if (firstResult.is(Token.IDENTIFIER))
		{
			ASTPatternList resultNames = new ASTPatternList();
			ASTTypeList resultTypes = new ASTTypeList();

			do
			{
				LexIdentifierToken rname = readIdToken("Expecting result identifier");
				resultNames.add(new ASTIdentifierPattern(idToName(rname)));
				checkFor(Token.COLON, 2104, "Missing colon in identifier/type return value");
				resultTypes.add(tr.readType());
			}
			while (ignore(Token.COMMA));

			if (lastToken().is(Token.IDENTIFIER))
			{
				throwMessage(2261, "Missing comma between return types?");
			}

			if (resultNames.size() > 1)
			{
				resultPattern = new ASTPatternTypePair(
					new ASTTuplePattern(firstResult.location, resultNames),
					new ASTProductType(firstResult.location, resultTypes));
			}
			else
			{
				resultPattern = new ASTPatternTypePair(
					resultNames.get(0), resultTypes.get(0));
			}
		}

		ASTStatement body = null;

		if (lastToken().is(Token.EQUALSEQUALS))		// extended implicit operation
		{
			nextToken();
			body = readOperationBody();
		}

		ASTSpecificationStatement spec = readSpecification(opName.location, body == null);

		ASTImplicitOperationDefinition def = new ASTImplicitOperationDefinition(
			idToName(opName), parameterPatterns, resultPattern, body, spec);

		return def;
	}

	public ASTSpecificationStatement readSpecification(
		LexLocation location, boolean postMandatory)
		throws ParserException, LexException
	{
		ASTExternalClauseList externals = null;

		if (lastToken().is(Token.EXTERNAL))
		{
			externals = new ASTExternalClauseList();
			nextToken();

			while (lastToken().is(Token.READ) || lastToken().is(Token.WRITE))
			{
				externals.add(readExternal());
			}

			if (externals.isEmpty())
			{
				throwMessage(2024, "Expecting external declarations after 'ext'");
			}
		}

		ExpressionReader expr = getExpressionReader();
		ASTExpression precondition = null;
		ASTExpression postcondition = null;

		if (lastToken().is(Token.PRE))
		{
			nextToken();
			precondition = expr.readExpression();
		}

		if (postMandatory)	// Mandatory for standard implicit operations
		{
			checkFor(Token.POST, 2105, "Implicit operation must define a post condition");
			postcondition = expr.readExpression();
		}
		else
		{
			if (lastToken().is(Token.POST))
			{
				nextToken();
				postcondition = expr.readExpression();
			}
		}

		ASTErrorCaseList errors = null;

		if (lastToken().is(Token.ERRS))
		{
			errors = new ASTErrorCaseList();
			nextToken();

			while (lastToken() instanceof LexIdentifierToken)
			{
				LexIdentifierToken name = readIdToken("Expecting error identifier");
				checkFor(Token.COLON, 2106, "Expecting ':' after name in errs clause");
				ASTExpression left = expr.readExpression();
				checkFor(Token.ARROW, 2107, "Expecting '->' in errs clause");
				ASTExpression right = expr.readExpression();
				errors.add(new ASTErrorCase(name, left, right));
			}

			if (errors.isEmpty())
			{
				throwMessage(2025, "Expecting <name>: exp->exp in errs clause");
			}
		}

		return new ASTSpecificationStatement(location,
						externals, precondition, postcondition, errors);
	}

	private ASTExternalClause readExternal() throws ParserException, LexException
	{
		LexToken mode = lastToken();

		if (mode.isNot(Token.READ) && mode.isNot(Token.WRITE))
		{
			throwMessage(2026, "Expecting 'rd' or 'wr' after 'ext'");
		}

		LexNameList names = new LexNameList();
		nextToken();
		names.add(readNameToken("Expecting name in external clause"));

		while (ignore(Token.COMMA))
		{
			names.add(readNameToken("Expecting name in external clause"));
		}

		ASTType type = null;

		if (lastToken().is(Token.COLON))
		{
			nextToken();
			type = getTypeReader().readType();
		}

		return new ASTExternalClause(mode, names, type);
	}

	public ASTEqualsDefinition readEqualsDefinition()
		throws ParserException, LexException
	{
       	// The grammar here says the form of the definition should be
		// "def" <patternBind>=<expression> "in" <expression>, but since
		// a set bind is "s in set S" that naively parses as
		// "s in set (S = <expression>)". Talking to PGL, we have to
		// make a special parse here. It is one of these forms:
		//
		//	"def" <pattern> "=" <expression> "in" ...
		//	"def" <type bind> "=" <expression> "in" ...
		//	"def" <pattern> "in set" <equals-expression> "in" ...
		//	"def" <pattern> "in seq" <equals-expression> "in" ...
		//
		// and the "=" is unpicked from the left and right of the equals
		// expression in the last two cases.

		LexLocation location = lastToken().location;
		ParserException equalsDefError = null;

    	try	// "def" <pattern> "=" <expression> "in" ...
    	{
        	reader.push();
    		ASTPattern pattern = getPatternReader().readPattern();
     		checkFor(Token.EQUALS, 2108, "Expecting <pattern>=<exp>");
     		ASTExpression test = getExpressionReader().readExpression();
    		reader.unpush();

     		return new ASTEqualsDefinition(location, pattern, test);
    	}
    	catch (ParserException e)
    	{
			e.adjustDepth(reader.getTokensRead());
    		reader.pop();
    		equalsDefError = e;
    	}

		try	// "def" <type bind> "=" <expression> "in" ...
		{
        	reader.push();
    		ASTTypeBind typebind = getBindReader().readTypeBind();
     		checkFor(Token.EQUALS, 2109, "Expecting <type bind>=<exp>");
     		ASTExpression test = getExpressionReader().readExpression();
    		reader.unpush();

     		return new ASTEqualsDefinition(location, typebind, test);
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
    		reader.pop();
			equalsDefError = e.deeperThan(equalsDefError) ? e : equalsDefError;
		}

		try
		{
        	reader.push();
    		ASTPattern pattern = getPatternReader().readPattern();
     		checkFor(Token.IN, 2110, "Expecting <pattern> in set|seq <exp>");
     		ASTBind bind = null;
     		ASTEqualsExpression test = null;
     		
     		switch (lastToken().type)
     		{
     			case SET:			// "def" <pattern> "in set" <equals-expression> "in" ...
     				nextToken();
             		test = getExpressionReader().readDefEqualsExpression();
             		bind = new ASTSetBind(pattern, test.left);
             		reader.unpush();
             		return new ASTEqualsDefinition(location, bind, test.right);
             		
     			case SEQ:			// "def" <pattern> "in seq" <equals-expression> "in" ...
					if (Settings.release == Release.CLASSIC)
					{
						throwMessage(2328, "Sequence binds are not available in classic");
					}

					nextToken();
             		test = getExpressionReader().readDefEqualsExpression();
             		bind = new ASTSeqBind(pattern, test.left);
             		reader.unpush();
             		return new ASTEqualsDefinition(location, bind, test.right);
             		
     			default:
     				throwMessage(2111, "Expecting <pattern> in set|seq <exp>");
     				return null;
     		}
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
    		reader.pop();
			throw e.deeperThan(equalsDefError) ? e : equalsDefError;
		}
 	}

	private ASTDefinition readInstanceVariableDefinition()
		throws ParserException, LexException
    {
		LexToken token = lastToken();

		if (token.is(Token.INV))
		{
			nextToken();
			ASTExpression exp = getExpressionReader().readExpression();
			String str = getCurrentModule();
			LexNameToken className = new LexNameToken(str, str, token.location);
			return new ASTClassInvariantDefinition(
				className.getInvName(token.location), exp);
		}
		else
		{
			ASTAnnotationList annotations = readAnnotations();
			ASTAccessSpecifier access = readAccessSpecifier(false, false);
			ASTAssignmentDefinition def = getStatementReader().readAssignmentDefinition();
			ASTInstanceVariableDefinition ivd =
				new ASTInstanceVariableDefinition(def.name, def.type, def.expression);
			ivd.setAccessSpecifier(access);
			ivd.setAnnotations(annotations);
			return ivd;
		}
    }

	private ASTDefinition readThreadDefinition() throws LexException, ParserException
	{
		LexToken token = lastToken();

		if (token.is(Token.PERIODIC))
		{
			if (dialect != Dialect.VDM_RT)
			{
				throwMessage(2316, "Periodic threads only available in VDM-RT");
			}

			nextToken();
			checkFor(Token.BRA, 2112, "Expecting '(' after periodic");
			ASTExpressionList args = getExpressionReader().readExpressionList();
			checkFor(Token.KET, 2113, "Expecting ')' after periodic arguments");
			checkFor(Token.BRA, 2114, "Expecting '(' after periodic(...)");
			LexNameToken name = readNameToken("Expecting (name) after periodic(...)");
			checkFor(Token.KET, 2115, "Expecting (name) after periodic(...)");

			return new ASTThreadDefinition(new ASTPeriodicStatement(name, args));
		}
		else if (token.is(Token.SPORADIC))
		{
			if (dialect != Dialect.VDM_RT)
			{
				throwMessage(2317, "Sporadic threads only available in VDM-RT");
			}

			nextToken();
			checkFor(Token.BRA, 2312, "Expecting '(' after sporadic");
			ASTExpressionList args = getExpressionReader().readExpressionList();
			checkFor(Token.KET, 2313, "Expecting ')' after sporadic arguments");
			checkFor(Token.BRA, 2314, "Expecting '(' after sporadic(...)");
			LexNameToken name = readNameToken("Expecting (name) after sporadic(...)");
			checkFor(Token.KET, 2315, "Expecting (name) after sporadic(...)");

			return new ASTThreadDefinition(new ASTSporadicStatement(name, args));
		}
		else
		{
			ASTStatement stmt = getStatementReader().readStatement();
			return new ASTThreadDefinition(stmt);
		}
	}

	private ASTDefinition readPermissionPredicateDefinition()
		throws LexException, ParserException
	{
		LexToken token = lastToken();

		switch (token.type)
		{
			case PER:
				nextToken();
				LexNameToken name = readNameToken("Expecting name after 'per'");
				checkFor(Token.IMPLIES, 2116, "Expecting <name> => <exp>");
				ASTExpression exp = getExpressionReader().readPerExpression();
				return new ASTPerSyncDefinition(token.location, name, exp);

			case MUTEX:
				nextToken();
				checkFor(Token.BRA, 2117, "Expecting '(' after mutex");
				LexNameList opnames = new LexNameList();

				switch (lastToken().type)
				{
					case ALL:
						nextToken();
						checkFor(Token.KET, 2118, "Expecting ')' after 'all'");
						break;

					default:
						LexNameToken op = readNameToken("Expecting a name");
						opnames.add(op);

						while (ignore(Token.COMMA))
						{
							op = readNameToken("Expecting a name");
							opnames.add(op);
						}

						checkFor(Token.KET, 2119, "Expecting ')'");
						break;
				}

				return new ASTMutexSyncDefinition(token.location, opnames);

			default:
				throwMessage(2028, "Expecting 'per' or 'mutex'");
				return null;
		}
	}

	private ASTDefinition readNamedTraceDefinition()
		throws ParserException, LexException
	{
		LexLocation start = lastToken().location;
		List<String> names = readTraceIdentifierList();
		checkFor(Token.COLON, 2264, "Expecting ':' after trace name(s)");
		ASTTraceDefinitionTermList traces = readTraceDefinitionList();

		return new ASTNamedTraceDefinition(start, names, traces);
	}

	private List<String> readTraceIdentifierList()
		throws ParserException, LexException
	{
		List<String> names = new Vector<String>();
		names.add(readIdToken("Expecting trace identifier").name);

		while (lastToken().is(Token.DIVIDE))
		{
			nextToken();
			names.add(readIdToken("Expecting trace identifier").name);
		}

		return names;
	}

	private ASTTraceDefinitionTermList readTraceDefinitionList()
		throws LexException, ParserException
	{
		ASTTraceDefinitionTermList list = new ASTTraceDefinitionTermList();
		list.add(readTraceDefinitionTerm());

		while (lastToken().is(Token.SEMICOLON))
		{
			try
			{
				reader.push();
				nextToken();
				list.add(readTraceDefinitionTerm());
				reader.unpush();
			}
	    	catch (ParserException e)
	    	{
	    		reader.pop();
				break;
	    	}
		}

		return list;
	}

	private ASTTraceDefinitionTerm readTraceDefinitionTerm()
		throws LexException, ParserException
	{
		ASTTraceDefinitionTerm term = new ASTTraceDefinitionTerm();
		term.add(readTraceDefinition());

		while (lastToken().is(Token.PIPE))
		{
			nextToken();
			term.add(readTraceDefinition());
		}

		return term;
	}

	private ASTTraceDefinition readTraceDefinition()
		throws LexException, ParserException
	{
		if (lastToken().is(Token.LET))
		{
			return readTraceBinding();
		}
		else
		{
			return readTraceRepeat();
		}
	}

	private ASTTraceDefinition readTraceRepeat()
		throws ParserException, LexException
	{
       	ASTTraceCoreDefinition core = readCoreTraceDefinition();

       	long from = 1;
       	long to = 1;
       	LexToken token = lastToken();

       	switch (token.type)
		{
			case TIMES:
				from = 0;
				to = Properties.traces_max_repeats;
				nextToken();
				break;

			case PLUS:
				from = 1;
				to = Properties.traces_max_repeats;
				nextToken();
				break;

			case QMARK:
				from = 0;
				to = 1;
				nextToken();
				break;

			case SET_OPEN:
				if (nextToken().isNot(Token.NUMBER))
				{
					throwMessage(2266, "Expecting '{n}' or '{n1, n2}' after trace definition");
				}

				LexIntegerToken lit = (LexIntegerToken)lastToken();
				from = lit.value;
				to = lit.value;

				switch (nextToken().type)
				{
					case COMMA:
						if (nextToken().isNot(Token.NUMBER))
						{
							throwMessage(2265, "Expecting '{n1, n2}' after trace definition");
						}

						lit = (LexIntegerToken)readToken();
						to = lit.value;
						checkFor(Token.SET_CLOSE, 2265, "Expecting '{n1, n2}' after trace definition");
						break;

					case SET_CLOSE:
						nextToken();
						break;

					default:
						throwMessage(2266, "Expecting '{n}' or '{n1, n2}' after trace definition");
				}
				break;
				
			default:
				break;
		}

       	return new ASTTraceRepeatDefinition(token.location, core, from, to);
	}

	private ASTTraceDefinition readTraceBinding()
		throws ParserException, LexException
	{
		checkFor(Token.LET, 2230, "Expecting 'let'");
		ParserException letDefError = null;

		try
		{
			reader.push();
			ASTTraceDefinition def = readLetDefBinding();
			reader.unpush();
			return def;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
			reader.pop();
			letDefError = e;
		}

		try
		{
			reader.push();
			ASTTraceDefinition def = readLetBeStBinding();
			reader.unpush();
			return def;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
			reader.pop();
			throw e.deeperThan(letDefError) ? e : letDefError;
		}
	}

	private ASTTraceDefinition readLetDefBinding()
		throws ParserException, LexException
	{
		ASTDefinitionList localDefs = new ASTDefinitionList();
		LexToken start = lastToken();

		ASTDefinition def = readLocalDefinition();

		if (!(def instanceof ASTValueDefinition))
		{
			throwMessage(2270, "Only value definitions allowed in traces");
		}

		localDefs.add((ASTValueDefinition)def);

		while (ignore(Token.COMMA))
		{
			def = readLocalDefinition();

			if (!(def instanceof ASTValueDefinition))
			{
				throwMessage(2270, "Only value definitions allowed in traces");
			}

			localDefs.add((ASTValueDefinition)def);
		}

		checkFor(Token.IN, 2231, "Expecting 'in' after local definitions");
		ASTTraceDefinition body = readTraceDefinition();

		return new ASTTraceLetDefBinding(start.location, localDefs, body);
	}

	private ASTTraceDefinition readLetBeStBinding()
		throws ParserException, LexException
	{
		LexToken start = lastToken();
		ASTMultipleBind bind = getBindReader().readMultipleBind();
		ASTExpression stexp = null;

		if (lastToken().is(Token.BE))
		{
			nextToken();
			checkFor(Token.ST, 2232, "Expecting 'st' after 'be' in let statement");
			stexp = getExpressionReader().readExpression();
		}

		checkFor(Token.IN, 2233, "Expecting 'in' after bind in let statement");
		ASTTraceDefinition body = readTraceDefinition();

		return new ASTTraceLetBeStBinding(start.location, bind, stexp, body);
	}

	private ASTTraceCoreDefinition readCoreTraceDefinition()
		throws ParserException, LexException
	{
		LexToken token = lastToken();

		switch (token.type)
		{
			case IDENTIFIER:
			case NAME:
			case SELF:
				StatementReader sr = getStatementReader();
				ASTStatement stmt = sr.readCallStatement();

				if (!(stmt instanceof ASTCallStatement) &&
					!(stmt instanceof ASTCallObjectStatement))
				{
					throwMessage(2267,
						"Expecting 'obj.op(args)' or 'op(args)'", token);
				}

				return new ASTTraceApplyExpression(stmt);

			case BRA:
				nextToken();
				ASTTraceDefinitionTermList list = readTraceDefinitionList();
				checkFor(Token.KET, 2269, "Expecting '(trace definitions)'");
				return new ASTTraceBracketedExpression(token.location, list);

			case PIPEPIPE:
				nextToken();
				checkFor(Token.BRA, 2292, "Expecting '|| (...)'");
				ASTTraceDefinitionList defs = new ASTTraceDefinitionList();
				defs.add(readTraceDefinition());
				checkFor(Token.COMMA, 2293, "Expecting '|| (a, b {,...})'");
				defs.add(readTraceDefinition());

				while (lastToken().is(Token.COMMA))
				{
					nextToken();
					defs.add(readTraceDefinition());
				}

				checkFor(Token.KET, 2294, "Expecting ')' ending || clause");
				return new ASTTraceConcurrentExpression(token.location, defs);

			default:
				throwMessage(2267, "Expecting 'obj.op(args)' or 'op(args)'", token);
				return null;
		}
	}
	
	private ASTExpression readFunctionBody() throws LexException, ParserException
	{
		LexToken token = lastToken();

		if (token.is(Token.IS))
		{
			switch (nextToken().type)
			{
				case NOT:
					nextToken();
					checkFor(Token.YET, 2125, "Expecting 'is not yet specified'");
					checkFor(Token.SPECIFIED, 2126, "Expecting 'is not yet specified'");
					return new ASTNotYetSpecifiedExpression(token.location);
	
				case SUBCLASS:
					nextToken();
					checkFor(Token.RESPONSIBILITY, 2127, "Expecting 'is subclass responsibility'");
					return new ASTSubclassResponsibilityExpression(token.location);

				default:
					if (dialect == Dialect.VDM_PP)
					{
						throwMessage(2033, "Expecting 'is not yet specified' or 'is subclass responsibility'", token);
					}
					else
					{
						throwMessage(2033, "Expecting 'is not yet specified'", token);
					}
					return null;
			}
		}
		else
		{
			ExpressionReader expr = getExpressionReader();
			return expr.readExpression();
		}
	}
	
	private ASTStatement readOperationBody() throws LexException, ParserException
	{
		LexToken token = lastToken();

		if (token.is(Token.IS))
		{
			switch (nextToken().type)
			{
				case NOT:
					nextToken();
					checkFor(Token.YET, 2187, "Expecting 'is not yet specified");
					checkFor(Token.SPECIFIED, 2188, "Expecting 'is not yet specified");
					return new ASTNotYetSpecifiedStatement(token.location);

				case SUBCLASS:
					nextToken();
					checkFor(Token.RESPONSIBILITY, 2189, "Expecting 'is subclass responsibility'");
					return new ASTSubclassResponsibilityStatement(token.location);

				default:
					if (dialect == Dialect.VDM_PP)
					{
						throwMessage(2062, "Expecting 'is not yet specified' or 'is subclass responsibility'", token);
					}
					else
					{
						throwMessage(2062, "Expecting 'is not yet specified'", token);
					}
					return null;
			}
		}
		else
		{
			StatementReader stmt = getStatementReader();
			return stmt.readStatement();
		}
	}
}
