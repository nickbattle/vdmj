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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSeqCompExpression extends TCSeqExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression first;
	public final TCBind bind;
	public final TCExpression predicate;
	
	private TCDefinition def = null;

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

		if (bind instanceof TCSetBind &&
			(bind.pattern.getVariableNames().size() != 1 || !def.getType().isNumeric(location)))
		{
			report(3155, "List comprehension must define one numeric bind variable");
		}

		Environment local = new FlatCheckedEnvironment(def, base, scope);
		TCType elemConstraint = null;
		
		if (constraint != null && constraint.isSeq(location))
		{
			elemConstraint = constraint.getSeq().seqof;
		}

		TCType etype = first.typeCheck(local, null, scope, elemConstraint);

		if (predicate != null)
		{
			if (!predicate.typeCheck(local, null, scope, new TCBooleanType(location)).isType(TCBooleanType.class, location))
			{
				predicate.report(3156, "Predicate is not boolean");
			}
		}

		local.unusedCheck();
		return new TCSeqType(location, etype);
	}

	@Override
	public TCNameSet getFreeVariables(Environment env)
	{
		Environment local = new FlatCheckedEnvironment(def, env, NameScope.NAMES);
		TCNameSet names = new TCNameSet();
		
		if (predicate != null)
		{
			predicate.getFreeVariables(local);
		}
		
		names.addAll(bind.getFreeVariables(local));
		return names;
	}
}
