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
import com.fujitsu.vdmj.ast.patterns.ASTBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBindList;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleSeqBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleSetBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleTypeBind;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPatternBind;
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.patterns.ASTSeqBind;
import com.fujitsu.vdmj.ast.patterns.ASTSetBind;
import com.fujitsu.vdmj.ast.patterns.ASTTypeBind;
import com.fujitsu.vdmj.ast.patterns.ASTTypeBindList;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;


/**
 * A syntax analyser to parse set and type binds.
 */

public class BindReader extends SyntaxReader
{
	public BindReader(LexTokenReader reader)
	{
		super(reader);
	}

	public ASTPatternBind readPatternOrBind() throws ParserException, LexException
	{
		ParserException bindError = null;

		try
		{
			reader.push();
			ASTBind bind = readBind();
			reader.unpush();
			return new ASTPatternBind(bind.location, bind);
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
			reader.pop();
			bindError = e;
		}

		try
		{
			reader.push();
			ASTPattern p = getPatternReader().readPattern();
			reader.unpush();
			return new ASTPatternBind(p.location, p);
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
			reader.pop();
			throw e.deeperThan(bindError) ? e : bindError;
		}
	}

	public ASTBind readBind() throws ParserException, LexException
	{
		ParserException setBindError = null;

		try
		{
			reader.push();
			ASTBind bind = readSetSeqBind();
			reader.unpush();
			return bind;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
    		reader.pop();
    		setBindError = e;
		}

		try
		{
			reader.push();
			ASTBind bind = readTypeBind();
			reader.unpush();
			return bind;
		}
		catch (ParserException e)
		{
			e.adjustDepth(reader.getTokensRead());
    		reader.pop();
			throw e.deeperThan(setBindError) ? e : setBindError;
		}
	}

	public ASTBind readSetSeqBind() throws LexException, ParserException
	{
		ASTPattern pattern = getPatternReader().readPattern();

		if (lastToken().is(Token.IN))
		{
			switch (nextToken().type)
			{
				case SET:
    				nextToken();
    				return new ASTSetBind(pattern, getExpressionReader().readExpression());

				case SEQ:
    				if (Settings.release == Release.CLASSIC)
    				{
    					throwMessage(2328, "Sequence binds are not available in classic");
    				}
    
    				nextToken();
    				return new ASTSeqBind(pattern, getExpressionReader().readExpression());
    				
    			default:
    				throwMessage(2000, "Expecting 'in set' or 'in seq' after pattern in binding");
			}
		}
		else
		{
			throwMessage(2001, "Expecting 'in set' or 'in seq' in bind");
		}
		
		return null;	// Not reached
	}

	public ASTTypeBind readTypeBind() throws LexException, ParserException
	{
		ASTPattern pattern = getPatternReader().readPattern();
		ASTTypeBind tb = null;

		if (lastToken().is(Token.COLON))
		{
			nextToken();
			tb = new ASTTypeBind(pattern, getTypeReader().readType());
		}
		else
		{
			throwMessage(2002, "Expecting ':' in type bind");
		}

		return tb;
	}

	public ASTTypeBindList readTypeBindList() throws ParserException, LexException
	{
		ASTTypeBindList list = new ASTTypeBindList();
		list.add(readTypeBind());

		while (ignore(Token.COMMA))
		{
			list.add(readTypeBind());
		}

		return list;
	}

	public ASTMultipleBind readMultipleBind() throws LexException, ParserException
	{
		ASTPatternList plist = getPatternReader().readPatternList();
		ASTMultipleBind mb = null;

		switch (lastToken().type)
		{
			case IN:
				switch (nextToken().type)
				{
					case SET:
    					nextToken();
    					mb = new ASTMultipleSetBind(plist, getExpressionReader().readExpression());
    					break;

					case SEQ:
    					if (Settings.release == Release.CLASSIC)
    					{
    						throwMessage(2328, "Sequence binds are not available in classic");
    					}
    
    					nextToken();
    					mb = new ASTMultipleSeqBind(plist, getExpressionReader().readExpression());
    					break;
    					
    				default:
    					throwMessage(2003, "Expecting 'in set' or 'in seq' after pattern in binding");
				}
				break;

			case COLON:
				nextToken();
				mb = new ASTMultipleTypeBind(plist, getTypeReader().readType());
				break;

			default:
				throwMessage(2004, "Expecting 'in set', 'in seq' or ':' after patterns");
		}

		return mb;
	}

	public ASTMultipleBindList readBindList() throws ParserException, LexException
	{
		ASTMultipleBindList list = new ASTMultipleBindList();
		list.add(readMultipleBind());

		while (ignore(Token.COMMA))
		{
			list.add(readMultipleBind());
		}

		return list;
	}
}
