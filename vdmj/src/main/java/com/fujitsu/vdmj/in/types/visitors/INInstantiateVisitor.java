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

package com.fujitsu.vdmj.in.types.visitors;

import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
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
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Value;

/**
 * Instantiate a type, which may contain @T parameters, using the params Context passed.
 */
public class INInstantiateVisitor extends TCTypeVisitor<TCType, Context>
{
	@Override
	public TCType caseType(TCType type, Context params)
	{
		return type;	// Unchanged
	}
	
	@Override
	public TCType caseBracketType(TCBracketType type, Context params)
	{
		TCBracketType btype = (TCBracketType) type;
		return new TCBracketType(btype.type.apply(this, params));
	}

	@Override
	public TCType caseInMapType(TCInMapType type, Context params)
	{
		TCInMapType mtype = (TCInMapType) type;
		return new TCInMapType(type.location, mtype.from.apply(this, params), mtype.to.apply(this, params));
	}

	@Override
	public TCType caseMapType(TCMapType type, Context params)
	{
		TCMapType mtype = (TCMapType) type;
		return new TCMapType(type.location, mtype.from.apply(this, params), mtype.to.apply(this, params));
	}

	@Override
	public TCType caseOptionalType(TCOptionalType type, Context params)
	{
		TCOptionalType otype = (TCOptionalType) type;
		return new TCOptionalType(type.location, otype.type.apply(this, params));
	}

	@Override
	public TCType caseFunctionType(TCFunctionType type, Context params)
	{
		TCFunctionType ftype = (TCFunctionType) type;
		ftype = new TCFunctionType(type.location, instantiate(ftype.parameters, params), ftype.partial,
				ftype.result.apply(this, params));
		ftype.instantiated = true;
		return ftype;
	}

	@Override
	public TCType caseParameterType(TCParameterType type, Context params)
	{
		TCParameterType pname = (TCParameterType) type;
		Value t = params.lookup(pname.name);

		if (t == null)
		{
			throw new InternalException(0, "No such type parameter " + pname + " in scope");
		}
		else if (t instanceof ParameterValue)
		{
			ParameterValue tv = (ParameterValue) t;
			return tv.type;
		}
		else
		{
			throw new InternalException(0, "Type parameter/local variable name clash, " + pname);
		}
	}

	@Override
	public TCType caseProductType(TCProductType type, Context params)
	{
		TCProductType ptype = (TCProductType) type;
		return new TCProductType(type.location, instantiate(ptype.types, params));
	}

	@Override
	public TCType caseSeqType(TCSeqType type, Context params)
	{
		TCSeqType stype = (TCSeqType) type;
		return new TCSeqType(type.location, stype.seqof.apply(this, params));
	}

	@Override
	public TCType caseSetType(TCSetType type, Context params)
	{
		TCSetType stype = (TCSetType) type;
		return new TCSetType(type.location, stype.setof.apply(this, params));
	}

	@Override
	public TCType caseUnionType(TCUnionType type, Context params)
	{
		TCUnionType utype = (TCUnionType) type;
		return new TCUnionType(type.location, instantiate(utype.types, params));
	}

	private TCTypeList instantiate(TCTypeList types, Context map)
	{
		TCTypeList instantiated = new TCTypeList();
		
		for (TCType type: types)
		{
			instantiated.add(type.apply(this, map));
		}
		
		return instantiated;
	}

	private TCTypeSet instantiate(TCTypeSet types, Context map)
	{
		TCTypeSet instantiated = new TCTypeSet();
		
		for (TCType type: types)
		{
			instantiated.add(type.apply(this, map));
		}
		
		return instantiated;
	}
}
