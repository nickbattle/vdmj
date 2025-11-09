
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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCExistsExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCMultipleBindList bindList;
	public final TCExpression predicate;
	
	public TCDefinition def = null;
	public boolean bindsUsed = true;		// True if some binding(s) used in predicate

	public TCExistsExpression(LexLocation location, TCMultipleBindList bindList, TCExpression predicate)
	{
		super(location);
		this.bindList = bindList;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "(exists " + bindList + " & " + predicate + ")";
	}

	@Override
	public TCType typeCheck(Environment base, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		def = new TCMultiBindListDefinition(location, bindList);
		def.typeCheck(base, scope);

		Environment local = new FlatCheckedEnvironment(def, base, scope);

		if (!predicate.typeCheck(local, null, scope, new TCBooleanType(location)).isType(TCBooleanType.class, location))
		{
			predicate.report(3089, "Predicate is not boolean");
		}

		bindsUsedCheck(local);
		return checkConstraint(constraint, new TCBooleanType(location));
	}

	private void bindsUsedCheck(Environment local)
	{
		TCDefinitionList list = def.getDefinitions();
		this.bindsUsed = list.isEmpty();

		for (TCDefinition d: list)
		{
			if (d.used)
			{
				this.bindsUsed = true;	// At least one!
				break;
			}
		}
		
		local.unusedCheck();	// Raise warnings last, marks as used
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExistsExpression(this, arg);
	}
}
