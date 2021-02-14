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

public class INObjectFieldDesignator extends INObjectDesignator
{
	private static final long serialVersionUID = 1L;
	public final INObjectDesignator object;
	public final TCNameToken classname;
	public final TCIdentifierToken fieldname;
	public final TCNameToken field;

	public INObjectFieldDesignator(INObjectDesignator object, TCNameToken classname, TCIdentifierToken fieldname, TCNameToken field)
	{
		super(object.location);
		this.object = object;
		this.classname = classname;
		this.fieldname = fieldname;
		this.field = field;
	}

	@Override
	public String toString()
	{
		return object + "." + (classname == null ? fieldname : classname);
	}

	@Override
	public Value eval(Context ctxt)
	{
		try
		{
			Value val = object.eval(ctxt).deref();

			if (val instanceof ObjectValue && field != null)
			{
    			ObjectValue ov = val.objectValue(ctxt);
    			Value rv = ov.get(field, (classname != null));

    			if (rv == null)
    			{
    				ExceptionHandler.abort(location, 4045, "Object does not contain value for field: " + field, ctxt);
    			}

    			return rv;
			}
			else if (val instanceof RecordValue)
			{
				RecordValue rec = val.recordValue(ctxt);
				Value result = rec.fieldmap.get(fieldname.getName());

				if (result == null)
				{
					ExceptionHandler.abort(location, 4046, "No such field: " + fieldname, ctxt);
				}

				return result;
			}
			else
			{
				ExceptionHandler.abort(location, 4020,
					"State value is neither a record nor an object", ctxt);
				
				return null;
			}
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}
}
