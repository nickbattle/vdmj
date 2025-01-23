/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package quickcheck.visitors;

import java.util.List;

import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCBinaryExpression;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCCaseAlternative;
import com.fujitsu.vdmj.tc.expressions.TCCasesExpression;
import com.fujitsu.vdmj.tc.expressions.TCCharLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCElseIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCExists1Expression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCIotaExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCLambdaExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetBeStExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapletExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkBasicExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCMuExpression;
import com.fujitsu.vdmj.tc.expressions.TCNewExpression;
import com.fujitsu.vdmj.tc.expressions.TCQuoteLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCRealLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCRecordModifier;
import com.fujitsu.vdmj.tc.expressions.TCSeqCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetCompExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubseqExpression;
import com.fujitsu.vdmj.tc.expressions.TCTupleExpression;
import com.fujitsu.vdmj.tc.expressions.TCUnaryExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * Find all the constant sub-expressions in an expression.
 */
public class ConstantExpressionFinder extends TCExpressionVisitor<Boolean, List<TCExpression>>
{
	private final Environment env;

	public ConstantExpressionFinder()
	{
		super();
		env = Interpreter.getInstance().getGlobalEnvironment();
	}
	
	private void allOf(TCExpressionList args, List<TCExpression> clist)
	{
		if (args != null)
		{
			for (TCExpression exp: args)
			{
				exp.apply(this,  clist);
			}
		}
	}
	
	private void ifNotNull(TCExpression exp, List<TCExpression> clist)
	{
		if (exp != null)
		{
			exp.apply(this,  clist);
		}
	}
	
	@Override
	public Boolean caseCasesExpression(TCCasesExpression node, List<TCExpression> arg)
	{
		node.exp.apply(this, arg);
		
		for (TCCaseAlternative alt: node.cases)
		{
			alt.cexp.apply(this, arg);
		}
		
		return false;
	}
	
	@Override
	public Boolean caseIfExpression(TCIfExpression node, List<TCExpression> arg)
	{
		node.ifExp.apply(this, arg);
		node.elseExp.apply(this, arg);
		
		for (TCElseIfExpression elif: node.elseList)
		{
			elif.elseIfExp.apply(this, arg);
		}
		
		return false;
	}
	
	@Override
	public Boolean caseExists1Expression(TCExists1Expression node, List<TCExpression> arg)
	{
		node.predicate.apply(this, arg);
		return false;
	}
	
	@Override
	public Boolean caseExistsExpression(TCExistsExpression node, List<TCExpression> arg)
	{
		node.predicate.apply(this, arg);
		return false;
	}
	
	@Override
	public Boolean caseForAllExpression(TCForAllExpression node, List<TCExpression> arg)
	{
		node.predicate.apply(this, arg);
		return false;
	}
	
	@Override
	public Boolean caseIotaExpression(TCIotaExpression node, List<TCExpression> arg)
	{
		node.predicate.apply(this, arg);
		return false;
	}
	
	@Override
	public Boolean caseIsExpression(TCIsExpression node, List<TCExpression> arg)
	{
		return node.test.apply(this, arg);
	}
	
	@Override
	public Boolean caseLambdaExpression(TCLambdaExpression node, List<TCExpression> arg)
	{
		node.expression.apply(this, arg);
		return false;
	}
	
	@Override
	public Boolean caseLetBeStExpression(TCLetBeStExpression node, List<TCExpression> arg)
	{
		ifNotNull(node.suchThat, arg);
		node.value.apply(this, arg);
		return false;
	}
	
	@Override
	public Boolean caseLetDefExpression(TCLetDefExpression node, List<TCExpression> arg)
	{
		node.expression.apply(this, arg);
		return false;
	}
	
	@Override
	public Boolean caseMapCompExpression(TCMapCompExpression node, List<TCExpression> arg)
	{
		node.first.left.apply(this, arg);
		node.first.right.apply(this, arg);
		node.predicate.apply(this, arg);
		return false;
	}
	
	@Override
	public Boolean caseMapEnumExpression(TCMapEnumExpression node, List<TCExpression> arg)
	{
		boolean constant = true;
		
		for (TCMapletExpression maplet: node.members)
		{
			constant = constant && maplet.left.apply(this, arg);
			constant = constant && maplet.right.apply(this, arg);
		}
		
		return constant;
	}
	
	@Override
	public Boolean caseMkBasicExpression(TCMkBasicExpression node, List<TCExpression> arg)
	{
		if (node.arg.apply(this, arg))
		{
			arg.add(node);
			return true;
		}
		
		return false;
	}
	
	@Override
	public Boolean caseMkTypeExpression(TCMkTypeExpression node, List<TCExpression> arg)
	{
		boolean constant = true;
		
		for (TCExpression field: node.args)
		{
			constant = constant && field.apply(this, arg);
		}
		
		if (constant)
		{
			arg.add(node);
		}
		
		return constant;
	}
	
	@Override
	public Boolean caseMuExpression(TCMuExpression node, List<TCExpression> arg)
	{
		node.record.apply(this, arg);
		
		for (TCRecordModifier mod: node.modifiers)
		{
			mod.value.apply(this, arg);
		}
		
		return false;
	}
	
	@Override
	public Boolean caseNewExpression(TCNewExpression node, List<TCExpression> arg)
	{
		for (TCExpression exp: node.args)
		{
			exp.apply(this, arg);
		}
		
		return false;
	}
	
	@Override
	public Boolean caseSeqCompExpression(TCSeqCompExpression node, List<TCExpression> arg)
	{
		node.first.apply(this, arg);
		node.predicate.apply(this, arg);
		
		return false;
	}
	
	@Override
	public Boolean caseSeqEnumExpression(TCSeqEnumExpression node, List<TCExpression> arg)
	{
		boolean constant = true;
		
		for (TCExpression exp: node.members)
		{
			constant = constant && exp.apply(this, arg);
		}
		
		if (constant)
		{
			arg.add(node);
		}
		
		return constant;
	}
	
	@Override
	public Boolean caseSetCompExpression(TCSetCompExpression node, List<TCExpression> arg)
	{
		node.first.apply(this, arg);
		node.predicate.apply(this, arg);
		
		return false;
	}
	
	@Override
	public Boolean caseSetEnumExpression(TCSetEnumExpression node, List<TCExpression> arg)
	{
		boolean constant = true;
		
		for (TCExpression exp: node.members)
		{
			constant = constant && exp.apply(this, arg);
		}
		
		if (constant)
		{
			arg.add(node);
		}
		
		return constant;
	}
	
	@Override
	public Boolean caseSubseqExpression(TCSubseqExpression node, List<TCExpression> arg)
	{
		node.seq.apply(this, arg);
		node.from.apply(this, arg);
		node.to.apply(this, arg);
		
		return false;
	}
	
	@Override
	public Boolean caseTupleExpression(TCTupleExpression node, List<TCExpression> arg)
	{
		boolean constant = true;
		
		for (TCExpression exp: node.args)
		{
			constant = constant && exp.apply(this, arg);
		}
		
		if (constant)
		{
			arg.add(node);
		}
		
		return constant;
	}
	
	@Override
	public Boolean caseVariableExpression(TCVariableExpression node, List<TCExpression> arg)
	{
		TCDefinition def = env.findName(node.name, NameScope.NAMES);
		
		if (def instanceof TCLocalDefinition)
		{
			TCLocalDefinition vdef = (TCLocalDefinition)def;
			
			if (vdef.valueDefinition != null)
			{
				vdef.valueDefinition.exp.apply(this, arg);
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public Boolean caseFieldExpression(TCFieldExpression node, List<TCExpression> arg)
	{
		node.object.apply(this, arg);
		return false;
	}

	@Override
	public Boolean caseBinaryExpression(TCBinaryExpression node, List<TCExpression> clist)
	{
		boolean left = node.left.apply(this, clist);
		boolean right = node.right.apply(this, clist);
		
		return left && right;
	}
	
	@Override
	public Boolean caseUnaryExpression(TCUnaryExpression node, List<TCExpression> clist)
	{
		return node.exp.apply(this, clist);
	}
	
	@Override
	public Boolean caseApplyExpression(TCApplyExpression node, List<TCExpression> clist)
	{
		allOf(node.args, clist);
		return false;
	}
	
	@Override
	public Boolean caseIntegerLiteralExpression(TCIntegerLiteralExpression node, List<TCExpression> clist)
	{
		clist.add(node);
		return true;
	}
	
	@Override
	public Boolean caseBooleanLiteralExpression(TCBooleanLiteralExpression node, List<TCExpression> clist)
	{
		clist.add(node);
		return true;
	}

	@Override
	public Boolean caseCharLiteralExpression(TCCharLiteralExpression node, List<TCExpression> clist)
	{
		clist.add(node);
		return true;
	}

	@Override
	public Boolean caseRealLiteralExpression(TCRealLiteralExpression node, List<TCExpression> clist)
	{
		clist.add(node);
		return true;
	}
	
	@Override
	public Boolean caseQuoteLiteralExpression(TCQuoteLiteralExpression node, List<TCExpression> clist)
	{
		clist.add(node);
		return true;
	}
	
	@Override
	public Boolean caseStringLiteralExpression(TCStringLiteralExpression node, List<TCExpression> clist)
	{
		clist.add(node);
		return true;
	}
	
	@Override
	public Boolean caseExpression(TCExpression node, List<TCExpression> clist)
	{
		return false;
	}
}
