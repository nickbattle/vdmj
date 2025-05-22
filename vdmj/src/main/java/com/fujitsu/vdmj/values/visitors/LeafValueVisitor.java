/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.values.visitors;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.values.CompFunctionValue;
import com.fujitsu.vdmj.values.FieldValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.InvariantValue;
import com.fujitsu.vdmj.values.IterFunctionValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;

public abstract class LeafValueVisitor<E, C extends Collection<E>, S> extends ValueVisitor<C, S>
{
	/**
	 * This is to avoid loops in the caseObjectValue method. Note that each instance of
	 * the leaf visitor has its own objrefs, so be careful with re-use! The reset method
	 * will clear the objrefs list.
	 */
	private List<Integer> objrefs = new Vector<Integer>();
	
	public void reset()
	{
		objrefs.clear();
	}
	
 	@Override
	public abstract C caseValue(Value node, S arg);

 	@Override
	public C caseCompFunctionValue(CompFunctionValue node, S arg)
	{
		return ifNotNull(arg, node.ff1, node.ff2);
	}

 	@Override
	public C caseFunctionValue(FunctionValue node, S arg)
	{
 		return ifNotNull(arg, node.precondition, node.postcondition);
	}

 	@Override
	public C caseInvariantValue(InvariantValue node, S arg)
	{
 		return ifNotNull(arg, node.deref(), node.equality, node.invariant, node.ordering);
	}

 	@Override
	public C caseIterFunctionValue(IterFunctionValue node, S arg)
	{
		return ifNotNull(arg, node.function);
	}

 	@Override
	public C caseMapValue(MapValue node, S arg)
	{
 		C all = ifNotNull(arg, node.values.keySet());
 		all.addAll(ifNotNull(arg, node.values.values()));
 		return all;
	}

 	@Override
	public C caseObjectValue(ObjectValue node, S arg)
	{
		C all = newCollection();
 		
		if (objrefs.contains(node.objectReference))
 		{
 			return all;		// already done before
 		}
		
		objrefs.add(node.objectReference);
 		
  		for (Value m: node.members.values())
 		{
 			all.addAll(m.apply(this, arg));
 		}
 		
		for (ObjectValue obj: node.superobjects)
		{
			all.addAll(obj.apply(this, arg));
		}
 		
 		return all;
	}

 	@Override
	public C caseOperationValue(OperationValue node, S arg)
	{
 		return ifNotNull(arg, node.precondition, node.postcondition);
	}

 	@Override
	public C caseRecordValue(RecordValue node, S arg)
	{
		C all = newCollection();
		
		for (FieldValue fv: node.fieldmap)
		{
			all.addAll(fv.value.apply(this, arg));
		}
		
		all.addAll(ifNotNull(arg, node.equality, node.invariant, node.ordering));
		
		return all;
	}

 	@Override
	public C caseSeqValue(SeqValue node, S arg)
	{
		return ifNotNull(arg, node.values);
	}

 	@Override
	public C caseSetValue(SetValue node, S arg)
	{
		return ifNotNull(arg, node.values);
	}

 	@Override
	public C caseTupleValue(TupleValue node, S arg)
	{
		return ifNotNull(arg, node.values);
	}

 	@Override
	public C caseUpdatableValue(UpdatableValue node, S arg)
	{
		return node.deref().apply(this, arg);
	}
	
	abstract protected C newCollection();
	
	private C ifNotNull(S arg, Value... values)
	{
		C all = newCollection();
		
		for (Value v: values)
		{
			if (v != null)
			{
				all.addAll(v.apply(this, arg));
			}
		}
		
		return all;
	}
	
	private C ifNotNull(S arg, Collection<Value> values)
	{
		C all = newCollection();
		
		for (Value v: values)
		{
			if (v != null)
			{
				all.addAll(v.apply(this, arg));
			}
		}
		
		return all;
	}
}