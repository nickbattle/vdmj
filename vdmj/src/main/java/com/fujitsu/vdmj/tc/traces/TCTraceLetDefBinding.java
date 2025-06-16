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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.traces;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A class representing a let-definition trace binding.
 */
public class TCTraceLetDefBinding extends TCTraceDefinition
{
    private static final long serialVersionUID = 1L;
	public final TCDefinitionList localDefs;
	public final TCTraceDefinition body;

	public TCTraceLetDefBinding(
		LexLocation location, TCDefinitionList localDefs, TCTraceDefinition body)
	{
		super(location);
		this.localDefs = localDefs;
		this.body = body;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder("let ");

		for (TCDefinition d: localDefs)
		{
			result.append(d.toString());
			result.append(" ");
		}

		result.append("in ");
		result.append(body);

		return result.toString();
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		Environment local = base;

		for (TCDefinition d: localDefs)
		{
			d.typeResolve(base);
			d.typeCheck(local, scope);
			local = new FlatCheckedEnvironment(d, local, scope);
		}

		body.typeCheck(local, scope);
		local.unusedCheck(base);
	}
}
