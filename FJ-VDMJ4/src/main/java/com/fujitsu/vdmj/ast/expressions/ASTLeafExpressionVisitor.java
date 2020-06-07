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

package com.fujitsu.vdmj.ast.expressions;

import java.util.Collection;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTEqualsDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;
import com.fujitsu.vdmj.ast.patterns.ASTBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleSeqBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleSetBind;
import com.fujitsu.vdmj.ast.patterns.ASTSeqBind;
import com.fujitsu.vdmj.ast.patterns.ASTSetBind;
import com.fujitsu.vdmj.ast.patterns.ASTTypeBind;

/**
 * This TCExpression visitor visits all of the leaves of an expression tree and calls
 * the basic processing methods for the simple expressions.
 */
abstract public class ASTLeafExpressionVisitor<E, C extends Collection<E>, S> extends ASTExpressionVisitor<C, S>
{
 	@Override
	public C caseApplyExpression(ASTApplyExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.root.apply(this, arg));
		
		for (ASTExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseBinaryExpression(ASTBinaryExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseCasesExpression(ASTCasesExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.exp.apply(this, arg));
		
		for (ASTCaseAlternative a: node.cases)
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
	public C caseDefExpression(ASTDefExpression node, S arg)
	{
		C all = newCollection();

		for (ASTDefinition def: node.localDefs)
 		{
 			if (def instanceof ASTEqualsDefinition)
 			{
 				ASTEqualsDefinition edef = (ASTEqualsDefinition)def;
 				all.addAll(edef.test.apply(this, arg));
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElementsExpression(ASTElementsExpression node, S arg)
	{
		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseElseIfExpression(ASTElseIfExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.elseIfExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseExists1Expression(ASTExists1Expression node, S arg)
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
	public C caseExistsExpression(ASTExistsExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTMultipleBind bind: node.bindList)
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
	public C caseFieldExpression(ASTFieldExpression node, S arg)
	{
		return node.object.apply(this, arg);
	}

 	@Override
	public C caseFieldNumberExpression(ASTFieldNumberExpression node, S arg)
	{
 		return node.tuple.apply(this, arg);
	}

 	@Override
	public C caseForAllExpression(ASTForAllExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTMultipleBind bind: node.bindList)
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
	public C caseFuncInstantiationExpression(ASTFuncInstantiationExpression node, S arg)
	{
		return node.function.apply(this, arg);
	}

 	@Override
	public C caseIfExpression(ASTIfExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.ifExp.apply(this, arg));
		all.addAll(node.thenExp.apply(this, arg));
		
		for (ASTElseIfExpression elseif: node.elseList)
		{
			all.addAll(elseif.apply(this, arg));
		}
		
		all.addAll(node.elseExp.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIotaExpression(ASTIotaExpression node, S arg)
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
	public C caseIsExpression(ASTIsExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public C caseIsOfBaseClassExpression(ASTIsOfBaseClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseIsOfClassExpression(ASTIsOfClassExpression node, S arg)
	{
 		return node.exp.apply(this, arg);
	}

 	@Override
	public C caseLambdaExpression(ASTLambdaExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTTypeBind bind: node.bindList)
		{
			all.addAll(caseBind(bind, arg));
		}
		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetBeStExpression(ASTLetBeStExpression node, S arg)
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
	public C caseLetDefExpression(ASTLetDefExpression node, S arg)
	{
		C all = newCollection();

		for (ASTDefinition def: node.localDefs)
 		{
 			if (def instanceof ASTValueDefinition)
 			{
 				ASTValueDefinition vdef = (ASTValueDefinition)def;
 				all.addAll(vdef.exp.apply(this, arg));
 			}
 		}
 		
		all.addAll(node.expression.apply(this, arg));
		return all;
	}

 	@Override
	public C caseMapCompExpression(ASTMapCompExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.first.left.apply(this, arg));
		all.addAll(node.first.right.apply(this, arg));
		
		for (ASTMultipleBind mbind: node.bindings)
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
	public C caseMapEnumExpression(ASTMapEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTMapletExpression maplet: node.members)
		{
			all.addAll(maplet.left.apply(this, arg));
			all.addAll(maplet.right.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMkBasicExpression(ASTMkBasicExpression node, S arg)
	{
		return node.arg.apply(this, arg);
	}

 	@Override
	public C caseMkTypeExpression(ASTMkTypeExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMuExpression(ASTMuExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTRecordModifier modifier: node.modifiers)
		{
			all.addAll(modifier.value.apply(this, arg));
		}
		
		all.addAll(node.record.apply(this, arg));
		return all;
	}

 	@Override
	public C caseNarrowExpression(ASTNarrowExpression node, S arg)
	{
		return node.test.apply(this, arg);
	}

 	@Override
	public C caseNewExpression(ASTNewExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression a: node.args)
		{
			all.addAll(a.apply(this, arg));
		}
		
		return all;
	}
 	
 	@Override
 	public C casePostOpExpression(ASTPostOpExpression node, S arg)
 	{
 		return node.postexpression.apply(this, arg);
 	}
 	
 	@Override
 	public C casePreExpression(ASTPreExpression node, S arg)
 	{
		C all = newCollection();
		all.addAll(node.function.apply(this, arg));
		
		for (ASTExpression exp: node.args)
		{
			all.addAll(exp.apply(this, arg));
		}
		
		return all;
 	}

 	@Override
 	public C casePreOpExpression(ASTPreOpExpression node, S arg)
 	{
 		return node.expression.apply(this, arg);
 	}

 	@Override
	public C caseSameBaseClassExpression(ASTSameBaseClassExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSameClassExpression(ASTSameClassExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.left.apply(this, arg));
		all.addAll(node.right.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSeqCompExpression(ASTSeqCompExpression node, S arg)
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
	public C caseSeqEnumExpression(ASTSeqEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSetCompExpression(ASTSetCompExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.first.apply(this, arg));
		
		for (ASTMultipleBind mbind: node.bindings)
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
	public C caseSetEnumExpression(ASTSetEnumExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression m: node.members)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSetRangeExpression(ASTSetRangeExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.first.apply(this, arg));
		all.addAll(node.last.apply(this, arg));
		return all;
	}

 	@Override
	public C caseSubseqExpression(ASTSubseqExpression node, S arg)
	{
		C all = node.seq.apply(this, arg);
		all.addAll(node.from.apply(this, arg));
		all.addAll(node.to.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTupleExpression(ASTTupleExpression node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression m: node.args)
		{
			all.addAll(m.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseUnaryExpression(ASTUnaryExpression node, S arg)
	{
		C all = newCollection();
		all.addAll(node.exp.apply(this, arg));
		return all;
	}

	private C caseBind(ASTBind bind, S arg)
	{
		C all = newCollection();
		
		if (bind instanceof ASTSetBind)
		{
			ASTSetBind sbind = (ASTSetBind)bind;
			all.addAll(sbind.set.apply(this, arg));
		}
		else if (bind instanceof ASTSeqBind)
		{
			ASTSeqBind sbind = (ASTSeqBind)bind;
			all.addAll(sbind.sequence.apply(this, arg));
		}
		
		return all;
	}

 	private C caseMultipleBind(ASTMultipleBind bind, S arg)
	{
		C all = newCollection();
		
		if (bind instanceof ASTMultipleSetBind)
		{
			ASTMultipleSetBind sbind = (ASTMultipleSetBind)bind;
			all.addAll(sbind.set.apply(this, arg));
		}
		else if (bind instanceof ASTMultipleSeqBind)
		{
			ASTMultipleSeqBind sbind = (ASTMultipleSeqBind)bind;
			all.addAll(sbind.sequence.apply(this, arg));
		}
		
		return all;
	}
	
	abstract protected C newCollection();
}
