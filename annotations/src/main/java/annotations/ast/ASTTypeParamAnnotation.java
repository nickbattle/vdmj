/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.types.ASTParameterType;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.syntax.TypeReader;

public class ASTTypeParamAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;
	
	public ASTParameterType qcParam = null;
	public ASTType qcType = null;

	public ASTTypeParamAnnotation(LexIdentifierToken name)
	{
		super(name);
	}
	
	@Override
	public String toString()
	{
		return "@" + name + " " + qcParam + " = " + qcType;
	}

	/**
	 * Override the default parse, and look for @TypeParam @T = <type>;
	 */
	@Override
	public void parse(LexTokenReader ltr) throws LexException, ParserException
	{
		ltr.nextToken();
		
		switch (ltr.getLast().type)
		{
			case AT:
				TypeReader tr = new TypeReader(ltr);
				ASTType start = tr.readType();
				
				if (start instanceof ASTParameterType)
				{
					qcParam = (ASTParameterType)start;
		
					if (!ltr.getLast().is(Token.EQUALS))
					{
						parseException("expecting @T = <type>;", qcParam.location);
					}
					
					ltr.nextToken();
					qcType = tr.readType();
				}
				else
				{
					parseException("expecting @T = <type>;", start.location);
				}
				break;
				
			default:
				parseException("expecting @T = <type>;", ltr.getLast().location);
				break;
		}
		
		if (ltr.getLast().isNot(Token.SEMICOLON))
		{
			parseException("missing ;", ltr.getLast().location);
		}
	}
}
