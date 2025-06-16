/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package examples.v2c.tr.definitions;

import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.tc.patterns.TCPattern;

import examples.v2c.tr.expressions.TRExpression;
import examples.v2c.tr.types.TRType;

public class TRValueDefinition extends TRDefinition
{
	private static final long serialVersionUID = 1L;
	private final String pattern;
	private final TRType type;
	private final TRExpression exp;
	
	public TRValueDefinition(LexCommentList comments, TCPattern pattern, TRType type, TRExpression exp)
	{
		super(comments);
		this.pattern = pattern.toString();
		this.type = type;
		this.exp = exp;
	}

	@Override
	public String translate()
	{
		return super.translate() + type.translate() + " " + pattern + " = " + exp.translate() + ";\n";
	}
}
