/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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

package com.fujitsu.vdmj.tc.types;

import java.util.Collection;

/**
 * This TCType visitor visits all of the leaves of a type tree and calls
 * the basic processing methods for the simple types.
 */
public abstract class TCLeafTypeVisitor<E, C extends Collection<E>, S> extends TCTypeVisitor<C, S>
{
	@Override
	public C caseFunctionType(TCFunctionType node, S arg)
	{
		C all = newCollection();
		
		for (TCType param: node.parameters)
		{
			all.addAll(param.apply(this, arg));
		}
		
		all.addAll(node.result.apply(this, arg));
		return all;
	}

	@Override
	public C caseInMapType(TCInMapType node, S arg)
	{
		return caseMapType(node, arg);
	}

	@Override
	public C caseMapType(TCMapType node, S arg)
	{
		C all = newCollection();
		all.addAll(node.from.apply(this, arg));
		all.addAll(node.to.apply(this, arg));
		return all;
	}

	@Override
	public C caseNamedType(TCNamedType node, S arg)
	{
		return node.type.apply(this, arg);
	}

	@Override
	public C caseOperationType(TCOperationType node, S arg)
	{
		C all = newCollection();
		
		for (TCType param: node.parameters)
		{
			all.addAll(param.apply(this, arg));
		}
		
		all.addAll(node.result.apply(this, arg));
		return all;
	}

	@Override
	public C caseOptionalType(TCOptionalType node, S arg)
	{
		return node.type.apply(this, arg);
	}

	@Override
	public C caseProductType(TCProductType node, S arg)
	{
		C all = newCollection();
		
		for (TCType param: node.types)
		{
			all.addAll(param.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseRecordType(TCRecordType node, S arg)
	{
		C all = newCollection();
		
		for (TCField field: node.fields)
		{
			all.addAll(field.type.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseSeq1Type(TCSeq1Type node, S arg)
	{
		return caseSeqType(node, arg);
	}

	@Override
	public C caseSeqType(TCSeqType node, S arg)
	{
		C all = newCollection();
		all.addAll(node.seqof.apply(this, arg));
		return all;
	}

	@Override
	public C caseSet1Type(TCSet1Type node, S arg)
	{
		return caseSetType(node, arg);
	}

	@Override
	public C caseSetType(TCSetType node, S arg)
	{
		C all = newCollection();
		all.addAll(node.setof.apply(this, arg));
		return all;
	}

	@Override
	public C caseUnionType(TCUnionType node, S arg)
	{
		C all = newCollection();
		
		for (TCType param: node.types)
		{
			all.addAll(param.apply(this, arg));
		}
		
		return all;
	}
	
	abstract protected C newCollection();
}
