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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.expressions;

import java.util.Collection;

import com.fujitsu.vdmj.po.annotations.POAnnotatedExpression;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POEqualsDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.patterns.POBind;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POMultipleSeqBind;
import com.fujitsu.vdmj.po.patterns.POMultipleSetBind;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;

/**
 * This POExpression visitor visits all of the leaves of an expression tree and calls
 * the basic processing methods for the simple expressions.
 */
abstract public class POLeafExpressionVisitor<E, C extends Collection<E>, S> extends POExpressionVisitor<C, S>
{
 	@Override
	public C caseApplyExpression(POApplyExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.root.apply(this, arg));
		
		for (POExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public C caseAnnotatedExpression(POAnnotatedExpression node, S arg)
 	{
 		return node.expression.apply(this, arg);
 	}

 	@Override
	public C caseBinaryExpression(POBinaryExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseCasesExpression(POCasesExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.exp.apply(this, arg));
		
		for (POCaseAlternative a: node.cases)
		{
			all.addAll(a.result.apply(this, arg));
		}
		
		if (node.others != null)
		{
			all.addAll(node.others.apply(this, arg));
		}

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
		C all = newCollection();
		all.addAll(node.elseIfExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseExists1Expression(POExists1Expression node, S arg)
	{
		C all = newCollection();
		all.addAll(caseBind(node.bind, arg));
		
		if (node.predicate != null)
		{
			all.addAll(node.predicate.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseExistsExpression(POExistsExpression node, S arg)
	{
		C all = newCollection();
		
		for (POMultipleBind bind: node.bindList)
		{
			all.addAll(caseMultipleBind(bind, arg));
		}
		
		if (node.predicate != null)
		{
			all.addAll(node.predicate.apply(this, arg));
		}
		
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
			all.addAll(caseMultipleBind(bind, arg));
		}
		
		if (node.predicate != null)
		{
			all.addAll(node.predicate.apply(this, arg));
		}
		
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
		C all = newCollection();
		all.addAll(node.ifExp.apply(this, arg));
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
		C all = newCollection();
		all.addAll(caseBind(node.bind, arg));
		
		if (node.predicate != null)
		{
			all.addAll(node.predicate.apply(this, arg));
		}
		
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
			all.addAll(caseBind(bind, arg));
		}
		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetBeStExpression(POLetBeStExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(caseMultipleBind(node.bind, arg));
		
		if (node.suchThat != null)
		{
			all.addAll(node.suchThat.apply(this, arg));
		}
		
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
		C all = newCollection();
		all.addAll(node.first.left.apply(this, arg));
		all.addAll(node.first.right.apply(this, arg));
		
		for (POMultipleBind mbind: node.bindings)
		{
			all.addAll(caseMultipleBind(mbind, arg));
		}
		
		if (node.predicate != null)
		{
			all.addAll(node.predicate.apply(this, arg));
		}
		
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
		C all = newCollection();
		all.addAll(node.function.apply(this, arg));
		
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
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSameClassExpression(POSameClassExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSeqCompExpression(POSeqCompExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.first.apply(this, arg));
		all.addAll(caseBind(node.bind, arg));
		
		if (node.predicate != null)
		{
			all.addAll(node.predicate.apply(this, arg));
		}
		
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
		C all = newCollection();
		all.addAll(node.first.apply(this, arg));
		
		for (POMultipleBind mbind: node.bindings)
		{
			all.addAll(caseMultipleBind(mbind, arg));
		}
		
		if (node.predicate != null)
		{
			all.addAll(node.predicate.apply(this, arg));
		}
		
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
		C all = newCollection();
		all.addAll(node.first.apply(this, arg));
		all.addAll(node.last.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSubseqExpression(POSubseqExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.from.apply(this, arg));
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
		C all = newCollection();
		all.addAll(node.exp.apply(this, arg));
		return all;
	}

	private C caseBind(POBind bind, S arg)
	{
		C all = newCollection();
		
		if (bind instanceof POSetBind)
		{
			POSetBind sbind = (POSetBind)bind;
			all.addAll(sbind.set.apply(this, arg));
		}
		else if (bind instanceof POSeqBind)
		{
			POSeqBind sbind = (POSeqBind)bind;
			all.addAll(sbind.sequence.apply(this, arg));
		}
		
		return all;
	}

 	private Collection<? extends E> caseMultipleBind(POMultipleBind bind, S arg)
	{
		C all = newCollection();
		
		if (bind instanceof POMultipleSetBind)
		{
			POMultipleSetBind sbind = (POMultipleSetBind)bind;
			all.addAll(sbind.set.apply(this, arg));
		}
		else if (bind instanceof POMultipleSeqBind)
		{
			POMultipleSeqBind sbind = (POMultipleSeqBind)bind;
			all.addAll(sbind.sequence.apply(this, arg));
		}
		
		return all;
	}

 	abstract protected C newCollection();
}
