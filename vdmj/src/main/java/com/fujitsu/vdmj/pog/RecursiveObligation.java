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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;

public class RecursiveObligation extends ProofObligation
{
	public final boolean mutuallyRecursive;
	
	private RecursiveObligation(LexLocation location, PODefinitionList defs, POApplyExpression apply, POContextStack ctxt)
	{
		super(location, POType.RECURSIVE, ctxt);
		
		mutuallyRecursive = (defs.size() > 2);	// Simple recursion = [f, f]
		
		int measureLexical = getLex(getMeasureDef(defs.get(0)));
		
		String lhs = getLHS(defs.get(0));
		String rhs = apply.getMeasureApply(getMeasureName(defs.get(1)));

		source = ctxt.getSource(greater(measureLexical, lhs, rhs));
	}
	
	private String getLHS(PODefinition def)
	{
		StringBuilder sb = new StringBuilder();
		
		if (def instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition edef = (POExplicitFunctionDefinition)def;
			sb.append(getMeasureName(edef));
			
			if (edef.typeParams != null)
			{
				String sep = "";
				sb.append("[");
				
				for (TCType type: edef.typeParams)
				{
					sb.append(sep);
					sb.append(type);
					sep = ", ";
				}
				
				sb.append("]");
			}
			
			for (POPatternList plist: edef.paramPatternList)
			{
				String sep = "";
				sb.append("(");

				for (POPattern p: plist)
				{
					sb.append(sep);
					sb.append(p.removeIgnorePatterns());
					sep = ", ";
				}

				sb.append(")");
			}
		}
		else if (def instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition idef = (POImplicitFunctionDefinition)def;
			sb.append(getMeasureName(idef));
			sb.append("(");

			for (POPatternListTypePair pltp: idef.parameterPatterns)
			{
				String sep = "";
				
				for (POPattern p: pltp.patterns)
				{
					sb.append(sep);
					sb.append(p.removeIgnorePatterns());
					sep = ", ";
				}
			}

			sb.append(")");
		}
		
		return sb.toString();
	}
	
	private String getMeasureName(PODefinition def)
	{
		if (def instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition edef = (POExplicitFunctionDefinition)def;
			
			if (edef.measureName != null)
			{
				return edef.measureName.getName();
			}
			else
			{
				return "measure_" + edef.name.getName();
			}
		}
		else if (def instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition idef = (POImplicitFunctionDefinition)def;
			
			if (idef.measureName != null)
			{
				return idef.measureName.getName();
			}
			else
			{
				return "measure_" + idef.name.getName();
			}
		}
		else
		{
			return null;
		}
	}

	private POExplicitFunctionDefinition getMeasureDef(PODefinition def)
	{
		if (def instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition edef = (POExplicitFunctionDefinition)def;
			return edef.measureDef;
		}
		else if (def instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition idef = (POImplicitFunctionDefinition)def;
			return idef.measureDef;
		}
		else
		{
			return null;
		}
	}

	private int getLex(POExplicitFunctionDefinition mdef)
	{
		if (mdef == null)
		{
			return 0;
		}
		
		TCFunctionType ftype = (TCFunctionType) mdef.getType();
		
		while (ftype.result instanceof TCFunctionType)	// Skip curries
		{
			ftype = (TCFunctionType)ftype.result;
		}
		
		if (ftype.result instanceof TCProductType)
		{
			TCProductType type = (TCProductType)ftype.result;
			return type.types.size();
		}
		else
		{
			return 0;
		}
	}

	private String greater(int lexical, String lhs, String rhs)
	{
		if (lexical > 0)
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("(let lhs = ");
			sb.append(lhs);
			sb.append(", rhs = ");
			sb.append(rhs);
			sb.append(" in\n  ");
			
			String kw = "if";
			
			for (int i=1; i < lexical; i++)
			{
				sb.append(String.format("%s lhs.#%d <> rhs.#%d then lhs.#%d > rhs.#%d ", kw, i, i, i, i)); 
				kw = "elseif";
			}
			
			sb.append(String.format("else lhs.#%d > rhs.#%d)", lexical, lexical));
			
			return sb.toString();
		}
		else
		{
			return lhs + " > " + rhs;
		}
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(LexLocation location, PODefinitionList defs, POApplyExpression apply, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new RecursiveObligation(location, defs, apply, choice));
		}
		
		return results;
	}
}
