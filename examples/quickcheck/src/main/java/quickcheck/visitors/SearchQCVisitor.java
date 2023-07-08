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

package quickcheck.visitors;

import com.fujitsu.vdmj.tc.expressions.TCBooleanBinaryExpression;
import com.fujitsu.vdmj.tc.expressions.TCEqualsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCRealLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;

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
	public NameValuePairList caseEqualsExpression(TCEqualsExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				nvpl.add(var.name, new IntegerValue(rhs.value.value + 1));	// ie. rhs + 1 is NOT = rhs
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					nvpl.add(var.name, new RealValue(rhs.value.value + 1));	// ie. rhs + 1 is NOT = rhs
				}
				catch (Exception e)
				{
					// ignore
				}
			}
			else if (node.right instanceof TCSeqEnumExpression)
			{
				TCSeqEnumExpression rhs = (TCSeqEnumExpression)node.right;
				
				if (!rhs.members.isEmpty())	// not empty sequence
				{
					nvpl.add(var.name, new SeqValue());	// ie. [] is NOT = rhs
				}
			}
			else if (node.right instanceof TCSetEnumExpression)
			{
				TCSetEnumExpression rhs = (TCSetEnumExpression)node.right;
				
				if (!rhs.members.isEmpty())	// not empty set
				{
					nvpl.add(var.name, new SetValue());	// ie. {} is NOT = rhs
				}
			}
		}
		
		return nvpl;
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
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					nvpl.add(var.name, new RealValue(rhs.value.value));	// ie. rhs is NOT <> rhs
				}
				catch (Exception e)
				{
					// ignore
				}
			}
			else if (node.right instanceof TCSeqEnumExpression)
			{
				TCSeqEnumExpression rhs = (TCSeqEnumExpression)node.right;
				
				if (rhs.members.isEmpty())	// empty sequence
				{
					nvpl.add(var.name, new SeqValue());	// ie. [] is NOT <> rhs
				}
			}
			else if (node.right instanceof TCSetEnumExpression)
			{
				TCSetEnumExpression rhs = (TCSetEnumExpression)node.right;
				
				if (rhs.members.isEmpty())	// empty set
				{
					nvpl.add(var.name, new SetValue());	// ie. {} is NOT <> rhs
				}
			}
		}
		
		return nvpl;
	}

	@Override
	public NameValuePairList caseGreaterExpression(TCGreaterExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT > rhs
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					nvpl.add(var.name, new RealValue(rhs.value.value));	// ie. rhs is NOT > rhs
				}
				catch (Exception e)
				{
					// ignore
				}
			}
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseGreaterEqualExpression(TCGreaterEqualExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				nvpl.add(var.name, new IntegerValue(rhs.value.value - 1));	// ie. rhs-1 is NOT >= rhs
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					nvpl.add(var.name, new RealValue(rhs.value.value - 1));	// ie. rhs-1 is NOT >= rhs
				}
				catch (Exception e)
				{
					// ignore
				}
			}
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseLessExpression(TCLessExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT < rhs
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					nvpl.add(var.name, new RealValue(rhs.value.value));	// ie. rhs is NOT < rhs
				}
				catch (Exception e)
				{
					// ignore
				}
			}
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseLessEqualExpression(TCLessEqualExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				nvpl.add(var.name, new IntegerValue(rhs.value.value + 1));	// ie. rhs+1 is NOT <= rhs
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					nvpl.add(var.name, new RealValue(rhs.value.value + 1));	// ie. rhs+1 is NOT <= rhs
				}
				catch (Exception e)
				{
					// ignore
				}
			}
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseIsExpression(TCIsExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.test instanceof TCVariableExpression &&
			node.basictype != null)
		{
			try
			{
				TCVariableExpression var = (TCVariableExpression)node.test;
				TCType vType = var.getType();
				TCType isType = node.basictype;
				Value toTry = null;
				
				if (isType instanceof TCNaturalOneType)
				{
					if (vType instanceof TCNaturalType ||
						vType instanceof TCIntegerType ||
						vType instanceof TCRealType)
					{
						toTry = new NaturalValue(0);
					}
				}
				else if (isType instanceof TCNaturalType)
				{
					if (vType instanceof TCIntegerType ||
						vType instanceof TCRealType)
					{
						toTry = new IntegerValue(-1);
					}
				}
				else if (isType instanceof TCIntegerType)
				{
					if (vType instanceof TCRealType)
					{
						toTry = new RealValue(1.23);
					}
				}
				
				if (toTry != null)
				{
					nvpl.add(var.name, toTry);
				}
			}
			catch (Exception e)
			{
				// can't happen
			}
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
	
	/**
	 * Just try everything over LHS/RHS
	 */
	@Override
	public NameValuePairList caseBooleanBinaryExpression(TCBooleanBinaryExpression node, Object arg)
	{
		NameValuePairList nvpl = node.left.apply(this, arg);
		nvpl.addAll(node.right.apply(this, arg));
		return nvpl;
	}

	@Override
	protected NameValuePairList newCollection()
	{
		return new NameValuePairList();
	}
}
