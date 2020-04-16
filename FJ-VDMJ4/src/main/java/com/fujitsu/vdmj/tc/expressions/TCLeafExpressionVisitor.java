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
abstract public class TCLeafExpressionVisitor<E, C extends Collection<E>, S> extends TCExpressionVisitor<C, S>
{
 	@Override
	public C caseApplyExpression(TCApplyExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.root.apply(this, arg));
		
		for (TCExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public C caseAnnotatedExpression(TCAnnotatedExpression node, S arg)
 	{
 		C all = newCollection();
 		
 		for (TCExpression a: node.annotation.args)
 		{
 			all.addAll(a.apply(this, arg));
 		}
 		
 		all.addAll(node.expression.apply(this, arg));
 		return all;
 	}

 	@Override
	public C caseBinaryExpression(TCBinaryExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseCasesExpression(TCCasesExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.exp.apply(this, arg));
		
		for (TCCaseAlternative a: node.cases)
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
	public C caseDefExpression(TCDefExpression node, S arg)
	{
		C all = newCollection();

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
	public C caseElementsExpression(TCElementsExpression node, S arg)
	{
		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseElseIfExpression(TCElseIfExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.elseIfExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseExists1Expression(TCExists1Expression node, S arg)
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
	public C caseExistsExpression(TCExistsExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCMultipleBind bind: node.bindList)
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
	public C caseFieldExpression(TCFieldExpression node, S arg)
	{
		return node.object.apply(this, arg);
	}

 	@Override
	public C caseFieldNumberExpression(TCFieldNumberExpression node, S arg)
	{
 		return node.tuple.apply(this, arg);
	}

 	@Override
	public C caseForAllExpression(TCForAllExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCMultipleBind bind: node.bindList)
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
	public C caseFuncInstantiationExpression(TCFuncInstantiationExpression node, S arg)
	{
		return node.function.apply(this, arg);
	}

 	@Override
	public C caseIfExpression(TCIfExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.ifExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		
		for (TCElseIfExpression elseif: node.elseList)
		{
			all.addAll(elseif.apply(this, arg));
		}
		
		all.addAll(node.elseExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIotaExpression(TCIotaExpression node, S arg)
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
	public C caseIsExpression(TCIsExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public C caseIsOfBaseClassExpression(TCIsOfBaseClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseIsOfClassExpression(TCIsOfClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseLambdaExpression(TCLambdaExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCTypeBind bind: node.bindList)
		{
			all.addAll(caseBind(bind, arg));
		}
		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetBeStExpression(TCLetBeStExpression node, S arg)
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
	public C caseLetDefExpression(TCLetDefExpression node, S arg)
	{
		C all = newCollection();

		for (TCDefinition def: node.localDefs)
 		{
 			if (def instanceof TCValueDefinition)
 			{
 				TCValueDefinition vdef = (TCValueDefinition)def;
 				all.addAll(vdef.exp.apply(this, arg));	// TODO apply the definition visitor here?
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseMapCompExpression(TCMapCompExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.first.left.apply(this, arg));
		all.addAll(node.first.right.apply(this, arg));
		
		for (TCMultipleBind mbind: node.bindings)
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
	public C caseMapEnumExpression(TCMapEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCMapletExpression maplet: node.members)
		{
			all.addAll(maplet.left.apply(this, arg));
			all.addAll(maplet.right.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMkBasicExpression(TCMkBasicExpression node, S arg)
	{
		return node.arg.apply(this, arg);
	}

 	@Override
	public C caseMkTypeExpression(TCMkTypeExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMuExpression(TCMuExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCRecordModifier modifier: node.modifiers)
		{
			all.addAll(modifier.value.apply(this, arg));
		}
		
		all.addAll(node.record.apply(this, arg));
		return all;
	}

 	@Override
	public C caseNarrowExpression(TCNarrowExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public C caseNewExpression(TCNewExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSameBaseClassExpression(TCSameBaseClassExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSameClassExpression(TCSameClassExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSeqCompExpression(TCSeqCompExpression node, S arg)
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
	public C caseSeqEnumExpression(TCSeqEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSetCompExpression(TCSetCompExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.first.apply(this, arg));
		
		for (TCMultipleBind mbind: node.bindings)
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
	public C caseSetEnumExpression(TCSetEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSetRangeExpression(TCSetRangeExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.first.apply(this, arg));
		all.addAll(node.last.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSubseqExpression(TCSubseqExpression node, S arg)
	{
		C all = node.seq.apply(this, arg);
		all.addAll(node.from.apply(this, arg));
		all.addAll(node.to.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTupleExpression(TCTupleExpression node, S arg)
	{
		C all = newCollection();
		
		for (TCExpression m: node.args)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseUnaryExpression(TCUnaryExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.exp.apply(this, arg));
		return all;
	}

	private C caseBind(TCBind bind, S arg)
	{
		C all = newCollection();
		
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

 	private C caseMultipleBind(TCMultipleBind bind, S arg)
	{
		C all = newCollection();
		
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
	
	abstract protected C newCollection();
}
