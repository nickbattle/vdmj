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

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POTypeDefinition;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingConstantVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Value;

abstract public class ProofObligation implements Comparable<ProofObligation>
{
	public final LexLocation location;
	public final POType kind;
	public final String name;

	public int number;
	public String value;
	public POStatus status;
	public PODefinition definition;
	public boolean isCheckable;
	public TCTypeList typeParams;
	public POAnnotationList annotations;
	public Context counterexample;
	public Context witness;
	public String message;
	public String provedBy;

	private int var = 1;
	private TCExpression checkedExpression = null;
	private boolean existential = false;
	private boolean hasObligations = false;

	public ProofObligation(LexLocation location, POType kind, POContextStack ctxt)
	{
		this.location = location;
		this.kind = kind;
		this.name = ctxt.getName();
		this.status = POStatus.UNPROVED;
		this.definition = ctxt.getDefinition();
		this.number = 0;
		this.isCheckable = ctxt.isCheckable();	// Set false for operation POs
		this.typeParams = ctxt.getTypeParams();
		this.annotations = ctxt.getAnnotations();
		this.counterexample = new Context(location, "Counterexample", null);
		this.witness = new Context(location, "Witness", null);
		this.message = null;
		this.provedBy = null;
		
		if (!isCheckable)
		{
			this.status = POStatus.UNCHECKED;	// Implies unproved
		}
		
		POGetMatchingExpressionVisitor.init();	// Reset the "any" count, before PO creation
	}

	public String getValue()
	{
		return value;
	}
	
	public void setStatus(POStatus status)
	{
		this.status = status;
	}
	
	public void setProvedBy(String provedBy)
	{
		this.provedBy = provedBy;
	}

	public void setCounterexample(Context path)
	{
		counterexample.clear();
		Context ctxt = path;
		
		while (ctxt != null && ctxt.outer != null)
		{
			counterexample.putAll(ctxt);
			ctxt = ctxt.outer;
		}
	}
	
	public void setWitness(Context path)
	{
		witness.clear();
		Context ctxt = path;
		
		while (ctxt != null && ctxt.outer != null)
		{
			witness.putAll(ctxt);
			ctxt = ctxt.outer;
		}
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
	
	/**
	 * This is used to mark obligations as unchecked, with a reason.
	 */
	public void markUnchecked(String message)
	{
		this.isCheckable = false;
		this.setStatus(POStatus.UNCHECKED);
		this.setMessage(message);
	}

	public boolean isExistential()
	{
		return existential;
	}
	
	/**
	 * True if there are multiple binds in the obligation which represent "the same" values,
	 * for example a forall x:nat... with an exists y:nat.. inside that is reasoning about
	 * the same set of nats. In this case, the random strategy has to include fixed values
	 * too. 
	 */
	public boolean hasCorrelatedBinds()
	{
		return false;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(name);
		sb.append(": ");
		sb.append(kind);
		sb.append(" obligation ");
		sb.append(location);
		sb.append("\n");
		sb.append(value);
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ProofObligation)
		{
			ProofObligation opo = (ProofObligation)other;
			return kind == opo.kind && location == opo.location;
		}
		
		return false; 
	}

	protected String getVar(String root)
	{
		return root + var++;
	}

	@Override
	public int compareTo(ProofObligation other)
	{
		return number - other.number;
	}

	public void setCheckedExpression(TCExpression checkedExpression)
	{
		this.checkedExpression = checkedExpression;
		this.existential = (checkedExpression instanceof TCExistsExpression);
	}
	
	public TCExpression getCheckedExpression()
	{
		return checkedExpression;
	}
	
	/**
	 * Fully qualify a type name, if it is not declared local to the PO.
	 */
	protected String explicitType(TCType type, LexLocation poLoc)
	{
		return type.toExplicitString(poLoc);
	}
	
	/**
	 * Generate a string invocation for the context passed.
	 */
	public String getCexLaunch()
	{
		if (counterexample.isEmpty())
		{
			return null;
		}
		
		return getLaunch(counterexample);
	}
	
	public String getWitnessLaunch()
	{
		if (witness.isEmpty())
		{
			return null;
		}
		
		return getLaunch(witness);
	}

	private String getLaunch(Context ctxt)
	{
		if (definition instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition efd = (POExplicitFunctionDefinition)definition;
			return launchExplicitFunction(efd, ctxt);
		}
		else if (definition instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition ifd = (POImplicitFunctionDefinition)definition;
			return launchImplicitFunction(ifd, ctxt);
		}
		else if (definition instanceof POTypeDefinition)
		{
			POTypeDefinition td = (POTypeDefinition)definition;
			
			if (kind == POType.TOTAL_ORDER && td.orddef != null)
			{
				// The PO defines params "x" < "y" but ord_T is "p1$" < "p2$".
				// So we create a patched context...
				Context patched = new Context(ctxt.location, ctxt.title, ctxt.outer);
				
				for (TCNameToken key: ctxt.keySet())
				{
					Value arg = ctxt.get(key);
					
					if (key.getName().equals("x"))
					{
						key = new TCNameToken(key.getLocation(), key.getModule(), "p1$");
					}
					else if (key.getName().equals("y"))
					{
						key = new TCNameToken(key.getLocation(), key.getModule(), "p2$");
					}

					patched.put(key, arg);
				}
				
				return launchExplicitFunction(td.orddef, patched);
			}
			else if (td.invdef != null)
			{
				return launchExplicitFunction(td.invdef, ctxt);
			}
		}

		return null;	// Unexpected definition
	}
	
	private String launchExplicitFunction(POExplicitFunctionDefinition efd, Context ctxt)
	{
		StringBuilder callString = new StringBuilder(efd.name.getName());
		PORemoveIgnoresVisitor.init();
		
		if (efd.typeParams != null)
		{
			String inst = addTypeParams(efd.typeParams, ctxt);
			
			if (inst == null)
			{
				return null;
			}
			
			callString.append(inst);
		}
		
		for (POPatternList pl: efd.paramPatternList)
		{
			String sep = "";
			callString.append("(");
			
			for (POPattern p: pl)
			{
				String match = paramMatch(p.removeIgnorePatterns(), ctxt);
				
				if (match == null)
				{
					return null;	// Can't match some params
				}

				callString.append(sep);
				callString.append(match);
				sep = ", ";
			}
				
			callString.append(")");
		}
		
		return  callString.toString();
	}
	
	private String launchImplicitFunction(POImplicitFunctionDefinition ifd, Context ctxt)
	{
		StringBuilder callString = new StringBuilder(ifd.name.getName());
		PORemoveIgnoresVisitor.init();
		
		if (ifd.typeParams != null)
		{
			String inst = addTypeParams(ifd.typeParams, ctxt);
			
			if (inst == null)
			{
				return null;
			}
			
			callString.append(inst);
		}

		String sep = "";
		callString.append("(");
		
		for (POPatternListTypePair pl: ifd.parameterPatterns)
		{
			for (POPattern p: pl.patterns)
			{
				String match = paramMatch(p.removeIgnorePatterns(), ctxt);
				
				if (match == null)
				{
					return null;	// Can't match some params
				}

				callString.append(sep);
				callString.append(match);
				sep = ", ";
			}
		}
		
		callString.append(")");
		
		return callString.toString();
	}
	
	private String addTypeParams(TCTypeList params, Context ctxt)
	{
		StringBuilder callString = new StringBuilder();
		String sep = "";
		callString.append("[");
		
		for (TCType p: params)
		{
			TCParameterType param = (TCParameterType)p;
			ParameterValue inst = (ParameterValue) ctxt.get(param.name);
			
			if (inst == null)
			{
				return null;	// Missing type parameter?
			}
			
			callString.append(sep);
			callString.append(inst.type);
			sep = ", ";
		}
		
		callString.append("]");
		return callString.toString();
	}

	private String paramMatch(POPattern p, Context ctxt)
	{
		POGetMatchingConstantVisitor visitor = new POGetMatchingConstantVisitor();
		String result = p.apply(visitor, ctxt);
		return visitor.hasFailed() ? null : result;
	}

	public boolean hasObligations()
	{
		return hasObligations ;
	}
	
	public void setHasObligations(boolean hasObligations)
	{
		this.hasObligations = hasObligations; 
	}
}
