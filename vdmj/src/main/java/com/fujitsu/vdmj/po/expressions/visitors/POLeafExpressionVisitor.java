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

package com.fujitsu.vdmj.po.expressions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.annotations.POAnnotatedExpression;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POEqualsDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POBinaryExpression;
import com.fujitsu.vdmj.po.expressions.POCaseAlternative;
import com.fujitsu.vdmj.po.expressions.POCasesExpression;
import com.fujitsu.vdmj.po.expressions.PODefExpression;
import com.fujitsu.vdmj.po.expressions.POElementsExpression;
import com.fujitsu.vdmj.po.expressions.POElseIfExpression;
import com.fujitsu.vdmj.po.expressions.POExists1Expression;
import com.fujitsu.vdmj.po.expressions.POExistsExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POFieldExpression;
import com.fujitsu.vdmj.po.expressions.POFieldNumberExpression;
import com.fujitsu.vdmj.po.expressions.POForAllExpression;
import com.fujitsu.vdmj.po.expressions.POFuncInstantiationExpression;
import com.fujitsu.vdmj.po.expressions.POIfExpression;
import com.fujitsu.vdmj.po.expressions.POIotaExpression;
import com.fujitsu.vdmj.po.expressions.POIsExpression;
import com.fujitsu.vdmj.po.expressions.POIsOfBaseClassExpression;
import com.fujitsu.vdmj.po.expressions.POIsOfClassExpression;
import com.fujitsu.vdmj.po.expressions.POLambdaExpression;
import com.fujitsu.vdmj.po.expressions.POLetBeStExpression;
import com.fujitsu.vdmj.po.expressions.POLetDefExpression;
import com.fujitsu.vdmj.po.expressions.POMapCompExpression;
import com.fujitsu.vdmj.po.expressions.POMapEnumExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpression;
import com.fujitsu.vdmj.po.expressions.POMkBasicExpression;
import com.fujitsu.vdmj.po.expressions.POMkTypeExpression;
import com.fujitsu.vdmj.po.expressions.POMuExpression;
import com.fujitsu.vdmj.po.expressions.PONarrowExpression;
import com.fujitsu.vdmj.po.expressions.PONewExpression;
import com.fujitsu.vdmj.po.expressions.POPostOpExpression;
import com.fujitsu.vdmj.po.expressions.POPreExpression;
import com.fujitsu.vdmj.po.expressions.POPreOpExpression;
import com.fujitsu.vdmj.po.expressions.PORecordModifier;
import com.fujitsu.vdmj.po.expressions.POSameBaseClassExpression;
import com.fujitsu.vdmj.po.expressions.POSameClassExpression;
import com.fujitsu.vdmj.po.expressions.POSeqCompExpression;
import com.fujitsu.vdmj.po.expressions.POSeqEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetCompExpression;
import com.fujitsu.vdmj.po.expressions.POSetEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetRangeExpression;
import com.fujitsu.vdmj.po.expressions.POSubseqExpression;
import com.fujitsu.vdmj.po.expressions.POTupleExpression;
import com.fujitsu.vdmj.po.expressions.POUnaryExpression;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;

/**
 * This POExpression visitor visits all of the leaves of an expression tree and calls
 * the basic processing methods for the simple expressions.
 */
abstract public class POLeafExpressionVisitor<E, C extends Collection<E>, S> extends POExpressionVisitor<C, S>
{
	protected POVisitorSet<E, C, S> visitorSet = new POVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			expressionVisitor = POLeafExpressionVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return POLeafExpressionVisitor.this.newCollection();
		}
	};

 	@Override
	public C caseApplyExpression(POApplyExpression node, S arg)
	{
		C all = node.root.apply(this, arg);
		
		for (POExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public C caseAnnotatedExpression(POAnnotatedExpression node, S arg)
 	{
 		C all = newCollection();
 		
 		for (POExpression exp: node.annotation.args)
 		{
 			all.addAll(exp.apply(this, arg));
 		}
 		
 		all.addAll(node.expression.apply(this, arg));
 		return all;
 	}

 	@Override
	public C caseBinaryExpression(POBinaryExpression node, S arg)
	{
		C all = node.left.apply(this, arg);
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseCasesExpression(POCasesExpression node, S arg)
	{
		C all = node.exp.apply(this, arg);
		
		for (POCaseAlternative a: node.cases)
		{
			all.addAll(visitorSet.applyPatternVisitor(a.pattern, arg));
			all.addAll(a.result.apply(this, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.others, arg));
		return all;
	}

 	@Override
	public C caseDefExpression(PODefExpression node, S arg)
	{
		C all = newCollection();

		for (PODefinition def: node.localDefs)
 		{
 			if (def instanceof POEqualsDefinition)
 			{
 				POEqualsDefinition edef = (POEqualsDefinition)def;
 				all.addAll(edef.test.apply(this, arg));
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElementsExpression(POElementsExpression node, S arg)
	{
		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseElseIfExpression(POElseIfExpression node, S arg)
	{
		C all = node.elseIfExp.apply(this, arg);
		all.addAll(node.thenExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseExists1Expression(POExists1Expression node, S arg)
	{
		C all = visitorSet.applyBindVisitor(node.bind, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseExistsExpression(POExistsExpression node, S arg)
	{
		C all = newCollection();
		
		for (POMultipleBind bind: node.bindList)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

	@Override
	public C caseFieldExpression(POFieldExpression node, S arg)
	{
		return node.object.apply(this, arg);
	}

 	@Override
	public C caseFieldNumberExpression(POFieldNumberExpression node, S arg)
	{
 		return node.tuple.apply(this, arg);
	}

 	@Override
	public C caseForAllExpression(POForAllExpression node, S arg)
	{
		C all = newCollection();
		
		for (POMultipleBind bind: node.bindList)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseFuncInstantiationExpression(POFuncInstantiationExpression node, S arg)
	{
		return node.function.apply(this, arg);
	}

 	@Override
	public C caseIfExpression(POIfExpression node, S arg)
	{
		C all = node.ifExp.apply(this, arg);
		all.addAll(node.thenExp.apply(this, arg));
		
		for (POElseIfExpression elseif: node.elseList)
		{
			all.addAll(elseif.apply(this, arg));
		}
		
		all.addAll(node.elseExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIotaExpression(POIotaExpression node, S arg)
	{
		C all = visitorSet.applyBindVisitor(node.bind, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseIsExpression(POIsExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public C caseIsOfBaseClassExpression(POIsOfBaseClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseIsOfClassExpression(POIsOfClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseLambdaExpression(POLambdaExpression node, S arg)
	{
		C all = newCollection();
		
		for (POTypeBind bind: node.bindList)
		{
			all.addAll(visitorSet.applyBindVisitor(bind, arg));
		}
		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetBeStExpression(POLetBeStExpression node, S arg)
	{
		C all = visitorSet.applyMultiBindVisitor(node.bind, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.suchThat, arg));
		all.addAll(node.value.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetDefExpression(POLetDefExpression node, S arg)
	{
		C all = newCollection();

		for (PODefinition def: node.localDefs)
 		{
 			if (def instanceof POValueDefinition)
 			{
 				POValueDefinition vdef = (POValueDefinition)def;
 				all.addAll(vdef.exp.apply(this, arg));
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseMapCompExpression(POMapCompExpression node, S arg)
	{
		C all = node.first.left.apply(this, arg);
		all.addAll(node.first.right.apply(this, arg));
		
		for (POMultipleBind mbind: node.bindings)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseMapEnumExpression(POMapEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (POMapletExpression maplet: node.members)
		{
			all.addAll(maplet.left.apply(this, arg));
			all.addAll(maplet.right.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMkBasicExpression(POMkBasicExpression node, S arg)
	{
		return node.arg.apply(this, arg);
	}

 	@Override
	public C caseMkTypeExpression(POMkTypeExpression node, S arg)
	{
		C all = newCollection();
		
		for (POExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMuExpression(POMuExpression node, S arg)
	{
		C all = newCollection();
		
		for (PORecordModifier modifier: node.modifiers)
		{
			all.addAll(modifier.value.apply(this, arg));
		}
		
		all.addAll(node.record.apply(this, arg));
		return all;
	}

 	@Override
	public C caseNarrowExpression(PONarrowExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public C caseNewExpression(PONewExpression node, S arg)
	{
		C all = newCollection();
		
		for (POExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public C casePostOpExpression(POPostOpExpression node, S arg)
 	{
 		return node.postexpression.apply(this, arg);
 	}

 	@Override
	public C casePreExpression(POPreExpression node, S arg)
	{
		C all = node.function.apply(this, arg);
		
		for (POExpression exp: node.args)
		{
			all.addAll(exp.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public C casePreOpExpression(POPreOpExpression node, S arg)
 	{
 		return node.expression.apply(this, arg);
 	}

 	@Override
	public C caseSameBaseClassExpression(POSameBaseClassExpression node, S arg)
	{
		C all = node.left.apply(this, arg);
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSameClassExpression(POSameClassExpression node, S arg)
	{
		C all = node.left.apply(this, arg);
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSeqCompExpression(POSeqCompExpression node, S arg)
	{
		C all = node.first.apply(this, arg);
		all.addAll(visitorSet.applyBindVisitor(node.bind, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseSeqEnumExpression(POSeqEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (POExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSetCompExpression(POSetCompExpression node, S arg)
	{
		C all = node.first.apply(this, arg);
		
		for (POMultipleBind mbind: node.bindings)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.predicate, arg));
		return all;
	}

 	@Override
	public C caseSetEnumExpression(POSetEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (POExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSetRangeExpression(POSetRangeExpression node, S arg)
	{
		C all = node.first.apply(this, arg);
		all.addAll(node.last.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSubseqExpression(POSubseqExpression node, S arg)
	{
		C all = node.from.apply(this, arg);
		all.addAll(node.to.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTupleExpression(POTupleExpression node, S arg)
	{
		C all = newCollection();
		
		for (POExpression m: node.args)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseUnaryExpression(POUnaryExpression node, S arg)
	{
		return node.exp.apply(this, arg);
	}

 	abstract protected C newCollection();
}
