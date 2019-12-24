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

package com.fujitsu.vdmj.tc.expressions;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.annotations.TCAnnotatedExpression;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCEqualsDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;

/**
 * This TCExpression visitor visits all of the leaves of an expression tree and calls
 * the basic processing methods for the simple expressions.
 */
abstract public class TCLeafExpressionVisitor<E, S> extends TCExpressionVisitor<List<E>, S>
{
 	@Override
	public List<E> caseApplyExpression(TCApplyExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.root.apply(this, arg));
		
		for (TCExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public List<E> caseAnnotatedExpression(TCAnnotatedExpression node, S arg)
 	{
 		return node.expression.apply(this, arg);
 	}

 	@Override
	public List<E> caseBinaryExpression(TCBinaryExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseCasesExpression(TCCasesExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.exp.apply(this, arg));
		
		for (TCCaseAlternative a: node.cases)
		{
			all.addAll(a.cexp.apply(this, arg));
		}
		
		all.addAll(node.others.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseDefExpression(TCDefExpression node, S arg)
	{
		List<E> all = new Vector<E>();

		for (TCDefinition def: node.localDefs)
 		{
 			if (def instanceof TCEqualsDefinition)
 			{
 				TCEqualsDefinition edef = (TCEqualsDefinition)def;
 				all.addAll(edef.test.apply(this, arg));
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseElementsExpression(TCElementsExpression node, S arg)
	{
		return node.exp.apply(this, arg);
	}

 	@Override
	public List<E> caseElseIfExpression(TCElseIfExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.elseIfExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseExists1Expression(TCExists1Expression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(caseBind(node.bind, arg));
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseExistsExpression(TCExistsExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCMultipleBind bind: node.bindList)
		{
			all.addAll(caseMultipleBind(bind, arg));
		}
		
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

	@Override
	public List<E> caseFieldExpression(TCFieldExpression node, S arg)
	{
		return node.object.apply(this, arg);
	}

 	@Override
	public List<E> caseFieldNumberExpression(TCFieldNumberExpression node, S arg)
	{
 		return node.tuple.apply(this, arg);
	}

 	@Override
	public List<E> caseForAllExpression(TCForAllExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCMultipleBind bind: node.bindList)
		{
			all.addAll(caseMultipleBind(bind, arg));
		}
		
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseFuncInstantiationExpression(TCFuncInstantiationExpression node, S arg)
	{
		return node.function.apply(this, arg);
	}

 	@Override
	public List<E> caseIfExpression(TCIfExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.ifExp.apply(this, arg));
		all.addAll(node.elseExp.apply(this, arg));
		
		for (TCElseIfExpression elseif: node.elseList)
		{
			all.addAll(elseif.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseIotaExpression(TCIotaExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(caseBind(node.bind, arg));
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseIsExpression(TCIsExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public List<E> caseIsOfBaseClassExpression(TCIsOfBaseClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public List<E> caseIsOfClassExpression(TCIsOfClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public List<E> caseLambdaExpression(TCLambdaExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCTypeBind bind: node.bindList)
		{
			all.addAll(caseBind(bind, arg));
		}
		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseLetBeStExpression(TCLetBeStExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(caseMultipleBind(node.bind, arg));
		all.addAll(node.suchThat.apply(this, arg));
		all.addAll(node.value.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseLetDefExpression(TCLetDefExpression node, S arg)
	{
		List<E> all = new Vector<E>();

		for (TCDefinition def: node.localDefs)
 		{
 			if (def instanceof TCValueDefinition)
 			{
 				TCValueDefinition vdef = (TCValueDefinition)def;
 				all.addAll(vdef.exp.apply(this, arg));
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseMapCompExpression(TCMapCompExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.first.left.apply(this, arg));
		all.addAll(node.first.right.apply(this, arg));
		
		for (TCMultipleBind mbind: node.bindings)
		{
			all.addAll(caseMultipleBind(mbind, arg));
		}
		
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseMapEnumExpression(TCMapEnumExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCMapletExpression maplet: node.members)
		{
			all.addAll(maplet.left.apply(this, arg));
			all.addAll(maplet.right.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseMkBasicExpression(TCMkBasicExpression node, S arg)
	{
		return node.arg.apply(this, arg);
	}

 	@Override
	public List<E> caseMkTypeExpression(TCMkTypeExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseMuExpression(TCMuExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCRecordModifier modifier: node.modifiers)
		{
			all.addAll(modifier.value.apply(this, arg));
		}
		
		all.addAll(node.record.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseNarrowExpression(TCNarrowExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public List<E> caseNewExpression(TCNewExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> casePreExpression(TCPreExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.function.apply(this, arg));
		
		for (TCExpression exp: node.args)
		{
			all.addAll(exp.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseSameBaseClassExpression(TCSameBaseClassExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSameClassExpression(TCSameClassExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSeqCompExpression(TCSeqCompExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.first.apply(this, arg));
		all.addAll(caseBind(node.bind, arg));
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSeqEnumExpression(TCSeqEnumExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseSetCompExpression(TCSetCompExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.first.apply(this, arg));
		
		for (TCMultipleBind mbind: node.bindings)
		{
			all.addAll(caseMultipleBind(mbind, arg));
		}
		
		all.addAll(node.predicate.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSetEnumExpression(TCSetEnumExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseSetRangeExpression(TCSetRangeExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.first.apply(this, arg));
		all.addAll(node.last.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseSubseqExpression(TCSubseqExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.from.apply(this, arg));
		all.addAll(node.to.apply(this, arg));
		return all;
	}

 	@Override
	public List<E> caseTupleExpression(TCTupleExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		
		for (TCExpression m: node.args)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public List<E> caseUnaryExpression(TCUnaryExpression node, S arg)
	{
		List<E> all = new Vector<E>();
		all.addAll(node.exp.apply(this, arg));
		return all;
	}

	private List<E> caseBind(TCBind bind, S arg)
	{
		List<E> all = new Vector<E>();
		
		if (bind instanceof TCSetBind)
		{
			TCSetBind sbind = (TCSetBind)bind;
			all.addAll(sbind.set.apply(this, arg));
		}
		else if (bind instanceof TCSeqBind)
		{
			TCSeqBind sbind = (TCSeqBind)bind;
			all.addAll(sbind.sequence.apply(this, arg));
		}
		
		return all;
	}

 	private Collection<? extends E> caseMultipleBind(TCMultipleBind bind, S arg)
	{
		List<E> all = new Vector<E>();
		
		if (bind instanceof TCMultipleSetBind)
		{
			TCMultipleSetBind sbind = (TCMultipleSetBind)bind;
			all.addAll(sbind.set.apply(this, arg));
		}
		else if (bind instanceof TCMultipleSeqBind)
		{
			TCMultipleSeqBind sbind = (TCMultipleSeqBind)bind;
			all.addAll(sbind.sequence.apply(this, arg));
		}
		
		return all;
	}
}
