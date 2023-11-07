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
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INUndefinedExpression;
import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.types.INInstantiate;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.util.KCombinator;
import com.fujitsu.vdmj.values.FunctionValue;
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
			
			int from = (size > 3) ? 3 : size;	// Avoid very small sets?
			
			for (int ss = from; ss <= size; ss++)
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
				}
			}
		}
	
		return results;
	}
	
	/**
	 * Return the lowest integer power of n that is <= limit. If n < 2,
	 * just return 1.
	 */
	protected int leastPower(int n, int limit)
	{
		int power = 1;

		if (n > 1)
		{
			int value = n;
			
			while (value < limit)
			{
				value = value * n;
				power++;
			}
		}
		
		return power;
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
}
