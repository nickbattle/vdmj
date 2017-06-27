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
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCMapCompExpression extends TCMapExpression
{
	private static final long serialVersionUID = 1L;
	public final TCMapletExpression first;
	public final TCMultipleBindList bindings;
	public final TCExpression predicate;
	
	private TCType maptype;
	private TCDefinition def = null;

	public TCMapCompExpression(LexLocation start,
		TCMapletExpression first, TCMultipleBindList bindings,
		TCExpression predicate)
	{
		super(start);
		this.first = first;
		this.bindings = bindings;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "{" + first + " | " + Utils.listToString(bindings) +
			(predicate == null ? "}" : " & " + predicate + "}");
	}

	@Override
	public TCType typeCheck(Environment base, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		def = new TCMultiBindListDefinition(location, bindings);
		def.typeCheck(base, scope);
		Environment local = new FlatCheckedEnvironment(def, base, scope);

		if (predicate != null &&
			!predicate.typeCheck(local, null, scope, new TCBooleanType(location)).isType(TCBooleanType.class, location))
		{
			predicate.report(3118, "Predicate is not boolean");
		}

		TCType domConstraint = null;
		TCType rngConstraint = null;
		
		if (constraint != null && constraint.isMap(location))
		{
			domConstraint = constraint.getMap().from;
			rngConstraint = constraint.getMap().to;
		}

		maptype = first.typeCheck(local, scope, domConstraint, rngConstraint);	// The map from/to type
		local.unusedCheck();
		return maptype;
	}

	@Override
	public TCNameSet getFreeVariables(Environment env)
	{
		Environment local = new FlatCheckedEnvironment(def, env, NameScope.NAMES);
		TCNameSet names = new TCNameSet();	// Note "first" is conditional
		
		if (predicate != null)
		{
			predicate.getFreeVariables(local);
		}
		
		for (TCMultipleBind mb: bindings)
		{
			names.addAll(mb.getFreeVariables(local));
		}
		
		return names;
	}
}
