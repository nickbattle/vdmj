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

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.visitors.ASTDefinitionVisitor;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.visitors.ASTExpressionVisitor;
import com.fujitsu.vdmj.ast.patterns.ASTBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.visitors.ASTBindVisitor;
import com.fujitsu.vdmj.ast.patterns.visitors.ASTMultipleBindVisitor;
import com.fujitsu.vdmj.ast.patterns.visitors.ASTPatternVisitor;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.ast.statements.visitors.ASTStatementVisitor;
import com.fujitsu.vdmj.ast.types.ASTType;
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
	
	/**
	 * This method is responsible for setting all of the visitors required in
	 * the set. It is typically called by the "lead" visitor (eg. the Definition
	 * visitor).
	 */
	abstract protected void setVisitors();
	
	/**
	 * This will usually just call Outer.this.newCollection(), assuming the VisitorSet
	 * is an inner class of the lead visitor. 
	 */
	abstract protected C newCollection();

	/**
	 * The remaining method allow visitors to be retrieved, or more often applied to
	 * members of their type, which checks for a "null" visitor first.
	 */
	
	public ASTDefinitionVisitor<C, S> getDefinitionVisitor()
 	{
 		return definitionVisitor;
 	}
	
	public C applyDefinitionVisitor(ASTDefinition def, S arg)
	{
 		if (definitionVisitor != null)
 		{
 			return def.apply(definitionVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public ASTExpressionVisitor<C, S> getExpressionVisitor()
 	{
 		return expressionVisitor;
 	}
 	
	public C applyExpressionVisitor(ASTExpression def, S arg)
	{
 		if (expressionVisitor != null)
 		{
 			return def.apply(expressionVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public ASTStatementVisitor<C, S> getStatementVisitor()
 	{
 		return statementVisitor;
 	}

	public C applyStatementVisitor(ASTStatement def, S arg)
	{
 		if (statementVisitor != null)
 		{
 			return def.apply(statementVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public ASTPatternVisitor<C, S> getPatternVisitor()
 	{
 		return patternVisitor;
 	}
 	
	public C applyPatternVisitor(ASTPattern def, S arg)
	{
 		if (patternVisitor != null)
 		{
 			return def.apply(patternVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public ASTTypeVisitor<C, S> getTypeVisitor()
 	{
 		return typeVisitor;
 	}

	public C applyTypeVisitor(ASTType def, S arg)
	{
 		if (typeVisitor != null)
 		{
 			return def.apply(typeVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public ASTBindVisitor<C, S> getBindVisitor()
	{
		return bindVisitor;
	}

	public C applyBindVisitor(ASTBind def, S arg)
	{
 		if (bindVisitor != null)
 		{
 			return def.apply(bindVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public ASTMultipleBindVisitor<C, S> getMultiBindVisitor()
	{
		return multiBindVisitor;
	}
	
	public C applyMultiBindVisitor(ASTMultipleBind def, S arg)
	{
 		if (multiBindVisitor != null)
 		{
 			return def.apply(multiBindVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
}
