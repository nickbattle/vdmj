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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.expressions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.annotations.TCAnnotatedExpression;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCEqualsDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCBinaryExpression;
import com.fujitsu.vdmj.tc.expressions.TCCaseAlternative;
import com.fujitsu.vdmj.tc.expressions.TCCasesExpression;
import com.fujitsu.vdmj.tc.expressions.TCDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCElementsExpression;
import com.fujitsu.vdmj.tc.expressions.TCElseIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCExists1Expression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCFieldNumberExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCFuncInstantiationExpression;
import com.fujitsu.vdmj.tc.expressions.TCIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCIotaExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsOfBaseClassExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsOfClassExpression;
import com.fujitsu.vdmj.tc.expressions.TCLambdaExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetBeStExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapletExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkBasicExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCMuExpression;
import com.fujitsu.vdmj.tc.expressions.TCNarrowExpression;
import com.fujitsu.vdmj.tc.expressions.TCNewExpression;
import com.fujitsu.vdmj.tc.expressions.TCPostOpExpression;
import com.fujitsu.vdmj.tc.expressions.TCPreExpression;
import com.fujitsu.vdmj.tc.expressions.TCPreOpExpression;
import com.fujitsu.vdmj.tc.expressions.TCRecordModifier;
import com.fujitsu.vdmj.tc.expressions.TCSameBaseClassExpression;
import com.fujitsu.vdmj.tc.expressions.TCSameClassExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetRangeExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubseqExpression;
import com.fujitsu.vdmj.tc.expressions.TCTupleExpression;
import com.fujitsu.vdmj.tc.expressions.TCUnaryExpression;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;

/**
 * This TCExpression visitor visits all of the leaves of an expression tree and calls
 * the basic processing methods for the simple expressions.
 */
abstract public class TCLeafExpressionVisitor<E, C extends Collection<E>, S> extends TCExpressionVisitor<C, S>
{
	protected TCVisitorSet<E, C, S> visitorSet = new TCVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			expressionVisitor = TCLeafExpressionVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return TCLeafExpressionVisitor.this.newCollection();
		}
	};
	
 	@Override
	public C caseApplyExpression(TCApplyExpression node, S arg)
	{
		C all = node.root.apply(this, arg);
		
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
		C all = node.exp.apply(this, arg);
		
		for (TCCaseAlternative a: node.cases)
		{
			all.addAll(visitorSet.applyPatternVisitor(a.pattern, arg));
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
		C all = visitorSet.applyBindVisitor(node.bind, arg);
		
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
			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
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
			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
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
		C all = visitorSet.applyBindVisitor(node.bind, arg);
		
		if (node.predicate != null)
		{
			all.addAll(node.predicate.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseIsExpression(TCIsExpression node, S arg)
	{
 		C all = newCollection();
 		
 		if (node.typedef != null)
 		{
 			all.addAll(visitorSet.applyDefinitionVisitor(node.typedef, arg));
 		}
 		
		all.addAll(node.test.apply(this, arg));
		return all;
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
			all.addAll(visitorSet.applyBindVisitor(bind, arg));
		}
		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetBeStExpression(TCLetBeStExpression node, S arg)
	{
		C all = visitorSet.applyMultiBindVisitor(node.bind, arg);
		
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
 				all.addAll(vdef.exp.apply(this, arg));
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
			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
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
 	public C casePostOpExpression(TCPostOpExpression node, S arg)
 	{
 		return node.postexpression.apply(this, arg);
 	}
	
 	@Override
 	public C casePreExpression(TCPreExpression node, S arg)
 	{
		C all = node.function.apply(this, arg);
		
		for (TCExpression exp: node.args)
		{
			all.addAll(exp.apply(this, arg));
		}
		
		return all;
 	}

 	@Override
 	public C casePreOpExpression(TCPreOpExpression node, S arg)
 	{
 		return node.expression.apply(this, arg);
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
		all.addAll(visitorSet.applyBindVisitor(node.bind, arg));
		
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
		C all = node.first.apply(this, arg);
		
		for (TCMultipleBind mbind: node.bindings)
		{
			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
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
	
	abstract protected C newCollection();
}
