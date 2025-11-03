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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.expressions.INExistsExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INLeafExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INBindingGlobals;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;

public class ExpressionTypeBindOverrider extends INLeafExpressionVisitor<INBindingOverride, List<INBindingOverride>, Object>
{
	public ExpressionTypeBindOverrider()
	{
		super(false);
		
		visitorSet = new INVisitorSet<INBindingOverride, List<INBindingOverride>, Object>()
		{
			@Override
			protected void setVisitors()
			{
				expressionVisitor = ExpressionTypeBindOverrider.this;
				multiBindVisitor = new MultiTypeBindOverrider(this);
				bindVisitor = new TypeBindOverrider(this);
				definitionVisitor = new DefinitionTypeBindOverrider(this);
			}
			
			@Override
			protected List<INBindingOverride> newCollection()
			{
				return ExpressionTypeBindOverrider.this.newCollection();
			}
		};
	}

	@Override
	protected List<INBindingOverride> newCollection()
	{
		return new Vector<INBindingOverride>();
	}
	
	@Override
	public List<INBindingOverride> caseForAllExpression(INForAllExpression node, Object arg)
	{
		node.globals = INBindingGlobals.getInstance();

		if (node.bindsUsed)
		{
			return super.caseForAllExpression(node, arg);
		}
		else
		{
			return newCollection();
		}
	}
	
	@Override
	public List<INBindingOverride> caseExistsExpression(INExistsExpression node, Object arg)
	{
		node.globals = INBindingGlobals.getInstance();

		if (node.bindsUsed)
		{
			return super.caseExistsExpression(node, arg);
		}
		else
		{
			return newCollection();
		}
	}

	@Override
	public List<INBindingOverride> caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}
}
