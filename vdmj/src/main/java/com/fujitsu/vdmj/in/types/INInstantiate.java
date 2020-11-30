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

package com.fujitsu.vdmj.in.types;

import com.fujitsu.vdmj.in.types.visitors.INInstantiateVisitor;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.tc.types.TCType;

public class INInstantiate
{
	/**
	 * Return an instantiated type, using the @T parameters in scope of "params". The extra
	 * context passed is just for reporting the ContextException (which may differ).
	 */
	public static TCType instantiate(TCType type, Context params, Context ctxt)
	{
		try
		{
			return type.apply(new INInstantiateVisitor(), params);
		}
		catch (InternalException e)	// visitor exception
		{
			ExceptionHandler.handle(new ContextException(4008, e.getMessage(), type.location, ctxt));
			return null;
		}
	}
}
