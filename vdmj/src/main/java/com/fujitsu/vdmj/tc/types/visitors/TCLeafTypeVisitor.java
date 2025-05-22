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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.types.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;

/**
 * This TCType visitor visits all of the leaves of a type tree and calls
 * the basic processing methods for the simple types.
 */
public abstract class TCLeafTypeVisitor<E, C extends Collection<E>, S> extends TCTypeVisitor<C, S>
{
	/**
	 * There is no visitor set here, because the TCType leaf visitor does not call out to any
	 * other grammatical group (like expVisitor etc). Also, since TCTypes are not mapped
	 * beyond the TC tree, you cannot use (say) an INVisitorSet here anyway. 
	 */
	
	// No visitorSet...
	
	/**
	 * We have to collect the nodes that have already been visited since types can be recursive,
	 * and the visitor will otherwise blow the stack. Note that this means you need a new visitor
	 * instance for every use (or only re-use with care!). This is tested and modified in the
	 * NamedType and RecordType entries.
	 */
	protected TCTypeSet done = new TCTypeSet();
	
	@Override
	public C caseBracketType(TCBracketType node, S arg)
	{
		return node.type.apply(this, arg);
	}
	
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
		C all = node.from.apply(this, arg);
		all.addAll(node.to.apply(this, arg));
		return all;
	}

	@Override
	public C caseNamedType(TCNamedType node, S arg)
	{
		if (done.contains(node))
		{
			return newCollection();
		}
		else
		{
			done.add(node);
			return node.type.apply(this, arg);
		}
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
		if (done.contains(node))
		{
			return newCollection();
		}
		else
		{
			done.add(node);
			C all = newCollection();
			
			for (TCField field: node.fields)
			{
				all.addAll(field.type.apply(this, arg));
			}
			
			return all;
		}
	}

	@Override
	public C caseSeq1Type(TCSeq1Type node, S arg)
	{
		return caseSeqType(node, arg);
	}

	@Override
	public C caseSeqType(TCSeqType node, S arg)
	{
		return node.seqof.apply(this, arg);
	}

	@Override
	public C caseSet1Type(TCSet1Type node, S arg)
	{
		return caseSetType(node, arg);
	}

	@Override
	public C caseSetType(TCSetType node, S arg)
	{
		return node.setof.apply(this, arg);
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
