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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import java.util.Arrays;
import java.util.List;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POFreeVariableFinder;
import com.fujitsu.vdmj.po.patterns.visitors.Locals;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.NameScope;

abstract public class ProofObligation implements Comparable<ProofObligation>
{
	// Arguments for the markUnchecked method.
	public static final String NOT_YET_SUPPORTED	= "Not yet supported for operations";
	public static final String MISSING_MEASURE		= "Obligation for missing measure function";
	public static final String UNCHECKED_VDMPP		= "Unchecked in VDM++/RT";
	public static final String HIDDEN_VARIABLES		= "Obligation patterns contain hidden variables";
	public static final String REQUIRES_VDM10		= "Obligation requires VDM10";
	public static final String HAS_AMBIGUOUS_STATE	= "Earlier statements create ambiguous state";
	public static final String PO_HAS_ERRORS		= "PO has errors";
	public static final String EXTERNAL_MODULE		= "Cannot determine target module state";
	
	public final LexLocation location;
	public final POType kind;
	public final String name;

	public int number;
	public String source;
	public POStatus status;
	public PODefinition definition;
	public boolean isCheckable;
	
	public String qualifier;
	public Context counterexample;
	public Context witness;
	public String message;
	public String provedBy;
	public TCNameSet obligationVars;
	public TCNameSet reasonsAbout;

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
		this.isCheckable = true;	// Set false for some operation POs
		
		this.obligationVars = null;
		this.reasonsAbout = null;
		
		POGetMatchingExpressionVisitor.init();	// Reset the "any" count, before PO creation
	}
	
	public void clearAnalysis()
	{
		this.qualifier = null;
		this.counterexample = null;
		this.witness = null;
		this.message = null;
		this.provedBy = null;
	}

	public String getSource()
	{
		return source;
	}

	public PODefinition getDefinition()
	{
		return definition;
	}

	public POAnnotationList getAnnotations()
	{
		return definition == null ? null : definition.annotations;
	}

	public TCTypeList getTypeParams()
	{
		if (definition instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition exdef = (POExplicitFunctionDefinition)definition;
			return exdef.typeParams;
		}
		else if (definition instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition imdef = (POImplicitFunctionDefinition)definition;
			return imdef.typeParams;
		}
		else
		{
			return null;
		}
	}

	public void setStatus(POStatus status)
	{
		this.status = status;
	}
	
	public void setQualifier(String qualifier)
	{
		this.qualifier = qualifier;
	}
	
	public void setProvedBy(String provedBy)
	{
		this.provedBy = provedBy;
	}

	public void setCounterexample(Context path)
	{
		if (path == null)
		{
			counterexample = null;
		}
		else
		{
			counterexample = new Context(path.location, "Counterexample", null);
			Context ctxt = path;
			
			while (ctxt != null && ctxt.outer != null)
			{
				for (TCNameToken key: ctxt.keySet())
				{
					// Only update unknown values, so "most recent" values show
					if (!counterexample.containsKey(key))
					{
						counterexample.put(key, ctxt.get(key));
					}
				}

				ctxt = ctxt.outer;
			}
		}
	}
	
	public void setWitness(Context path)
	{
		if (path == null)
		{
			witness = null;
		}
		else
		{
			witness = new Context(path.location, "Witness", null);
			Context ctxt = path;
			
			while (ctxt != null && ctxt.outer != null)
			{
				for (TCNameToken key: ctxt.keySet())
				{
					// Only update unknown values, so "most recent" values show
					if (!witness.containsKey(key))
					{
						witness.put(key, ctxt.get(key));
					}
				}

				ctxt = ctxt.outer;
			}
		}
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
	
	public void setObligationVars(POContextStack ctxt, POExpression... expressions)
	{
		List<POExpression> list = Arrays.asList(expressions);
		setObligationVars(ctxt, list);
	}
	
	public void setObligationVars(POContextStack ctxt, List<POExpression> expressions)
	{	
		if (obligationVars == null)
		{
			obligationVars = new TCNameSet();
		}
		
		TCNameSet ignoreThese = new TCNameSet();
		POFreeVariableFinder visitor = new POFreeVariableFinder();
		
		for (POExpression exp: expressions)
		{
			List<POVariableExpression> freevars = exp.apply(visitor, new Locals());
			
			for (POVariableExpression freev: freevars)
			{
				TCType etype = freev.getExptype();
				
				if (etype instanceof TCInvariantType)
				{
					TCInvariantType itype = (TCInvariantType)etype;
					
					if (itype.invdef != null)
					{
						// The variable's invariant "reasons about" this exp
						ignoreThese.add(freev.name);
					}
				}
				
				if (freev.vardef != null && freev.vardef.nameScope == NameScope.GLOBAL)
				{
					ignoreThese.add(freev.name);	// Globals are a given
				}
				
				obligationVars.add(freev.name);
			}
		}
		
		if (ctxt.hasAmbiguous(obligationVars))	// Including invariant checked ones
		{
			markUnchecked(HAS_AMBIGUOUS_STATE);
		}
		
		// Finally, remove the ones that are covered by invariant checks.
		obligationVars.removeAll(ignoreThese);
	}
	
	public void setReasonsAbout(TCNameSet... reasons)
	{
		if (reasonsAbout == null)
		{
			reasonsAbout = new TCNameSet();
		}
		
		for (TCNameSet set: reasons)
		{
			this.reasonsAbout.addAll(set);
		}
	}
	
	/**
	 * This is used to mark obligations as unchecked, with a message.
	 */
	public ProofObligation markUnchecked(String message)
	{
		this.isCheckable = false;
		this.setStatus(POStatus.UNCHECKED);
		this.setMessage(message);
		
		return this;	// Convenient for new XYZObligation().markUnchecked(REASON)
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
		sb.append(source);
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ProofObligation)
		{
			ProofObligation opo = (ProofObligation)other;
			return kind == opo.kind && location == opo.location && source.equals(opo.source);
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
	 * Generate a string invocation for the PO context.
	 */
	public String getCexLaunch()
	{
		POLaunchFactory factory = new POLaunchFactory(this);
		return factory.getCexLaunch();
	}
	
	public Context getCexState()
	{
		POLaunchFactory factory = new POLaunchFactory(this);
		return factory.getCexState();
	}
	
	public String getWitnessLaunch()
	{
		POLaunchFactory factory = new POLaunchFactory(this);
		return factory.getWitnessLaunch();
	}
	
	public Context getWitnessState()
	{
		POLaunchFactory factory = new POLaunchFactory(this);
		return factory.getWitnessState();
	}
	
	public String getLaunch()
	{
		POLaunchFactory factory = new POLaunchFactory(this);
		return factory.getLaunch(null);
	}

	/**
	 * True if the PO itself generates proof obligations.
	 */
	public boolean hasObligations()
	{
		return hasObligations ;
	}
	
	public void setHasObligations(boolean hasObligations)
	{
		this.hasObligations = hasObligations; 
	}
}
