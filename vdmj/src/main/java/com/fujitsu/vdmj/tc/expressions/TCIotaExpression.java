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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCIotaExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCBind bind;
	public final TCExpression predicate;
	
	public TCDefinition def = null;

	public TCIotaExpression(LexLocation location, TCBind bind, TCExpression predicate)
	{
		super(location);
		this.bind = bind;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "(iota " + bind + " & " + predicate + ")";
	}

	@Override
	public TCType typeCheck(Environment base, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		def = new TCMultiBindListDefinition(location, bind.getMultipleBindList());
		def.typeCheck(base, scope);
		TCType rt = null;

		if (bind instanceof TCSetBind)
		{
			TCSetBind sb = (TCSetBind)bind;
			rt = sb.set.typeCheck(base, null, scope, null);

			if (rt.isSet(location))
			{
				rt = rt.getSet().setof;
			}
			else
			{
				report(3112, "Iota set bind is not a set");
			}
		}
		else if (bind instanceof TCSeqBind)
		{
			TCSeqBind sb = (TCSeqBind)bind;
			rt = sb.sequence.typeCheck(base, null, scope, null);

			if (rt.isSeq(location))
			{
				rt = rt.getSeq().seqof;
			}
			else
			{
				report(3112, "Iota seq bind is not a sequence");
			}
		}
		else
		{
			TCTypeBind tb = (TCTypeBind)bind;
			tb.typeResolve(base);
			rt = tb.type;
		}

		Environment local = new FlatCheckedEnvironment(def, base, scope);
		
		if (!predicate.typeCheck(local, null, scope, new TCBooleanType(location)).isType(TCBooleanType.class, location))
		{
			predicate.report(3088, "Predicate is not boolean");
		}

		local.unusedCheck();
		return setType(rt);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIotaExpression(this, arg);
	}
}
