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
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexQuoteToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.types.ASTBooleanType;
import com.fujitsu.vdmj.ast.types.ASTBracketType;
import com.fujitsu.vdmj.ast.types.ASTCharacterType;
import com.fujitsu.vdmj.ast.types.ASTField;
import com.fujitsu.vdmj.ast.types.ASTFieldList;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.ast.types.ASTInMapType;
import com.fujitsu.vdmj.ast.types.ASTIntegerType;
import com.fujitsu.vdmj.ast.types.ASTMapType;
import com.fujitsu.vdmj.ast.types.ASTNaturalOneType;
import com.fujitsu.vdmj.ast.types.ASTNaturalType;
import com.fujitsu.vdmj.ast.types.ASTOperationType;
import com.fujitsu.vdmj.ast.types.ASTOptionalType;
import com.fujitsu.vdmj.ast.types.ASTParameterType;
import com.fujitsu.vdmj.ast.types.ASTProductType;
import com.fujitsu.vdmj.ast.types.ASTQuoteType;
import com.fujitsu.vdmj.ast.types.ASTRationalType;
import com.fujitsu.vdmj.ast.types.ASTRealType;
import com.fujitsu.vdmj.ast.types.ASTRecordType;
import com.fujitsu.vdmj.ast.types.ASTSeq1Type;
import com.fujitsu.vdmj.ast.types.ASTSeqType;
import com.fujitsu.vdmj.ast.types.ASTSet1Type;
import com.fujitsu.vdmj.ast.types.ASTSetType;
import com.fujitsu.vdmj.ast.types.ASTTokenType;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.ast.types.ASTTypeSet;
import com.fujitsu.vdmj.ast.types.ASTUnionType;
import com.fujitsu.vdmj.ast.types.ASTUnknownType;
import com.fujitsu.vdmj.ast.types.ASTUnresolvedType;
import com.fujitsu.vdmj.ast.types.ASTVoidType;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;

/**
 * A syntax analyser to parse type expressions.
 */
public class TypeReader extends SyntaxReader
{
	public TypeReader(LexTokenReader reader)
	{
		super(reader);
	}

	public ASTType readType()
		throws ParserException, LexException
	{
		ASTType type = readDiscretionaryType();

		if (lastToken().is(Token.ARROW) ||
			lastToken().is(Token.TOTAL_FUNCTION))
		{
			LexToken token = lastToken();
			nextToken();
			ASTType result = readType();

			type = new ASTFunctionType(token.location,
				token.is(Token.ARROW), productExpand(type), result);
		}
		else if (type instanceof ASTVoidType)
		{
			throwMessage(2070, "Cannot use '()' type here");
		}

		return type;
	}
	
	private ASTType readDiscretionaryType()
			throws ParserException, LexException
	{
		LexToken token = lastToken();
		LexLocation location = token.location;
		ASTType type = null;

		if (token.is(Token.BRA))
		{
			reader.push();

			if (nextToken().is(Token.KET))
			{
				type = new ASTVoidType(location);
				nextToken();
				reader.unpush();
				return type;
			}
			else
			{
				reader.pop();
			}
		}

		return readUnionType();
	}

	private ASTType readUnionType()
		throws ParserException, LexException
	{
		LexToken token = lastToken();
		ASTType type = readProductType();
		ASTTypeSet union = new ASTTypeSet();
		union.add(type);

		while (lastToken().type == Token.PIPE)
		{
			nextToken();
			union.add(readProductType());
		}

		if (union.size() == 1)
		{
			return type;
		}
		else
		{
			return new ASTUnionType(token.location, union);
		}
	}

	private ASTType readProductType()
		throws ParserException, LexException
	{
		LexToken token = lastToken();
		ASTType type = readComposeType();
		ASTTypeList productList = new ASTTypeList(type);
	
		while (lastToken().type == Token.TIMES)
		{
			nextToken();
			productList.add(readComposeType());
		}
	
		if (productList.size() == 1)
		{
			return type;
		}
	
		return new ASTProductType(token.location, productList);
	}

	private ASTType readComposeType()
		throws ParserException, LexException
	{
		ASTType type = null;

		if (lastToken().is(Token.COMPOSE))
		{
			nextToken();
			LexIdentifierToken id = readIdToken("Compose not followed by record identifier");
			checkFor(Token.OF, 2249, "Missing 'of' in compose type");
			type = new ASTRecordType(idToName(id), readFieldList(), true);
			checkFor(Token.END, 2250, "Missing 'end' in compose type");
		}
		else
		{
			type = readMapType();
		}

		return type;
	}

	public ASTFieldList readFieldList() throws ParserException, LexException
	{
		ASTFieldList list = new ASTFieldList();

		while (lastToken().isNot(Token.END) &&
			   lastToken().isNot(Token.SEMICOLON) &&
			   lastToken().isNot(Token.INV))
		{
			reader.push();
			LexToken tag = lastToken();
			LexToken separator = nextToken();

			if (separator.is(Token.COLON))
			{
				if (tag.isNot(Token.IDENTIFIER))
				{
					throwMessage(2071, "Expecting field identifier before ':'");
				}

				nextToken();
				LexIdentifierToken tagid = (LexIdentifierToken)tag;

				if (tagid.old)
				{
					throwMessage(2295, "Can't use old name " + tagid + " here", tag);
				}
				
				LexNameToken tagname = idToName(tagid);
				list.add(new ASTField(tagname, tagid.name, readType(), false));
				reader.unpush();
			}
			else if (separator.is(Token.EQABST))
			{
				if (tag.isNot(Token.IDENTIFIER))
				{
					throwMessage(2072, "Expecting field name before ':-'");
				}

				nextToken();
				LexIdentifierToken tagid = (LexIdentifierToken)tag;

				if (tagid.old)
				{
					throwMessage(2295, "Can't use old name " + tagid + " here", tag);
				}

				LexNameToken tagname = idToName(tagid);
				list.add(new ASTField(tagname, tagid.name, readType(), true));
				reader.unpush();
			}
			else	// Anonymous field or end of fields
			{
				try
				{
					reader.retry();
					String anon = Integer.toString(list.size() + 1);
					ASTType ftype = readType();
					LexNameToken tagname = new LexNameToken(
						getCurrentModule(), anon, ftype.location);
					list.add(new ASTField(tagname, anon, ftype, false));
					reader.unpush();
				}
				catch (Exception e)
				{
					// End? EOF? Or badly formed type, fails elsewhere...
					reader.pop();
					break;
				}
			}
		}

		for (ASTField f1: list)
		{
			for (ASTField f2: list)
			{
				if (f1 != f2 && f1.tag.equals(f2.tag))
				{
					throwMessage(2073, "Duplicate field names in record type");
				}
			}
		}

		return list;
	}

	private ASTType readMapType()
		throws ParserException, LexException
	{
		ASTType type = null;
		LexToken token = lastToken();

		switch (token.type)
		{
			case MAP:
				nextToken();
				type = readType();	// Effectively bracketed by 'to'
				checkFor(Token.TO, 2251, "Expecting 'to' in map type");
				type = new ASTMapType(token.location, type, readComposeType());
				break;

			case INMAP:
				nextToken();
				type = readType();	// Effectively bracketed by 'to'
				checkFor(Token.TO, 2252, "Expecting 'to' in inmap type");
				type = new ASTInMapType(token.location, type, readComposeType());
				break;

			default:
				type = readSetSeqType();
				break;
		}

		return type;
	}

	private ASTType readSetSeqType()
		throws ParserException, LexException
	{
		ASTType type = null;
		LexToken token = lastToken();

		switch (token.type)
		{
			case SET:
				nextToken();
				checkFor(Token.OF, 2253, "Expecting 'of' after set");
				type = new ASTSetType(token.location, readComposeType());
				break;

			case SET1:
				if (Settings.release == Release.CLASSIC)
				{
					throwMessage(2327, "Type set1 is not available in classic");
				}

				nextToken();
				checkFor(Token.OF, 2326, "Expecting 'of' after set1");
				type = new ASTSet1Type(token.location, readComposeType());
				break;

			case SEQ:
				nextToken();
				checkFor(Token.OF, 2254, "Expecting 'of' after seq");
				type = new ASTSeqType(token.location, readComposeType());
				break;

			case SEQ1:
				nextToken();
				checkFor(Token.OF, 2255, "Expecting 'of' after seq1");
				type = new ASTSeq1Type(token.location, readComposeType());
				break;

			default:
				type = readBasicType();
				break;
		}

		return type;
	}

	private ASTType readBasicType()
		throws ParserException, LexException
	{
		ASTType type = null;
		LexToken token = lastToken();
		LexLocation location = token.location;

		switch (token.type)
		{
			case NAT:
				type = new ASTNaturalType(location);
				nextToken();
				break;

			case NAT1:
				type = new ASTNaturalOneType(location);
				nextToken();
				break;

			case BOOL:
				type = new ASTBooleanType(location);
				nextToken();
				break;

			case REAL:
				type = new ASTRealType(location);
				nextToken();
				break;

			case INT:
				type = new ASTIntegerType(location);
				nextToken();
				break;

			case RAT:
				type = new ASTRationalType(location);
				nextToken();
				break;

			case CHAR:
				type = new ASTCharacterType(location);
				nextToken();
				break;

			case TOKEN:
				type = new ASTTokenType(location);
				nextToken();
				break;

			case QUOTE:
				type = new ASTQuoteType((LexQuoteToken)token);
				nextToken();
				break;

			case BRA:
				nextToken();
				type = new ASTBracketType(location, readType());
				checkFor(Token.KET, 2256, "Bracket mismatch");
				break;

			case SEQ_OPEN:
				nextToken();
				type = new ASTOptionalType(location, readType());
				checkFor(Token.SEQ_CLOSE, 2257, "Missing close bracket after optional type");
				break;

			case IDENTIFIER:
				nextToken();
				type = new ASTUnresolvedType(idToName((LexIdentifierToken)token), ignore(Token.PLING));
				break;

			case NAME:
				nextToken();
				type = new ASTUnresolvedType((LexNameToken)token, ignore(Token.PLING));
				break;

			case AT:
				nextToken();
				type = new ASTParameterType(
						idToName(readIdToken("Invalid type parameter")));
				break;

			case QMARK:
				nextToken();
				type = new ASTUnknownType(location);	// Not strictly VDM :-)
				break;

			default:
				throwMessage(2074, "Unexpected token in type expression");
		}

		return type;
	}

	public ASTOperationType readOperationType()
		throws ParserException, LexException
	{
		ASTType paramtype = readDiscretionaryType();
		LexToken arrow = lastToken();
		checkFor(Token.OPDEF, 2258, "Expecting '==>' in explicit operation type");
		ASTType resulttype = readDiscretionaryType();
		return new ASTOperationType(arrow.location, productExpand(paramtype), resulttype);
	}

	private ASTTypeList productExpand(ASTType parameters)
	{
		ASTTypeList types = new ASTTypeList();

		if (parameters instanceof ASTProductType)
		{
			// Expand unbracketed product types
			ASTProductType pt = (ASTProductType)parameters;
			types.addAll(pt.types);
		}
		else if (parameters instanceof ASTVoidType)
		{
			// No type
		}
		else
		{
			// One parameter, including bracketed product types
			types.add(parameters);
		}

		return types;
	}
}
