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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.definitions.INCPUClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

public class INNotYetSpecifiedStatement extends INStatement
{
	private static final long serialVersionUID = 1L;

	public INNotYetSpecifiedStatement(LexLocation location)
	{
		super(location);
		location.executable(false);		// ie. ignore coverage for these
	}

	@Override
	public String toString()
	{
		return "is not yet specified";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		if (Settings.dialect == Dialect.VDM_SL)
		{
			ModuleInterpreter i = (ModuleInterpreter)Interpreter.getInstance();
			INModule module = i.findModule(location.module);

			if (module != null)
			{
				if (module.hasDelegate())
				{
					return module.invokeDelegate(ctxt);
				}
			}
		}
		else
		{
    		ObjectValue self = ctxt.getSelf();

    		if (self == null)
    		{
    			ClassInterpreter i = (ClassInterpreter)Interpreter.getInstance();
    			INClassDefinition cls = i.findClass(location.module);

    			if (cls != null)
    			{
    				if (cls.hasDelegate())
    				{
    					return cls.invokeDelegate(ctxt);
    				}
    			}
    		}
    		else
    		{
    			if (self.hasDelegate())
    			{
    				return self.invokeDelegate(ctxt);
    			}
    		}
		}

		if (location.module.equals("CPU"))
		{
    		try
			{
				if (ctxt.title.equals("deploy(obj)"))
				{
					return INCPUClassDefinition.deploy(ctxt);
				}
				else if (ctxt.title.equals("deploy(obj, name)"))
				{
					return INCPUClassDefinition.deploy(ctxt);
				}
				else if (ctxt.title.equals("setPriority(opname, priority)"))
				{
					return INCPUClassDefinition.setPriority(ctxt);
				}
			}
			catch (ValueException e)
			{
				abort(e);
			}
		}

		return abort(4041, "'is not yet specified' statement reached", ctxt);
	}
}
