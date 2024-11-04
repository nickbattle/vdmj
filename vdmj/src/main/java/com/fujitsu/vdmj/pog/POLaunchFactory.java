/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POPatternListList;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingConstantVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.po.types.POPatternListTypePairList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.values.ParameterValue;

/**
 * A class to help with the construction of launch invocations for POs.
 */
public class POLaunchFactory
{
	private static class ApplyCall
	{
		public String applyName = null;
		public List<String> applyTypes = new Vector<String>();
		public List<ApplyArg> applyArgs = new Vector<ApplyArg>();
	}
	
	private static class ApplyArg
	{
		public String name;
		public String type;
		public String value;
		
		public ApplyArg(String name, String type, String value)
		{
			this.name = name;
			this.type = type;
			this.value = value;
		}
	}
	
	private final ProofObligation po;
	private ApplyCall applyCall = null;
	
	public POLaunchFactory(ProofObligation po)
	{
		this.po = po;
		init();
	}
	
	private void init()
	{
		applyCall = null;	// Also used on exceptions
	}

	/**
	 * Generate a string invocation or ApplyCall for the PO context.
	 */
	public String getCexLaunch()
	{
		if (po.counterexample == null)
		{
			return getLaunch(Interpreter.getInstance().getInitialContext());
		}
		
		return getLaunch(po.counterexample);
	}
	
	public String getWitnessLaunch()
	{
		if (po.witness == null)
		{
			return getLaunch(Interpreter.getInstance().getInitialContext());
		}
		
		return getLaunch(po.witness);
	}
	
	public ApplyCall getCexApply()
	{
		getCexLaunch();
		return applyCall;
	}
	
	public ApplyCall getWitnessApply()
	{
		getWitnessLaunch();
		return applyCall;
	}

	public String getLaunch(Context ctxt)
	{
		try
		{
			applyCall = new ApplyCall();

			if (po.definition instanceof POExplicitFunctionDefinition)
			{
				POExplicitFunctionDefinition efd = (POExplicitFunctionDefinition)po.definition;
				return
					launchNameTypes(efd.name, efd.typeParams, ctxt) +
					launchArguments(efd.paramPatternList, efd.type, ctxt);
			}
			else if (po.definition instanceof POImplicitFunctionDefinition)
			{
				POImplicitFunctionDefinition ifd = (POImplicitFunctionDefinition)po.definition;
				return
					launchNameTypes(ifd.name, ifd.typeParams, ctxt) +
					launchArguments(ifd.parameterPatterns, ctxt);
			}
			else if (po.definition instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition eop = (POExplicitOperationDefinition)po.definition;
				return
					launchNameTypes(eop.name, null, ctxt) +
					launchArguments(eop.parameterPatterns, eop.type.parameters, ctxt);
			}
			else if (po.definition instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition iop = (POImplicitOperationDefinition)po.definition;
				return
					launchNameTypes(iop.name, null, ctxt) +
					launchArguments(iop.parameterPatterns, ctxt);
			}
			else if (po.kind.isStandAlone())
			{
				// PO is a stand alone expression, so just execute that
				return po.source.trim();
			}
		}
		catch (Exception e)
		{
			// Cannot match all parameters from context
			init();
		}

		return null;
	}
	
	private String launchNameTypes(TCNameToken name, TCTypeList typeParams, Context ctxt) throws Exception
	{
		StringBuilder callString = new StringBuilder(name.getName());
		PORemoveIgnoresVisitor.init();
		applyCall.applyName = name.getName();
		
		if (typeParams != null)
		{
			String inst = addTypeParams(typeParams, ctxt);
			callString.append(inst);
		}

		return  callString.toString();
	}

	private String launchArguments(POPatternListList paramPatternList, TCFunctionType type, Context ctxt) throws Exception
	{
		StringBuilder callString = new StringBuilder();
		TCType _type = type;

		for (POPatternList pl: paramPatternList)
		{
			TCFunctionType ftype = (TCFunctionType)_type;
			callString.append(launchArguments(pl, ftype.parameters, ctxt));
			_type = ftype.result;
		}
		
		return  callString.toString();
	}

	private String launchArguments(POPatternList paramPatternList, TCTypeList types, Context ctxt) throws Exception
	{
		StringBuilder callString = new StringBuilder();
		callString.append("(");
		String sep = "";
		int i = 0;
		
		for (POPattern p: paramPatternList)
		{
			String match = paramMatch(p.removeIgnorePatterns(), types.get(i++), ctxt);
			callString.append(sep);
			callString.append(match);
			sep = ", ";
		}
		
		callString.append(")");
		return callString.toString();
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
				String match = paramMatch(p.removeIgnorePatterns(), pl.type, ctxt);
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
			applyCall.applyTypes.add(inst.type.toString());
			sep = ", ";
		}
		
		callString.append("]");
		return callString.toString();
	}

	private String paramMatch(POPattern p, TCType type, Context ctxt) throws Exception
	{
		POGetMatchingConstantVisitor visitor = new POGetMatchingConstantVisitor();
		String result = p.apply(visitor, ctxt);
		
		if (visitor.hasFailed())
		{
			throw new Exception("Can't match param " + p);
		}
		else
		{
			applyCall.applyArgs.add(new ApplyArg(p.toString(), type.toString(), result));
			return result;
		}
	}
}
