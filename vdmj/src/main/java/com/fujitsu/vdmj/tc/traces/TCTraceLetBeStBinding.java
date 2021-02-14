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

package com.fujitsu.vdmj.tc.traces;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * A class representing a let-be-st trace binding.
 */
public class TCTraceLetBeStBinding extends TCTraceDefinition
{
    private static final long serialVersionUID = 1L;
	public final TCMultipleBind bind;
	public final TCExpression stexp;
	public final TCTraceDefinition body;

	private TCMultiBindListDefinition def = null;

	public TCTraceLetBeStBinding(
		LexLocation location, TCMultipleBind bind, TCExpression stexp, TCTraceDefinition body)
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

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		def = new TCMultiBindListDefinition(bind.location, bind.getMultipleBindList());
		def.typeResolve(base);
		def.typeCheck(base, scope);
		Environment local = new FlatCheckedEnvironment(def, base, scope);

		if (stexp != null &&
			!stexp.typeCheck(local, null, scope, null).isType(TCBooleanType.class, location))
		{
			TypeChecker.report(3225,
				"Such that clause is not boolean", stexp.location);
		}

		body.typeCheck(local, scope);
		local.unusedCheck();
	}
}
