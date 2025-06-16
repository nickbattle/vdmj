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
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSeqCompExpression extends TCSeqExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression first;
	public final TCBind bind;
	public final TCExpression predicate;
	
	public TCDefinition def = null;

	public TCSeqCompExpression(LexLocation start, TCExpression first, TCBind bind, TCExpression predicate)
	{
		super(start);
		this.first = first;
		this.bind = bind;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "[" + first + " | " + bind +
			(predicate == null ? "]" : " & " + predicate + "]");
	}

	@Override
	public TCType typeCheck(Environment base, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		def = new TCMultiBindListDefinition(location, bind.getMultipleBindList());
		def.typeCheck(base, scope);

		if (bind instanceof TCTypeBind)
		{
			TCTypeBind tb = (TCTypeBind)bind;
			tb.typeResolve(base);
		}

		if ((bind instanceof TCSetBind || bind instanceof TCTypeBind) &&
			(bind.pattern.getVariableNames().size() > 1 || !def.getType().isOrdered(location)))
		{
			report(3155, "Sequence comprehension must define one ordered bind variable");
		}

		Environment local = new FlatCheckedEnvironment(def, base, scope);
		TCType elemConstraint = null;
		
		if (constraint != null && constraint.isSeq(location))
		{
			elemConstraint = constraint.getSeq().seqof;
		}

		if (predicate != null)
		{
			if (!predicate.typeCheck(local, null, scope, new TCBooleanType(location)).isType(TCBooleanType.class, location))
			{
				predicate.report(3156, "Predicate is not boolean");
			}

			TCDefinitionList qualified = predicate.getQualifiedDefs(local);
			
			if (!qualified.isEmpty())
			{
				local = new FlatEnvironment(qualified, local);
			}
		}

		TCType etype = first.typeCheck(local, null, scope, elemConstraint);

		local.unusedCheck();
		return setType(new TCSeqType(location, etype));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSeqCompExpression(this, arg);
	}
}
