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
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
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
	public TCType caseBracketType(TCBracketType btype, Context params)
	{
		return new TCBracketType(btype.type.apply(this, params));
	}

	@Override
	public TCType caseInMapType(TCInMapType mtype, Context params)
	{
		return new TCInMapType(mtype.location, mtype.from.apply(this, params), mtype.to.apply(this, params));
	}

	@Override
	public TCType caseMapType(TCMapType mtype, Context params)
	{
		return new TCMapType(mtype.location, mtype.from.apply(this, params), mtype.to.apply(this, params));
	}

	@Override
	public TCType caseOptionalType(TCOptionalType otype, Context params)
	{
		return new TCOptionalType(otype.location, otype.type.apply(this, params));
	}

	@Override
	public TCType caseFunctionType(TCFunctionType ftype, Context params)
	{
		ftype = new TCFunctionType(ftype.location, instantiate(ftype.parameters, params), ftype.partial,
				ftype.result.apply(this, params));
		ftype.instantiated = true;
		return ftype;
	}

	@Override
	public TCType caseParameterType(TCParameterType pname, Context params)
	{
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
	public TCType caseProductType(TCProductType ptype, Context params)
	{
		return new TCProductType(ptype.location, instantiate(ptype.types, params));
	}

	@Override
	public TCType caseSeqType(TCSeqType stype, Context params)
	{
		return new TCSeqType(stype.location, stype.seqof.apply(this, params));
	}
	
	@Override
	public TCType caseSeq1Type(TCSeq1Type stype, Context params)
	{
		return new TCSeq1Type(stype.location, stype.seqof.apply(this, params));
	}

	@Override
	public TCType caseSetType(TCSetType stype, Context params)
	{
		return new TCSetType(stype.location, stype.setof.apply(this, params));
	}

	@Override
	public TCType caseSet1Type(TCSet1Type stype, Context params)
	{
		return new TCSet1Type(stype.location, stype.setof.apply(this, params));
	}

	@Override
	public TCType caseUnionType(TCUnionType utype, Context params)
	{
		return new TCUnionType(utype.location, instantiate(utype.types, params));
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
