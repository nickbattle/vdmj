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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.annotations.POOperationMeasureAnnotation;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.statements.POCallObjectStatement;
import com.fujitsu.vdmj.po.statements.POCallStatement;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;

public class RecursiveObligation extends ProofObligation
{
	public final boolean mutuallyRecursive;
	
	private RecursiveObligation(LexLocation location, POType potype, PODefinitionList defs, POApplyExpression apply, POContextStack ctxt)
	{
		super(location, potype, ctxt);
		
		mutuallyRecursive = (defs.size() > 2);	// Simple recursion = [f, f]
		
		int measureLexical = getLex(getMeasureType(defs.get(0)));
		
		String lhs = potype == POType.OP_RECURSIVE ? getGhostName(defs.get(0)) : getLHS(defs.get(0));
		String rhs = apply.getMeasureApply(getMeasureName(defs.get(1)));

		source = ctxt.getSource(greater(measureLexical, lhs, rhs));
		uncheckedTests(defs.get(0), defs.get(1));
	}

	private RecursiveObligation(LexLocation location, PODefinitionList defs, POCallStatement call, POContextStack ctxt)
	{
		super(location, POType.OP_RECURSIVE, ctxt);
		
		mutuallyRecursive = (defs.size() > 2);	// Simple recursion = [f, f]
		
		int measureLexical = getLex(getMeasureType(defs.get(0)));
		
		String lhs = getGhostName(defs.get(0));
		String rhs = call.getMeasureApply(getMeasureName(defs.get(1)));

		source = ctxt.getSource(greater(measureLexical, lhs, rhs));
		uncheckedTests(defs.get(0), defs.get(1));
	}

	private RecursiveObligation(LexLocation location, PODefinitionList defs, POCallObjectStatement call, POContextStack ctxt)
	{
		super(location, POType.OP_RECURSIVE, ctxt);
		
		mutuallyRecursive = (defs.size() > 2);	// Simple recursion = [f, f]
		
		int measureLexical = getLex(getMeasureType(defs.get(0)));
		
		String lhs = getGhostName(defs.get(0));
		String rhs = call.getMeasureApply(getMeasureName(defs.get(1)));

		source = ctxt.getSource(greater(measureLexical, lhs, rhs));
		uncheckedTests(defs.get(0), defs.get(1));
	}

	private void uncheckedTests(PODefinition first, PODefinition second)
	{
		boolean isOp = 
			second instanceof POExplicitOperationDefinition ||
			second instanceof POImplicitOperationDefinition;

		if (isOp && Settings.dialect != Dialect.VDM_SL)
		{
			if (!first.classDefinition.hasNew() ||
				first != second && !second.classDefinition.hasNew())
			{
				// We cannot call new A(x,y,z) to construct an object, so...
				markUnchecked(ProofObligation.UNCHECKED_VDMPP);
			}
		}
	}

	private String getGhostName(PODefinition def)
	{
		if (def.annotations != null)
		{
			POOperationMeasureAnnotation measure = def.annotations.getInstance(POOperationMeasureAnnotation.class);
			
			if (measure != null)
			{
				return measure.ghostName.getName();
			}
		}

		markUnchecked(ProofObligation.MISSING_MEASURE);
		return getLHS(def);		// eg. "measure_op(args, state)"
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
		else if (def instanceof POExplicitOperationDefinition)
		{
			POExplicitOperationDefinition edef = (POExplicitOperationDefinition)def;
			sb.append(getMeasureName(edef));
			sb.append("(");
			String sep = "";
			
			for (POPattern p: edef.parameterPatterns)
			{
				sb.append(sep);
				sb.append(p.removeIgnorePatterns());
				sep = ", ";
			}

			if (edef.stateDefinition != null)
			{
				sb.append(sep);

				if (!def.location.sameModule(location))
				{
					sb.append(POSaveStateContext.getOldName());
				}
				else
				{
					sb.append(edef.stateDefinition.toPattern(false, location));
				}
			}
			else if (edef.classDefinition != null)
			{
				sb.append(sep);

				if (!def.location.sameModule(location))
				{
					sb.append(POSaveStateContext.getOldName());
				}
				else
				{
					sb.append(edef.classDefinition.toNew());
				}
			}

			sb.append(")");
		}
		else if (def instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition idef = (POImplicitOperationDefinition)def;
			sb.append(getMeasureName(idef));
			sb.append("(");
			String sep = "";

			for (POPatternListTypePair pltp: idef.parameterPatterns)
			{
				for (POPattern p: pltp.patterns)
				{
					sb.append(sep);
					sb.append(p.removeIgnorePatterns());
					sep = ", ";
				}
			}

			if (idef.stateDefinition != null)
			{
				sb.append(sep);

				if (!def.location.sameModule(location))
				{
					sb.append(POSaveStateContext.getOldName());
				}
				else
				{
					sb.append(idef.stateDefinition.toPattern(false, location));
				}
			}
			else if (idef.classDefinition != null)
			{
				sb.append(sep);

				if (!def.location.sameModule(location))
				{
					sb.append(POSaveStateContext.getOldName());
				}
				else
				{
					sb.append(idef.classDefinition.toNew());
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
				return edef.measureName.toExplicitString(location);
			}
		}
		else if (def instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition idef = (POImplicitFunctionDefinition)def;
			
			if (idef.measureName != null)
			{
				return idef.measureName.toExplicitString(location);
			}
		}
		else if (def instanceof POExplicitOperationDefinition)
		{
			POExplicitOperationDefinition edef = (POExplicitOperationDefinition)def;
			
			if (edef.measureDef != null && edef.measureDef.name != null)
			{
				return edef.measureDef.name.toExplicitString(location);
			}
		}
		else if (def instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition idef = (POImplicitOperationDefinition)def;
			
			if (idef.measureDef != null && idef.measureDef.name != null)
			{
				return idef.measureDef.name.toExplicitString(location);
			}
		}

		return "measure_" + def.name.toExplicitString(location);		// Fallback?
	}

	private TCType getMeasureType(PODefinition def)
	{
		if (def instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition edef = (POExplicitFunctionDefinition)def;

			if (edef.measureDef != null)
			{
				return edef.measureDef.getType();
			}
		}
		else if (def instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition idef = (POImplicitFunctionDefinition)def;

			if (idef.measureDef != null)
			{
				return idef.measureDef.getType();
			}
		}
		else if (def instanceof POExplicitOperationDefinition)
		{
			POExplicitOperationDefinition exop = (POExplicitOperationDefinition)def;

			if (exop.measureDef != null)
			{
				return exop.measureDef.getType();
			}
		}
		else if (def instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition imop = (POImplicitOperationDefinition)def;

			if (imop.measureDef != null)
			{
				return imop.measureDef.getType();
			}
		}
		
		return new TCUnknownType(location);
	}

	private int getLex(TCType type)
	{
		if (type == null)
		{
			return 0;
		}
		else if (type instanceof TCFunctionType)
		{
			TCFunctionType ftype = (TCFunctionType) type;
			
			while (ftype.result instanceof TCFunctionType)	// Skip curries
			{
				ftype = (TCFunctionType)ftype.result;
			}

			if (ftype.result instanceof TCProductType)
			{
				TCProductType ptype = (TCProductType)ftype.result;
				return ptype.types.size();
			}
			else
			{
				return 0;
			}
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
	 * @param potype 
	 */
	public static ProofObligationList getAllPOs(LexLocation location, POType potype, PODefinitionList defs, POApplyExpression apply, POContextStack ctxt)
	{
		ProofObligationList results = new ProofObligationList();
		int popto = addRemoteState(location, apply.args, apply.opdef, ctxt);
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new RecursiveObligation(location, potype, defs, apply, choice));
		}
		
		ctxt.popTo(popto);
		return results;
	}

	public static ProofObligationList getAllPOs(LexLocation location, PODefinitionList defs, POCallStatement call, POContextStack ctxt)
	{
		ProofObligationList results = new ProofObligationList();
		int popto = addRemoteState(location, call.args, call.opdef, ctxt);
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new RecursiveObligation(location, defs, call, choice));
		}
		
		ctxt.popTo(popto);
		return results;
	}

	public static ProofObligationList getAllPOs(LexLocation location, PODefinitionList defs, POCallObjectStatement call, POContextStack ctxt)
	{
		ProofObligationList results = new ProofObligationList();
		int popto = addRemoteState(location, call.args, call.fdef, ctxt);
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new RecursiveObligation(location, defs, call, choice));
		}
		
		ctxt.popTo(popto);
		return results;
	}

	/**
	 * If we are measuring an operation call in a remote module/class, we have to
	 * create a quantifier for the state and check any preconditions.
	 */
	private static int addRemoteState(LexLocation from, POExpressionList args, PODefinition opdef, POContextStack ctxt)
	{
		int popto = ctxt.size();

		if (opdef != null && !opdef.location.sameModule(from))
		{
			ctxt.push(new POSaveStateContext(opdef, from, false));

			if (opdef instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition exop = (POExplicitOperationDefinition)opdef;

				if (exop.predef != null)
				{
					ctxt.push(new POImpliesContext(preApply(from, args, exop.predef)));
				}
			}
		}

		return popto;
	}

	private static POExpression preApply(LexLocation from, POExpressionList callargs, POExplicitFunctionDefinition predef)
	{
		POExpressionList preargs = new POExpressionList();
		preargs.addAll(callargs);
		
		if (predef.stateDefinition != null || predef.classDefinition != null)
		{
			TCNameToken state = new TCNameToken(predef.location, predef.name.getModule(), POSaveStateContext.getOldName());
			preargs.add(new POVariableExpression(state, null));
		}

		boolean remote = !predef.location.sameModule(from);

		return new POApplyExpression(
			new POVariableExpression(predef.name.getExplicit(remote), null),
			preargs,
			predef.getType(),
			null,
			null,
			null,
			null);
	}
}