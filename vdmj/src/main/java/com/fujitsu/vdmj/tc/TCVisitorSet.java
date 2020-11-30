/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc;

import java.util.Collection;

import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCPatternVisitor;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

/**
 * A collection of visitors to pass between types of Leaf visitor as they process a tree.
 * This abstract class is made concrete and defines visitors of the different types that
 * can be called by the Leaf visitors for this particular application. 
 *
 * @param <E>
 * @param <C>
 * @param <S>
 */
abstract public class TCVisitorSet<E, C extends Collection<E>, S>
{
	public TCDefinitionVisitor<C, S> getDefinitionVisitor()
 	{
 		return null;
 	}
	
	public TCExpressionVisitor<C, S> getExpressionVisitor()
 	{
 		return null;
 	}
 	
	public TCStatementVisitor<C, S> getStatementVisitor()
 	{
 		return null;
 	}

	public TCPatternVisitor<C, S> getPatternVisitor()
 	{
 		return null;
 	}
 	
	public TCTypeVisitor<C, S> getTypeVisitor()
 	{
 		return null;
 	}

	public TCBindVisitor<C, S> getBindVisitor()
	{
		return null;
	}

	public TCMultipleBindVisitor<C, S> getMultiBindVisitor()
	{
		return null;
	}
}
