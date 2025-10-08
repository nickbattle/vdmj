/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.expressions.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCRenamedDefinition;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCCaseAlternative;
import com.fujitsu.vdmj.tc.expressions.TCCasesExpression;
import com.fujitsu.vdmj.tc.expressions.TCDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCExists1Expression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCIotaExpression;
import com.fujitsu.vdmj.tc.expressions.TCLambdaExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetBeStExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.patterns.visitors.TCFreeVariableBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCFreeVariableMultipleBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCFreeVariablePatternVisitor;
import com.fujitsu.vdmj.tc.types.visitors.TCFreeVariableTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCFreeVariableExpressionVisitor extends TCLeafExpressionVisitor<TCNameToken, TCNameSet, Environment>
{
	public TCFreeVariableExpressionVisitor(TCVisitorSet<TCNameToken, TCNameSet, Environment> visitors)
	{
		assert visitors != null : "Visitor set cannot be null";
		visitorSet = visitors;
	}

	/**
	 * Special constructor that creates a visitorSet independent of the Definition visitor. This is
	 * used in TCExpression.getFreeVariables().
	 */
	public TCFreeVariableExpressionVisitor()
	{
		visitorSet = new TCVisitorSet<TCNameToken, TCNameSet, Environment>()
		{
			@Override
			protected void setVisitors()
			{
				expressionVisitor = new TCFreeVariableExpressionVisitor(this);
				patternVisitor = new TCFreeVariablePatternVisitor(this);
				typeVisitor = new TCFreeVariableTypeVisitor();
				bindVisitor = new TCFreeVariableBindVisitor(this);
				multiBindVisitor = new TCFreeVariableMultipleBindVisitor(this); 
			}

			@Override
			protected TCNameSet newCollection()
			{
				return TCFreeVariableExpressionVisitor.this.newCollection();
			}
		};
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseExpression(TCExpression node, Environment arg)
	{
		return newCollection();
	}

	@Override
	public TCNameSet caseApplyExpression(TCApplyExpression node, Environment arg)
	{
		TCNameSet names = visitorSet.applyExpressionVisitor(node.root, arg);
		
		for (TCExpression a: node.args)
		{
			names.addAll(a.apply(this, arg));
		}
		
		return names;
	}
	
 	@Override
	public TCNameSet caseCasesExpression(TCCasesExpression node, Environment arg)
	{
 		TCNameSet all = node.exp.apply(this, arg);
		
		for (TCCaseAlternative a: node.cases)
		{
			Environment local = new FlatEnvironment(a.pattern.getDefinitions(node.expType, NameScope.LOCAL), arg);
			all.addAll(visitorSet.applyPatternVisitor(a.pattern, local));
			all.addAll(a.result.apply(this, local));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.others, arg));
		return all;
	}
	
 	@Override
 	public TCNameSet caseDefExpression(TCDefExpression node, Environment arg)
 	{
		Environment local = arg;
		TCNameSet names = new TCNameSet();

		for (TCDefinition d: node.localDefs)
		{
			if (d instanceof TCExplicitFunctionDefinition)
			{
				// ignore
			}
			else
			{
				local = new FlatEnvironment(d, local);
				names.addAll(visitorSet.applyDefinitionVisitor(d, local));
			}
		}

		names.addAll(node.expression.apply(this, local));
		return names;
 	}
 	
	@Override
	public TCNameSet caseExists1Expression(TCExists1Expression node, Environment arg)
	{
		TCNameSet all = visitorSet.applyBindVisitor(node.bind, arg);
		
		if (node.predicate != null)
		{
			Environment local = new FlatEnvironment(node.def, arg);
			all.addAll(node.predicate.apply(this, local));
		}
		
		return all;
	}
	
	@Override
	public TCNameSet caseExistsExpression(TCExistsExpression node, Environment arg)
	{
		TCNameSet all = newCollection();
		
		for (TCMultipleBind bind: node.bindList)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
		}
		
		if (node.predicate != null)
		{
			Environment local = new FlatEnvironment(node.def, arg);
			all.addAll(node.predicate.apply(this, local));
		}
		
		return all;
	}
	
	@Override
	public TCNameSet caseForAllExpression(TCForAllExpression node, Environment arg)
	{
		TCNameSet all = newCollection();
		
		for (TCMultipleBind bind: node.bindList)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
		}
		
		if (node.predicate != null)
		{
			Environment local = new FlatEnvironment(node.def, arg);
			all.addAll(node.predicate.apply(this, local));
		}
		
		return all;
	}
	
	@Override
	public TCNameSet caseIotaExpression(TCIotaExpression node, Environment arg)
	{
		TCNameSet all = visitorSet.applyBindVisitor(node.bind, arg);
		
		if (node.predicate != null)
		{
			Environment local = new FlatEnvironment(node.def, arg);
			all.addAll(node.predicate.apply(this, local));
		}
		
		return all;
	}
	
	@Override
	public TCNameSet caseLambdaExpression(TCLambdaExpression node, Environment arg)
	{
		TCNameSet all = newCollection();
		
		for (TCTypeBind bind: node.bindList)
		{
			all.addAll(visitorSet.applyBindVisitor(bind, arg));
		}
		
		Environment local = new FlatEnvironment(node.def, arg);
		all.addAll(node.expression.apply(this, local));
		return all;
	}
	
	@Override
	public TCNameSet caseLetBeStExpression(TCLetBeStExpression node, Environment arg)
	{
		TCNameSet all = visitorSet.applyMultiBindVisitor(node.bind, arg);
		Environment local = new FlatEnvironment(node.def, arg);
		
		if (node.suchThat != null)
		{
			all.addAll(node.suchThat.apply(this, local));
		}
		
		all.addAll(node.value.apply(this, local));
		return all;
	}
	
	@Override
	public TCNameSet caseLetDefExpression(TCLetDefExpression node, Environment arg)
	{
		Environment local = arg;
		TCNameSet names = new TCNameSet();

		for (TCDefinition d: node.localDefs)
		{
			if (d instanceof TCExplicitFunctionDefinition)
			{
				// ignore
			}
			else
			{
				local = new FlatEnvironment(d, local);
				names.addAll(visitorSet.applyDefinitionVisitor(d, local));
			}
		}

		names.addAll(node.expression.apply(this, local));
		return names;
	}
	
	@Override
	public TCNameSet caseMapCompExpression(TCMapCompExpression node, Environment arg)
	{
		TCNameSet all = newCollection();
		
		for (TCMultipleBind mbind: node.bindings)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
		}

		Environment local = new FlatEnvironment(node.def, arg);
		all.addAll(node.first.left.apply(this, local));
		all.addAll(node.first.right.apply(this, local));
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, local));
		
		return all;
	}
	
	@Override
	public TCNameSet caseSeqCompExpression(TCSeqCompExpression node, Environment arg)
	{
		TCNameSet all = newCollection();
		all.addAll(visitorSet.applyBindVisitor(node.bind, arg));

		Environment local = new FlatEnvironment(node.def, arg);
		all.addAll(node.first.apply(this, local));
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, local));

		return all;
	}
	
	@Override
	public TCNameSet caseSetCompExpression(TCSetCompExpression node, Environment arg)
	{
		TCNameSet all = newCollection();
		
		for (TCMultipleBind mbind: node.bindings)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
		}

		Environment local = new FlatEnvironment(node.def, arg);
		all.addAll(node.first.apply(this, local));
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, local));
		
		return all;
	}
	
	@Override
	public TCNameSet caseVariableExpression(TCVariableExpression node, Environment arg)
	{
		TCDefinition d = arg.findName(node.name, NameScope.NAMESANDANYSTATE);
		
		if (d != null && d.isFunction())
		{
			return new TCNameSet();
		}
		
		if (d instanceof TCRenamedDefinition)
		{
			TCRenamedDefinition rd = (TCRenamedDefinition)d;
			
			if (rd.def.name != null)
			{
				return new TCNameSet(rd.def.name.getExplicit(true));
			}
		}
		
		if (d == null)
		{
			return new TCNameSet(node.name.getExplicit(true));
		}
		else
		{
			return new TCNameSet();
		}
	}
}
