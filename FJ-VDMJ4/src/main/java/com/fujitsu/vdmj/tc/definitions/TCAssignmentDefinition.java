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

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to represent assignable variable definitions.
 */
public class TCAssignmentDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;

	public TCType type;
	public final TCExpression expression;
	public TCType expType;

	public TCAssignmentDefinition(TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCType type, TCExpression expression)
	{
		this(accessSpecifier, name, type, expression, NameScope.STATE);
	}

	public TCAssignmentDefinition(TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCType type, TCExpression expression, NameScope scope)
	{
		super(Pass.VALUES, name.getLocation(), name, scope);
		this.accessSpecifier = accessSpecifier;
		this.type = type;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return name + ":" + type + " := " + expression;
	}
	
	@Override
	public String kind()
	{
		return "assignment";
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(this);
	}

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList(name);
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		type = type.typeResolve(base, null);
		getDefinitions().setExcluded(true);
		expType = expression.typeCheck(base, null, scope, type);
		getDefinitions().setExcluded(false);
		
		TypeComparator.checkComposeTypes(type, base, false);

		if (expType instanceof TCVoidType)
		{
			expression.report(3048, "Expression does not return a value");
		}

		if (!TypeComparator.compatible(type, expType))
		{
			report(3000, "Expression does not match declared type");
			detail2("Declared", type, "Expression", expType);
		}
	}

	@Override
	public boolean isUpdatable()
	{
		return true;
	}
}
