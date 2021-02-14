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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.definitions.TCQualifiedDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLetBeStStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCMultipleBind bind;
	public final TCExpression suchThat;
	public final TCStatement statement;
	public TCMultiBindListDefinition def = null;

	public TCLetBeStStatement(LexLocation location, TCMultipleBind bind,
		TCExpression suchThat, TCStatement statement)
	{
		super(location);
		this.bind = bind;
		this.suchThat = suchThat;
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return "let " + bind +
			(suchThat == null ? "" : " be st " + suchThat) + " in " + statement;
	}

	@Override
	public TCType typeCheck(Environment base, NameScope scope, TCType constraint, boolean mandatory)
	{
		def = new TCMultiBindListDefinition(location, bind.getMultipleBindList());
		def.typeCheck(new FlatEnvironment(base, Settings.strict, false), scope);	// NB. functional if -strict
		
		// Definitions create by the let statement are not references to state, so they
		// cannot be updated. Therefore we wrap them in a local TCQualifiedDefinition.
		TCDefinitionList qualified = new TCDefinitionList();
		
		for (TCDefinition d: def.getDefinitions())
		{
			qualified.add(new TCQualifiedDefinition(d, NameScope.LOCAL));
		}
		
		Environment local = new FlatCheckedEnvironment(qualified, base, scope);

		if (suchThat != null && !suchThat.typeCheck(local, null, scope, null).isType(TCBooleanType.class, location))
		{
			report(3225, "Such that clause is not boolean");
		}

		TCType r = statement.typeCheck(local, scope, constraint, mandatory);
		local.unusedCheck();
		return r;
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLetBeStStatement(this, arg);
	}
}
