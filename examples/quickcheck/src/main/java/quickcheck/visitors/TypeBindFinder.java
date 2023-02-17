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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INLeafExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;

public class TypeBindFinder extends INLeafExpressionVisitor<INBindingSetter, List<INBindingSetter>, Object>
{
	public TypeBindFinder()
	{
		super(false);
		
		visitorSet = new INVisitorSet<INBindingSetter, List<INBindingSetter>, Object>()
		{
			@Override
			protected void setVisitors()
			{
				expressionVisitor = TypeBindFinder.this;
				multiBindVisitor = new MultiTypeBindFinder(this);
				bindVisitor = new SingleTypeBindFinder(this);
				definitionVisitor = new DefinitionTypeBindFinder(this);
			}
			
			@Override
			protected List<INBindingSetter> newCollection()
			{
				return TypeBindFinder.this.newCollection();
			}
		};
	}

	@Override
	protected List<INBindingSetter> newCollection()
	{
		return new Vector<INBindingSetter>();
	}

	@Override
	public List<INBindingSetter> caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}
}
