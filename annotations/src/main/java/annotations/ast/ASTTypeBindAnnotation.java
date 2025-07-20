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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package annotations.ast;

import java.text.ParseException;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTSetExpression;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleTypeBind;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.syntax.BindReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;

/**
 * An annotation to override forall and exists type binds.
 */
public class ASTTypeBindAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;

	private ASTMultipleTypeBind typebind;
	private ASTExpression expression;

	public ASTTypeBindAnnotation(LexIdentifierToken name)
	{
		super(name);
	}

	@Override
	public String toString()
	{
		return "@" + name + " " + typebind + " = " + expression + ";";
	}
	
	/**
	 * Override the default parse, and look for @TypeBind &lt;typebind&gt; = &lt;exp&gt;;
	 */
	@Override
	public void parse(LexTokenReader ltr) throws LexException, ParserException
	{
		ltr.nextToken();	// Skip "@TypeBind"

		BindReader br = new BindReader(ltr);
		br.setCurrentModule(ltr.currentModule);
		ASTMultipleBind mbind = br.readMultipleBind();
		checkFor(ltr, Token.EQUALS, "Expecting <multiple type bind> '=' <set expression>;");

		ExpressionReader er = new ExpressionReader(ltr);
		er.setCurrentModule(ltr.currentModule);
		ASTExpression exp = er.readExpression();
		checkFor(ltr, Token.SEMICOLON, "Expecting semi-colon after <set expression>");

		if (mbind instanceof ASTMultipleTypeBind)
		{
			typebind = (ASTMultipleTypeBind)mbind;
			expression = exp;
			return;
		}

		parseException("Expecting <multiple type bind> '=' <set expression>;", name.location);
	}
	
	private void checkFor(LexTokenReader reader, Token expected, String message) throws LexException
	{
		LexToken last = reader.getLast();
		
		if (last.isNot(expected))
		{
			parseException(message, last.location);
		}
		
		reader.nextToken();
	}
}
