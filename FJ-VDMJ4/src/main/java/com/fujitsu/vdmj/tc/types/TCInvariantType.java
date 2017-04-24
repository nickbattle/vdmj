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
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.Value;

public abstract class TCInvariantType extends TCType
{
	private static final long serialVersionUID = 1L;
	public TCExplicitFunctionDefinition invdef = null;
	public TCExplicitFunctionDefinition eqdef = null;
	public TCExplicitFunctionDefinition orddef = null;

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

	public void setEquality(TCExplicitFunctionDefinition eqdef)
	{
		this.eqdef = eqdef;
	}

	public void setOrder(TCExplicitFunctionDefinition orddef)
	{
		this.orddef = orddef;
	}

	public FunctionValue getInvariant(Context ctxt)
	{
		return getFunction(invdef, ctxt);
	}

	public FunctionValue getEquality(Context ctxt)
	{
		return getFunction(eqdef, ctxt);
	}

	public FunctionValue getOrder(Context ctxt)
	{
		return getFunction(orddef, ctxt);
	}

	private FunctionValue getFunction(TCExplicitFunctionDefinition def, Context ctxt)
	{
		if (def != null)
		{
			try
			{
				Value v = ctxt.getGlobal().lookup(def.name);
				return v.functionValue(ctxt);
			}
			catch (ValueException e)
			{
				//abort(e);
			}
		}

		return null;
	}
}
