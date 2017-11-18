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

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTAssignmentDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.expressions.ASTUndefinedExpression;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPatternBind;
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.statements.ASTAlwaysStatement;
import com.fujitsu.vdmj.ast.statements.ASTAssignmentStatement;
import com.fujitsu.vdmj.ast.statements.ASTAssignmentStatementList;
import com.fujitsu.vdmj.ast.statements.ASTAtomicStatement;
import com.fujitsu.vdmj.ast.statements.ASTBlockStatement;
import com.fujitsu.vdmj.ast.statements.ASTCallObjectStatement;
import com.fujitsu.vdmj.ast.statements.ASTCallStatement;
import com.fujitsu.vdmj.ast.statements.ASTCaseStmtAlternative;
import com.fujitsu.vdmj.ast.statements.ASTCaseStmtAlternativeList;
import com.fujitsu.vdmj.ast.statements.ASTCasesStatement;
import com.fujitsu.vdmj.ast.statements.ASTCyclesStatement;
import com.fujitsu.vdmj.ast.statements.ASTDefStatement;
import com.fujitsu.vdmj.ast.statements.ASTDurationStatement;
import com.fujitsu.vdmj.ast.statements.ASTElseIfStatement;
import com.fujitsu.vdmj.ast.statements.ASTElseIfStatementList;
import com.fujitsu.vdmj.ast.statements.ASTErrorStatement;
import com.fujitsu.vdmj.ast.statements.ASTExitStatement;
import com.fujitsu.vdmj.ast.statements.ASTFieldDesignator;
import com.fujitsu.vdmj.ast.statements.ASTForAllStatement;
import com.fujitsu.vdmj.ast.statements.ASTForIndexStatement;
import com.fujitsu.vdmj.ast.statements.ASTForPatternBindStatement;
import com.fujitsu.vdmj.ast.statements.ASTIdentifierDesignator;
import com.fujitsu.vdmj.ast.statements.ASTIfStatement;
import com.fujitsu.vdmj.ast.statements.ASTLetBeStStatement;
import com.fujitsu.vdmj.ast.statements.ASTLetDefStatement;
import com.fujitsu.vdmj.ast.statements.ASTMapSeqDesignator;
import com.fujitsu.vdmj.ast.statements.ASTNonDeterministicStatement;
import com.fujitsu.vdmj.ast.statements.ASTObjectApplyDesignator;
import com.fujitsu.vdmj.ast.statements.ASTObjectDesignator;
import com.fujitsu.vdmj.ast.statements.ASTObjectFieldDesignator;
import com.fujitsu.vdmj.ast.statements.ASTObjectIdentifierDesignator;
import com.fujitsu.vdmj.ast.statements.ASTObjectNewDesignator;
import com.fujitsu.vdmj.ast.statements.ASTObjectSelfDesignator;
import com.fujitsu.vdmj.ast.statements.ASTReturnStatement;
import com.fujitsu.vdmj.ast.statements.ASTSkipStatement;
import com.fujitsu.vdmj.ast.statements.ASTSpecificationStatement;
import com.fujitsu.vdmj.ast.statements.ASTStartStatement;
import com.fujitsu.vdmj.ast.statements.ASTStateDesignator;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.ast.statements.ASTStopStatement;
import com.fujitsu.vdmj.ast.statements.ASTTixeStatement;
import com.fujitsu.vdmj.ast.statements.ASTTixeStmtAlternative;
import com.fujitsu.vdmj.ast.statements.ASTTixeStmtAlternativeList;
import com.fujitsu.vdmj.ast.statements.ASTTrapStatement;
import com.fujitsu.vdmj.ast.statements.ASTWhileStatement;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;

/**
 * A syntax analyser to parse statements.
 */
public class StatementReader extends SyntaxReader
{
	public StatementReader(LexTokenReader reader)
	{
		super(reader);
	}

	public ASTStatement readStatement() throws ParserException, LexException
	{
		ASTStatement stmt = null;
		LexToken token = lastToken();
		LexLocation location = token.location;

		switch (token.type)
		{
			case LET:
				stmt = readLetStatement(token);
				break;

			case RETURN:
				stmt = readReturnStatement(location);
				break;

			case BRA:
				stmt = readBlockStatement(location);
				break;

			case NAME:
			case IDENTIFIER:
			case NEW:
			case SELF:
				stmt = readAssignmentOrCallStatement(token);
				break;

			case IF:
				nextToken();	// to allow elseif to call it too
				stmt = readConditionalStatement(location);
				break;

			case CASES:
				stmt = readCasesStatement(location);
				break;

			case FOR:
				stmt = readForStatement(location);
				break;

			case WHILE:
				stmt = readWhileStatement(location);
				break;

			case PIPEPIPE:
				stmt = readNonDetStatement(location);
				break;

			case ALWAYS:
				stmt = readAlwaysStatement(location);
				break;

			case ATOMIC:
				stmt = readAtomicStatement(location);
				break;

			case TRAP:
				stmt = readTrapStatement(location);
				break;

			case TIXE:
				stmt = readTixeStatement(location);
				break;

			case DEF:
				stmt = readDefStatement(location);
				break;

			case EXIT:
				stmt = readExitStatement(location);
				break;

			case SEQ_OPEN:
				stmt = readSpecStatement(location);
				break;

			case ERROR:
				stmt = new ASTErrorStatement(location);
				nextToken();
				break;

			case SKIP:
				stmt = new ASTSkipStatement(location);
				nextToken();
				break;

			case START:
				stmt = readStartStatement(location);
				break;

			case STARTLIST:
				stmt = readStartlistStatement(location);
				break;

			case STOP:
				if (Settings.release == Release.CLASSIC)
				{
					throwMessage(2304, "'stop' not available in VDM classic");
				}

				stmt = readStopStatement(location);
				break;

			case STOPLIST:
				if (Settings.release == Release.CLASSIC)
				{
					throwMessage(2305, "'stoplist' not available in VDM classic");
				}

				stmt = readStoplistStatement(location);
				break;

			case CYCLES:
				stmt = readCyclesStatement(location);
				break;

			case DURATION:
				stmt = readDurationStatement(location);
				break;
				
			// These are special error cases that can be caused by spurious semi-colons
			// after a statement, such as "if test then skip; else skip;"
			case ELSE:
			case ELSEIF:
			case IN:
				throwMessage(2063, "Unexpected token in statement - spurious semi-colon?");

			default:
				throwMessage(2063, "Unexpected token in statement");
		}

		return stmt;
	}

	private ASTStatement readExitStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.EXIT, 2190, "Expecting 'exit'");

		try
		{
			reader.push();
			ASTExpression exp = getExpressionReader().readExpression();
			reader.unpush();
			return new ASTExitStatement(token, exp);
		}
		catch (ParserException e)
		{
			reader.pop();
		}

		return new ASTExitStatement(token);
	}

	private ASTStatement readTixeStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.TIXE, 2191, "Expecting 'tixe'");

		ASTTixeStmtAlternativeList traps = new ASTTixeStmtAlternativeList();
		BindReader br = getBindReader();
		checkFor(Token.SET_OPEN, 2192, "Expecting '{' after 'tixe'");

		while (lastToken().isNot(Token.SET_CLOSE))
		{
			ASTPatternBind patternBind = br.readPatternOrBind();
			checkFor(Token.MAPLET, 2193, "Expecting '|->' after pattern bind");
			ASTStatement result = readStatement();
			traps.add(new ASTTixeStmtAlternative(patternBind, result));
			ignore(Token.COMMA);
		}

		nextToken();
		checkFor(Token.IN, 2194, "Expecting 'in' after tixe traps");
		ASTStatement body = getStatementReader().readStatement();

		return new ASTTixeStatement(token, traps, body);
	}

	private ASTStatement readTrapStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.TRAP, 2195, "Expecting 'trap'");
		ASTPatternBind patternBind = getBindReader().readPatternOrBind();
		checkFor(Token.WITH, 2196, "Expecting 'with' in trap statement");
		ASTStatement with = getStatementReader().readStatement();
		checkFor(Token.IN, 2197, "Expecting 'in' in trap statement");
		ASTStatement body = getStatementReader().readStatement();
		return new ASTTrapStatement(token, patternBind, with, body);
	}

	private ASTStatement readAlwaysStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.ALWAYS, 2198, "Expecting 'always'");
		ASTStatement always = getStatementReader().readStatement();
		checkFor(Token.IN, 2199, "Expecting 'in' after 'always' statement");
		ASTStatement body = getStatementReader().readStatement();
		return new ASTAlwaysStatement(token, always, body);
	}

	private ASTStatement readNonDetStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.PIPEPIPE, 2200, "Expecting '||'");
		checkFor(Token.BRA, 2201, "Expecting '(' after '||'");
		ASTNonDeterministicStatement block = new ASTNonDeterministicStatement(token);
		block.add(readStatement());		// Must be one

		while (ignore(Token.COMMA))
		{
			block.add(readStatement());
		}

		checkFor(Token.KET, 2202, "Expecting ')' at end of '||' block");
		return block;
	}

	private ASTStatement readAssignmentOrCallStatement(LexToken token)
		throws ParserException, LexException
	{
		ParserException assignError = null;
		ASTStatement stmt = null;

		try
		{
			reader.push();
			stmt = readAssignmentStatement(token.location);
			reader.unpush();
			return stmt;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
			reader.pop();
			assignError = e;
		}

		try
		{
			reader.push();
			stmt = readCallStatement();
			reader.unpush();
			return stmt;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
			reader.pop();
			throw e.deeperThan(assignError) ? e : assignError;
		}
	}

	private ASTStatement readAtomicStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.ATOMIC, 2203, "Expecting 'atomic'");
		checkFor(Token.BRA, 2204, "Expecting '(' after 'atomic'");
		ASTAssignmentStatementList assignments = new ASTAssignmentStatementList();

		assignments.add(readAssignmentStatement(lastToken().location));
		checkFor(Token.SEMICOLON, 2205, "Expecting ';' after atomic assignment");
		assignments.add(readAssignmentStatement(lastToken().location));

		while (lastToken().isNot(Token.KET))
		{
			checkFor(Token.SEMICOLON, 2205, "Expecting ';' after atomic assignment");
			
			if (lastToken().isNot(Token.KET))
			{
				assignments.add(readAssignmentStatement(lastToken().location));
			}
		}

		nextToken();
		return new ASTAtomicStatement(token, assignments);
	}

	public ASTStatement readCallStatement()
		throws ParserException, LexException
	{
		if (dialect != Dialect.VDM_SL)
		{
			return readObjectCallStatement();
		}
		else
		{
			return readSimpleCallStatement();
		}
	}

	private ASTStatement readSimpleCallStatement()
		throws ParserException, LexException
	{
		LexNameToken name =
			readNameToken("Expecting operation name in call statement");

		checkFor(Token.BRA, 2206, "Expecting '(' after call operation name");
		ASTExpressionList args = new ASTExpressionList();
		ExpressionReader er = getExpressionReader();

		if (lastToken().isNot(Token.KET))
		{
			args.add(er.readExpression());

			while (ignore(Token.COMMA))
			{
				args.add(er.readExpression());
			}
		}

    	checkFor(Token.KET, 2124, "Expecting ')' after args");

		return new ASTCallStatement(name, args);
	}

	private ASTStatement readObjectCallStatement()
		throws ParserException, LexException
    {
		ASTObjectDesignator designator = readObjectDesignator();

		// All operation calls actually look like object apply designators,
		// since they end with <name>([args]). So we unpick the apply
		// designator to extract the operation name and args.

		if (!(designator instanceof ASTObjectApplyDesignator))
		{
			throwMessage(2064, "Expecting <object>.identifier(args) or name(args)");
		}

		ASTObjectApplyDesignator oad = (ASTObjectApplyDesignator)designator;
		ASTExpressionList args = oad.args;

		if (oad.object instanceof ASTObjectFieldDesignator)
		{
			ASTObjectFieldDesignator ofd = (ASTObjectFieldDesignator)oad.object;
			
			if (ofd.classname != null)
			{
	    		return new ASTCallObjectStatement(ofd.object, ofd.classname, args);
			}
			else
			{
	    		return new ASTCallObjectStatement(ofd.object, ofd.fieldname, args);
			}
		}
		else if (oad.object instanceof ASTObjectIdentifierDesignator)
		{
			ASTObjectIdentifierDesignator oid = (ASTObjectIdentifierDesignator)oad.object;
			return new ASTCallStatement(oid.name, args);
		}
		else
		{
			throwMessage(2065, "Expecting <object>.name(args) or name(args)");
			return null;
		}
    }

	private ASTObjectDesignator readObjectDesignator()
		throws ParserException, LexException
	{
		ASTObjectDesignator des = readSimpleObjectDesignator();
		boolean done = false;

		while (!done)
		{
			switch (lastToken().type)
			{
				case POINT:
					LexToken field = nextToken();

					// If we just read a qualified name, we're dealing with
					// something like new A().X`op(), else it's the more usual
					// new A().op().

					switch (field.type)
					{
						case IDENTIFIER:
							des = new ASTObjectFieldDesignator(des, (LexIdentifierToken)field);
							break;

						case NAME:
							des = new ASTObjectFieldDesignator(des, (LexNameToken)field);
							break;

						default:
							throwMessage(2066, "Expecting object field name");
					}

					nextToken();
					break;

				case BRA:
					nextToken();
			    	ExpressionReader er = getExpressionReader();
			    	ASTExpressionList args = new ASTExpressionList();

			    	if (lastToken().isNot(Token.KET))
			    	{
			    		args.add(er.readExpression());

			    		while (ignore(Token.COMMA))
			    		{
			    			args.add(er.readExpression());
			    		}
			    	}

			    	checkFor(Token.KET, 2124, "Expecting ')' after args");
					des = new ASTObjectApplyDesignator(des, args);
					break;

				default:
					done = true;
					break;
			}
		}

		return des;
	}

	private ASTObjectDesignator readSimpleObjectDesignator()
		throws LexException, ParserException
	{
		LexToken token = readToken();

		switch (token.type)
		{
			case SELF:
				return new ASTObjectSelfDesignator(token.location);

			case IDENTIFIER:
				return new ASTObjectIdentifierDesignator(idToName((LexIdentifierToken)token));

			case NAME:
				return new ASTObjectIdentifierDesignator((LexNameToken)token);

			case NEW:
				LexIdentifierToken name = readIdToken("Expecting class name after 'new'");
				checkFor(Token.BRA, 2207, "Expecting '(' after new class name");

		    	ASTExpressionList args = new ASTExpressionList();
		    	ExpressionReader er = getExpressionReader();

		    	if (lastToken().isNot(Token.KET))
		    	{
		    		args.add(er.readExpression());

		    		while (ignore(Token.COMMA))
		    		{
		    			args.add(er.readExpression());
		    		}
		    	}

		    	checkFor(Token.KET, 2124, "Expecting ')' after constructor args");
				return new ASTObjectNewDesignator(name, args);

			default:
				throwMessage(2067, "Expecting 'self', 'new' or name in object designator");
				break;
		}

		return null;
	}

	private ASTStatement readWhileStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.WHILE, 2208, "Expecting 'while'");
		ASTExpression exp = getExpressionReader().readExpression();
		checkFor(Token.DO, 2209, "Expecting 'do' after while expression");
		ASTStatement body = getStatementReader().readStatement();
		return new ASTWhileStatement(token, exp, body);
	}

	private ASTStatement readForStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.FOR, 2210, "Expecting 'for'");
		ASTStatement forstmt = null;

		if (lastToken().is(Token.ALL))
		{
			nextToken();
			ASTPattern p = getPatternReader().readPattern();
			checkFor(Token.IN, 2211, "Expecting 'in set' after 'for all'");
			checkFor(Token.SET, 2212, "Expecting 'in set' after 'for all'");
			ASTExpression set = getExpressionReader().readExpression();
			checkFor(Token.DO, 2213, "Expecting 'do' after for all expression");
			ASTStatement body = getStatementReader().readStatement();
			return new ASTForAllStatement(token, p, set, body);
		}
		else
		{
			ParserException forIndexError = null;

			try
			{
				reader.push();
				forstmt = readForIndexStatement(token);
				reader.unpush();
				return forstmt;
			}
			catch (ParserException e)
			{
				e.adjustDepth(reader.getTokensRead());
				reader.pop();
				forIndexError = e;
			}

			try
			{
				reader.push();
				forstmt = readForPatternBindStatement(token);
				reader.unpush();
				return forstmt;
			}
			catch (ParserException e)
			{
				e.adjustDepth(reader.getTokensRead());
				reader.pop();
				throw e.deeperThan(forIndexError) ? e : forIndexError;
			}
		}
	}

	private ASTStatement readForPatternBindStatement(LexLocation token)
		throws ParserException, LexException
	{
		ASTPatternBind pb = getBindReader().readPatternOrBind();
		checkFor(Token.IN, 2214, "Expecting 'in' after pattern bind");

		// The old syntax used to include a "reverse" keyword as part
		// of the loop grammar, whereas the new VDM-10 syntax (LB:2791065)
		// makes the reverse a unary sequence operator.

		if (Settings.release == Release.VDM_10)
		{
    		ASTExpression exp = getExpressionReader().readExpression();
    		checkFor(Token.DO, 2215, "Expecting 'do' before loop statement");
    		ASTStatement body = getStatementReader().readStatement();
    		return new ASTForPatternBindStatement(token, pb, false, exp, body);
		}
		else
		{
			boolean reverse = ignore(Token.REVERSE);
			ASTExpression exp = getExpressionReader().readExpression();
    		checkFor(Token.DO, 2215, "Expecting 'do' before loop statement");
    		ASTStatement body = getStatementReader().readStatement();
    		return new ASTForPatternBindStatement(token, pb, reverse, exp, body);
		}
	}

	private ASTStatement readForIndexStatement(LexLocation token)
		throws ParserException, LexException
	{
		LexIdentifierToken var = readIdToken("Expecting variable identifier");
		checkFor(Token.EQUALS, 2216, "Expecting '=' after for variable");
		ASTExpression from = getExpressionReader().readExpression();
		checkFor(Token.TO, 2217, "Expecting 'to' after from expression");
		ASTExpression to = getExpressionReader().readExpression();
		ASTExpression by = null;

		if (lastToken().is(Token.BY))
		{
			nextToken();
			by = getExpressionReader().readExpression();
		}

		checkFor(Token.DO, 2218, "Expecting 'do' before loop statement");
		ASTStatement body = getStatementReader().readStatement();
		return new ASTForIndexStatement(token, idToName(var), from, to, by, body);
	}

	private ASTStatement readConditionalStatement(LexLocation token)
		throws ParserException, LexException
	{
		ASTExpression exp = getExpressionReader().readExpression();
		checkFor(Token.THEN, 2219, "Missing 'then'");
		ASTStatement thenStmt = readStatement();
		ASTElseIfStatementList elseIfList = new ASTElseIfStatementList();

		while (lastToken().is(Token.ELSEIF))
		{
			LexToken elseif = lastToken();
			nextToken();
			elseIfList.add(readElseIfStatement(elseif.location));
		}

		ASTStatement elseStmt = null;

		if (lastToken().is(Token.ELSE))
		{
			nextToken();
			elseStmt = readStatement();
		}

		return new ASTIfStatement(token, exp, thenStmt, elseIfList, elseStmt);
	}

	private ASTElseIfStatement readElseIfStatement(LexLocation token)
		throws ParserException, LexException
	{
		ASTExpression exp = getExpressionReader().readExpression();
		checkFor(Token.THEN, 2220, "Missing 'then' after 'elseif' expression");
		ASTStatement thenStmt = readStatement();
		return new ASTElseIfStatement(token, exp, thenStmt);
	}

	private ASTAssignmentStatement readAssignmentStatement(LexLocation token)
		throws ParserException, LexException
	{
		ASTStateDesignator sd = readStateDesignator();
		checkFor(Token.ASSIGN, 2222, "Expecting ':=' in state assignment statement");
		return new ASTAssignmentStatement(token, sd, getExpressionReader().readExpression());
	}

	private ASTStateDesignator readStateDesignator()
		throws ParserException, LexException
	{
		LexNameToken name =
			readNameToken("Expecting name in assignment statement");

		ASTStateDesignator sd = new ASTIdentifierDesignator(name);

		while (lastToken().is(Token.POINT) || lastToken().is(Token.BRA))
		{
			if (lastToken().is(Token.POINT))
			{
				if (nextToken().isNot(Token.IDENTIFIER))
				{
					throwMessage(2068, "Expecting field identifier");
				}

				sd = new ASTFieldDesignator(sd, lastIdToken());
				nextToken();
			}
			else
			{
				nextToken();
				ASTExpression exp = getExpressionReader().readExpression();
				checkFor(Token.KET, 2223, "Expecting ')' after map/seq reference");
				sd = new ASTMapSeqDesignator(sd, exp);
			}
		}

		return sd;
	}

	public ASTStatement readBlockStatement(LexLocation token)
		throws ParserException, LexException
	{
		LexToken start = lastToken();
		checkFor(Token.BRA, 2224, "Expecting statement block");
		ASTBlockStatement block = new ASTBlockStatement(token, readDclStatements());
		boolean problems = false;

		while (true)	// Loop for continue in exceptions
		{
			try
			{
				while (!lastToken().is(Token.KET))
				{
    				block.add(readStatement());
    				
    				if (lastToken().isNot(Token.KET) && lastToken().isNot(Token.SEMICOLON))
    				{
    					throwMessage(2225, "Expecting ';' after statement");
    				}
    				
    				ignore(Token.SEMICOLON);
    			}

				break;
			}
			catch (ParserException e)
			{
				problems = true;
				
				if (lastToken().is(Token.EOF))
				{
					break;
				}

				Token[] after = { Token.SEMICOLON };
				Token[] upto = { Token.KET };
				report(e, after, upto);
				continue;
			}
		}

		checkFor(Token.KET, 2226, "Expecting ')' at end of statement block");
		
		if (!problems && block.statements.isEmpty())
		{
			throwMessage(2296, "Block cannot be empty", start);
		}
		
		return block;
	}

	private ASTDefinitionList readDclStatements()
		throws ParserException, LexException
	{
		ASTDefinitionList defs = new ASTDefinitionList();

		while (lastToken().is(Token.DCL))
		{
			nextToken();
			defs.add(readAssignmentDefinition());

			while (ignore(Token.COMMA))
			{
				defs.add(readAssignmentDefinition());
			}

			checkFor(Token.SEMICOLON, 2227, "Expecting ';' after declarations");
		}

		return defs;
	}

	public ASTAssignmentDefinition readAssignmentDefinition()
		throws ParserException, LexException
	{
		LexIdentifierToken name = readIdToken("Expecting variable identifier");
		checkFor(Token.COLON, 2228, "Expecting name:type in declaration");
		ASTType type = getTypeReader().readType();
		ASTExpression exp = null;

		if (lastToken().is(Token.ASSIGN))
		{
			nextToken();
			exp = getExpressionReader().readExpression();
		}
		else if (lastToken().is(Token.EQUALSEQUALS) || lastToken().is(Token.EQUALS))
		{
			throwMessage(2069, "Expecting <identifier>:<type> := <expression>");
		}
		else
		{
			exp = new ASTUndefinedExpression(name.location);
		}

		return new ASTAssignmentDefinition(idToName(name), type, exp);
	}

	private ASTStatement readReturnStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.RETURN, 2229, "Expecting 'return'");

		try
		{
			reader.push();
			ASTExpression exp = getExpressionReader().readExpression();
			reader.unpush();
			return new ASTReturnStatement(token, exp);
		}
		catch (ParserException e)
		{
			int count = reader.getTokensRead();
			e.adjustDepth(count);
			reader.pop();

			if (count > 2)
			{
				// We got some way, so error is probably in exp
				throw e;
			}
			else
			{
				// Probably just a simple return
				return new ASTReturnStatement(token);
			}
		}
	}

	private ASTStatement readLetStatement(LexToken token)
		throws ParserException, LexException
	{
		checkFor(Token.LET, 2230, "Expecting 'let'");
		ParserException letDefError = null;

		try
		{
			reader.push();
			ASTLetDefStatement stmt = readLetDefStatement(token.location);
			reader.unpush();
			return stmt;
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
			ASTLetBeStStatement stmt = readLetBeStStatement(token.location);
			reader.unpush();
			return stmt;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
			reader.pop();
			throw e.deeperThan(letDefError) ? e : letDefError;
		}
	}

	private ASTLetDefStatement readLetDefStatement(LexLocation token)
		throws ParserException, LexException
	{
		DefinitionReader dr = getDefinitionReader();
		ASTDefinitionList localDefs = new ASTDefinitionList();
		localDefs.add(dr.readLocalDefinition());

		while (ignore(Token.COMMA))
		{
			localDefs.add(dr.readLocalDefinition());
		}

		checkFor(Token.IN, 2231, "Expecting 'in' after local definitions");
		return new ASTLetDefStatement(token, localDefs, readStatement());
	}

	private ASTLetBeStStatement readLetBeStStatement(LexLocation token)
		throws ParserException, LexException
	{
		ASTMultipleBind bind = getBindReader().readMultipleBind();
		ASTExpression stexp = null;

		if (lastToken().is(Token.BE))
		{
			nextToken();
			checkFor(Token.ST, 2232, "Expecting 'st' after 'be' in let statement");
			stexp = getExpressionReader().readExpression();
		}

		checkFor(Token.IN, 2233, "Expecting 'in' after bind in let statement");
		return new ASTLetBeStStatement(token, bind, stexp, readStatement());
	}

	private ASTCasesStatement readCasesStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.CASES, 2234, "Expecting 'cases'");
		ASTExpression exp = getExpressionReader().readExpression();
		checkFor(Token.COLON, 2235, "Expecting ':' after cases expression");

		ASTCaseStmtAlternativeList cases = new ASTCaseStmtAlternativeList();
		ASTStatement others = null;
		cases.addAll(readCaseAlternatives());

		while (lastToken().is(Token.COMMA))
		{
			if (nextToken().is(Token.OTHERS))
			{
				nextToken();
				checkFor(Token.ARROW, 2237, "Expecting '->' after others");
				others = readStatement();
				break;
			}
			else
			{
				cases.addAll(readCaseAlternatives());
			}
		}

		checkFor(Token.END, 2238, "Expecting ', case alternative' or 'end' after cases");
		return new ASTCasesStatement(token, exp, cases, others);
	}

	private ASTCaseStmtAlternativeList readCaseAlternatives()
    	throws ParserException, LexException
    {
    	ASTCaseStmtAlternativeList alts = new ASTCaseStmtAlternativeList();
    	ASTPatternList plist = getPatternReader().readPatternList();
    	checkFor(Token.ARROW, 2236, "Expecting '->' after case pattern list");
    	ASTStatement result = readStatement();

    	for (ASTPattern p: plist)
    	{
    		alts.add(new ASTCaseStmtAlternative(p, result));
    	}

    	return alts;
    }

	private ASTDefStatement readDefStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.DEF, 2239, "Expecting 'def'");
		DefinitionReader dr = getDefinitionReader();
		ASTDefinitionList equalsDefs = new ASTDefinitionList();

		while (lastToken().isNot(Token.IN))
		{
			equalsDefs.add(dr.readEqualsDefinition());
			ignore(Token.SEMICOLON);
		}

		checkFor(Token.IN, 2240, "Expecting 'in' after equals definitions");
		return new ASTDefStatement(token, equalsDefs, readStatement());
	}

	private ASTSpecificationStatement readSpecStatement(LexLocation token)
		throws ParserException, LexException
	{
		checkFor(Token.SEQ_OPEN, 2241, "Expecting '['");
		DefinitionReader dr = getDefinitionReader();
		ASTSpecificationStatement stmt = dr.readSpecification(token, false);
		checkFor(Token.SEQ_CLOSE, 2242, "Expecting ']' after specification statement");
		return stmt;
	}

	private ASTStatement readStartStatement(LexLocation location)
		throws LexException, ParserException
	{
		checkFor(Token.START, 2243, "Expecting 'start'");
		checkFor(Token.BRA, 2244, "Expecting 'start('");
		ASTExpression obj = getExpressionReader().readExpression();
		checkFor(Token.KET, 2245, "Expecting ')' after start object");
		return new ASTStartStatement(location, obj);
	}

	private ASTStatement readStartlistStatement(LexLocation location)
		throws LexException, ParserException
	{
		checkFor(Token.STARTLIST, 2246, "Expecting 'startlist'");
		checkFor(Token.BRA, 2247, "Expecting 'startlist('");
		ASTExpression set = getExpressionReader().readExpression();
		checkFor(Token.KET, 2248, "Expecting ')' after startlist objects");
		return new ASTStartStatement(location, set);
	}

	private ASTStatement readStopStatement(LexLocation location)
		throws LexException, ParserException
	{
		checkFor(Token.STOP, 2306, "Expecting 'stop'");
		checkFor(Token.BRA, 2307, "Expecting 'stop('");
		ASTExpression obj = getExpressionReader().readExpression();
		checkFor(Token.KET, 2308, "Expecting ')' after stop object");
		return new ASTStopStatement(location, obj);
	}

	private ASTStatement readStoplistStatement(LexLocation location)
		throws LexException, ParserException
	{
		checkFor(Token.STOPLIST, 2309, "Expecting 'stoplist'");
		checkFor(Token.BRA, 2310, "Expecting 'stoplist('");
		ASTExpression set = getExpressionReader().readExpression();
		checkFor(Token.KET, 2311, "Expecting ')' after stoplist objects");
		return new ASTStopStatement(location, set);
	}

	private ASTStatement readDurationStatement(LexLocation location)
		throws LexException, ParserException
	{
		checkFor(Token.DURATION, 2271, "Expecting 'duration'");
		checkFor(Token.BRA, 2272, "Expecting 'duration('");
		ASTExpression duration = getExpressionReader().readExpression();
		checkFor(Token.KET, 2273, "Expecting ')' after duration");
		ASTStatement stmt = readStatement();
		return new ASTDurationStatement(location, duration, stmt);
	}

	private ASTStatement readCyclesStatement(LexLocation location)
		throws LexException, ParserException
	{
		checkFor(Token.CYCLES, 2274, "Expecting 'cycles'");
		checkFor(Token.BRA, 2275, "Expecting 'cycles('");
		ASTExpression duration = getExpressionReader().readExpression();
		checkFor(Token.KET, 2276, "Expecting ')' after cycles");
		ASTStatement stmt = readStatement();
		return new ASTCyclesStatement(location, duration, stmt);
	}
}
