/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.syntax.TypeReader;

public class ASTQuickCheckAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;
	
	public ASTParameterType qcParam = null;
	public ASTTypeList qcTypes = null;

	public ASTQuickCheckAnnotation(LexIdentifierToken name)
	{
		super(name);
	}
	
	/**
	 * Override the default parse, and look for @QuickCheck @T = <type> [,<type>*];
	 */
	@Override
	public void parse(LexTokenReader ltr) throws LexException, ParserException
	{
		ltr.nextToken();
		TypeReader er = new TypeReader(ltr);
		ASTType start = er.readType();
		qcTypes = new ASTTypeList();
		
		if (start instanceof ASTParameterType)
		{
			qcParam = (ASTParameterType)start;

			if (!ltr.getLast().is(Token.EQUALS))
			{
				parseException("expecting @T = <type>;", qcParam.location);
			}
			
			do
			{
				ltr.nextToken();
				ASTType type = er.readType();
				qcTypes.add(type);
			}
			while (ltr.getLast().is(Token.COMMA));
		}
		else
		{
			parseException("expecting @T = <type> [,<type>*];", start.location);
		}
		
		if (ltr.getLast().isNot(Token.SEMICOLON))
		{
			parseException("missing ;", ltr.getLast().location);
		}
	}
}
