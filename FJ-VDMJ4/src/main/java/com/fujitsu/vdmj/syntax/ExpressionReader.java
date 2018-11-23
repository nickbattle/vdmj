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

import java.util.Collections;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotatedExpression;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotationList;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.expressions.*;
import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.ast.lex.LexCharacterToken;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexQuoteToken;
import com.fujitsu.vdmj.ast.lex.LexRealToken;
import com.fujitsu.vdmj.ast.lex.LexStringToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBindList;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.patterns.ASTTypeBindList;
import com.fujitsu.vdmj.ast.types.ASTBooleanType;
import com.fujitsu.vdmj.ast.types.ASTCharacterType;
import com.fujitsu.vdmj.ast.types.ASTIntegerType;
import com.fujitsu.vdmj.ast.types.ASTNaturalOneType;
import com.fujitsu.vdmj.ast.types.ASTNaturalType;
import com.fujitsu.vdmj.ast.types.ASTRationalType;
import com.fujitsu.vdmj.ast.types.ASTRealType;
import com.fujitsu.vdmj.ast.types.ASTTokenType;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.ast.types.ASTUnresolvedType;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;

/**
 * A syntax analyser to parse expressions.
 */
public class ExpressionReader extends SyntaxReader
{
	public ExpressionReader(LexTokenReader reader)
	{
		super(reader);
	}

	public ASTExpressionList readExpressionList() throws ParserException, LexException
	{
		ASTExpressionList list = new ASTExpressionList();
		list.add(readExpression());

		while (ignore(Token.COMMA))
		{
			list.add(readExpression());
		}

		return list;
	}

	// Constructor Family... and pick up any annotations here.

	public ASTExpression readExpression() throws ParserException, LexException
	{
		return readConnectiveExpression();
	}

	// Connectives Family. All (recursive) right grouping...

	private ASTExpression readConnectiveExpression()
		throws ParserException, LexException
	{
		ASTExpression exp = readImpliesExpression();
		LexToken token = lastToken();

		if (token.is(Token.EQUIVALENT))
		{
			nextToken();
			exp = new ASTEquivalentExpression(exp, token, readConnectiveExpression());
		}

		return exp;
	}

	private ASTExpression readImpliesExpression() throws ParserException, LexException
	{
		ASTExpression exp = readOrExpression();
		LexToken token = lastToken();

		if (token.is(Token.IMPLIES))
		{
			nextToken();
			exp = new ASTImpliesExpression(exp, token, readImpliesExpression());
		}

		return exp;
	}


	private ASTExpression readOrExpression() throws ParserException, LexException
	{
		ASTExpression exp = readAndExpression();
		LexToken token = lastToken();

		if (token.is(Token.OR))
		{
			nextToken();
			exp = new ASTOrExpression(exp, token, readOrExpression());
		}

		return exp;
	}

	private ASTExpression readAndExpression() throws ParserException, LexException
	{
		ASTExpression exp = readNotExpression();
		LexToken token = lastToken();

		if (token.is(Token.AND))
		{
			nextToken();
			exp = new ASTAndExpression(exp, token, readAndExpression());
		}

		return exp;
	}

	private ASTExpression readNotExpression() throws ParserException, LexException
	{
		ASTExpression exp = null;
		LexToken token = lastToken();

		if (token.is(Token.NOT))
		{
			nextToken();
			exp = new ASTNotExpression(token.location, readNotExpression());
		}
		else
		{
			exp = readRelationalExpression();
		}

		return exp;
	}

	// Relations Family...

	public ASTEqualsExpression readDefEqualsExpression()
		throws ParserException, LexException
	{
		// This is an oddball parse for the "def" expression :-)

		ASTExpression exp = readEvaluatorP1Expression();
		LexToken token = lastToken();

		if (readToken().is(Token.EQUALS))
		{
			return new ASTEqualsExpression(exp, token, readEvaluatorP1Expression());
		}

		throwMessage(2029, "Expecting <set bind> = <expression>");
		return null;
	}

	private ASTExpression readRelationalExpression()
		throws ParserException, LexException
	{
		ASTExpression exp = readEvaluatorP1Expression();
		LexToken token = lastToken();

		if (token.is(Token.NOT))
		{
			// Check for "not in set"
			reader.push();

			if (nextToken().is(Token.IN))
			{
				if (nextToken().is(Token.SET))
				{
					token = new LexKeywordToken(Token.NOTINSET, token.location);
					reader.unpush();
				}
				else
				{
					reader.pop();
				}
			}
			else
			{
				reader.pop();
			}
		}
		else if (token.is(Token.IN))
		{
			// Check for "in set"
			reader.push();

			if (nextToken().is(Token.SET))
			{
				token = new LexKeywordToken(Token.INSET, token.location);
				reader.unpush();
			}
			else
			{
				reader.pop();
			}
		}

		// No grouping for relationals...
		switch (token.type)
		{
			case LT:
				nextToken();
				exp = new ASTLessExpression(exp, token, readNotExpression());
				break;

			case LE:
				nextToken();
				exp = new ASTLessEqualExpression(exp, token, readNotExpression());
				break;

			case GT:
				nextToken();
				exp = new ASTGreaterExpression(exp, token, readNotExpression());
				break;

			case GE:
				nextToken();
				exp = new ASTGreaterEqualExpression(exp, token, readNotExpression());
				break;

			case NE:
				nextToken();
				exp = new ASTNotEqualExpression(exp, token, readNotExpression());
				break;

			case EQUALS:
				nextToken();
				exp = new ASTEqualsExpression(exp, token, readNotExpression());
				break;

			case SUBSET:
				nextToken();
				exp = new ASTSubsetExpression(exp, token, readNotExpression());
				break;

			case PSUBSET:
				nextToken();
				exp = new ASTProperSubsetExpression(exp, token, readNotExpression());
				break;

			case INSET:
				nextToken();
				exp = new ASTInSetExpression(exp, token, readNotExpression());
				break;

			case NOTINSET:
				nextToken();
				exp = new ASTNotInSetExpression(exp, token, readNotExpression());
				break;
				
			default:
				break;
		}

		return exp;
	}

	// Evaluator Family...

	private ASTExpression readEvaluatorP1Expression()
		throws ParserException, LexException
	{
		ASTExpression exp = readEvaluatorP2Expression();
		boolean more = true;

		while (more)	// Left grouping
		{
			LexToken token = lastToken();

			switch (token.type)
			{
				case PLUS:
					nextToken();
					exp = new ASTPlusExpression(exp, token, readEvaluatorP2Expression());
					break;

				case MINUS:
					nextToken();
					exp = new ASTSubtractExpression(exp, token, readEvaluatorP2Expression());
					break;

				case UNION:
					nextToken();
					exp = new ASTSetUnionExpression(exp, token, readEvaluatorP2Expression());
					break;

				case SETDIFF:
					nextToken();
					exp = new ASTSetDifferenceExpression(exp, token, readEvaluatorP2Expression());
					break;

				case MUNION:
					nextToken();
					exp = new ASTMapUnionExpression(exp, token, readEvaluatorP2Expression());
					break;

				case PLUSPLUS:
					nextToken();
					exp = new ASTPlusPlusExpression(exp, token, readEvaluatorP2Expression());
					break;

				case CONCATENATE:
					nextToken();
					exp = new ASTSeqConcatExpression(exp, token, readEvaluatorP2Expression());
					break;

				default:
					more = false;
					break;
			}
		}

		return exp;
	}

	private ASTExpression readEvaluatorP2Expression()
		throws ParserException, LexException
	{
		ASTExpression exp = readEvaluatorP3Expression();
		boolean more = true;

		while (more)	// Left grouping
		{
			LexToken token = lastToken();

			switch (token.type)
			{
				case TIMES:
					nextToken();
					exp = new ASTTimesExpression(exp, token, readEvaluatorP3Expression());
					break;

				case DIVIDE:
					nextToken();
					exp = new ASTDivideExpression(exp, token, readEvaluatorP3Expression());
					break;

				case REM:
					nextToken();
					exp = new ASTRemExpression(exp, token, readEvaluatorP3Expression());
					break;

				case MOD:
					nextToken();
					exp = new ASTModExpression(exp, token, readEvaluatorP3Expression());
					break;

				case DIV:
					nextToken();
					exp = new ASTDivExpression(exp, token, readEvaluatorP3Expression());
					break;

				case INTER:
					nextToken();
					exp = new ASTSetIntersectExpression(exp, token, readEvaluatorP3Expression());
					break;

				default:
					more = false;
					break;
			}
		}

		return exp;
	}

	private ASTExpression readEvaluatorP3Expression()
		throws ParserException, LexException
	{
		ASTExpression exp = null;
		LexToken token = lastToken();

		if (token.is(Token.INVERSE))
		{
			nextToken();
			// Unary, so recursion OK for left grouping
			exp = new ASTMapInverseExpression(token.location, readEvaluatorP3Expression());
		}
		else
		{
			exp = readEvaluatorP4Expression();
		}

		return exp;
	}

	private ASTExpression readEvaluatorP4Expression()
		throws ParserException, LexException
	{
		ASTExpression exp = readEvaluatorP5Expression();
		boolean more = true;

		while (more)
		{
			LexToken token = lastToken();

			switch (token.type)
			{
				case DOMRESTO:
					nextToken();
					exp = new ASTDomainResToExpression(exp, token, readEvaluatorP5Expression());
					break;

				case DOMRESBY:
					nextToken();
					exp = new ASTDomainResByExpression(exp, token, readEvaluatorP5Expression());
					break;

				default:
					more = false;
					break;
			}
		}

		return exp;
	}

	private ASTExpression readEvaluatorP5Expression()
		throws ParserException, LexException
	{
		ASTExpression exp = readEvaluatorP6Expression();
		boolean more = true;

		while (more)
		{
			LexToken token = lastToken();

			switch (token.type)
			{
				case RANGERESTO:
					nextToken();
					exp = new ASTRangeResToExpression(exp, token, readEvaluatorP6Expression());
					break;

				case RANGERESBY:
					nextToken();
					exp = new ASTRangeResByExpression(exp, token, readEvaluatorP6Expression());
					break;

				default:
					more = false;
					break;
			}
		}

		return exp;
	}

	private ASTExpression readEvaluatorP6Expression()
		throws ParserException, LexException
	{
		ASTExpression exp = null;
		LexToken token = lastToken();
		LexLocation location = token.location;

		// Unary operators, so recursion OK for left grouping
		switch (token.type)
		{
			case PLUS:
				nextToken();
				exp = new ASTUnaryPlusExpression(location, readEvaluatorP6Expression());
				break;

			case MINUS:
				nextToken();
				exp = new ASTUnaryMinusExpression(location, readEvaluatorP6Expression());
				break;

			case CARD:
				nextToken();
				exp = new ASTCardinalityExpression(location, readEvaluatorP6Expression());
				break;

			case DOM:
				nextToken();
				exp = new ASTMapDomainExpression(location, readEvaluatorP6Expression());
				break;

			case LEN:
				nextToken();
				exp = new ASTLenExpression(location, readEvaluatorP6Expression());
				break;

			case POWER:
				nextToken();
				exp = new ASTPowerSetExpression(location, readEvaluatorP6Expression());
				break;

			case RNG:
				nextToken();
				exp = new ASTMapRangeExpression(location, readEvaluatorP6Expression());
				break;

			case ELEMS:
				nextToken();
				exp = new ASTElementsExpression(location, readEvaluatorP6Expression());
				break;

			case ABS:
				nextToken();
				exp = new ASTAbsoluteExpression(location, readEvaluatorP6Expression());
				break;

			case DINTER:
				nextToken();
				exp = new ASTDistIntersectExpression(location, readEvaluatorP6Expression());
				break;

			case MERGE:
				nextToken();
				exp = new ASTDistMergeExpression(location, readEvaluatorP6Expression());
				break;

			case HEAD:
				nextToken();
				exp = new ASTHeadExpression(location, readEvaluatorP6Expression());
				break;

			case TAIL:
				nextToken();
				exp = new ASTTailExpression(location, readEvaluatorP6Expression());
				break;

			case REVERSE:
				if (Settings.release == Release.CLASSIC)
				{
					throwMessage(2291, "'reverse' not available in VDM classic");
				}

				nextToken();
				exp = new ASTReverseExpression(location, readEvaluatorP6Expression());
				break;

			case FLOOR:
				nextToken();
				exp = new ASTFloorExpression(location, readEvaluatorP6Expression());
				break;

			case DUNION:
				nextToken();
				exp = new ASTDistUnionExpression(location, readEvaluatorP6Expression());
				break;

			case DISTCONC:
				nextToken();
				exp = new ASTDistConcatExpression(location, readEvaluatorP6Expression());
				break;

			case INDS:
				nextToken();
				exp = new ASTIndicesExpression(location, readEvaluatorP6Expression());
				break;

			default:
				exp = readApplicatorExpression();
				break;
		}

		return exp;
	}

	// Applicator Family. Left grouping(?)

	private ASTExpression readApplicatorExpression()
		throws ParserException, LexException
	{
		ASTExpression exp = readAnnotatedExpression();
		boolean more = true;

		while (more)
		{
			LexToken token = lastToken();

			switch (token.type)
    		{
    			case BRA:
    				// Either sequence(from, ..., to) or func(args) or map(key)
    				// or mk_*(), is_*(), mu(), pre_*(), post_*(),
    				// init_*() or inv_*()

    				if (nextToken().is(Token.KET))
    				{
    					if (exp instanceof ASTVariableExpression)
    					{
    						ASTVariableExpression ve = (ASTVariableExpression)exp;
    						String name = ve.name.name;

        					if (name.startsWith("mk_"))
    						{
        						// a mk_TYPE() with no field values
    							exp = readMkExpression(ve);
    							break;
    						}
        				}

   						exp = new ASTApplyExpression(exp);
    					nextToken();
    				}
    				else
    				{
    					if (exp instanceof ASTVariableExpression)
    					{
    						ASTVariableExpression ve = (ASTVariableExpression)exp;
    						String name = ve.name.name;

    						if (name.equals("mu"))
    						{
    							exp = readMuExpression(ve);
    							break;
    						}
    						else if (name.startsWith("mk_"))
    						{
    							exp = readMkExpression(ve);
    							break;
    						}
    						else if (name.startsWith("is_"))
    						{
    							exp = readIsExpression(ve);
    							break;
    						}
    						else if (name.equals("pre_"))
    						{
    							exp = readPreExpression(ve);
    							break;
    						}
    						else if (name.equals("narrow_"))
    						{
    							if (Settings.release == Release.CLASSIC)
    							{
    								throwMessage(2303, "Narrow not available in VDM classic", ve.name);
    							}
    							else
    							{
    								exp = readNarrowExpression(ve);
    							}
    							break;
    						}
     					}

    					// So we're a function/operation call, a list subsequence or
    					// a map index...

    					ASTExpression first = readExpression();

    					if (lastToken().is(Token.COMMA))
    					{
    						reader.push();

    						if (nextToken().is(Token.RANGE))
    						{
    							nextToken();
    							checkFor(Token.COMMA, 2120, "Expecting 'e1,...,e2' in subsequence");
    							ASTExpression last = readExpression();
    							checkFor(Token.KET, 2121, "Expecting ')' after subsequence");
    							reader.unpush();
    							exp = new ASTSubseqExpression(exp, first, last);
    							break;
    						}

    						reader.pop();	// Not a subsequence then...
    					}

    					// OK, so read a (list, of, arguments)...

						ASTExpressionList args = new ASTExpressionList();
						args.add(first);

						while (ignore(Token.COMMA))
						{
							args.add(readExpression());
						}

						checkFor(Token.KET, 2122, "Expecting ')' after function args");
   						exp = new ASTApplyExpression(exp, args);
    				}
    				break;

    			case SEQ_OPEN:
    				// Polymorphic function instantiation
    				ASTTypeList types = new ASTTypeList();
    				TypeReader tr = getTypeReader();

    				nextToken();
    				types.add(tr.readType());

    				while (ignore(Token.COMMA))
    				{
    					types.add(tr.readType());
    				}

    				checkFor(Token.SEQ_CLOSE, 2123, "Expecting ']' after function instantiation");
   					exp = new ASTFuncInstantiationExpression(exp, types);
    				break;

    			case POINT:
    				// ASTField selection by name or number
    				switch (nextToken().type)
    				{
    					case NAME:
    						if (dialect != Dialect.VDM_SL)
    						{
        						exp = new ASTFieldExpression(exp, lastNameToken());
    						}
    						else
    						{
    							throwMessage(2030, "Expecting simple field identifier");
    						}
    						break;

    					case IDENTIFIER:
    						exp = new ASTFieldExpression(exp, lastIdToken());
    						break;

    					case HASH:
    						if (nextToken().isNot(Token.NUMBER))
    						{
    							throwMessage(2031, "Expecting field number after .#");
    						}

    						LexIntegerToken num = (LexIntegerToken)lastToken();
    						exp = new ASTFieldNumberExpression(exp, num);
    						break;

    					default:
    						throwMessage(2032, "Expecting field name");
    				}

    				nextToken();
    				break;

    			default:
    				more = false;
    				break;
    		}
		}

		// If we've collected as many applicators as we can, but we're still
		// just a variable, this is a bare variable expression. In VDM++, these
		// are always qualified (ie. x refers to C`x where it was declared, not
		// an overriding version lower down).

		if (exp instanceof ASTVariableExpression)
		{
			ASTVariableExpression ve = (ASTVariableExpression)exp;
			ve.setExplicit(true);
		}

		// Combinator Family. Right grouping.
		LexToken token = lastToken();

		if (token.is(Token.COMP))
		{
			nextToken();
			return new ASTCompExpression(exp, token, readApplicatorExpression());
		}

		if (token.is(Token.STARSTAR))
		{
			nextToken();
			return new ASTStarStarExpression(exp, token, readEvaluatorP6Expression());
		}

		return exp;
	}

	private ASTExpression readAnnotatedExpression() throws ParserException, LexException
	{
		ASTAnnotationList annotations = readAnnotations();
		ASTExpression body = null;

		if (!annotations.isEmpty())
		{
			annotations.before(this);
			body = readBasicExpression();
			annotations.after(this, body);

			Collections.reverse(annotations);	// Build the chain backwards
			
			for (ASTAnnotation annotation: annotations)
			{
				body = new ASTAnnotatedExpression(annotation.name.location, annotation, body);
			}
		}
		else
		{
			body = readBasicExpression();
		}
		
		return body;
	}

	private ASTExpression readBasicExpression()
		throws ParserException, LexException
	{
		LexToken token = lastToken();

		switch (token.type)
		{
			case NUMBER:
				nextToken();
				return new ASTIntegerLiteralExpression((LexIntegerToken)token);

			case REALNUMBER:
				nextToken();
				return new ASTRealLiteralExpression((LexRealToken)token);

			case NAME:
				// Includes mk_ constructors
				LexNameToken name = lastNameToken();
				nextToken();
				return new ASTVariableExpression(name);

			case IDENTIFIER:
				// Includes mk_ constructors
				// Note we can't use lastNameToken as this checks that we don't
				// use old~ names.
				LexNameToken id =
					new LexNameToken(reader.currentModule, (LexIdentifierToken)token);
				nextToken();
				return new ASTVariableExpression(id);

			case STRING:
				nextToken();
				return new ASTStringLiteralExpression((LexStringToken)token);

			case CHARACTER:
				nextToken();
				return new ASTCharLiteralExpression((LexCharacterToken)token);

			case QUOTE:
				nextToken();
				return new ASTQuoteLiteralExpression((LexQuoteToken)token);

			case TRUE:
			case FALSE:
				nextToken();
				return new ASTBooleanLiteralExpression((LexBooleanToken)token);

			case UNDEFINED:
				nextToken();
				return new ASTUndefinedExpression(token.location);

			case NIL:
				nextToken();
				return new ASTNilExpression(token.location);

			case THREADID:
				nextToken();
				return new ASTThreadIdExpression(token.location);

			case BRA:
				nextToken();
				ASTExpression exp = readExpression();
				checkFor(Token.KET, 2124, "Expecting ')'");
				return exp;
				
			case SET_OPEN:
				nextToken();
				return readSetOrMapExpression(token.location);

			case SEQ_OPEN:
				nextToken();
				return readSeqExpression(token.location);

			case FORALL:
				nextToken();
				return readForAllExpression(token.location);

			case EXISTS:
				nextToken();
				return readExistsExpression(token.location);

			case EXISTS1:
				nextToken();
				return readExists1Expression(token.location);

			case IOTA:
				nextToken();
				return readIotaExpression(token.location);

			case LAMBDA:
				nextToken();
				return readLambdaExpression(token.location);

			case IF:
				nextToken();
				return readIfExpression(token.location);

			case CASES:
				nextToken();
				return readCasesExpression(token.location);

			case LET:
				nextToken();
				return readLetExpression(token.location);

			case DEF:
				nextToken();
				return readDefExpression(token.location);

			case NEW:
				nextToken();
				return readNewExpression(token.location);

			case SELF:
				nextToken();
				return new ASTSelfExpression(token.location);

			case ISOFBASECLASS:
				nextToken();
				return readIsOfBaseExpression(token.location);

			case ISOFCLASS:
				nextToken();
				return readIsOfClassExpression(token.location);

			case SAMEBASECLASS:
				nextToken();
				return readSameBaseExpression(token.location);

			case SAMECLASS:
				nextToken();
				return readSameClassExpression(token.location);

			case REQ: case ACT: case FIN: case ACTIVE: case WAITING:
				return readHistoryExpression(token.location);

			case TIME:
				return readTimeExpression(token.location);

			default:
				throwMessage(2034, "Unexpected token in expression");
				return null;
		}
	}

	private ASTExpression readTimeExpression(LexLocation location) throws LexException
	{
		nextToken();
		return new ASTTimeExpression(location);
	}

	private ASTMuExpression readMuExpression(ASTVariableExpression ve)
		throws ParserException, LexException
	{
		ASTRecordModifierList args = new ASTRecordModifierList();
		ASTExpression record = readExpression();

		do
		{
			checkFor(Token.COMMA, 2128, "Expecting comma separated record modifiers");
			LexIdentifierToken id = readIdToken("Expecting <identifier> |-> <expression>");
			checkFor(Token.MAPLET, 2129, "Expecting <identifier> |-> <expression>");
			args.add(new ASTRecordModifier(id, readExpression()));
		}
		while (lastToken().is(Token.COMMA));

		checkFor(Token.KET, 2130, "Expecting ')' after mu maplets");
		return new ASTMuExpression(ve.location, record, args);
	}

	private ASTExpression readMkExpression(ASTVariableExpression ve)
		throws ParserException, LexException
	{
		ASTExpressionList args = new ASTExpressionList();

		if (lastToken().isNot(Token.KET))	// NB. mk_T() is legal
		{
			args.add(readExpression());

			while (ignore(Token.COMMA))
			{
				args.add(readExpression());
			}
		}

		checkFor(Token.KET, 2131, "Expecting ')' after mk_ tuple");
		ASTExpression exp = null;

		if (ve.name.name.equals("mk_"))
		{
			if (args.size() < 2)
			{
				throwMessage(2035, "Tuple must have >1 argument");
			}

			exp = new ASTTupleExpression(ve.location, args);
		}
		else
		{
			LexNameToken typename = getMkTypeName(ve.name);
			Token type = Token.lookup(typename.name, Dialect.VDM_SL);

			if (type != null)
			{
				if (args.size() != 1)
				{
					throwMessage(2300, "mk_<type> must have a single argument");
				}

				ASTExpression value = args.get(0);

				switch (type)
				{
					case BOOL:
						exp = new ASTMkBasicExpression(new ASTBooleanType(ve.location), value);
						break;

					case NAT:
						exp = new ASTMkBasicExpression(new ASTNaturalType(ve.location), value);
						break;

					case NAT1:
						exp = new ASTMkBasicExpression(new ASTNaturalOneType(ve.location), value);
						break;

					case INT:
						exp = new ASTMkBasicExpression(new ASTIntegerType(ve.location), value);
						break;

					case RAT:
						exp = new ASTMkBasicExpression(new ASTRationalType(ve.location), value);
						break;

					case REAL:
						exp = new ASTMkBasicExpression(new ASTRealType(ve.location), value);
						break;

					case CHAR:
						exp = new ASTMkBasicExpression(new ASTCharacterType(ve.location), value);
						break;

					case TOKEN:
						exp = new ASTMkBasicExpression(new ASTTokenType(ve.location), value);
						break;

					default:
						throwMessage(2036, "Expecting mk_<type>");
				}
			}
			else
			{
				exp = new ASTMkTypeExpression(typename, args);
			}
		}

		return exp;
	}

	private LexNameToken getMkTypeName(LexNameToken mktoken)
		throws ParserException, LexException
	{
		String typename = mktoken.name.substring(3);	// mk_... or is_...
		String[] parts = typename.split("`");

		switch (parts.length)
		{
			case 1:
				return new LexNameToken(getCurrentModule(), parts[0], mktoken.location);

			case 2:
				return new LexNameToken(parts[0], parts[1], mktoken.location, false, true);

			default:
				throwMessage(2037, "Malformed mk_<type> name " + typename);
		}

		return null;
	}

	private ASTIsExpression readIsExpression(ASTVariableExpression ve)
		throws ParserException, LexException
	{
		String name = ve.name.name;
		ASTIsExpression exp = null;

		if (name.equals("is_"))
		{
			ASTExpression test = readExpression();
			checkFor(Token.COMMA, 2132, "Expecting is_(expression, type)");
			TypeReader tr = getTypeReader();
			ASTType type = tr.readType();

			if (type instanceof ASTUnresolvedType)
			{
				ASTUnresolvedType nt = (ASTUnresolvedType)type;
				exp = new ASTIsExpression(ve.location, nt.typename, test);
			}
			else
			{
				exp = new ASTIsExpression(ve.location, type, test);
			}
		}
		else
		{
			LexNameToken typename = getMkTypeName(ve.name);
			Token type = Token.lookup(typename.name, Dialect.VDM_SL);

			if (type != null)
			{
				switch (type)
				{
					case BOOL:
						exp = new ASTIsExpression(ve.location, new ASTBooleanType(ve.location), readExpression());
						break;

					case NAT:
						exp = new ASTIsExpression(ve.location, new ASTNaturalType(ve.location), readExpression());
						break;

					case NAT1:
						exp = new ASTIsExpression(ve.location, new ASTNaturalOneType(ve.location), readExpression());
						break;

					case INT:
						exp = new ASTIsExpression(ve.location, new ASTIntegerType(ve.location), readExpression());
						break;

					case RAT:
						exp = new ASTIsExpression(ve.location, new ASTRationalType(ve.location), readExpression());
						break;

					case REAL:
						exp = new ASTIsExpression(ve.location, new ASTRealType(ve.location), readExpression());
						break;

					case CHAR:
						exp = new ASTIsExpression(ve.location, new ASTCharacterType(ve.location), readExpression());
						break;

					case TOKEN:
						exp = new ASTIsExpression(ve.location, new ASTTokenType(ve.location), readExpression());
						break;

					default:
						throwMessage(2038, "Expecting is_<type>");
				}
			}
			else
			{
				exp = new ASTIsExpression(ve.location, typename, readExpression());
			}
		}

		checkFor(Token.KET, 2133, "Expecting ')' after is_ expression");
		return exp;
	}

	private ASTExpression readNarrowExpression(ASTVariableExpression ve)
		throws LexException, ParserException
	{
		ASTNarrowExpression exp = null;

		ASTExpression test = readExpression();
		checkFor(Token.COMMA, 2301, "Expecting narrow_(expression, type)");
		TypeReader tr = getTypeReader();
		ASTType type = tr.readType();

		if (type instanceof ASTUnresolvedType)
		{
			ASTUnresolvedType nt = (ASTUnresolvedType)type;
			exp = new ASTNarrowExpression(ve.location, nt.typename, test);
		}
		else
		{
			exp = new ASTNarrowExpression(ve.location, type, test);
		}

		checkFor(Token.KET, 2302, "Expecting ')' after narrow_ expression");
		return exp;
	}

	private ASTPreExpression readPreExpression(ASTVariableExpression ve)
		throws ParserException, LexException
	{
		ASTExpressionList args = new ASTExpressionList();
		ASTExpression function = readExpression();

		while (ignore(Token.COMMA))
		{
			args.add(readExpression());
		}

		checkFor(Token.KET, 2134, "Expecting pre_(function [,args])");

		return new ASTPreExpression(ve.location, function, args);
	}

	private ASTExpression readSetOrMapExpression(LexLocation start)
		throws ParserException, LexException
	{
		LexToken token = lastToken();

		if (token.is(Token.SET_CLOSE))
		{
			nextToken();
			return new ASTSetEnumExpression(start);		// empty set
		}
		else if (token.is(Token.MAPLET))
		{
			nextToken();
			checkFor(Token.SET_CLOSE, 2135, "Expecting '}' in empty map");
			return new ASTMapEnumExpression(start);		// empty map
		}

		ASTExpression first = readExpression();
		token = lastToken();

		if (token.is(Token.MAPLET))
		{
			nextToken();
			ASTMapletExpression maplet = new ASTMapletExpression(first, token, readExpression());
			return readMapExpression(start, maplet);
		}
		else
		{
			return readSetExpression(start, first);
		}
	}

	private ASTSetExpression readSetExpression(LexLocation start, ASTExpression first)
		throws ParserException, LexException
	{
		ASTSetExpression result = null;

		if (lastToken().is(Token.PIPE))
		{
			nextToken();
			BindReader br = getBindReader();
			ASTMultipleBindList bindings = br.readBindList();
			ASTExpression exp = null;

			if (lastToken().is(Token.AMPERSAND))
			{
				nextToken();
				exp = readExpression();
			}

			checkFor(Token.SET_CLOSE, 2136, "Expecting '}' after set comprehension");
			result = new ASTSetCompExpression(start, first, bindings, exp);
		}
		else
		{
			if (lastToken().is(Token.COMMA))
			{
				reader.push();

				if (nextToken().is(Token.RANGE))
				{
					nextToken();
					checkFor(Token.COMMA, 2137, "Expecting 'e1,...,e2' in set range");
					ASTExpression end = readExpression();
					checkFor(Token.SET_CLOSE, 2138, "Expecting '}' after set range");
					reader.unpush();
					return new ASTSetRangeExpression(start, first, end);
				}

				reader.pop();	// Not a set range then...
			}

			ASTExpressionList members = new ASTExpressionList();
			members.add(first);

			while (ignore(Token.COMMA))
			{
				members.add(readExpression());
			}

			checkFor(Token.SET_CLOSE, 2139, "Expecting '}' after set enumeration");
			result = new ASTSetEnumExpression(start, members);
		}

		return result;
	}

	private ASTMapExpression readMapExpression(LexLocation start, ASTMapletExpression first)
		throws ParserException, LexException
	{
		ASTMapExpression result = null;

		if (lastToken().is(Token.PIPE))
		{
			nextToken();
			BindReader br = getBindReader();
			ASTMultipleBindList bindings = br.readBindList();
			ASTExpression exp = null;

			if (lastToken().is(Token.AMPERSAND))
			{
				nextToken();
				exp = readExpression();
			}

			checkFor(Token.SET_CLOSE, 2140, "Expecting '}' after map comprehension");
			result = new ASTMapCompExpression(start, first, bindings, exp);
		}
		else
		{
			ASTMapletExpressionList members = new ASTMapletExpressionList();
			members.add(first);

			while (ignore(Token.COMMA))
			{
				ASTExpression member = readExpression();
				LexToken token = lastToken();

				if (token.is(Token.MAPLET))
				{
					nextToken();
					ASTMapletExpression maplet = new ASTMapletExpression(member, token, readExpression());
					members.add(maplet);
				}
				else
				{
					throwMessage(2039, "Expecting maplet in map enumeration");
				}
			}

			checkFor(Token.SET_CLOSE, 2141, "Expecting '}' after map enumeration");
			result = new ASTMapEnumExpression(start, members);
		}

		return result;
	}

	private ASTSeqExpression readSeqExpression(LexLocation start)
		throws ParserException, LexException
	{
		if (lastToken().is(Token.SEQ_CLOSE))
		{
			nextToken();
			return new ASTSeqEnumExpression(start);		// empty list
		}

		ASTSeqExpression result = null;
		ASTExpression first = readExpression();

		if (lastToken().is(Token.PIPE))
		{
			nextToken();
			BindReader br = getBindReader();
			ASTBind bind = br.readSetSeqBind();
			ASTExpression exp = null;

			if (lastToken().is(Token.AMPERSAND))
			{
				nextToken();
				exp = readExpression();
			}

			checkFor(Token.SEQ_CLOSE, 2142, "Expecting ']' after list comprehension");
			result = new ASTSeqCompExpression(start, first, bind, exp);
		}
		else
		{
			ASTExpressionList members = new ASTExpressionList();
			members.add(first);

			while (ignore(Token.COMMA))
			{
				members.add(readExpression());
			}

			checkFor(Token.SEQ_CLOSE, 2143, "Expecting ']' after list enumeration");
			result = new ASTSeqEnumExpression(start, members);
		}

		return result;
	}

	private ASTIfExpression readIfExpression(LexLocation start)
		throws ParserException, LexException
	{
		ASTExpression exp = readExpression();
		checkFor(Token.THEN, 2144, "Missing 'then'");
		ASTExpression thenExp = readExpression();
		ASTElseIfExpressionList elseList = new ASTElseIfExpressionList();

		while (lastToken().is(Token.ELSEIF))
		{
			nextToken();
			elseList.add(readElseIfExpression(lastToken().location));
		}

		ASTExpression elseExp = null;

		if (lastToken().is(Token.ELSE))
		{
			nextToken();
			elseExp = readConnectiveExpression();	// Precedence < maplet?
		}
		else
		{
			throwMessage(2040, "Expecting 'else' in 'if' expression");
		}

		return new ASTIfExpression(start, exp, thenExp, elseList, elseExp);
	}


	private ASTElseIfExpression readElseIfExpression(LexLocation start)
		throws ParserException, LexException
	{
		ASTExpression exp = readExpression();
		checkFor(Token.THEN, 2145, "Missing 'then' after 'elseif'");
		ASTExpression thenExp = readExpression();
		return new ASTElseIfExpression(start, exp, thenExp);
	}

	private ASTCasesExpression readCasesExpression(LexLocation start)
		throws ParserException, LexException
	{
		ASTExpression exp = readExpression();
		checkFor(Token.COLON, 2146, "Expecting ':' after cases expression");

		ASTCaseAlternativeList cases = new ASTCaseAlternativeList();
		ASTExpression others = null;
		cases.addAll(readCaseAlternatives(exp));

		while (lastToken().is(Token.COMMA))
		{
			if (nextToken().is(Token.OTHERS))
			{
				nextToken();
				checkFor(Token.ARROW, 2147, "Expecting '->' after others");
				others = readExpression();
				break;
			}
			else
			{
				cases.addAll(readCaseAlternatives(exp));
			}
		}

		checkFor(Token.END, 2148, "Expecting ', case alternative' or 'end' after cases");
		return new ASTCasesExpression(start, exp, cases, others);
	}

	private ASTCaseAlternativeList readCaseAlternatives(ASTExpression exp)
		throws ParserException, LexException
	{
		ASTCaseAlternativeList alts = new ASTCaseAlternativeList();
		ASTPatternList plist = getPatternReader().readPatternList();
		checkFor(Token.ARROW, 2149, "Expecting '->' after case pattern list");
		ASTExpression then = readExpression();

		for (ASTPattern p: plist)
		{
			alts.add(new ASTCaseAlternative(exp, p, then));
		}

		return alts;
	}

	private ASTExpression readLetExpression(LexLocation start)
		throws ParserException, LexException
	{
		ParserException letDefError = null;

		try
		{
			reader.push();
			ASTLetDefExpression exp = readLetDefExpression(start);
			reader.unpush();
			return exp;
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
			ASTLetBeStExpression exp = readLetBeStExpression(start);
			reader.unpush();
			return exp;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
			reader.pop();
			throw e.deeperThan(letDefError) ? e : letDefError;
		}
	}

	private ASTLetDefExpression readLetDefExpression(LexLocation start)
		throws ParserException, LexException
	{
		DefinitionReader dr = getDefinitionReader();
		ASTDefinitionList localDefs = new ASTDefinitionList();

		localDefs.add(dr.readLocalDefinition());

		while (ignore(Token.COMMA))
		{
			localDefs.add(dr.readLocalDefinition());
		}

		checkFor(Token.IN, 2150, "Expecting 'in' after local definitions");
		// Note we read a Connective expression for the body, so that |->
		// terminates the parse.
		return new ASTLetDefExpression(start, localDefs, readConnectiveExpression());
	}

	private ASTLetBeStExpression readLetBeStExpression(LexLocation start)
		throws ParserException, LexException
	{
		ASTMultipleBind bind = getBindReader().readMultipleBind();
		ASTExpression stexp = null;

		if (lastToken().is(Token.BE))
		{
			nextToken();
			checkFor(Token.ST, 2151, "Expecting 'st' after 'be' in let expression");
			stexp = readExpression();
		}

		checkFor(Token.IN, 2152, "Expecting 'in' after bind in let expression");
		// Note we read a Connective expression for the body, so that |->
		// terminates the parse.
		return new ASTLetBeStExpression(start, bind, stexp, readConnectiveExpression());
	}

	private ASTForAllExpression readForAllExpression(LexLocation start)
		throws ParserException, LexException
	{
		ASTMultipleBindList bindList = getBindReader().readBindList();
		checkFor(Token.AMPERSAND, 2153, "Expecting '&' after bind list in forall");
		return new ASTForAllExpression(start, bindList, readExpression());
	}

	private ASTExistsExpression readExistsExpression(LexLocation start)
		throws ParserException, LexException
	{
		ASTMultipleBindList bindList = getBindReader().readBindList();
		checkFor(Token.AMPERSAND, 2154, "Expecting '&' after bind list in exists");
		return new ASTExistsExpression(start, bindList, readExpression());
	}

	private ASTExists1Expression readExists1Expression(LexLocation start)
		throws ParserException, LexException
	{
		ASTBind bind = getBindReader().readBind();
		checkFor(Token.AMPERSAND, 2155, "Expecting '&' after single bind in exists1");
		return new ASTExists1Expression(start, bind, readExpression());
	}

	private ASTIotaExpression readIotaExpression(LexLocation start)
		throws ParserException, LexException
	{
		ASTBind bind = getBindReader().readBind();
		checkFor(Token.AMPERSAND, 2156, "Expecting '&' after single bind in iota");
		return new ASTIotaExpression(start, bind, readExpression());
	}

	private ASTLambdaExpression readLambdaExpression(LexLocation start)
		throws ParserException, LexException
	{
		ASTTypeBindList bindList = getBindReader().readTypeBindList();
		checkFor(Token.AMPERSAND, 2157, "Expecting '&' after bind list in lambda");
		return new ASTLambdaExpression(start, bindList, readExpression());
	}

	private ASTDefExpression readDefExpression(LexLocation start)
		throws ParserException, LexException
	{
		DefinitionReader dr = getDefinitionReader();
		ASTDefinitionList equalsDefs = new ASTDefinitionList();

		while (lastToken().isNot(Token.IN))
		{
			equalsDefs.add(dr.readEqualsDefinition());
			ignore(Token.SEMICOLON);
		}

		checkFor(Token.IN, 2158, "Expecting 'in' after equals definitions");
		return new ASTDefExpression(start, equalsDefs, readExpression());
	}

	private ASTNewExpression readNewExpression(LexLocation start)
		throws ParserException, LexException
	{
		LexIdentifierToken name = readIdToken("Expecting class name after 'new'");
		checkFor(Token.BRA, 2159, "Expecting '(' after new class name");

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
    	return new ASTNewExpression(start, name, args);
    }

	private ASTIsOfBaseClassExpression readIsOfBaseExpression(LexLocation start)
		throws ParserException, LexException
	{
		checkFor(Token.BRA, 2160, "Expecting '(' after 'isofbase'");
    	ASTExpressionList args = readExpressionList();
    	checkFor(Token.KET, 2161, "Expecting ')' after 'isofbase' args");

    	if (args.size() != 2)
    	{
    		throwMessage(2041, "Expecting two arguments for 'isofbase'");
    	}

    	if (!(args.get(0) instanceof ASTVariableExpression))
    	{
    		throwMessage(2042, "Expecting (<class>,<exp>) arguments for 'isofbase'");
    	}

    	LexNameToken classname = ((ASTVariableExpression)args.get(0)).name;

		if (classname.old)
		{
			throwMessage(2295, "Can't use old name here", classname);
		}

		return new ASTIsOfBaseClassExpression(start, classname, args.get(1));
    }

	private ASTIsOfClassExpression readIsOfClassExpression(LexLocation start)
		throws ParserException, LexException
	{
		checkFor(Token.BRA, 2162, "Expecting '(' after 'isofclass'");
    	ASTExpressionList args = readExpressionList();
    	checkFor(Token.KET, 2163, "Expecting ')' after 'isofclass' args");

    	if (args.size() != 2)
    	{
    		throwMessage(2043, "Expecting two arguments for 'isofclass'");
    	}

    	if (!(args.get(0) instanceof ASTVariableExpression))
    	{
    		throwMessage(2044, "Expecting (<class>,<exp>) arguments for 'isofclass'");
    	}

    	LexNameToken classname = ((ASTVariableExpression)args.get(0)).name;

		if (classname.old)
		{
			throwMessage(2295, "Can't use old name here", classname);
		}

		return new ASTIsOfClassExpression(start, classname, args.get(1));
    }

	private ASTSameBaseClassExpression readSameBaseExpression(LexLocation start)
		throws ParserException, LexException
	{
		checkFor(Token.BRA, 2164, "Expecting '(' after 'samebaseclass'");
    	ASTExpressionList args = readExpressionList();
    	checkFor(Token.KET, 2165, "Expecting ')' after 'samebaseclass' args");

    	if (args.size() != 2)
    	{
    		throwMessage(2045, "Expecting two expressions in 'samebaseclass'");
    	}

    	return new ASTSameBaseClassExpression(start, args);
    }

	private ASTSameClassExpression readSameClassExpression(LexLocation start)
		throws ParserException, LexException
	{
		checkFor(Token.BRA, 2166, "Expecting '(' after 'sameclass'");
    	ASTExpressionList args = readExpressionList();
    	checkFor(Token.KET, 2167, "Expecting ')' after 'sameclass' args");

    	if (args.size() != 2)
    	{
    		throwMessage(2046, "Expecting two expressions in 'sameclass'");
    	}

    	return new ASTSameClassExpression(start, args.get(0), args.get(1));
    }

	private boolean inPerExpression = false;

	public ASTExpression readPerExpression() throws ParserException, LexException
	{
		inPerExpression = true;
		ASTExpression e = readExpression();
		inPerExpression = false;
		return e;
	}

	private ASTExpression readHistoryExpression(LexLocation location)
		throws ParserException, LexException
	{
		if (!inPerExpression)
		{
			throwMessage(2047, "Can't use history expression here");
		}

		LexToken op = lastToken();
		String s = op.type.toString().toLowerCase();

		switch (op.type)
		{
			case ACT:
			case FIN:
			case ACTIVE:
			case REQ:
			case WAITING:
				nextToken();
				checkFor(Token.BRA, 2168, "Expecting " + s + "(name(s))");

				LexNameList opnames = new LexNameList();
				LexNameToken opname = readNameToken("Expecting a name");
				opnames.add(opname);

				while (ignore(Token.COMMA))
				{
					opname = readNameToken("Expecting " + s + "(name(s))");
					opnames.add(opname);
				}

				checkFor(Token.KET, 2169, "Expecting " + s + "(name(s))");
				return new ASTHistoryExpression(location, op.type, opnames);

			default:
				throwMessage(2048, "Expecting #act, #active, #fin, #req or #waiting");
				return null;
		}
	}
}
