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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INBindingGlobals;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Quantifier;
import com.fujitsu.vdmj.values.QuantifierList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INForAllExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INMultipleBindList bindList;
	public final INExpression predicate;
	
	/** Result information for QuickCheck */
	public INBindingGlobals globals = null;

	public INForAllExpression(LexLocation location,	INMultipleBindList bindList, INExpression predicate)
	{
		super(location);
		this.bindList = bindList;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "(forall " + Utils.listToString(bindList) + " & " + predicate + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		long start = System.currentTimeMillis();
		long timeout = globals == null ? 0 : globals.getTimeout();

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

			quantifiers.init(ctxt, false);

			while (quantifiers.hasNext())
			{
				if (timeout > 0)
				{
					if (System.currentTimeMillis() - start > timeout)
					{
						if (globals != null)
						{
							globals.setCounterexample(null);
							globals.setDidTimeout(true);
							globals.setMaybe(true);
						}
						
						return new BooleanValue(true);
					}
				}
				
				Context evalContext = new Context(location, "forall", ctxt);
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
							matches = false;
							break;	// This quantifier set does not match
						}
					}
				}

				try
				{
					if (matches && !predicate.eval(evalContext).boolValue(ctxt))
					{
						if (globals != null)
						{
							globals.setCounterexample(evalContext);
							globals.setDidTimeout(false);
							globals.setMaybe();
						}
						
						return new BooleanValue(false);
					}
				}
				catch (ContextException e)
				{
					if (globals != null)
					{
						globals.setCounterexample(evalContext);
						globals.setDidTimeout(false);
						globals.setMaybe();
					}
					
					throw e;
				}
				catch (ValueException e)
				{
					predicate.abort(e);
				}
			}
		}
	    catch (ValueException e)
	    {
	    	return abort(e);
	    }

		if (globals != null)
		{
			globals.setMaybe();
		}
		
		return new BooleanValue(true);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForAllExpression(this, arg);
	}
}
