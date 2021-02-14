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

import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.pog.EquivRelationObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SatisfiabilityObligation;
import com.fujitsu.vdmj.pog.StrictOrderObligation;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to hold a type definition.
 */
public class POTypeDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final TCInvariantType type;
	public final POPattern invPattern;
	public final POExpression invExpression;
	public final POPattern eqPattern1;
	public final POPattern eqPattern2;
	public final POExpression eqExpression;
	public final POPattern ordPattern1;
	public final POPattern ordPattern2;
	public final POExpression ordExpression;

	public POTypeDefinition(POAnnotationList annotations, TCNameToken name, TCInvariantType type,
		POPattern invPattern, POExpression invExpression,
		POPattern eqPattern1, POPattern eqPattern2, POExpression eqExpression,
		POPattern ordPattern1, POPattern ordPattern2, POExpression ordExpression)
	{
		super(name.getLocation(), name);

		this.annotations = annotations;
		this.type = type;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		this.eqPattern1 = eqPattern1;
		this.eqPattern2 = eqPattern2;
		this.eqExpression = eqExpression;
		this.ordPattern1 = ordPattern1;
		this.ordPattern2 = ordPattern2;
		this.ordExpression = ordExpression;
	}

	@Override
	public String toString()
	{
		return name.getName() + " = " + type.toDetailedString() +
				(invPattern == null ? "" :
					"\n\tinv " + invPattern + " == " + invExpression) +
        		(eqPattern1 == null ? "" :
        			"\n\teq " + eqPattern1 + " = " + eqPattern2 + " == " + eqExpression) +
        		(ordPattern1 == null ? "" :
        			"\n\tord " + ordPattern1 + " = " + ordPattern2 + " == " + ordExpression);
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list =
				(annotations != null) ? annotations.poBefore(this, ctxt) : new ProofObligationList();

		if (invExpression != null)
		{
			list.addAll(invExpression.getProofObligations(ctxt));
			list.add(new SatisfiabilityObligation(this, ctxt));
		}

		if (eqExpression != null)
		{
			list.addAll(eqExpression.getProofObligations(ctxt));
			list.add(new EquivRelationObligation(this, ctxt));
		}

		if (ordExpression != null)
		{
			list.addAll(ordExpression.getProofObligations(ctxt));
			list.add(new StrictOrderObligation(this, ctxt));
		}

		if (annotations != null) annotations.poAfter(this, list, ctxt);
		return list;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTypeDefinition(this, arg);
	}
}
