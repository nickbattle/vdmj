/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package annotations.ast;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;

abstract public class ASTConjectureAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;

	public ASTConjectureAnnotation(LexIdentifierToken name)
	{
		super(name);
	}
	
	@Override
	public ASTExpressionList parse(LexTokenReader ltr) throws LexException, ParserException
	{
		ASTExpressionList args = new ASTExpressionList();
		
		try
		{
			if (ltr.nextToken().is(Token.BRA))
			{
				if (ltr.nextToken().isNot(Token.KET))
				{
					ExpressionReader er = new ExpressionReader(ltr);
					args.add(er.readPerExpression());
			
					while (ltr.getLast().is(Token.COMMA))
					{
						ltr.nextToken();
						args.add(er.readPerExpression());	// To allow #req, #act, #fin
					}
				}

				if (ltr.getLast().isNot(Token.KET))
				{
					parseException("Expecting ')' after annotation", ltr.getLast().location);
				}
			}
		}
		catch (Exception e)
		{
			// Return blank args, which will probably cause a TC error too, rather than
			// cause this annotation to be ignored.
			args.clear();
		}
		
		return args;
	}

}
