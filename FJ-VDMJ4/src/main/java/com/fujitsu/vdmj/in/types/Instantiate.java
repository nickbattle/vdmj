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
	public static TCType instantiate(TCType type, Context ctxt)
	{
		if (type instanceof TCBracketType)
		{
			TCBracketType btype = (TCBracketType)type;
			return new TCBracketType(instantiate(btype.type, ctxt));
		}
		else if (type instanceof TCInMapType)
		{
			TCInMapType mtype = (TCInMapType)type;
			return new TCInMapType(type.location, instantiate(mtype.from, ctxt), instantiate(mtype.to, ctxt));
		}
		else if (type instanceof TCMapType)
		{
			TCMapType mtype = (TCMapType)type;
			return new TCMapType(type.location, instantiate(mtype.from, ctxt), instantiate(mtype.to, ctxt));
		}
		else if (type instanceof TCOptionalType)
		{
			TCOptionalType otype = (TCOptionalType)type;
			return new TCOptionalType(type.location, instantiate(otype.type, ctxt));
		}
		else if (type instanceof TCParameterType)
		{
			TCParameterType pname = (TCParameterType)type;
			Value t = ctxt.lookup(pname.name);

			if (t == null)
			{
				ExceptionHandler.handle(new ContextException(4008,
					"No such type parameter @" + pname + " in scope", type.location, ctxt));
			}
			else if (t instanceof ParameterValue)
			{
				ParameterValue tv = (ParameterValue)t;
				return tv.type;
			}
			else
			{
				ExceptionHandler.handle(new ContextException(4009,
					"Type parameter/local variable name clash, @" + pname, type.location, ctxt));
			}

			return null;	// Not reached
		}
		else if (type instanceof TCProductType)
		{
			TCProductType ptype = (TCProductType)type;
			return new TCProductType(type.location, instantiate(ptype.types, ctxt));
		}
		else if (type instanceof TCSeqType)
		{
			TCSeqType stype = (TCSeqType)type;
			return new TCSeqType(type.location, instantiate(stype.seqof, ctxt));
		}
		else if (type instanceof TCSetType)
		{
			TCSetType stype = (TCSetType)type;
			return new TCSetType(type.location, instantiate(stype.setof, ctxt));
		}
		else if (type instanceof TCUnionType)
		{
			TCUnionType utype = (TCUnionType)type;
			return new TCUnionType(type.location, instantiate(utype.types, ctxt));
		}
		else
		{
			return type;	// Unchanged
		}
	}

	private static TCTypeList instantiate(TCTypeList types, Context ctxt)
	{
		TCTypeList instantiated = new TCTypeList();
		
		for (TCType type: types)
		{
			instantiated.add(instantiate(type, ctxt));
		}
		
		return instantiated;
	}

	private static TCTypeSet instantiate(TCTypeSet types, Context ctxt)
	{
		TCTypeSet instantiated = new TCTypeSet();
		
		for (TCType type: types)
		{
			instantiated.add(instantiate(type, ctxt));
		}
		
		return instantiated;
	}
}
