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
import java.util.List;
import java.util.Vector;

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
abstract public class POLeafExpressionVisitor<E, S> extends POExpressionVisitor<List<E>, S>
{
 	@Override
	public List<E> caseApplyExpression(POApplyExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.root.apply(this, arg));
		
		for (POExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseBinaryExpression(POBinaryExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseCasesExpression(POCasesExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.exp.apply(this, arg));
		
		for (POCaseAlternative a: node.cases)
		{
			all.addAll(a.cexp.apply(this, arg));
		}
		
		all.addAll(node.others.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseDefExpression(PODefExpression node, S arg)
	{
		List<E> all = new Vector<E>();

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
	public List<E> caseElementsExpression(POElementsExpression node, S arg)
	{
		return node.exp.apply(this, arg);
	}

 	@Override
	public List<E> caseElseIfExpression(POElseIfExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.elseIfExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseExists1Expression(POExists1Expression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(caseBind(node.bind, arg));
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseExistsExpression(POExistsExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POMultipleBind bind: node.bindList)
		{
			all.addAll(caseMultipleBind(bind, arg));
		}
		
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

	@Override
	public List<E> caseFieldExpression(POFieldExpression node, S arg)
	{
		return node.object.apply(this, arg);
	}

 	@Override
	public List<E> caseFieldNumberExpression(POFieldNumberExpression node, S arg)
	{
 		return node.tuple.apply(this, arg);
	}

 	@Override
	public List<E> caseForAllExpression(POForAllExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POMultipleBind bind: node.bindList)
		{
			all.addAll(caseMultipleBind(bind, arg));
		}
		
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseFuncInstantiationExpression(POFuncInstantiationExpression node, S arg)
	{
		return node.function.apply(this, arg);
	}

 	@Override
	public List<E> caseIfExpression(POIfExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.ifExp.apply(this, arg));
		all.addAll(node.elseExp.apply(this, arg));
		
		for (POElseIfExpression elseif: node.elseList)
		{
			all.addAll(elseif.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseIotaExpression(POIotaExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(caseBind(node.bind, arg));
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseIsExpression(POIsExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public List<E> caseIsOfBaseClassExpression(POIsOfBaseClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public List<E> caseIsOfClassExpression(POIsOfClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public List<E> caseLambdaExpression(POLambdaExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POTypeBind bind: node.bindList)
		{
			all.addAll(caseBind(bind, arg));
		}
		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseLetBeStExpression(POLetBeStExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(caseMultipleBind(node.bind, arg));
		all.addAll(node.suchThat.apply(this, arg));
		all.addAll(node.value.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseLetDefExpression(POLetDefExpression node, S arg)
	{
		List<E> all = new Vector<E>();

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
	public List<E> caseMapCompExpression(POMapCompExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.first.left.apply(this, arg));
		all.addAll(node.first.right.apply(this, arg));
		
		for (POMultipleBind mbind: node.bindings)
		{
			all.addAll(caseMultipleBind(mbind, arg));
		}
		
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseMapEnumExpression(POMapEnumExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POMapletExpression maplet: node.members)
		{
			all.addAll(maplet.left.apply(this, arg));
			all.addAll(maplet.right.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseMkBasicExpression(POMkBasicExpression node, S arg)
	{
		return node.arg.apply(this, arg);
	}

 	@Override
	public List<E> caseMkTypeExpression(POMkTypeExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseMuExpression(POMuExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (PORecordModifier modifier: node.modifiers)
		{
			all.addAll(modifier.value.apply(this, arg));
		}
		
		all.addAll(node.record.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseNarrowExpression(PONarrowExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public List<E> caseNewExpression(PONewExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> casePreExpression(POPreExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.function.apply(this, arg));
		
		for (POExpression exp: node.args)
		{
			all.addAll(exp.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseSameBaseClassExpression(POSameBaseClassExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSameClassExpression(POSameClassExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSeqCompExpression(POSeqCompExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.first.apply(this, arg));
		all.addAll(caseBind(node.bind, arg));
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSeqEnumExpression(POSeqEnumExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseSetCompExpression(POSetCompExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.first.apply(this, arg));
		
		for (POMultipleBind mbind: node.bindings)
		{
			all.addAll(caseMultipleBind(mbind, arg));
		}
		
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSetEnumExpression(POSetEnumExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseSetRangeExpression(POSetRangeExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.first.apply(this, arg));
		all.addAll(node.last.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSubseqExpression(POSubseqExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.from.apply(this, arg));
		all.addAll(node.to.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseTupleExpression(POTupleExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (POExpression m: node.args)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseUnaryExpression(POUnaryExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.exp.apply(this, arg));
		return all;
	}

	private List<E> caseBind(POBind bind, S arg)
	{
		List<E> all = new Vector<E>();
		
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
		List<E> all = new Vector<E>();
		
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
}
