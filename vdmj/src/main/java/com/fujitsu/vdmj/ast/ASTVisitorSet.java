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
 *	SPDX-License-Identifier: GPL-3.0-or-later
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
 * @param <E> - an element of the collection result
 * @param <C> - the collection result
 * @param <S> - the argument type.
 */
abstract public class ASTVisitorSet<E, C extends Collection<E>, S>
{
	protected ASTDefinitionVisitor<C, S> definitionVisitor = null;
	protected ASTExpressionVisitor<C, S> expressionVisitor = null;
	protected ASTStatementVisitor<C, S> statementVisitor = null;
	protected ASTPatternVisitor<C, S> patternVisitor = null;
	protected ASTTypeVisitor<C, S> typeVisitor = null;
	protected ASTBindVisitor<C, S> bindVisitor = null;
	protected ASTMultipleBindVisitor<C, S> multiBindVisitor = null;
	
	protected ASTVisitorSet()
	{
		setVisitors();	// Calls override version in Java :-)
	}
	
	abstract protected void setVisitors();

	public ASTDefinitionVisitor<C, S> getDefinitionVisitor()
 	{
 		return definitionVisitor;
 	}
	
	public ASTExpressionVisitor<C, S> getExpressionVisitor()
 	{
 		return expressionVisitor;
 	}
 	
	public ASTStatementVisitor<C, S> getStatementVisitor()
 	{
 		return statementVisitor;
 	}

	public ASTPatternVisitor<C, S> getPatternVisitor()
 	{
 		return patternVisitor;
 	}
 	
	public ASTTypeVisitor<C, S> getTypeVisitor()
 	{
 		return typeVisitor;
 	}

	public ASTBindVisitor<C, S> getBindVisitor()
	{
		return bindVisitor;
	}

	public ASTMultipleBindVisitor<C, S> getMultiBindVisitor()
	{
		return multiBindVisitor;
	}
}
