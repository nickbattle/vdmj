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
import com.fujitsu.vdmj.tc.lex.TCNameSet;
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
			return rtype;
		}

		if (rtype.isRecord(location))
		{
			recordType = rtype.getRecord();
			modTypes = new TCTypeList();

    		for (TCRecordModifier rm: modifiers)
    		{
    			TCType mtype = rm.value.typeCheck(env, null, scope, null);
    			modTypes.add(mtype);
    			TCField f = recordType.findField(rm.tag.getName());

    			if (f != null)
    			{
					if (!TypeComparator.compatible(f.type, mtype))
					{
						report(3130, "Modifier for " + f.tag + " should be " + f.type);
						detail("Actual", mtype);
					}
    			}
    			else
    			{
    				report(3131, "Modifier tag " + rm.tag + " not found in record");
    			}
    		}
		}
		else
		{
			report(3132, "mu operation on non-record type");
		}

		return rtype;
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		TCNameSet names = record.getFreeVariables(globals, env);
		
		for (TCRecordModifier rm: modifiers)
		{
			names.addAll(rm.value.getFreeVariables(globals, env));
		}
		
		return names;
	}
}
