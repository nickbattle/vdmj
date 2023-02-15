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

package discharge.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INLeafExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;

public class TypeBindFinder extends INLeafExpressionVisitor<INMultipleTypeBind, List<INMultipleTypeBind>, Object>
{
	public TypeBindFinder()
	{
		super(false);
		
		visitorSet = new INVisitorSet<INMultipleTypeBind, List<INMultipleTypeBind>, Object>()
		{
			@Override
			protected void setVisitors()
			{
				expressionVisitor = TypeBindFinder.this;
				multiBindVisitor = new MultiTypeBindFinder(this);
				definitionVisitor = new DefinitionTypeBindFinder(this);
			}
			
			@Override
			protected List<INMultipleTypeBind> newCollection()
			{
				return TypeBindFinder.this.newCollection();
			}
		};
	}

	@Override
	protected List<INMultipleTypeBind> newCollection()
	{
		return new Vector<INMultipleTypeBind>();
	}

	@Override
	public List<INMultipleTypeBind> caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}
}
