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

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Value;

/**
 * This class is a compromise between having a tree of "IN" classes for all of the TCTypes just
 * to provide the instantiate functionality, and implementing a visitor just for this purpose. 
 */
public class Instantiate
{
	/**
	 * Return an instantiated type, using the @T parameters in scope of "params". The extra
	 * context passed is just for reporting the ContextException (which may differ).
	 */
	public static TCType instantiate(TCType type, Context params, Context ctxt)
	{
		try
		{
			return instantiate(type, params);
		}
		catch (Exception e)
		{
			ExceptionHandler.handle(new ContextException(4008, e.getMessage(), type.location, ctxt));
			return null;
		}
	}

	private static TCType instantiate(TCType type, Context params) throws Exception
	{
		if (type instanceof TCBracketType)
		{
			TCBracketType btype = (TCBracketType)type;
			return new TCBracketType(instantiate(btype.type, params));
		}
		else if (type instanceof TCInMapType)
		{
			TCInMapType mtype = (TCInMapType)type;
			return new TCInMapType(type.location, instantiate(mtype.from, params), instantiate(mtype.to, params));
		}
		else if (type instanceof TCMapType)
		{
			TCMapType mtype = (TCMapType)type;
			return new TCMapType(type.location, instantiate(mtype.from, params), instantiate(mtype.to, params));
		}
		else if (type instanceof TCOptionalType)
		{
			TCOptionalType otype = (TCOptionalType)type;
			return new TCOptionalType(type.location, instantiate(otype.type, params));
		}
		else if (type instanceof TCFunctionType)
		{
			TCFunctionType ftype = (TCFunctionType)type;
			ftype = new TCFunctionType(type.location,
				instantiate(ftype.parameters, params), ftype.partial, instantiate(ftype.result, params));
			ftype.instantiated = true;
			return ftype;
		}
		else if (type instanceof TCParameterType)
		{
			TCParameterType pname = (TCParameterType)type;
			Value t = params.lookup(pname.name);

			if (t == null)
			{
				throw new Exception("No such type parameter " + pname + " in scope");
			}
			else if (t instanceof ParameterValue)
			{
				ParameterValue tv = (ParameterValue)t;
				return tv.type;
			}
			else
			{
				throw new Exception("Type parameter/local variable name clash, " + pname);
			}
		}
		else if (type instanceof TCProductType)
		{
			TCProductType ptype = (TCProductType)type;
			return new TCProductType(type.location, instantiate(ptype.types, params));
		}
		else if (type instanceof TCSeqType)
		{
			TCSeqType stype = (TCSeqType)type;
			return new TCSeqType(type.location, instantiate(stype.seqof, params));
		}
		else if (type instanceof TCSetType)
		{
			TCSetType stype = (TCSetType)type;
			return new TCSetType(type.location, instantiate(stype.setof, params));
		}
		else if (type instanceof TCUnionType)
		{
			TCUnionType utype = (TCUnionType)type;
			return new TCUnionType(type.location, instantiate(utype.types, params));
		}
		else
		{
			return type;	// Unchanged
		}
	}

	private static TCTypeList instantiate(TCTypeList types, Context map) throws Exception
	{
		TCTypeList instantiated = new TCTypeList();
		
		for (TCType type: types)
		{
			instantiated.add(instantiate(type, map));
		}
		
		return instantiated;
	}

	private static TCTypeSet instantiate(TCTypeSet types, Context map) throws Exception
	{
		TCTypeSet instantiated = new TCTypeSet();
		
		for (TCType type: types)
		{
			instantiated.add(instantiate(type, map));
		}
		
		return instantiated;
	}
}
