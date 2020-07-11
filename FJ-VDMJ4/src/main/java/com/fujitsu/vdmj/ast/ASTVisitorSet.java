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

package com.fujitsu.vdmj.ast;

import java.util.Collection;

import com.fujitsu.vdmj.ast.definitions.visitors.ASTDefinitionVisitor;
import com.fujitsu.vdmj.ast.expressions.visitors.ASTExpressionVisitor;
import com.fujitsu.vdmj.ast.patterns.visitors.ASTBindVisitor;
import com.fujitsu.vdmj.ast.patterns.visitors.ASTMultipleBindVisitor;
import com.fujitsu.vdmj.ast.patterns.visitors.ASTPatternVisitor;
import com.fujitsu.vdmj.ast.statements.visitors.ASTStatementVisitor;
import com.fujitsu.vdmj.ast.types.visitors.ASTTypeVisitor;

/**
 * A collection of visitors to pass between types of Leaf visitor as they process a tree.
 * This abstract class is made concrete and defines visitors of the different types that
 * can be called by the Leaf visitors for this particular application. 
 *
 * @param <E>
 * @param <C>
 * @param <S>
 */
abstract public class ASTVisitorSet<E, C extends Collection<E>, S>
{
	public ASTDefinitionVisitor<C, S> getDefinitionVisitor()
 	{
 		return null;
 	}
	
	public ASTExpressionVisitor<C, S> getExpressionVisitor()
 	{
 		return null;
 	}
 	
	public ASTStatementVisitor<C, S> getStatementVisitor()
 	{
 		return null;
 	}

	public ASTPatternVisitor<C, S> getPatternVisitor()
 	{
 		return null;
 	}
 	
	public ASTTypeVisitor<C, S> getTypeVisitor()
 	{
 		return null;
 	}

	public ASTBindVisitor<C, S> getBindVisitor()
	{
		return null;
	}

	public ASTMultipleBindVisitor<C, S> getMultiBindVisitor()
	{
		return null;
	}
}
