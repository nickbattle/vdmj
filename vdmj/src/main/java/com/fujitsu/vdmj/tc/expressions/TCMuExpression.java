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
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class TCMuExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression record;
	public final TCRecordModifierList modifiers;

	private TCRecordType recordType = null;
	private TCTypeList modTypes = null;

	public TCMuExpression(LexLocation location,
		TCExpression record, TCRecordModifierList modifiers)
	{
		super(location);
		this.record = record;
		this.modifiers = modifiers;
	}

	@Override
	public String toString()
	{
		return "mu(" + record + ", " + Utils.listToString(modifiers) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType rtype = record.typeCheck(env, null, scope, null);

		if (rtype instanceof TCUnknownType)
		{
			return setType(rtype);
		}

		if (rtype.isRecord(location))
		{
			recordType = rtype.getRecord();
			modTypes = new TCTypeList();

    		for (TCRecordModifier rm: modifiers)
    		{
    			TCField f = recordType.findField(rm.tag.getName());

    			if (f != null)
    			{
        			TCType mtype = rm.value.typeCheck(env, null, scope, f.type);
        			modTypes.add(mtype);

					if (!TypeComparator.compatible(f.type, mtype))
					{
						rm.value.report(3130, "Modifier for " + f.tag + " should be " + f.type);
						detail("Actual", mtype);
					}
    			}
    			else
    			{
    				rm.tag.report(3131, "Modifier tag " + rm.tag + " not found in record");
    			}
    		}
		}
		else
		{
			report(3132, "mu operation on non-record type");
		}

		return setType(rtype);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMuExpression(this, arg);
	}
}
