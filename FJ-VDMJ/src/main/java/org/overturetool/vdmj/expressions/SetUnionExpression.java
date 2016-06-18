/*******************************************************************************
 *
 *	Copyright (c) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.expressions;

import org.overturetool.vdmj.lex.LexToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.Set1Type;
import org.overturetool.vdmj.types.SetType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.types.TypeSet;
import org.overturetool.vdmj.types.UnknownType;
import org.overturetool.vdmj.values.SetValue;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueSet;

public class SetUnionExpression extends BinaryExpression
{
	private static final long serialVersionUID = 1L;

	public SetUnionExpression(Expression left, LexToken op, Expression right)
	{
		super(left, op, right);
	}

	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		ltype = left.typeCheck(env, null, scope, constraint);
		rtype = right.typeCheck(env, null, scope, constraint);

		if (!ltype.isSet())
		{
			report(3168, "Left hand of " + op + " is not a set");
			ltype = new SetType(location, new UnknownType(location));
		}

		if (!rtype.isSet())
		{
			report(3169, "Right hand of " + op + " is not a set");
			rtype = new SetType(location, new UnknownType(location));
		}

		Type lof = ltype.getSet();
		Type rof = rtype.getSet();
		boolean set1 = (lof instanceof Set1Type) || (rof instanceof Set1Type);
		
		lof = ((SetType)lof).setof;
		rof = ((SetType)rof).setof;
		TypeSet ts = new TypeSet(lof, rof);
		
		return set1 ?
			new Set1Type(location, ts.getType(location)) :
			new SetType(location, ts.getType(location));
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		try
		{
    		ValueSet result = new ValueSet();
    		result.addAll(left.eval(ctxt).setValue(ctxt));
    		result.addAll(right.eval(ctxt).setValue(ctxt));
    		return new SetValue(result);
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public String kind()
	{
		return "union";
	}
}
