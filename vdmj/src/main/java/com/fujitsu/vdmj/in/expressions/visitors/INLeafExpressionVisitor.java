/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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

package com.fujitsu.vdmj.in.expressions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.annotations.INAnnotatedExpression;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INEqualsDefinition;
import com.fujitsu.vdmj.in.definitions.INValueDefinition;
import com.fujitsu.vdmj.in.expressions.INApplyExpression;
import com.fujitsu.vdmj.in.expressions.INBinaryExpression;
import com.fujitsu.vdmj.in.expressions.INCaseAlternative;
import com.fujitsu.vdmj.in.expressions.INCasesExpression;
import com.fujitsu.vdmj.in.expressions.INDefExpression;
import com.fujitsu.vdmj.in.expressions.INElementsExpression;
import com.fujitsu.vdmj.in.expressions.INElseIfExpression;
import com.fujitsu.vdmj.in.expressions.INExists1Expression;
import com.fujitsu.vdmj.in.expressions.INExistsExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INFieldExpression;
import com.fujitsu.vdmj.in.expressions.INFieldNumberExpression;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.expressions.INFuncInstantiationExpression;
import com.fujitsu.vdmj.in.expressions.INIfExpression;
import com.fujitsu.vdmj.in.expressions.INIotaExpression;
import com.fujitsu.vdmj.in.expressions.INIsExpression;
import com.fujitsu.vdmj.in.expressions.INIsOfBaseClassExpression;
import com.fujitsu.vdmj.in.expressions.INIsOfClassExpression;
import com.fujitsu.vdmj.in.expressions.INLambdaExpression;
import com.fujitsu.vdmj.in.expressions.INLetBeStExpression;
import com.fujitsu.vdmj.in.expressions.INLetDefExpression;
import com.fujitsu.vdmj.in.expressions.INMapCompExpression;
import com.fujitsu.vdmj.in.expressions.INMapEnumExpression;
import com.fujitsu.vdmj.in.expressions.INMapletExpression;
import com.fujitsu.vdmj.in.expressions.INMkBasicExpression;
import com.fujitsu.vdmj.in.expressions.INMkTypeExpression;
import com.fujitsu.vdmj.in.expressions.INMuExpression;
import com.fujitsu.vdmj.in.expressions.INNarrowExpression;
import com.fujitsu.vdmj.in.expressions.INNewExpression;
import com.fujitsu.vdmj.in.expressions.INPostOpExpression;
import com.fujitsu.vdmj.in.expressions.INPreExpression;
import com.fujitsu.vdmj.in.expressions.INPreOpExpression;
import com.fujitsu.vdmj.in.expressions.INRecordModifier;
import com.fujitsu.vdmj.in.expressions.INSameBaseClassExpression;
import com.fujitsu.vdmj.in.expressions.INSameClassExpression;
import com.fujitsu.vdmj.in.expressions.INSeqCompExpression;
import com.fujitsu.vdmj.in.expressions.INSeqEnumExpression;
import com.fujitsu.vdmj.in.expressions.INSetCompExpression;
import com.fujitsu.vdmj.in.expressions.INSetEnumExpression;
import com.fujitsu.vdmj.in.expressions.INSetRangeExpression;
import com.fujitsu.vdmj.in.expressions.INSubseqExpression;
import com.fujitsu.vdmj.in.expressions.INTupleExpression;
import com.fujitsu.vdmj.in.expressions.INUnaryExpression;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;

/**
 * This INExpression visitor visits all of the leaves of an expression tree and calls
 * the basic processing methods for the simple expressions.
 */
abstract public class INLeafExpressionVisitor<E, C extends Collection<E>, S> extends INExpressionVisitor<C, S>
{
	protected INVisitorSet<E, C, S> visitorSet = new INVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			expressionVisitor = INLeafExpressionVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return INLeafExpressionVisitor.this.newCollection();
		}
	};

	private final boolean allNodes;
	
	public INLeafExpressionVisitor(boolean allNodes)
	{
		this.allNodes = allNodes;
	}
	
 	@Override
	public C caseApplyExpression(INApplyExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.root.apply(this, arg));
		
		for (INExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public C caseAnnotatedExpression(INAnnotatedExpression node, S arg)
 	{
 		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
 		
 		for (INExpression a: node.annotation.args)
 		{
 			all.addAll(a.apply(this, arg));
 		}
 		
 		all.addAll(node.expression.apply(this, arg));
 		return all;
 	}

 	@Override
	public C caseBinaryExpression(INBinaryExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseCasesExpression(INCasesExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.exp.apply(this, arg));
		
		for (INCaseAlternative a: node.cases)
		{
			all.addAll(a.result.apply(this, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.others, arg));
		return all;
	}

 	@Override
	public C caseDefExpression(INDefExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();

		for (INDefinition def: node.localDefs)
 		{
 			if (def instanceof INEqualsDefinition)
 			{
 				INEqualsDefinition edef = (INEqualsDefinition)def;
 				all.addAll(edef.test.apply(this, arg));
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElementsExpression(INElementsExpression node, S arg)
	{
 		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.exp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElseIfExpression(INElseIfExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.elseIfExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseExists1Expression(INExists1Expression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyBindVisitor(node.bind, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseExistsExpression(INExistsExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INMultipleBind bind: node.bindList)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

	@Override
	public C caseFieldExpression(INFieldExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.object.apply(this, arg));
		return all;
	}

 	@Override
	public C caseFieldNumberExpression(INFieldNumberExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
 		all.addAll(node.tuple.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseForAllExpression(INForAllExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INMultipleBind bind: node.bindList)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseFuncInstantiationExpression(INFuncInstantiationExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.function.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseIfExpression(INIfExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.ifExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		
		for (INElseIfExpression elseif: node.elseList)
		{
			all.addAll(elseif.apply(this, arg));
		}
		
		all.addAll(node.elseExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIotaExpression(INIotaExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyBindVisitor(node.bind, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseIsExpression(INIsExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.test.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseIsOfBaseClassExpression(INIsOfBaseClassExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
 		all.addAll(node.exp.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseIsOfClassExpression(INIsOfClassExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
 		all.addAll(node.exp.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseLambdaExpression(INLambdaExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetBeStExpression(INLetBeStExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyMultiBindVisitor(node.bind, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.suchThat, arg));
		all.addAll(node.value.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetDefExpression(INLetDefExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();

		for (INDefinition def: node.localDefs)
 		{
 			if (def instanceof INValueDefinition)
 			{
 				INValueDefinition vdef = (INValueDefinition)def;
 				all.addAll(visitorSet.applyPatternVisitor(vdef.pattern, arg));
 				if (vdef.type != null) all.addAll(visitorSet.applyTypeVisitor(vdef.type, arg));
 				all.addAll(vdef.exp.apply(this, arg));
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseMapCompExpression(INMapCompExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.first.left.apply(this, arg));
		all.addAll(node.first.right.apply(this, arg));
		
		for (INMultipleBind mbind: node.bindings)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseMapEnumExpression(INMapEnumExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INMapletExpression maplet: node.members)
		{
			all.addAll(maplet.left.apply(this, arg));
			all.addAll(maplet.right.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMkBasicExpression(INMkBasicExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.arg.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseMkTypeExpression(INMkTypeExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMuExpression(INMuExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INRecordModifier modifier: node.modifiers)
		{
			all.addAll(modifier.value.apply(this, arg));
		}
		
		all.addAll(node.record.apply(this, arg));
		return all;
	}

 	@Override
	public C caseNarrowExpression(INNarrowExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.test.apply(this, arg));
 		return all;
	}

 	@Override
	public C caseNewExpression(INNewExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public C casePostOpExpression(INPostOpExpression node, S arg)
 	{
 		return node.postexpression.apply(this, arg);
 	}
 	
 	@Override
 	public C casePreExpression(INPreExpression node, S arg)
 	{
		C all = node.function.apply(this, arg);
		
		for (INExpression exp: node.args)
		{
			all.addAll(exp.apply(this, arg));
		}
		
		return all;
 	}
 	
 	@Override
 	public C casePreOpExpression(INPreOpExpression node, S arg)
 	{
 		return node.expression.apply(this, arg);
 	}

 	@Override
	public C caseSameBaseClassExpression(INSameBaseClassExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSameClassExpression(INSameClassExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSeqCompExpression(INSeqCompExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.first.apply(this, arg));
		all.addAll(visitorSet.applyBindVisitor(node.bind, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseSeqEnumExpression(INSeqEnumExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSetCompExpression(INSetCompExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.first.apply(this, arg));
		
		for (INMultipleBind mbind: node.bindings)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseSetEnumExpression(INSetEnumExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSetRangeExpression(INSetRangeExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.first.apply(this, arg));
		all.addAll(node.last.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSubseqExpression(INSubseqExpression node, S arg)
	{
		C all = node.seq.apply(this, arg);
		all.addAll(node.from.apply(this, arg));
		all.addAll(node.to.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTupleExpression(INTupleExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		
		for (INExpression m: node.args)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseUnaryExpression(INUnaryExpression node, S arg)
	{
		C all = allNodes ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.exp.apply(this, arg));
		return all;
	}

	abstract protected C newCollection();
	
	protected C caseNonLeafNode(INExpression node, S arg)
	{
		throw new RuntimeException("caseNonLeafNode must be overridden if allNodes is set");
	}
}
