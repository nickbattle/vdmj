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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.Value;

public abstract class TCInvariantType extends TCType
{
	private static final long serialVersionUID = 1L;
	public TCExplicitFunctionDefinition invdef = null;
	public boolean opaque = false;
	protected boolean inNarrower = false;

	public TCInvariantType(LexLocation location)
	{
		super(location);
	}

	@Override
	abstract protected String toDisplay();

	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	public void setInvariant(TCExplicitFunctionDefinition invdef)
	{
		this.invdef = invdef;
	}

	public FunctionValue getInvariant(Context ctxt)
	{
		if (invdef != null)
		{
			try
			{
				Value v = ctxt.getGlobal().lookup(invdef.name);
				return v.functionValue(ctxt);
			}
			catch (ValueException e)
			{
				//abort(e);
			}
		}

		return null;
	}

	/**
	 * Get a list of free variables needed to initialize the type invariant. 
	 * @param env
	 */
	@Override
	public TCNameSet getFreeVariables(Environment env)
	{
		if (invdef != null)
		{
			return invdef.getFreeVariables();
		}
		else
		{
			return super.getFreeVariables(env);
		}
	}
}
