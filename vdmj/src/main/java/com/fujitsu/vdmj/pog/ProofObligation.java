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
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POPatternListList;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingConstantVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.po.types.POPatternListTypePairList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.values.ParameterValue;

abstract public class ProofObligation implements Comparable<ProofObligation>
{
	// Arguments for the markUnchecked method.
	public static final String NOT_YET_SUPPORTED	= "Not yet supported for operations";
	public static final String MISSING_MEASURE		= "Obligation for missing measure function";
	public static final String UNCHECKED_VDMPP		= "Unchecked in VDM++/RT";
	public static final String HIDDEN_VARIABLES		= "Obligation patterns contain hidden variables";
	public static final String REQUIRES_VDM10		= "Obligation requires VDM10";
	public static final String LOOP_STATEMENT		= "Loop modifies state";
	public static final String HAS_UPDATED_STATE	= "Earlier statements updated state";
	public static final String COMPLEX_ASSIGNMENT	= "Assignment too complex";
	
	public final LexLocation location;
	public final POType kind;
	public final String name;

	public int number;
	public String source;
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
		this.isCheckable = true;	// Set false for some operation POs
		this.typeParams = ctxt.getTypeParams();
		this.annotations = ctxt.getAnnotations();
		this.counterexample = null;
		this.witness = null;
		this.message = null;
		this.provedBy = null;
		
		POGetMatchingExpressionVisitor.init();	// Reset the "any" count, before PO creation
	}

	public String getSource()
	{
		return source;
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
		if (path == null)
		{
			counterexample = null;
		}
		else
		{
			counterexample = new Context(location, "Counterexample", null);
			Context ctxt = path;
			
			while (ctxt != null && ctxt.outer != null)
			{
				counterexample.putAll(ctxt);
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
			witness = new Context(location, "Witness", null);
			Context ctxt = path;
			
			while (ctxt != null && ctxt.outer != null)
			{
				witness.putAll(ctxt);
				ctxt = ctxt.outer;
			}
		}
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
	
	/**
	 * This is used to mark obligations as unchecked, with a reason code.
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
		if (counterexample == null)
		{
			return getLaunch(Interpreter.getInstance().getInitialContext());
		}
		
		return getLaunch(counterexample);
	}
	
	public String getWitnessLaunch()
	{
		if (witness == null)
		{
			return getLaunch(Interpreter.getInstance().getInitialContext());
		}
		
		return getLaunch(witness);
	}

	public String getLaunch(Context ctxt)
	{
		try
		{
			if (definition instanceof POExplicitFunctionDefinition)
			{
				POExplicitFunctionDefinition efd = (POExplicitFunctionDefinition)definition;
				return
					launchNameTypes(efd.name, efd.typeParams, ctxt) +
					launchArguments(efd.paramPatternList, ctxt);
			}
			else if (definition instanceof POImplicitFunctionDefinition)
			{
				POImplicitFunctionDefinition ifd = (POImplicitFunctionDefinition)definition;
				return
					launchNameTypes(ifd.name, ifd.typeParams, ctxt) +
					launchArguments(ifd.parameterPatterns, ctxt);
			}
			else if (definition instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition eop = (POExplicitOperationDefinition)definition;
				return
					launchNameTypes(eop.name, null, ctxt) +
					launchArguments(eop.getParamPatternList(), ctxt);
			}
			else if (definition instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition iop = (POImplicitOperationDefinition)definition;
				return
					launchNameTypes(iop.name, null, ctxt) +
					launchArguments(iop.parameterPatterns, ctxt);
			}
			else if (kind.isStandAlone())
			{
				// PO is a stand alone expression, so just execute that
				return source.trim();
			}
		}
		catch (Exception e)
		{
			// Cannot match all parameters from context
		}

		return null;
	}
	
	private String launchNameTypes(TCNameToken name, TCTypeList typeParams, Context ctxt) throws Exception
	{
		StringBuilder callString = new StringBuilder(name.getName());
		PORemoveIgnoresVisitor.init();
		
		if (typeParams != null)
		{
			String inst = addTypeParams(typeParams, ctxt);
			callString.append(inst);
		}

		return  callString.toString();
	}

	private String launchArguments(POPatternListList paramPatternList, Context ctxt) throws Exception
	{
		StringBuilder callString = new StringBuilder();

		for (POPatternList pl: paramPatternList)
		{
			String sep = "";
			callString.append("(");
			
			for (POPattern p: pl)
			{
				String match = paramMatch(p.removeIgnorePatterns(), ctxt);
				callString.append(sep);
				callString.append(match);
				sep = ", ";
			}
				
			callString.append(")");
		}
		
		return  callString.toString();
	}
	
	private String launchArguments(POPatternListTypePairList parameterPatterns, Context ctxt) throws Exception
	{
		StringBuilder callString = new StringBuilder();
		String sep = "";
		callString.append("(");
		
		for (POPatternListTypePair pl: parameterPatterns)
		{
			for (POPattern p: pl.patterns)
			{
				String match = paramMatch(p.removeIgnorePatterns(), ctxt);
				callString.append(sep);
				callString.append(match);
				sep = ", ";
			}
		}
		
		callString.append(")");
		
		return callString.toString();
	}

	private String addTypeParams(TCTypeList params, Context ctxt) throws Exception
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
				throw new Exception("Can't match type param " + param);
			}
			
			callString.append(sep);
			callString.append(inst.type);
			sep = ", ";
		}
		
		callString.append("]");
		return callString.toString();
	}

	private String paramMatch(POPattern p, Context ctxt) throws Exception
	{
		POGetMatchingConstantVisitor visitor = new POGetMatchingConstantVisitor();
		String result = p.apply(visitor, ctxt);
		
		if (visitor.hasFailed())
		{
			throw new Exception("Can't match param " + p);
		}
		else
		{
			return result;
		}
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
