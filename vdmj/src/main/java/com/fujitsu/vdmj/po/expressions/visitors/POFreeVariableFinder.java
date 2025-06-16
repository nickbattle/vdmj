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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.expressions.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POCaseAlternative;
import com.fujitsu.vdmj.po.expressions.POCasesExpression;
import com.fujitsu.vdmj.po.expressions.POExists1Expression;
import com.fujitsu.vdmj.po.expressions.POExistsExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POForAllExpression;
import com.fujitsu.vdmj.po.expressions.POIotaExpression;
import com.fujitsu.vdmj.po.expressions.POLambdaExpression;
import com.fujitsu.vdmj.po.expressions.POLetBeStExpression;
import com.fujitsu.vdmj.po.expressions.POLetDefExpression;
import com.fujitsu.vdmj.po.expressions.POMapCompExpression;
import com.fujitsu.vdmj.po.expressions.POSeqCompExpression;
import com.fujitsu.vdmj.po.expressions.POSetCompExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.visitors.Locals;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;

/**
 * A visitor set to explore the PO tree and return the free variable expressions accessed.
 * These are used to check the "reasons about" for obligations.
 */
public class POFreeVariableFinder extends POLeafExpressionVisitor<POVariableExpression, List<POVariableExpression>, Locals>
{
	@Override
	public List<POVariableExpression> caseVariableExpression(POVariableExpression node, Locals arg)
	{
		List<POVariableExpression> all = newCollection();
		
		if (!arg.find(node.name))
		{
			all.add(node);
		}

		return all;
	}
	
	@Override
	public List<POVariableExpression> caseApplyExpression(POApplyExpression node, Locals arg)
	{
		List<POVariableExpression> results = newCollection();
		
		if (node.root instanceof POVariableExpression)
		{
			POVariableExpression var = (POVariableExpression)node.root;
			
			if (var.name.isReserved())
			{
				return results;		// Don't bother checking pre/post/etc
			}
		}
		
		for (POExpression a: node.args)
		{
			results.addAll(a.apply(this, arg));
		}
		
		return results;
	}
	
	@Override
	public List<POVariableExpression> caseForAllExpression(POForAllExpression node, Locals arg)
	{
		TCNameSet locals = node.bindList.getVariableNames();
		return node.predicate.apply(this, new Locals(locals, arg));
	}
	
	@Override
	public List<POVariableExpression> caseExists1Expression(POExists1Expression node, Locals arg)
	{
		TCNameSet locals = node.bind.getVariableNames();
		return node.predicate.apply(this, new Locals(locals, arg));
	}
	
	@Override
	public List<POVariableExpression> caseExistsExpression(POExistsExpression node, Locals arg)
	{
		TCNameSet locals = node.bindList.getVariableNames();
		return node.predicate.apply(this, new Locals(locals, arg));
	}
	
	@Override
	public List<POVariableExpression> caseIotaExpression(POIotaExpression node, Locals arg)
	{
		TCNameSet locals = node.bind.getVariableNames();
		return node.predicate.apply(this, new Locals(locals, arg));
	}
	
	@Override
	public List<POVariableExpression> caseLetDefExpression(POLetDefExpression node, Locals arg)
	{
		TCNameSet locals = new TCNameSet();
		
		for (PODefinition def: node.localDefs)
		{
			locals.addAll(def.getVariableNames());
		}
		
		return node.expression.apply(this, new Locals(locals, arg));
	}

	@Override
	public List<POVariableExpression> caseLetBeStExpression(POLetBeStExpression node, Locals arg)
	{
		TCNameSet locals = node.bind.getVariableNames();
		return node.value.apply(this, new Locals(locals, arg));
	}
	
	@Override
	public List<POVariableExpression> caseCasesExpression(POCasesExpression node, Locals arg)
	{
		List<POVariableExpression> results = node.exp.apply(this, arg);
		
		for (POCaseAlternative each: node.cases)
		{
			TCNameList locals = each.pattern.getVariableNames();
			results.addAll(each.result.apply(this, new Locals(locals, arg)));
		}
		
		if (node.others != null)
		{
			results.addAll(node.others.apply(this, arg));
		}
		
		return results;
	}
	
	@Override
	public List<POVariableExpression> caseLambdaExpression(POLambdaExpression node, Locals arg)
	{
		TCNameSet locals = node.bindList.getVariableNames();
		return node.expression.apply(this, new Locals(locals, arg));
	}
	
	@Override
	public List<POVariableExpression> caseMapCompExpression(POMapCompExpression node, Locals arg)
	{
		if (node.predicate != null)
		{
			TCNameSet locals = node.bindings.getVariableNames();
			return node.predicate.apply(this, new Locals(locals, arg));
		}
		else
		{
			return newCollection();
		}
	}
	
	@Override
	public List<POVariableExpression> caseSeqCompExpression(POSeqCompExpression node, Locals arg)
	{
		if (node.predicate != null)
		{
			TCNameSet locals = node.bind.getVariableNames();
			return node.predicate.apply(this, new Locals(locals, arg));
		}
		else
		{
			return newCollection();
		}
	}
	
	@Override
	public List<POVariableExpression> caseSetCompExpression(POSetCompExpression node, Locals arg)
	{
		if (node.predicate != null)
		{
			TCNameSet locals = node.bindings.getVariableNames();
			return node.predicate.apply(this, new Locals(locals, arg));
		}
		else
		{
			return newCollection();
		}
	}
	
	@Override
	protected List<POVariableExpression> newCollection()
	{
		return new Vector<POVariableExpression>();
	}

	@Override
	public List<POVariableExpression> caseExpression(POExpression node, Locals arg)
	{
		return newCollection();
	}
}
