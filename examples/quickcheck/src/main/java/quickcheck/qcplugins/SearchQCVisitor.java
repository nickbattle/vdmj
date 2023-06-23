/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.qcplugins;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INGreaterEqualExpression;
import com.fujitsu.vdmj.in.expressions.INGreaterExpression;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INLessEqualExpression;
import com.fujitsu.vdmj.in.expressions.INLessExpression;
import com.fujitsu.vdmj.in.expressions.INNotEqualExpression;
import com.fujitsu.vdmj.in.expressions.INSeqEnumExpression;
import com.fujitsu.vdmj.in.expressions.INSetEnumExpression;
import com.fujitsu.vdmj.in.expressions.INVariableExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INLeafExpressionVisitor;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;

public class SearchQCVisitor extends INLeafExpressionVisitor<NameValuePair, NameValuePairList, Object>
{
	public SearchQCVisitor()
	{
		super(false);
	}

	@Override
	public NameValuePairList caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public NameValuePairList caseNotEqualExpression(INNotEqualExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof INVariableExpression)
		{
			INVariableExpression var = (INVariableExpression)node.left;

			if (node.right instanceof INIntegerLiteralExpression)
			{
				INIntegerLiteralExpression rhs = (INIntegerLiteralExpression)node.right;
				nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT <> rhs
			}
			else if (node.right instanceof INSeqEnumExpression)
			{
				INSeqEnumExpression rhs = (INSeqEnumExpression)node.right;
				
				if (rhs.members.isEmpty())	// empty sequence
				{
					nvpl.add(var.name, new SeqValue());	// ie. rhs is NOT <> rhs
				}
			}
			else if (node.right instanceof INSetEnumExpression)
			{
				INSetEnumExpression rhs = (INSetEnumExpression)node.right;
				
				if (rhs.members.isEmpty())	// empty set
				{
					nvpl.add(var.name, new SetValue());	// ie. rhs is NOT <> rhs
				}
			}
		}
		
		return nvpl;
	}

	@Override
	public NameValuePairList caseGreaterExpression(INGreaterExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof INVariableExpression &&
			node.right instanceof INIntegerLiteralExpression)
		{
			INVariableExpression var = (INVariableExpression)node.left;
			INIntegerLiteralExpression rhs = (INIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT > rhs
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseGreaterEqualExpression(INGreaterEqualExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof INVariableExpression &&
			node.right instanceof INIntegerLiteralExpression)
		{
			INVariableExpression var = (INVariableExpression)node.left;
			INIntegerLiteralExpression rhs = (INIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value - 1));	// ie. rhs-1 is NOT >= rhs
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseLessExpression(INLessExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof INVariableExpression &&
			node.right instanceof INIntegerLiteralExpression)
		{
			INVariableExpression var = (INVariableExpression)node.left;
			INIntegerLiteralExpression rhs = (INIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT < rhs
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseLessEqualExpression(INLessEqualExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof INVariableExpression &&
			node.right instanceof INIntegerLiteralExpression)
		{
			INVariableExpression var = (INVariableExpression)node.left;
			INIntegerLiteralExpression rhs = (INIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value + 1));	// ie. rhs + 1 is NOT >= rhs
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseVariableExpression(INVariableExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		// if boolean, then var.name = false
		return nvpl;
	}

	@Override
	protected NameValuePairList newCollection()
	{
		return new NameValuePairList();
	}
}
