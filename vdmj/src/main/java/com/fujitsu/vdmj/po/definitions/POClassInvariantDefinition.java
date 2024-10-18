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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.StateInvariantObligation;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

/**
 * A VDM class invariant definition.
 */
public class POClassInvariantDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final POExpression expression;

	public POClassInvariantDefinition(TCNameToken name, POExpression expression, POClassDefinition classDefinition)
	{
		super(name.getLocation(), name);
		this.expression = expression;
		this.classDefinition = classDefinition;
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
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList list = new ProofObligationList();

		if (!classDefinition.hasConstructors)
		{
			list.add(new StateInvariantObligation(this, ctxt).markUnchecked(ProofObligation.UNCHECKED_VDMPP));
		}

		return list;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseClassInvariantDefinition(this, arg);
	}
}
