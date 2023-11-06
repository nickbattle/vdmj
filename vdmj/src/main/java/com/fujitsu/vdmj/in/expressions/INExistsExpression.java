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
 *
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Quantifier;
import com.fujitsu.vdmj.values.QuantifierList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INExistsExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INMultipleBindList bindList;
	public final INExpression predicate;

	public INExistsExpression(LexLocation location, INMultipleBindList bindList, INExpression predicate)
	{
		super(location);
		this.bindList = bindList;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "(exists " + bindList + " & " + predicate + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		long start = System.currentTimeMillis();
		long timeout = getTimeout();

		try
		{
			QuantifierList quantifiers = new QuantifierList();

			for (INMultipleBind mb: bindList)
			{
				ValueList bvals = mb.getBindValues(ctxt, false);

				for (INPattern p: mb.plist)
				{
					Quantifier q = new Quantifier(p, bvals);
					quantifiers.add(q);
				}
			}

			quantifiers.init(ctxt, true);

			while (quantifiers.hasNext())
			{
				if (timeout > 0)
				{
					if (System.currentTimeMillis() - start > timeout)
					{
						setCounterexample(null, true);
						return new BooleanValue(true);
					}
				}
				
				Context evalContext = new Context(location, "exists", ctxt);
				NameValuePairList nvpl = quantifiers.next();
				boolean matches = true;

				for (NameValuePair nvp: nvpl)
				{
					Value v = evalContext.get(nvp.name);

					if (v == null)
					{
						evalContext.put(nvp.name, nvp.value);
					}
					else
					{
						if (!v.equals(nvp.value))
						{
							setWitness(evalContext);
							matches = false;
							break;	// This quantifier set does not match
						}
					}
				}

				try
				{
					if (matches && predicate.eval(evalContext).boolValue(ctxt))
					{
						setWitness(evalContext);
						return new BooleanValue(true);
					}
				}
				catch (ValueException e)
				{
					predicate.abort(e);
				}
			}
		}
	    catch (ValueException e)
	    {
	    	abort(e);
	    }

		return new BooleanValue(false);
	}
	
	/**
	 * This is used by the QuickCheck plugin to limit PO execution times.
	 */
	private long getTimeout()
	{
		for (INMultipleBind bind: bindList)
		{
			if (bind instanceof INBindingSetter)			// Type and multitype binds
			{
				INBindingSetter setter = (INBindingSetter)bind;
				long timeout = setter.getTimeout();
				
				if (timeout > 0)
				{
					return timeout;
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * This is used by the QuickCheck plugin to report which values failed.
	 */
	private void setCounterexample(Context ctxt, boolean didTimeout)
	{
		for (INMultipleBind bind: bindList)
		{
			if (bind instanceof INBindingSetter)			// Type and multitype binds
			{
				INBindingSetter setter = (INBindingSetter)bind;
				
				if (setter.getBindValues() != null)			// One we care about (set QC values for)
				{
					setter.setCounterexample(ctxt, didTimeout);
					break;									// Just one will do - see QC printFailPath
				}
			}
		}
	}
	
	/**
	 * This is used by the QuickCheck plugin to report which values succeeded.
	 */
	private void setWitness(Context ctxt)
	{
		for (INMultipleBind bind: bindList)
		{
			if (bind instanceof INBindingSetter)			// Type and multitype binds
			{
				INBindingSetter setter = (INBindingSetter)bind;
				
				if (setter.getBindValues() != null)			// One we care about (set QC values for)
				{
					setter.setWitness(ctxt);
					break;									// Just one will do - see QC printFailPath
				}
			}
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExistsExpression(this, arg);
	}
}
