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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import smtlib.ast.Source;
import smtlib.ast.Text;

/**
 * Generate fresh names
 */
public class LocalNames
{
	// private static int count = 1;

	public static void reset()
	{
		// count = 1;
	}

	public static Source get(String name)
	{
		return new Text("$_" + name);
	}

	public static TCNameToken token(String name)
	{
		return new TCNameToken(LexLocation.ANY, "?", "$_" + name);
	}

	public static TCVariableExpression variable(String name)
	{
		TCNameToken loop = token(name);
		return new TCVariableExpression(LexLocation.ANY, loop, loop.getName());
	}

	public static TCIdentifierPattern pattern(String name)
	{
		return new TCIdentifierPattern(token(name));
	}
}
