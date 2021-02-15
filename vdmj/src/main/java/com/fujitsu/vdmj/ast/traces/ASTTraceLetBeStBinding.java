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

package com.fujitsu.vdmj.ast.traces;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * A class representing a let-be-st trace binding.
 */
public class ASTTraceLetBeStBinding extends ASTTraceDefinition
{
    private static final long serialVersionUID = 1L;
	public final ASTMultipleBind bind;
	public final ASTExpression stexp;
	public final ASTTraceDefinition body;

	public ASTTraceLetBeStBinding(
		LexLocation location, ASTMultipleBind bind, ASTExpression stexp, ASTTraceDefinition body)
	{
		super(location);
		this.bind = bind;
		this.stexp = stexp;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "let " + bind +
			(stexp == null ? "" : " be st " + stexp.toString()) + " in " + body;
	}
}
