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

package com.fujitsu.vdmj.values;

import java.io.Serializable;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;

public class InvariantValueListener implements ValueListener, Serializable
{
    private static final long serialVersionUID = 1L;
	private UpdatableValue root = null;

	public InvariantValueListener()
	{
		this.root = null;
	}

	// We have to set the root separately from the constructor as the tree of
	// updatable values has to be created with the listener before we know
	// the objref of the object just created. See the getUpdatable method of
	// InvariantValue.
	
	public void setValue(UpdatableValue value)
	{
		this.root = value;		// Always an updatable InvariantValue
	}
	
	public UpdatableValue getValue()
	{
		return root;
	}

	@Override
	public void changedValue(LexLocation location, Value value, Context ctxt) throws ValueException
	{
		// InvariantValueListeners are created at every Value point (with
		// an inv function) in a structure, but the simplest level is actually
		// covered by the convertTo call in the arguments to "set". So to avoid
		// another unnecessary inv check, we also test whether root = value, which
		// is true for these simplest levels. Note that we also check for whether
		// we are inside an atomic block.
		
		if (root != null && root.value != value && Settings.invchecks)
		{
			if (root.value instanceof InvariantValue)
			{
				InvariantValue ival = (InvariantValue) root.value;
				ival.checkInvariant(ctxt);
			}
			else if (root.value instanceof RecordValue)
			{
				RecordValue rval = (RecordValue) root.value;
				rval.checkInvariant(ctxt);
			} 
		}
	}
}
