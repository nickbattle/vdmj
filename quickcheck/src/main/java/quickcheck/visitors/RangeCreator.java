/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.visitors;

import java.math.BigInteger;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INUndefinedExpression;
import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.types.INInstantiate;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassInvariantDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInstanceVariableDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.util.KCombinator;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.ValueSet;

public abstract class RangeCreator extends TCTypeVisitor<ValueSet, Integer>
{
	protected final Context ctxt;
	protected final TCTypeSet done;

	protected RangeCreator(Context ctxt)
	{
		this.ctxt = ctxt;
		this.done = new TCTypeSet();
	}

	protected List<ValueSet> powerLimit(ValueSet source, int limit, boolean incEmpty)
	{
		// Generate a power set, up to limit values from the full power set.
		List<ValueSet> results = new Vector<ValueSet>();
		
		if (source.isEmpty())
		{
			if (incEmpty)
			{
				results.add(new ValueSet());	// Just {}
			}
		}
		else
		{
			int size = source.size();
			long count = 0;
			
			if (incEmpty)
			{
				results.add(new ValueSet());	// Add {}
				count++;
			}
			
			int persize = limit / 5 + 1;		// Try up to five sizes?
			
			for (int ss = 1; ss <= size; ss++)
			{
				for (int[] kc: new KCombinator(size, ss))
				{
					ValueSet ns = new ValueSet(ss);
	
					for (int i=0; i<ss; i++)
					{
						ns.add(source.get(kc[i]));
					}
					
					results.add(ns);
					
					if (++count >= limit)
					{
						return results;
					}
					
					if (count % persize == 0)
					{
						break;	// Try next ss
					}
				}
			}
		}
	
		return results;
	}
	
	/**
	 * Return the lowest integer power of n that is &lt;= limit. If n &lt; 2,
	 * just return limit.
	 * 
	 * Used to calculate how many field values to generate for an n-field object.
	 * For example, a three field tuple/record with one value each gives 1x1x1=1 records;
	 * with two values each it gives 2x2x2=8 values, with three 3x3x3=27 values. So to
	 * generate up to N values from F field combinations, we need to find the largest n,
	 * such that n^F < N.
	 * 
	 * If there is only one field, we can generate N values.
	 */
	protected int leastPower(int F, int N)
	{
		if (F > 1)
		{
			int power = 1;
			int value = F;
			
			while (value < N)
			{
				value = value * F;
				power++;
			}

			return power - 1;
		}
		else
		{
			return N;
		}
	}

	/**
	 * Attempt to produce a function that matches a (possibly polymorphic) TCFunctionType.
	 */
	protected FunctionValue instantiate(TCFunctionType node)
	{
		INPatternList params = new INPatternList();
		INExpression body = null;
		
		// Resolve any @T types referred to in the type parameters
		TCFunctionType ptype = (TCFunctionType) INInstantiate.instantiate(node, ctxt, ctxt);
		
		for (int i=1; i <= ptype.parameters.size(); i++)
		{
			params.add(new INIdentifierPattern(new TCNameToken(node.location, node.location.module, "p" + i)));
		}
		
		if (ptype.result instanceof TCBooleanType)
		{
			body = new INBooleanLiteralExpression(new LexBooleanToken(true, node.location));
		}
		else if (ptype.result instanceof TCNumericType)
		{
			body = new INIntegerLiteralExpression(new LexIntegerToken(BigInteger.ONE, node.location));
		}
		else
		{
			body = new INUndefinedExpression(node.location);
		}
		
		return new FunctionValue(node.location, "$qc_dummy", ptype, params, body, null);
	}
	
	/**
	 * try to make an object that matches the class definition passed, without using
	 * any construction etc. This is just a means to create something that matches
	 * the object pattern in the top level forall, to try to enable simple VDM++
	 * specifications to be tested with QC. Note that and class invariants are
	 * checked separately by checkObject below.
	 */
	protected ObjectValue createObject(TCClassDefinition cdef, int seed)
	{
		TCClassType ctype = (TCClassType)cdef.getType();
		List<ObjectValue> superobjects = new Vector<ObjectValue>();
		NameValuePairMap members = new NameValuePairMap();
		
		for (TCClassDefinition sdef: cdef.superdefs)
		{
			superobjects.add(createObject(sdef, seed));
		}
		
		if (superobjects.contains(null))
		{
			return null;	// A superclass failed
		}
		
		// Seed with the number passed in, and passed this generator, so that
		// we can copy the "done" types.
		RandomRangeCreator generator = new RandomRangeCreator(ctxt, seed, this);
		
		for (TCDefinition def: cdef.definitions)
		{
			if (def instanceof TCInstanceVariableDefinition)
			{
				TCInstanceVariableDefinition idef = (TCInstanceVariableDefinition)def;
				TCType itype = idef.getType();
				
				ValueSet value = itype.apply(generator, 1);
				
				if (!value.isEmpty())
				{
					members.put(new NameValuePair(idef.name, value.get(0)));
				}
				else
				{
					members.put(new NameValuePair(idef.name, new UndefinedValue()));
				}
			}
		}
			
		return new ObjectValue(ctype, members, superobjects, CPUValue.vCPU, null);
	}
	
	/**
	 * Check whether an object value meets its class invariants.
	 */
	protected boolean checkObject(TCClassDefinition cdef, ObjectValue object)
	{
		if (!cdef.getInvDefs().isEmpty())
		{
			Context invCtxt = new ObjectContext(LexLocation.ANY, "class invariant", ctxt, object);
			
			for (TCDefinition inv: cdef.getInvDefs())
			{
				try
				{
					TCClassInvariantDefinition cinv = (TCClassInvariantDefinition)inv;
					INExpression inexp = ClassMapper.getInstance(INNode.MAPPINGS).convertLocal(cinv.expression);
					
					if (!inexp.eval(invCtxt).boolValue(invCtxt))
					{
						return false;
					}
				}
				catch (Exception e)
				{
					return false;
				}
			}
		}
	
		return true;
	}
}
