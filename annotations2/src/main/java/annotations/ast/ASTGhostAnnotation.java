/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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
import com.fujitsu.vdmj.ast.expressions.ASTEqualsExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.expressions.ASTVariableExpression;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;

/**
 * A test of an annotation with an overridden parse function.
 */
public class ASTGhostAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;

	public ASTGhostAnnotation(LexIdentifierToken name)
	{
		super(name);
	}
	
	/**
	 * Override the default parse, and look for @Ghost <name> = <exp>;
	 */
	@Override
	public void parse(LexTokenReader ltr) throws LexException, ParserException
	{
		this.args = new ASTExpressionList();

		ltr.nextToken();
		ExpressionReader er = new ExpressionReader(ltr);
		ASTExpression exp = er.readExpression();
		
		if (exp instanceof ASTEqualsExpression)		// Should parse as an equals expression
		{
			ASTEqualsExpression eqexp = (ASTEqualsExpression)exp;
			
			if (eqexp.left instanceof ASTVariableExpression)
			{
				args.add(eqexp.left);
				args.add(eqexp.right);
			}
			else
			{
				parseException("expecting <name> = <exp>;", eqexp.location);
			}
		}
		else
		{
			parseException("expecting <name> = <exp>;", exp.location);
		}
		
		if (ltr.getLast().isNot(Token.SEMICOLON))
		{
			parseException("missing ;", ltr.getLast().location);
		}
	}
}
