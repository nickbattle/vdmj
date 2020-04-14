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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FieldMap;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INMuExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression record;
	public final INRecordModifierList modifiers;

	public INMuExpression(LexLocation location,
		INExpression record, INRecordModifierList modifiers)
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
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		RecordValue r = record.eval(ctxt).recordValue(ctxt);
    		FieldMap fields = new FieldMap(r.fieldmap);

    		for (INRecordModifier rm: modifiers)
    		{
    			TCField f = r.type.findField(rm.tag.getName());

    			if (f == null)
    			{
        			abort(4023, "Mu type conflict? No field tag " + rm.tag.getName(), ctxt);
    			}
    			else
    			{
    				fields.add(rm.tag.getName(), rm.value.eval(ctxt), !f.equalityAbstraction);
    			}
     		}

     		return new RecordValue(r.type, fields, ctxt);
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		return record.findExpression(lineno);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = record.getValues(ctxt);

		for (INRecordModifier rm: modifiers)
		{
			list.addAll(rm.getValues(ctxt));
		}

		return list;
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = record.getOldNames();

		for (INRecordModifier rm: modifiers)
		{
			list.addAll(rm.getOldNames());
		}

		return list;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMuExpression(this, arg);
	}
}
