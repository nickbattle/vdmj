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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;

public class INFieldDesignator extends INStateDesignator
{
	private static final long serialVersionUID = 1L;
	public final INStateDesignator object;
	public final TCIdentifierToken field;
	public final TCNameToken objectfield;

	public INFieldDesignator(INStateDesignator object, TCIdentifierToken field, TCNameToken objectfield)
	{
		super(object.location);
		this.object = object;
		this.field = field;
		this.objectfield = objectfield;
	}

	@Override
	public String toString()
	{
		return object + "." + field;
	}

	@Override
	public Value eval(Context ctxt)
	{
		Value result = null;

		try
		{
			result = object.eval(ctxt).deref();

			if (result instanceof ObjectValue && objectfield != null)
			{
    			ObjectValue ov = result.objectValue(ctxt);
    			Value rv = ov.get(objectfield, false);

    			if (rv == null)
    			{
    				abort(4045, "Object does not contain value for field: " + field, ctxt);
    			}

    			return rv;
			}
			else if (result instanceof RecordValue)
			{
    			RecordValue rec = result.recordValue(ctxt);
    			result = rec.fieldmap.get(field.getName());

    			if (result == null)
    			{
    				ExceptionHandler.abort(location, 4037, "No such field: " + field, ctxt);
    			}

    			return result;
			}
		}
		catch (ValueException e)
		{
			abort(e);
		}

		return result;
	}
}
