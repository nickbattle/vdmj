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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.VoidValue;

public class INCPUClassDefinition extends INClassDefinition
{
	private static final long serialVersionUID = 1L;

	public INCPUClassDefinition(TCNameToken className, TCClassType type, TCNameList supernames, INDefinitionList definitions)
	{
		super(className, type, supernames, definitions, new INDefinitionList(),
			new INDefinitionList(), new INClassList(), null, false);
	}

	@Override
	public ObjectValue newInstance(
		INDefinition ctorDefinition, ValueList argvals, Context ctxt)
	{
		NameValuePairList nvpl = definitions.getNamedValues(ctxt);
		NameValuePairMap map = new NameValuePairMap();
		map.putAll(nvpl);

		return new CPUValue((TCClassType)getType(), map, argvals, this);
	}

	public static Value deploy(Context ctxt) throws ValueException
	{
		try
		{
    		ObjectContext octxt = (ObjectContext)ctxt;
    		CPUValue cpu = (CPUValue)octxt.self;
    		ObjectValue obj = (ObjectValue)octxt.lookup(varName("obj"));

    		obj.setCPU(cpu);
    		cpu.deploy(obj);

  			return new VoidValue();
		}
		catch (Exception e)
		{
			throw new ValueException(4136, "Cannot deploy to CPU", ctxt);
		}
	}

	public static Value setPriority(Context ctxt) throws ValueException
	{
		try
		{
    		ObjectContext octxt = (ObjectContext)ctxt;
    		CPUValue cpu = (CPUValue)octxt.self;
    		SeqValue opname = (SeqValue)octxt.lookup(varName("opname"));
    		NaturalValue priority = (NaturalValue)octxt.check(varName("priority"));

    		cpu.setPriority(opname.stringValue(ctxt), priority.intValue(ctxt).longValue());
   			return new VoidValue();
		}
		catch (Exception e)
		{
			throw new ValueException(4137, "Cannot set priority: " + e.getMessage(), ctxt);
		}
	}

	private static TCNameToken varName(String name)
	{
		return new TCNameToken(LexLocation.ANY, "CPU", name);
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCPUClassDefinition(this, arg);
	}
}
