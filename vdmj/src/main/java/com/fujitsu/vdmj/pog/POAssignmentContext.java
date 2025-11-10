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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
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
import com.fujitsu.vdmj.po.expressions.POMuExpression;
import com.fujitsu.vdmj.po.expressions.POPlusPlusExpression;
import com.fujitsu.vdmj.po.expressions.PORecordModifier;
import com.fujitsu.vdmj.po.expressions.PORecordModifierList;
import com.fujitsu.vdmj.po.expressions.POUndefinedExpression;
import com.fujitsu.vdmj.po.statements.POFieldDesignator;
import com.fujitsu.vdmj.po.statements.POIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POMapSeqDesignator;
import com.fujitsu.vdmj.po.statements.POStateDesignator;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

public class POAssignmentContext extends POContext
{
	public final PODefinitionList assignmentDefs;
	public final String pattern;
	public TCType type;
	public final POExpression expression;

	public POAssignmentContext(PODefinitionList assignmentDefs)
	{
		this.assignmentDefs = new PODefinitionList();
		this.pattern = null;
		this.type = null;
		this.expression = null;

		this.assignmentDefs.addAll(assignmentDefs);
	}

	public POAssignmentContext(POStateDesignator target, TCType type, POExpression expression)
	{
		this.assignmentDefs = null;
		
		this.type = type;
		this.pattern = updatedVariable(target);
		this.expression = updateExpression(target, expression);
	}
	
	/**
	 * The simple updated variable name, x := 1, x(i) := 1 and x(i)(2).fld := 1
	 * all return the updated variable "x".
	 */
	private String updatedVariable(POStateDesignator designator)
	{
		TCType type = designator.updatedVariableType();
		
		if (type != null)
		{
			this.type = type;	// eg. a map or seq type
		}
		
		return designator.updatedVariableName().getName();
	}
	
	/**
	 * Return a POExpression for the update value for the updatedVariable above.
	 * 
	 * For x := E updates, it is "E"
	 * For x(i) := E updates, it is "x ++ {i |-&gt; E}"
	 * For x.fld := E updates, it is "mu(x, {fld |-&gt; E})"
	 * 
	 * More complex combinations nest these expressions. Note that if the designator is
	 * actually an object field update, we can't handle this case.
	 */
	private POExpression updateExpression(POStateDesignator designator, POExpression update)
	{
		if (designator instanceof POIdentifierDesignator)
		{
			return update;
		}
		else if (designator instanceof POMapSeqDesignator)
		{
			POMapSeqDesignator msd = (POMapSeqDesignator)designator;
			
			POMapletExpressionList maplets = new POMapletExpressionList();
			maplets.add(new POMapletExpression(msd.location, msd.exp, update));
			
			TCTypeList ltypes = new TCTypeList(msd.exp.getExptype());
			TCTypeList rtypes = new TCTypeList(update.getExptype());
			
			POPlusPlusExpression ppe = new POPlusPlusExpression(
				msd.mapseq.toExpression(),
				new LexKeywordToken(Token.PLUSPLUS, msd.location),
				new POMapEnumExpression(msd.location, maplets, ltypes, rtypes),
				msd.exp.getExptype(), update.getExptype());
			
			return updateExpression(msd.mapseq, ppe);
		}
		else if (designator instanceof POFieldDesignator)
		{
			POFieldDesignator fld = (POFieldDesignator)designator;

			if (fld.clsType != null)
			{
				// This is an object field assignment, which we can't "mu". So
				// we just return the original expression here.
				return update;
			}
			
			PORecordModifierList muUpdates = new PORecordModifierList();
			muUpdates.add(new PORecordModifier(fld.field, update));
			
			TCField rfield = fld.recType.findField(fld.field.getName());
			TCTypeList ftypes = new TCTypeList(rfield.type);
			TCRecordType rtype = fld.recType;
			
			POMuExpression mu = new POMuExpression(fld.location,
				fld.object.toExpression(), muUpdates, rtype, ftypes);
			
			return updateExpression(fld.object, mu);
		}
		else
		{
			throw new IllegalArgumentException("Unexpected designator?");
		}
	}

	@Override
	public boolean isScopeBoundary()
	{
		return true;
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

			// This is because (say) "let x : nat = undefined" will fail, whereas
			// "let x = undefined" will be okay, as long as x is defined before it
			// is used. This occurs with "dcl x : nat;" without an initializer.

			if (!(expression instanceof POUndefinedExpression))
			{
				sb.append(" : ");
				sb.append(type.toExplicitString(expression.location));
			}
			
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

				// This is because (say) "let x : nat = undefined" will fail, whereas
				// "let x = undefined" will be okay, as long as x is defined before it
				// is used. This occurs with "dcl x : nat;" without an initializer.
			
				if (!(adef.expression instanceof POUndefinedExpression))
				{
					sb.append(" : ");
					sb.append(adef.type.toExplicitString(adef.location));
				}
			
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
