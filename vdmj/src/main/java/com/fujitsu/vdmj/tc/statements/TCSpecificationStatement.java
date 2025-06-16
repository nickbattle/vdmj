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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSpecificationStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExternalClauseList externals;
	public final TCExpression precondition;
	public final TCExpression postcondition;
	public final TCErrorCaseList errors;
	
	public TCStateDefinition stateDefinition;

	public TCSpecificationStatement(LexLocation location,
		TCExternalClauseList externals, TCExpression precondition,
		TCExpression postcondition, TCErrorCaseList errors)
	{
		super(location);

		this.externals = externals;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.errors = errors;
	}

	@Override
	public String toString()
	{
		return "[" +
    		(externals == null ? "" : "\n\text " + externals) +
    		(precondition == null ? "" : "\n\tpre " + precondition) +
    		(postcondition == null ? "" : "\n\tpost " + postcondition) +
    		(errors == null ? "" : "\n\terrs " + errors) + "]";
	}

	@Override
	public TCType typeCheck(Environment base, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCDefinitionList defs = new TCDefinitionList();

		// Now we build local definitions for each of the externals, so
		// that they can be added to the local environment, while the
		// global state is made inaccessible.

		if (externals != null)
		{
    		for (TCExternalClause clause: externals)
    		{
    			for (TCNameToken name: clause.identifiers)
    			{
    				if (base.findName(name, NameScope.STATE) == null)
    				{
    					name.report(3274, "External variable is not in scope: " + name);
    				}
    				else
    				{
    					defs.add(new TCLocalDefinition(name.getLocation(), name, clause.type));
    				}
    			}
    		}
		}

		if (errors != null)
		{
			for (TCErrorCase err: errors)
			{
				TCType lt = err.left.typeCheck(base, null, NameScope.NAMESANDSTATE, null);
				TCType rt = err.right.typeCheck(base, null, NameScope.NAMESANDSTATE, null);

				if (!lt.isType(TCBooleanType.class, location))
				{
					err.left.report(3275, "Error clause must be a boolean");
				}

				if (!rt.isType(TCBooleanType.class, location))
				{
					err.right.report(3275, "Error clause must be a boolean");
				}
			}
		}

		defs.typeCheck(base, scope);
		Environment local = new FlatEnvironment(defs, base);	// NB. No check

		if (precondition != null &&
			!precondition.typeCheck(local, null, NameScope.NAMESANDSTATE, null).isType(TCBooleanType.class, location))
		{
			precondition.report(3233, "Precondition is not a boolean expression");
		}

		if (postcondition != null &&
			!postcondition.typeCheck(local, null, NameScope.NAMESANDANYSTATE, null).isType(TCBooleanType.class, location))
		{
			postcondition.report(3234, "postcondition is not a boolean expression");
		}
		
		stateDefinition = base.findStateDefinition();

		return setType(new TCVoidType(location));
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSpecificationStatement(this, arg);
	}
}
