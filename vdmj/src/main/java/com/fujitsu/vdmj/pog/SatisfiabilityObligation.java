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

import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.definitions.POTypeDefinition;
import com.fujitsu.vdmj.po.statements.POSpecificationStatement;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.po.types.POPatternTypePair;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class SatisfiabilityObligation extends ProofObligation
{
	private String separator = "";

	public SatisfiabilityObligation(POImplicitFunctionDefinition func, POContextStack ctxt)
	{
		super(func.location, POType.FUNC_SATISFIABILITY, ctxt);
		StringBuilder sb = new StringBuilder();

		if (func.predef != null)
		{
    		sb.append(func.predef.name.getName());
    		sb.append("(");
			separator = "";
    		appendParamPatterns(sb, func.parameterPatterns);
    		sb.append(")");
    		sb.append(" => ");
		}

		sb.append("exists ");
		sb.append(func.result);
		sb.append(" & ");
		sb.append(func.postdef.name.getName());
		sb.append("(");
		separator = "";
		appendParamPatterns(sb, func.parameterPatterns);
		sb.append(separator);
		sb.append(func.result.pattern);
		sb.append(")");

		source = ctxt.getSource(sb.toString());
	}

	public SatisfiabilityObligation(POImplicitOperationDefinition op,
		PODefinition stateDefinition, POContextStack ctxt)
	{
		super(op.location, POType.OP_SATISFIABILITY, ctxt);
		StringBuilder sb = new StringBuilder();

		if (op.predef != null)
		{
    		sb.append(op.predef.name.getName());
    		sb.append("(");
    		separator = "";
    		appendParamPatterns(sb, op.parameterPatterns);
    		appendStatePatterns(sb, stateDefinition, true, false);
    		sb.append(")");
    		sb.append(" =>\n");
		}

		if (op.result != null || stateDefinition != null)
		{
			sb.append("exists ");
			separator = "";
			appendResult(sb, op.result);
			appendStatePatterns(sb, stateDefinition, false, true);
			// appendStatePatterns(sb, stateDefinition, true, true);
			sb.append(" & ");
		}

		sb.append(op.postdef.name.getName());
		sb.append("(");
		separator = "";
		appendParamPatterns(sb, op.parameterPatterns);
		appendResultPattern(sb, op.result);
		appendStatePatterns(sb, stateDefinition, true, false);
		appendStatePatterns(sb, stateDefinition, false, false);
		sb.append(")");

		source = ctxt.getSource(sb.toString());
	}

	public SatisfiabilityObligation(POSpecificationStatement spec, POContextStack ctxt, Environment env)
	{
		super(spec.location, POType.STMT_SATISFIABILITY, ctxt);
		StringBuilder sb = new StringBuilder();
		TCNameSet names = new TCNameSet();

		if (spec.precondition != null)
		{
    		sb.append(spec.precondition);
    		sb.append(" => ");
			names.addAll(spec.precondition.getVariableNames());
		}

		names.addAll(spec.postcondition.getVariableNames());

		if (!names.isEmpty())
		{
			sb.append("exists ");
			String sep = "";

			for (TCNameToken name: names)
			{
				TCDefinition def = env.findName(name, NameScope.NAMESANDSTATE);
				
				if (def != null && !def.name.isOld())
				{
					sb.append(sep);
					sb.append(def.name);
					sb.append(":");
					sb.append(def.getType());
					sep = ", ";
				}
			}

			sb.append(" & ");
		}

		sb.append(spec.postcondition);

		source = ctxt.getSource(sb.toString().replaceAll("~", "\\$"));
	}

	public SatisfiabilityObligation(POTypeDefinition typedef, POContextStack ctxt)
	{
		super(typedef.invPattern.location, POType.INV_SATISFIABILITY, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("exists ");
		separator = "";
		sb.append(typedef.invPattern);
		sb.append(" : ");
		
		if (typedef.type instanceof TCNamedType)
		{
			TCNamedType nt = (TCNamedType)typedef.type;
			sb.append(explicitType(nt.type, typedef.location));
		}
		else
		{
			sb.append(typedef.type);
		}
		
		sb.append(" & ");
		sb.append(typedef.invExpression);
		
		source = ctxt.getSource(sb.toString());
	}

	public SatisfiabilityObligation(POStateDefinition statedef, POContextStack ctxt)
	{
		super(statedef.invPattern.location, POType.INV_SATISFIABILITY, ctxt);
		this.definition = statedef.invdef;
		StringBuilder sb = new StringBuilder();

		sb.append("exists ");
		separator = "";
		sb.append(statedef.invPattern);
		sb.append(" : ");
		sb.append(statedef.name);
		sb.append(" & ");
		sb.append(statedef.invExpression);
		
		source = ctxt.getSource(sb.toString());
	}

	private void appendResult(StringBuilder sb, POPatternTypePair ptp)
	{
		if (ptp != null)
		{
			sb.append(separator);
			sb.append(ptp);
			separator = ", ";
		}
	}

	private void appendResultPattern(StringBuilder sb, POPatternTypePair ptp)
	{
		if (ptp != null)
		{
			sb.append(separator);
			sb.append(ptp.pattern);
			separator = ", ";
		}
	}

	private void appendStatePatterns(StringBuilder sb, PODefinition state, boolean old, boolean typed)
	{
		if (state == null)
		{
			return;
		}
		else if (state instanceof POStateDefinition)
		{
			if (old)
			{
				sb.append(separator);
				sb.append("oldstate");
			}
			else
			{
				sb.append(separator);
				sb.append("newstate");
			}

			if (typed)
			{
				POStateDefinition def = (POStateDefinition)state;
				sb.append(":");
				sb.append(def.name.getName());
			}
		}
		else
		{
			if (old)
			{
				sb.append(separator);
				sb.append("oldself");
			}
			else
			{
				sb.append(separator);
				sb.append("newself");
			}

			if (typed)
			{
				POClassDefinition def = (POClassDefinition)state;
				sb.append(":");
				sb.append(def.name.getName());
			}
		}

		separator = ", ";
	}

	private void appendParamPatterns(StringBuilder sb, List<POPatternListTypePair> params)
	{
		for (POPatternListTypePair pltp: params)
		{
			sb.append(separator);
			sb.append(pltp.patterns.getMatchingExpressionList());
			separator = ", ";
		}
	}

	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(POSpecificationStatement spec,
		POContextStack ctxt, Environment env)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new SatisfiabilityObligation(spec, choice, env));
		}
		
		return results;
	}
}
