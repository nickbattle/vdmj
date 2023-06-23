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

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;

public class SearchQCVisitor extends TCLeafExpressionVisitor<NameValuePair, NameValuePairList, Object>
{
	public SearchQCVisitor()
	{
		super();
	}

	@Override
	public NameValuePairList caseExpression(TCExpression node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public NameValuePairList caseNotEqualExpression(TCNotEqualExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT <> rhs
			}
			else if (node.right instanceof TCSeqEnumExpression)
			{
				TCSeqEnumExpression rhs = (TCSeqEnumExpression)node.right;
				
				if (rhs.members.isEmpty())	// empty sequence
				{
					nvpl.add(var.name, new SeqValue());	// ie. rhs is NOT <> rhs
				}
			}
			else if (node.right instanceof TCSetEnumExpression)
			{
				TCSetEnumExpression rhs = (TCSetEnumExpression)node.right;
				
				if (rhs.members.isEmpty())	// empty set
				{
					nvpl.add(var.name, new SetValue());	// ie. rhs is NOT <> rhs
				}
			}
		}
		
		return nvpl;
	}

	@Override
	public NameValuePairList caseGreaterExpression(TCGreaterExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression &&
			node.right instanceof TCIntegerLiteralExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT > rhs
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseGreaterEqualExpression(TCGreaterEqualExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression &&
			node.right instanceof TCIntegerLiteralExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value - 1));	// ie. rhs-1 is NOT >= rhs
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseLessExpression(TCLessExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression &&
			node.right instanceof TCIntegerLiteralExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT < rhs
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseLessEqualExpression(TCLessEqualExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression &&
			node.right instanceof TCIntegerLiteralExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value + 1));	// ie. rhs + 1 is NOT >= rhs
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseVariableExpression(TCVariableExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.getType() instanceof TCBooleanType)
		{
			nvpl.add(node.name, new BooleanValue(false));
		}
		
		return nvpl;
	}

	@Override
	protected NameValuePairList newCollection()
	{
		return new NameValuePairList();
	}
}
