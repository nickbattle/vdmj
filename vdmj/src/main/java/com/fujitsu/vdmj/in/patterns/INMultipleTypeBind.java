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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.patterns.visitors.INMultipleBindVisitor;
import com.fujitsu.vdmj.in.types.visitors.INGetAllValuesVisitor;
import com.fujitsu.vdmj.in.types.visitors.INTypeSizeVisitor;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.visitors.TCParameterCollector;
import com.fujitsu.vdmj.values.ValueList;

public class INMultipleTypeBind extends INMultipleBind
{
	private static final long serialVersionUID = 1L;
	public final TCType type;
	public final boolean hasTypeParams;
	
	public INBindingOverride setter = null;
	private ValueList bindValues = null;
	private boolean bindPermuted = false;

	public INMultipleTypeBind(INPatternList plist, TCType type)
	{
		super(plist);
		this.type = type;
		this.hasTypeParams = !type.apply(new TCParameterCollector(), null).isEmpty();
	}

	@Override
	public String toString()
	{
		return plist + ":" + type.toExplicitString(location);
	}
	
	public TCType getType()
	{
		return type;
	}

	@Override
	public ValueList getBindValues(Context ctxt, boolean permuted) throws ValueException
	{
		if (setter != null && setter.hasOverride())
		{
			return setter.getBindValues();
		}
		
		if (bindValues != null && bindPermuted == permuted && !hasTypeParams)
		{
			return bindValues;		// Should be exactly the same
		}
		
		try
		{
			long size = type.apply(new INTypeSizeVisitor(), ctxt);
			
	   		if (size > Properties.in_typebind_limit)
			{
				throw new ContextException(5039, "Cannot evaluate type bind of size " + size, location, ctxt);
			}

	   		bindValues = type.apply(new INGetAllValuesVisitor(), ctxt);
	   		bindPermuted = permuted;
	   		return bindValues;
		}
		catch (ArithmeticException e)
		{
			throw new ContextException(5040, "Cannot evaluate type bind, size exceeds long", location, ctxt);
		}
		catch (InternalException e)		// Used while visitors don't have exceptions
		{
			throw new ValueException(e.number, e.getMessage(), ctxt);
		}
	}

	@Override
	public <R, S> R apply(INMultipleBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMultipleTypeBind(this, arg);
	}
}
