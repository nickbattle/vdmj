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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;

/**
 * This TCType visitor visits all of the leaves of a type tree and calls
 * the basic processing methods for the simple types. It is common for
 * many visitors to have this functionality - eg. processing a TCMapType
 * processes its "from" and "to" types. 
 */

public abstract class TCLeafTypeVisitor<E, S> extends TCTypeVisitor<List<E>, S>
{
	@Override
	public List<E> caseClassType(TCClassType node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCDefinition def: node.classdef.getDefinitions())
		{
			all.addAll(def.getType().apply(this, arg));
		}
		
		return all;
	}

	@Override
	public List<E> caseFunctionType(TCFunctionType node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCType param: node.parameters)
		{
			all.addAll(param.apply(this, arg));
		}
		
		all.addAll(node.result.apply(this, arg));
		return all;
	}

	@Override
	public List<E> caseInMapType(TCInMapType node, S arg)
	{
		return caseMapType(node, arg);
	}

	@Override
	public List<E> caseMapType(TCMapType node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.from.apply(this, arg));
		all.addAll(node.to.apply(this, arg));
		return all;
	}

	@Override
	public List<E> caseNamedType(TCNamedType node, S arg)
	{
		return node.type.apply(this, arg);
	}

	@Override
	public List<E> caseOperationType(TCOperationType node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCType param: node.parameters)
		{
			all.addAll(param.apply(this, arg));
		}
		
		all.addAll(node.result.apply(this, arg));
		return all;
	}

	@Override
	public List<E> caseOptionalType(TCOptionalType node, S arg)
	{
		return node.type.apply(this, arg);
	}

	@Override
	public List<E> caseProductType(TCProductType node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCType param: node.types)
		{
			all.addAll(param.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public List<E> caseRecordType(TCRecordType node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCField field: node.fields)
		{
			all.addAll(field.type.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public List<E> caseSeq1Type(TCSeq1Type node, S arg)
	{
		return caseSeqType(node, arg);
	}

	@Override
	public List<E> caseSeqType(TCSeqType node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.seqof.apply(this, arg));
		return all;
	}

	@Override
	public List<E> caseSet1Type(TCSet1Type node, S arg)
	{
		return caseSetType(node, arg);
	}

	@Override
	public List<E> caseSetType(TCSetType node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.setof.apply(this, arg));
		return all;
	}

	@Override
	public List<E> caseUnionType(TCUnionType node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCType param: node.types)
		{
			all.addAll(param.apply(this, arg));
		}
		
		return all;
	}
}
