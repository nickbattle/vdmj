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

package com.fujitsu.vdmj.tc.types;

import java.util.Map;

import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class TCInstantiate
{
	/**
	 * Return an instantiated type, using the @T parameters in the map passed.
	 */
	public static TCType instantiate(TCType type, Map<TCNameToken, TCType> map)
	{
		if (type instanceof TCBracketType)
		{
			TCBracketType btype = (TCBracketType)type;
			return new TCBracketType(instantiate(btype.type, map));
		}
		else if (type instanceof TCInMapType)
		{
			TCInMapType mtype = (TCInMapType)type;
			return new TCInMapType(type.location, instantiate(mtype.from, map), instantiate(mtype.to, map));
		}
		else if (type instanceof TCMapType)
		{
			TCMapType mtype = (TCMapType)type;
			return new TCMapType(type.location, instantiate(mtype.from, map), instantiate(mtype.to, map));
		}
		else if (type instanceof TCOptionalType)
		{
			TCOptionalType otype = (TCOptionalType)type;
			return new TCOptionalType(type.location, instantiate(otype.type, map));
		}
		else if (type instanceof TCFunctionType)
		{
			TCFunctionType ftype = (TCFunctionType)type;
			ftype = new TCFunctionType(type.location,
				instantiate(ftype.parameters, map), ftype.partial, instantiate(ftype.result, map));
			ftype.instantiated = true;
			return ftype;
		}
		else if (type instanceof TCParameterType)
		{
			TCParameterType pname = (TCParameterType)type;
			return map.get(pname.name);
		}
		else if (type instanceof TCProductType)
		{
			TCProductType ptype = (TCProductType)type;
			return new TCProductType(type.location, instantiate(ptype.types, map));
		}
		else if (type instanceof TCSeqType)
		{
			TCSeqType stype = (TCSeqType)type;
			return new TCSeqType(type.location, instantiate(stype.seqof, map));
		}
		else if (type instanceof TCSetType)
		{
			TCSetType stype = (TCSetType)type;
			return new TCSetType(type.location, instantiate(stype.setof, map));
		}
		else if (type instanceof TCUnionType)
		{
			TCUnionType utype = (TCUnionType)type;
			return new TCUnionType(type.location, instantiate(utype.types, map));
		}
		else
		{
			return type;	// Unchanged
		}
	}

	private static TCTypeList instantiate(TCTypeList types, Map<TCNameToken, TCType> map)
	{
		TCTypeList instantiated = new TCTypeList();
		
		for (TCType type: types)
		{
			instantiated.add(instantiate(type, map));
		}
		
		return instantiated;
	}

	private static TCTypeSet instantiate(TCTypeSet types, Map<TCNameToken, TCType> map)
	{
		TCTypeSet instantiated = new TCTypeSet();
		
		for (TCType type: types)
		{
			instantiated.add(instantiate(type, map));
		}
		
		return instantiated;
	}
}
