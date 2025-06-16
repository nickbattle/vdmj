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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package quickcheck.visitors;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fujitsu.vdmj.tc.expressions.TCAndExpression;
import com.fujitsu.vdmj.tc.expressions.TCEqualsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterExpression;
import com.fujitsu.vdmj.tc.expressions.TCInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCIndicesExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapDomainExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapRangeExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCRealLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.RationalValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;

public class SearchQCVisitor extends TCLeafExpressionVisitor<NameValuePair, NameValuePairList, Object>
{
	private final boolean exists;
	
	public SearchQCVisitor(boolean exists)
	{
		super();
		this.exists = exists;
	}

	@Override
	public NameValuePairList caseExpression(TCExpression node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public NameValuePairList caseEqualsExpression(TCEqualsExpression node, Object arg)
	{
		NameValuePairList nvpl = super.caseEqualsExpression(node, arg);
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				
				if (exists)
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value));
				}
				else
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value.add(BigInteger.ONE)));	// ie. rhs + 1 is NOT = rhs
				}
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					
					if (exists)
					{
						nvpl.add(var.name, new RealValue(rhs.value.value));
					}
					else
					{
						nvpl.add(var.name, new RealValue(rhs.value.value.add(BigDecimal.ONE)));	// ie. rhs + 1 is NOT = rhs
					}
				}
				catch (Exception e)
				{
					// ignore
				}
			}
			else if (node.right instanceof TCSeqEnumExpression)
			{
				TCSeqEnumExpression rhs = (TCSeqEnumExpression)node.right;
				
				if (!rhs.members.isEmpty() || exists)	// not empty sequence, or empty and exists
				{
					nvpl.add(var.name, new SeqValue());	// ie. [] is NOT = rhs
				}
			}
			else if (node.right instanceof TCSetEnumExpression)
			{
				TCSetEnumExpression rhs = (TCSetEnumExpression)node.right;
				
				if (!rhs.members.isEmpty() || exists)	// not empty set, or empty and exists
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
		NameValuePairList nvpl = super.caseNotEqualExpression(node, arg);
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				
				if (exists)
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value.add(BigInteger.ONE)));
				}
				else
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT <> rhs
				}
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					
					if (exists)
					{
						nvpl.add(var.name, new RealValue(rhs.value.value.add(BigDecimal.ONE)));
					}
					else
					{
						nvpl.add(var.name, new RealValue(rhs.value.value));	// ie. rhs is NOT <> rhs
					}
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
		NameValuePairList nvpl = super.caseGreaterExpression(node, arg);
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				
				if (exists)
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value.add(BigInteger.ONE)));
				}
				else
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT > rhs
				}
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					
					if (exists)
					{
						nvpl.add(var.name, new RealValue(rhs.value.value.add(BigDecimal.ONE)));
					}
					else
					{
						nvpl.add(var.name, new RealValue(rhs.value.value));	// ie. rhs is NOT > rhs
					}
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
		NameValuePairList nvpl = super.caseGreaterEqualExpression(node, arg);
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				
				if (exists)
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value));
				}
				else
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value.subtract(BigInteger.ONE)));	// ie. rhs-1 is NOT >= rhs
				}
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					
					if (exists)
					{
						nvpl.add(var.name, new RealValue(rhs.value.value));
					}
					else
					{
						nvpl.add(var.name, new RealValue(rhs.value.value.subtract(BigDecimal.ONE)));	// ie. rhs-1 is NOT >= rhs
					}
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
		NameValuePairList nvpl = super.caseLessExpression(node, arg);
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				
				if (exists)
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value.subtract(BigInteger.ONE)));
				}
				else
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value));	// ie. rhs is NOT < rhs
				}
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					
					if (exists)
					{
						nvpl.add(var.name, new RealValue(rhs.value.value.subtract(BigDecimal.ONE)));
					}
					else
					{
						nvpl.add(var.name, new RealValue(rhs.value.value));	// ie. rhs is NOT < rhs
					}
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
		NameValuePairList nvpl = super.caseLessEqualExpression(node, arg);
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;

			if (node.right instanceof TCIntegerLiteralExpression)
			{
				TCIntegerLiteralExpression rhs = (TCIntegerLiteralExpression)node.right;
				
				if (exists)
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value));
				}
				else
				{
					nvpl.add(var.name, new IntegerValue(rhs.value.value.add(BigInteger.ONE)));	// ie. rhs+1 is NOT <= rhs
				}
			}
			else if (node.right instanceof TCRealLiteralExpression)
			{
				try
				{
					TCRealLiteralExpression rhs = (TCRealLiteralExpression)node.right;
					
					if (exists)
					{
						nvpl.add(var.name, new RealValue(rhs.value.value));
					}
					else
					{
						nvpl.add(var.name, new RealValue(rhs.value.value.add(BigDecimal.ONE)));	// ie. rhs+1 is NOT <= rhs
					}
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
		NameValuePairList nvpl = super.caseIsExpression(node, arg);
		
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
	
	@Override
	public NameValuePairList caseInSetExpression(TCInSetExpression node, Object arg)
	{
		NameValuePairList nvpl = super.caseInSetExpression(node, arg);
		
		if (node.right instanceof TCIndicesExpression)
		{
			if (node.left instanceof TCVariableExpression)
			{
				TCVariableExpression var = (TCVariableExpression)node.left;
				TCType vartype = var.getType();

				if (vartype instanceof TCNumericType)
				{
					TCNumericType numtype = (TCNumericType)vartype;
					BigInteger index = (exists ? BigInteger.ONE : BigInteger.ZERO);

					try
					{
						switch (numtype.getWeight())
						{
							case 0:	// nat1, so can't set to zero
								break;
								
							case 1:	// nat
								nvpl.add(var.name, new NaturalValue(index));
								break;
								
							case 2:	// int
								nvpl.add(var.name, new IntegerValue(index));
								break;
								
							case 3:	// rat
								nvpl.add(var.name, new RationalValue(index));
								break;
								
							case 4:	// real
								nvpl.add(var.name, new RealValue(index));
								break;
								
							default:	// No idea!
								break;
						}
					}
					catch (Exception e)
					{
						// Can't happen
					}
				}
			}
		}
		else if (node.right instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.right;
			TCType vartype = var.getType();
			
			if (!(vartype instanceof TCSet1Type))
			{
				nvpl.add(var.name, new SeqValue());
			}
		}
		else if (node.right instanceof TCMapDomainExpression)
		{
			TCMapDomainExpression dom = (TCMapDomainExpression)node.right;
			
			if (dom.exp instanceof TCVariableExpression)
			{
				TCVariableExpression mapvar = (TCVariableExpression)dom.exp;
				nvpl.add(mapvar.name, new MapValue());
			}
		}
		else if (node.right instanceof TCMapRangeExpression)
		{
			TCMapRangeExpression rng = (TCMapRangeExpression)node.right;
			
			if (rng.exp instanceof TCVariableExpression)
			{
				TCVariableExpression mapvar = (TCVariableExpression)rng.exp;
				nvpl.add(mapvar.name, new MapValue());
			}
		}
		
		return nvpl;
	}
	
	@Override
	public NameValuePairList caseSubsetExpression(TCSubsetExpression node, Object arg)
	{
		NameValuePairList nvpl = super.caseSubsetExpression(node, arg);
		
		if (node.right instanceof TCMapDomainExpression)
		{
			TCMapDomainExpression dom = (TCMapDomainExpression)node.right;
			
			if (dom.exp instanceof TCVariableExpression)
			{
				TCVariableExpression mapvar = (TCVariableExpression)dom.exp;
				nvpl.add(mapvar.name, new MapValue());
			}
		}
		else if (node.right instanceof TCMapRangeExpression)
		{
			TCMapRangeExpression rng = (TCMapRangeExpression)node.right;
			
			if (rng.exp instanceof TCVariableExpression)
			{
				TCVariableExpression mapvar = (TCVariableExpression)rng.exp;
				nvpl.add(mapvar.name, new MapValue());
			}
		}
		
		return nvpl;
	}
	
	/**
	 * Just try everything over LHS/RHS
	 */
	@Override
	public NameValuePairList caseAndExpression(TCAndExpression node, Object arg)
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
