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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

/**
 * A VDM class invariant definition.
 */
public class TCClassInvariantDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCExpression expression;

	public TCClassInvariantDefinition(TCAccessSpecifier accessSpecifier, TCNameToken name, TCExpression expression)
	{
		super(Pass.DEFS, name.getLocation(), name, NameScope.GLOBAL);
		
		this.accessSpecifier = accessSpecifier;
		this.expression = expression;
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		return null;		// We can never find inv_C().
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList();
	}

	@Override
	public TCType getType()
	{
		return new TCBooleanType(location);
	}

	@Override
	public String toString()
	{
		return "inv " + expression;
	}
	
	@Override
	public String kind()
	{
		return "invariant";
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		Environment functional = new FlatEnvironment(base, true, true);
		functional.setEnclosingDefinition(this);
		TCType type = expression.typeCheck(functional, null, NameScope.NAMESANDSTATE, new TCBooleanType(location));

		if (!type.isType(TCBooleanType.class, location))
		{
			report(3013, "Class invariant is not a boolean expression");
		}
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseClassInvariantDefinition(this, arg);
	}
}
