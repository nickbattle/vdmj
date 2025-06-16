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

package com.fujitsu.vdmj.in.definitions;

import java.util.HashMap;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.scheduler.ResourceScheduler;
import com.fujitsu.vdmj.tc.definitions.TCBUSClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCCPUClassDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUndefinedType;
import com.fujitsu.vdmj.values.BUSValue;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.QuoteValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

public class INSystemDefinition extends INClassDefinition
{
	private static final long serialVersionUID = 1L;
	private static ObjectValue systemObject = null;

	public static NameValuePairList getSystemMembers()
	{
		if (systemObject != null)
		{
			return systemObject.members.asList();
		}

		return null;
	}
	
	public static ObjectValue getSystemObject()
	{
		return systemObject;
	}

	public INSystemDefinition(INAnnotationList annotations, TCNameToken className, TCClassType type, INDefinitionList members)
	{
		super(annotations, className, type, new TCNameList(), members, new INDefinitionList(),
			new INDefinitionList(), new INClassList(), null, false);
	}

	public void systemInit(ResourceScheduler scheduler, RootContext initialContext)
	{
		initialContext.setThreadState(CPUValue.vCPU);

		try
		{
			// First go through the definitions, looking for CPUs to decl
			// before we can deploy to them in the constructor. We have to
			// predict the CPU numbers at this point.

			INDefinitionList cpudefs = new INDefinitionList();
			int cpuNumber = 1;
			TCCPUClassDefinition cpudef = null;

			for (INDefinition d: definitions)
			{
				TCType t = d.getType();

				if (t instanceof TCClassType)
				{
					INInstanceVariableDefinition ivd = (INInstanceVariableDefinition)d;
					TCClassType ct = (TCClassType)t;

					if (ct.classdef instanceof TCCPUClassDefinition)
					{
						cpudefs.add(d);
						cpudef = (TCCPUClassDefinition)ct.classdef;

	    				RTLogger.log(
	    					"CPUdecl -> id: " + (cpuNumber++) +
	    					" expl: " + !(ivd.expType instanceof TCUndefinedType) +
	    					" sys: \"" + name.getName() + "\"" +
	    					" name: \"" + d.name.getName() + "\"");
					}
				}
			}

			// Run the constructor to do any deploys etc.

			systemObject = makeNewInstance(null, new ValueList(),
					initialContext, new HashMap<TCNameToken, ObjectValue>(), false);

			// Do CPUs first so that default BUSses can connect all CPUs.

			INCPUClassDefinition instance = ClassMapper.getInstance(INNode.MAPPINGS).convert(cpudef);
			ValueSet cpus = new ValueSet();

			for (INDefinition d: cpudefs)
			{
    			UpdatableValue v = (UpdatableValue)systemObject.members.get(d.name);
    			CPUValue cpu = null;

    			if (v.isUndefined())
    			{
    				ValueList args = new ValueList();

    				args.add(new QuoteValue("FCFS"));	// Default policy
    				args.add(new RealValue(0));			// Default speed

    				cpu = (CPUValue)instance.newInstance(null, args, initialContext);
    				v.set(location, cpu, initialContext);
    			}
    			else
    			{
    				cpu = (CPUValue)v.deref();
    			}

    			// Set the name and scheduler for the CPU resource, and
    			// associate the resource with the scheduler.

    			cpu.setup(scheduler, d.name.getName());
    			cpus.add(cpu);
			}

			// We can create vBUS now that all the CPUs have been created
			// This must be first, to ensure it's bus number 0.

			BUSValue.vBUS = INBUSClassDefinition.makeVirtualBUS(cpus);
			BUSValue.vBUS.setup(scheduler, "vBUS");

			for (INDefinition d: definitions)
			{
				TCType t = d.getType();

				if (t instanceof TCClassType)
				{
					TCClassType ct = (TCClassType)t;

					if (ct.classdef instanceof TCBUSClassDefinition)
					{
						UpdatableValue v = (UpdatableValue)systemObject.members.get(d.name);
	    				BUSValue bus = null;

						if (!v.isUndefined())
						{
							bus = (BUSValue)v.deref();

							// Set the name and scheduler for the BUS resource, and
							// associate the resource with the scheduler.

							bus.setup(scheduler, d.name.getName());
						}
					}
				}
			}

			// For efficiency, we create a 2D array of CPU-to-CPU bus links
			BUSValue.createMap(initialContext, cpus);
		}
		catch (ContextException e)
		{
			throw e;
		}
		catch (ValueException e)
		{
			abort(e);
		}
    	catch (Exception e)
    	{
    		abort(4135, "Cannot instantiate a system class", initialContext);
    	}
	}

	@Override
	public ObjectValue newInstance(
		INDefinition ctorDefinition, ValueList argvals, Context ctxt)
	{
		abort(4135, "Cannot instantiate system class " + name, ctxt);
		return null;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSystemDefinition(this, arg);
	}
}
