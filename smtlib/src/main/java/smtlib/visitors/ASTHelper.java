/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package smtlib.visitors;

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCType;

public interface ASTHelper
{
	public static LexLocation ANYWHERE = LexLocation.ANY;
	public static TCType INT = new TCIntegerType(ANYWHERE);
	public static LexToken GE = new LexKeywordToken(Token.GE, ANYWHERE);
	public static LexToken GT = new LexKeywordToken(Token.GT, ANYWHERE);
	public static LexToken LE = new LexKeywordToken(Token.LE, ANYWHERE);
	public static LexToken LT = new LexKeywordToken(Token.LT, ANYWHERE);
	public static LexToken AND = new LexKeywordToken(Token.AND, ANYWHERE);
	public static LexToken OR = new LexKeywordToken(Token.OR, ANYWHERE);
	public static LexToken EQ = new LexKeywordToken(Token.EQ, ANYWHERE);
	public static LexToken NE = new LexKeywordToken(Token.NE, LexLocation.ANY);
	public static LexToken IMPLIES = new LexKeywordToken(Token.IMPLIES, LexLocation.ANY);
	public static LexToken INSET = new LexKeywordToken(Token.INSET, ANYWHERE);
	public static LexToken DIVIDE = new LexKeywordToken(Token.DIVIDE, LexLocation.ANY);
	public static LexToken DIV = new LexKeywordToken(Token.DIV, LexLocation.ANY);
	public static LexIntegerToken ZEROT = new LexIntegerToken(0, LexLocation.ANY);
	
	public static TCIntegerLiteralExpression ZERO = new TCIntegerLiteralExpression(ZEROT);
}
