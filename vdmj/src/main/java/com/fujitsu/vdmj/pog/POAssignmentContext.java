/*******************************************************************************
 *
 *	Copyright (c) 2024 Fujitsu Services Ltd.
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

import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POMapEnumExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpressionList;
import com.fujitsu.vdmj.po.expressions.POPlusPlusExpression;
import com.fujitsu.vdmj.po.expressions.POUndefinedExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.statements.POIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POMapSeqDesignator;
import com.fujitsu.vdmj.po.statements.POStateDesignator;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

public class POAssignmentContext extends POContext
{
	public final PODefinitionList assignmentDefs;
	public final String pattern;
	public final TCType type;
	public final POExpression expression;
	public final String tooComplex;

	public POAssignmentContext(PODefinitionList assignmentDefs)
	{
		this.assignmentDefs = new PODefinitionList();
		this.pattern = null;
		this.type = null;
		this.expression = null;
		this.tooComplex = null;
		
		/**
		 * Filter out things like "dcl x:nat;", which become lets with undefined values.
		 */
		for (PODefinition def: assignmentDefs)
		{
			POAssignmentDefinition adef = (POAssignmentDefinition) def;

			if (!(adef.expression instanceof POUndefinedExpression))
			{
				this.assignmentDefs.add(adef);
			}
		}
	}

	public POAssignmentContext(POStateDesignator target, TCType type, POExpression expression, boolean tooComplex)
	{
		this.assignmentDefs = null;
		
		if (tooComplex)
		{
			this.pattern = "/* " + target + " */ -";
			this.tooComplex = ProofObligation.COMPLEX_ASSIGNMENT;
			this.expression = expression;
			this.type = type;
		}
		else if (target instanceof POIdentifierDesignator)
		{
			POIdentifierDesignator id = (POIdentifierDesignator)target;
			this.pattern = id.toString();
			this.tooComplex = null;
			this.expression = expression;
			this.type = type;
		}
		else if (target instanceof POMapSeqDesignator)
		{
			POMapSeqDesignator ms = (POMapSeqDesignator)target;
			
			if (ms.mapseq instanceof POIdentifierDesignator)
			{
				// For "s(i) = e" create "let s = s ++ {i |-> e} in ..." 
				
				POIdentifierDesignator id = (POIdentifierDesignator)ms.mapseq;
				this.pattern = id.toString();
				this.tooComplex = null;
				
				if (id.vardef != null)
				{
					this.type = id.vardef.getType();		// eg. m(k) is a map
				}
				else
				{
					this.type = type;						// eg. x := 123 is a nat
				}
				
				POMapletExpressionList maplets = new POMapletExpressionList();
				maplets.add(new POMapletExpression(target.location, ms.exp, expression));
				
				TCTypeList ltypes = new TCTypeList(ms.exp.getExptype());
				TCTypeList rtypes = new TCTypeList(expression.getExptype());
				
				this.expression = new POPlusPlusExpression(
					new POVariableExpression(id.name, null),
					new LexKeywordToken(Token.PLUSPLUS, target.location),
					new POMapEnumExpression(target.location, maplets, ltypes, rtypes),
					ms.exp.getExptype(), expression.getExptype());
			}
			else
			{
				throw new IllegalArgumentException("Designator too complex");
			}
		}
		else
		{
			throw new IllegalArgumentException("Designator too complex");
		}
	}

	@Override
	public boolean isScopeBoundary()
	{
		return true;
	}
	
	@Override
	public String markObligation()
	{
		return tooComplex;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		String sep = "";
		
		if (assignmentDefs == null)
		{
			sb.append("let ");
			sb.append(pattern);
			sb.append(" : ");
			sb.append(type.toExplicitString(expression.location));
			sb.append(" = ");
			sb.append(expression);
			sb.append(" in");
		}
		else if (!assignmentDefs.isEmpty())
		{
			sb.append("let ");

			for (PODefinition def: assignmentDefs)
			{
				sb.append(sep);
				POAssignmentDefinition adef = (POAssignmentDefinition)def;
				sb.append(adef.name);
				sb.append(" : ");
				sb.append(adef.type.toExplicitString(adef.location));
				sb.append(" = ");
				sb.append(adef.expression);
				sep = ", ";
			}

			sb.append(" in");
		}

		return sb.toString();
	}
	
	@Override
	public TCNameSet reasonsAbout()
	{
		TCNameSet names = new TCNameSet();
		
		if (assignmentDefs == null)
		{
			names.addAll(expression.getVariableNames());
			TCNameToken pname = new TCNameToken(expression.location, expression.location.module, pattern);
			names.add(pname);
		}
		else
		{
			for (PODefinition def: assignmentDefs)
			{
				POAssignmentDefinition adef = (POAssignmentDefinition) def;
				names.addAll(adef.expression.getVariableNames());
			}
		}
		
		return names;
	}
}
