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
import com.fujitsu.vdmj.tc.expressions.TCUndefinedExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCSystemDefinition extends TCClassDefinition
{
	private static final long serialVersionUID = 1L;

	public TCSystemDefinition(TCNameToken className, TCDefinitionList members)
	{
		super(className, new TCNameList(), members);
	}

	@Override
	public void implicitDefinitions(Environment publicClasses)
	{
		super.implicitDefinitions(publicClasses);

		for (TCDefinition d: definitions)
		{
			if (d instanceof TCInstanceVariableDefinition)
			{
				TCInstanceVariableDefinition iv = (TCInstanceVariableDefinition)d;

				if (iv.type instanceof TCUnresolvedType &&
					iv.expression instanceof TCUndefinedExpression)
				{
					TCUnresolvedType ut = (TCUnresolvedType)iv.type;

					if (ut.typename.getName().equals("BUS"))
					{
						d.warning(5014, "Uninitialized BUS unmapped");
					}
				}
			}
			else if (d instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition edef = (TCExplicitOperationDefinition)d;

				if (!edef.name.getName().equals(name.getName()) ||
					!edef.parameterPatterns.isEmpty())
				{
					d.report(3285, "System class can only define a default constructor");
				}
			}
			else if (d instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition idef = (TCImplicitOperationDefinition)d;

				if (!d.name.getName().equals(name.getName()))
				{
					d.report(3285, "System class can only define a default constructor");
				}

				if (idef.body == null)
				{
					d.report(3283, "System class constructor cannot be implicit");
				}
			}
			else
			{
				d.report(3284, "System class can only define instance variables and a constructor");
			}
		}
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSystemDefinition(this, arg);
	}
}
