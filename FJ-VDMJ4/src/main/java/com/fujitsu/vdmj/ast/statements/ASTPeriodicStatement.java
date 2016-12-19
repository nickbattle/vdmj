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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.statements;

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.util.Utils;

public class ASTPeriodicStatement extends ASTStatement
{
	private static final long serialVersionUID = 1L;
	public final LexNameToken opname;
	public final ASTExpressionList args;

	public long[] values = new long[4];

	public ASTPeriodicStatement(LexNameToken opname, ASTExpressionList args)
	{
		super(opname.location);
		this.opname = opname;
		this.args = args;
	}

	@Override
	public String kind()
	{
		return "periodic statement";
	}

	@Override
	public String toString()
	{
		return "periodic(" + Utils.listToString(args) + ")(" + opname + ")";
	}
}
