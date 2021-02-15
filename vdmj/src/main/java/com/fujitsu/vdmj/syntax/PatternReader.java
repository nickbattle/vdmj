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

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.ast.lex.LexCharacterToken;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexQuoteToken;
import com.fujitsu.vdmj.ast.lex.LexRealToken;
import com.fujitsu.vdmj.ast.lex.LexStringToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTBooleanPattern;
import com.fujitsu.vdmj.ast.patterns.ASTCharacterPattern;
import com.fujitsu.vdmj.ast.patterns.ASTConcatenationPattern;
import com.fujitsu.vdmj.ast.patterns.ASTExpressionPattern;
import com.fujitsu.vdmj.ast.patterns.ASTIdentifierPattern;
import com.fujitsu.vdmj.ast.patterns.ASTIgnorePattern;
import com.fujitsu.vdmj.ast.patterns.ASTIntegerPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapUnionPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapletPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapletPatternList;
import com.fujitsu.vdmj.ast.patterns.ASTNamePatternPair;
import com.fujitsu.vdmj.ast.patterns.ASTNamePatternPairList;
import com.fujitsu.vdmj.ast.patterns.ASTNilPattern;
import com.fujitsu.vdmj.ast.patterns.ASTObjectPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.patterns.ASTQuotePattern;
import com.fujitsu.vdmj.ast.patterns.ASTRealPattern;
import com.fujitsu.vdmj.ast.patterns.ASTRecordPattern;
import com.fujitsu.vdmj.ast.patterns.ASTSeqPattern;
import com.fujitsu.vdmj.ast.patterns.ASTSetPattern;
import com.fujitsu.vdmj.ast.patterns.ASTStringPattern;
import com.fujitsu.vdmj.ast.patterns.ASTTuplePattern;
import com.fujitsu.vdmj.ast.patterns.ASTUnionPattern;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;

/**
 * A syntax analyser to parse pattern definitions.
 */

public class PatternReader extends SyntaxReader
{
	public PatternReader(LexTokenReader reader)
	{
		super(reader);
	}

	public ASTPattern readPattern() throws ParserException, LexException
	{
		ASTPattern pattern = readSimplePattern();

		while (lastToken().is(Token.UNION) || lastToken().is(Token.CONCATENATE) || lastToken().is(Token.MUNION))
		{
			LexToken token = lastToken();

			switch (token.type)
			{
				case UNION:
					nextToken();
					pattern = new ASTUnionPattern(pattern, token.location, readPattern());
					break;

				case CONCATENATE:
					nextToken();
					pattern = new ASTConcatenationPattern(pattern, token.location, readPattern());
					break;

				case MUNION:
					if (Settings.release == Release.VDM_10)
					{
						nextToken();
						pattern = new ASTMapUnionPattern(pattern, token.location, readPattern());
					}
					else
					{
						throwMessage(2298, "Map patterns not available in VDM classic");
					}
					break;
					
				default:
					break;
			}
		}

		return pattern;
	}

	private ASTPattern readSimplePattern() throws ParserException, LexException
	{
		ASTPattern pattern = null;
		LexToken token = lastToken();
		boolean rdtok = true;

		switch (token.type)
		{
			case NUMBER:
				pattern = new ASTIntegerPattern((LexIntegerToken)token);
				break;

			case REALNUMBER:
				pattern = new ASTRealPattern((LexRealToken)token);
				break;

			case CHARACTER:
				pattern = new ASTCharacterPattern((LexCharacterToken)token);
				break;

			case STRING:
				pattern = new ASTStringPattern((LexStringToken)token);
				break;

			case QUOTE:
				pattern = new ASTQuotePattern((LexQuoteToken)token);
				break;

			case TRUE:
			case FALSE:
				pattern = new ASTBooleanPattern((LexBooleanToken)token);
				break;

			case NIL:
				pattern = new ASTNilPattern((LexKeywordToken)token);
				break;

			case BRA:
				nextToken();
				ExpressionReader expr = getExpressionReader();
				pattern = new ASTExpressionPattern(expr.readExpression());
				checkFor(Token.KET, 2180, "Mismatched brackets in pattern");
				rdtok = false;
				break;

			case SET_OPEN:
				if (nextToken().is(Token.SET_CLOSE))
				{
					pattern = new ASTSetPattern(token.location, new ASTPatternList());
				}
				else if (lastToken().is(Token.MAPLET))
				{
					if (Settings.release == Release.VDM_10)
					{
						pattern = new ASTMapPattern(token.location, new ASTMapletPatternList());
						nextToken();
						checkFor(Token.SET_CLOSE, 2299, "Expecting {|->} empty map pattern");
						rdtok = false;
					}
					else
					{
						throwMessage(2298, "Map patterns not available in VDM classic", Integer.MAX_VALUE);
					}
				}
				else
				{
					reader.push();
					readPattern();	// unmapped

					if (lastToken().is(Token.MAPLET))
					{
						reader.pop();

						if (Settings.release == Release.VDM_10)
						{
	    					pattern = new ASTMapPattern(token.location, readMapletPatternList());
						}
						else
						{
							throwMessage(2298, "Map patterns not available in VDM classic");
						}
					}
					else
					{
						reader.pop();
    					pattern = new ASTSetPattern(token.location, readPatternList());
					}

					checkFor(Token.SET_CLOSE, 2181, "Mismatched braces in pattern");
					rdtok = false;
				}
				break;

			case SEQ_OPEN:
				if (nextToken().is(Token.SEQ_CLOSE))
				{
					pattern = new ASTSeqPattern(token.location, new ASTPatternList());
				}
				else
				{
					pattern = new ASTSeqPattern(token.location, readPatternList());
					checkFor(Token.SEQ_CLOSE, 2182, "Mismatched square brackets in pattern");
					rdtok = false;
				}
				break;

			case NAME:
				throwMessage(2056, "Cannot use module'id name in patterns");
				break;

			case IDENTIFIER:
				LexIdentifierToken id = lastIdToken();
				
				if (isReserved(id.name))
				{
					throwMessage(2295, "Name " + id.name + " contains a reserved prefix", id);
				}

				if (id.name.startsWith("mk_"))
				{
					nextToken();

					if (id.name.equals("mk_"))
					{
						checkFor(Token.BRA, 2183, "Expecting '(' after mk_ tuple");
						ASTPatternList args = readPatternList();
						
						if (args.size() <= 1)
						{
							throwMessage(2183, "Tuple pattern must have >1 args");
						}
						
						pattern = new ASTTuplePattern(token.location, args);
						checkFor(Token.KET, 2184, "Expecting ')' after mk_ tuple");
					}
					else
					{
						checkFor(Token.BRA, 2185, "Expecting '(' after " + id + " record");
						LexNameToken typename = null;
						int backtick = id.name.indexOf('`');

						if (backtick >= 0)
						{
							// Strange case of "mk_MOD`name"
							String mod = id.name.substring(3, backtick);
							String name = id.name.substring(backtick + 1);
							typename = new LexNameToken(mod, name, id.location);
						}
						else
						{
							// Regular case of "mk_Name"
							LexIdentifierToken type = new LexIdentifierToken(
								id.name.substring(3), false, id.location);
							typename = idToName(type);
						}

						if (lastToken().is(Token.KET))
						{
							// An empty pattern list
							pattern = new ASTRecordPattern(typename, new ASTPatternList());
							nextToken();
						}
						else
						{
							pattern = new ASTRecordPattern(typename, readPatternList());
							checkFor(Token.KET, 2186, "Expecting ')' after " + id + " record");
						}
					}

					rdtok = false;
				}
				else if (id.name.startsWith("obj_"))	// Object pattern
				{
					if (Settings.release == Release.CLASSIC)
					{
						throwMessage(2323, "Object patterns not available in VDM classic", Integer.MAX_VALUE);
					}
					else if (id.name.equals("obj_"))
					{
						throwMessage(2319, "Expecting class name after obj_ in object pattern");
					}
					else
					{
						nextToken();
						String classname = id.name.substring(4);
						LexNameToken name = new LexNameToken("CLASS", classname, id.location);
						checkFor(Token.BRA, 2320, "Expecting '(' after obj_ pattern");
						pattern = new ASTObjectPattern(token.location, name, readNamePatternList(classname));
						checkFor(Token.KET, 2322, "Expecting ')' after obj_ pattern");
						rdtok = false;
					}
				}
				else
				{
					pattern = new ASTIdentifierPattern(idToName(id));
				}
				break;

			case MINUS:
				pattern = new ASTIgnorePattern(token.location);
				break;

			default:
				throwMessage(2057, "Unexpected token in pattern");
		}

		if (rdtok) nextToken();
		return pattern;
	}

	private ASTMapletPatternList readMapletPatternList() throws LexException, ParserException
	{
		ASTMapletPatternList list = new ASTMapletPatternList();
		list.add(readMaplet());

		while (ignore(Token.COMMA))
		{
			list.add(readMaplet());
		}

		return list;
	}

	private ASTNamePatternPair readNamePatternPair(String classname) throws LexException, ParserException
	{
		LexNameToken fieldname = lastNameToken().getModifiedName(classname);
		nextToken();
		checkFor(Token.MAPLET, 2321, "Expecting '|->' in object pattern");
		ASTPattern pattern = readPattern();

		return new ASTNamePatternPair(fieldname, pattern);
	}

	private ASTNamePatternPairList readNamePatternList(String classname) throws LexException, ParserException
	{
		ASTNamePatternPairList list = new ASTNamePatternPairList();
		
		if (lastToken().is(Token.IDENTIFIER))	// Can be empty
		{
			list.add(readNamePatternPair(classname));
	
			while (ignore(Token.COMMA))
			{
				list.add(readNamePatternPair(classname));
			}
		}

		return list;
	}

	private ASTMapletPattern readMaplet() throws LexException, ParserException
	{
		ASTPattern key = readPattern();
		checkFor(Token.MAPLET, 2297, "Expecting '|->' in map pattern");
		ASTPattern value = readPattern();

		return new ASTMapletPattern(key, value);
	}

	public ASTPatternList readPatternList() throws ParserException, LexException
	{
		ASTPatternList list = new ASTPatternList();
		list.add(readPattern());

		while (ignore(Token.COMMA))
		{
			list.add(readPattern());
		}

		return list;
	}
}
