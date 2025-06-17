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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in;

import java.util.Collection;

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INBind;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.visitors.INBindVisitor;
import com.fujitsu.vdmj.in.patterns.visitors.INMultipleBindVisitor;
import com.fujitsu.vdmj.in.patterns.visitors.INPatternVisitor;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

/**
 * A collection of visitors to pass between types of Leaf visitor as they process a tree.
 * This abstract class is made concrete and defines visitors of the different types that
 * can be called by the Leaf visitors for this particular application. 
 *
 * @param <E> - an element of the collection result
 * @param <C> - the collection result
 * @param <S> - the argument type.
 */
abstract public class INVisitorSet<E, C extends Collection<E>, S>
{
	protected INDefinitionVisitor<C, S> definitionVisitor = null;
	protected INExpressionVisitor<C, S> expressionVisitor = null;
	protected INStatementVisitor<C, S> statementVisitor = null;
	protected INPatternVisitor<C, S> patternVisitor = null;
	protected TCTypeVisitor<C, S> typeVisitor = null;		// NOTE! This is the TC visitor
	protected INBindVisitor<C, S> bindVisitor = null;
	protected INMultipleBindVisitor<C, S> multiBindVisitor = null;
	
	protected INVisitorSet()
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
	
	public INDefinitionVisitor<C, S> getDefinitionVisitor()
 	{
 		return definitionVisitor;
 	}
	
	public C applyDefinitionVisitor(INDefinition def, S arg)
	{
 		if (definitionVisitor != null && def != null)
 		{
 			return def.apply(definitionVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public INExpressionVisitor<C, S> getExpressionVisitor()
 	{
 		return expressionVisitor;
 	}
 	
	public C applyExpressionVisitor(INExpression def, S arg)
	{
 		if (expressionVisitor != null && def != null)
 		{
 			return def.apply(expressionVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public INStatementVisitor<C, S> getStatementVisitor()
 	{
 		return statementVisitor;
 	}

	public C applyStatementVisitor(INStatement def, S arg)
	{
 		if (statementVisitor != null && def != null)
 		{
 			return def.apply(statementVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public INPatternVisitor<C, S> getPatternVisitor()
 	{
 		return patternVisitor;
 	}
 	
	public C applyPatternVisitor(INPattern def, S arg)
	{
 		if (patternVisitor != null && def != null)
 		{
 			return def.apply(patternVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public TCTypeVisitor<C, S> getTypeVisitor()
 	{
 		return typeVisitor;
 	}

	public C applyTypeVisitor(TCType def, S arg)
	{
 		if (typeVisitor != null && def != null)
 		{
 			return def.apply(typeVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public INBindVisitor<C, S> getBindVisitor()
	{
		return bindVisitor;
	}

	public C applyBindVisitor(INBind def, S arg)
	{
 		if (bindVisitor != null && def != null)
 		{
 			return def.apply(bindVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
	
	public INMultipleBindVisitor<C, S> getMultiBindVisitor()
	{
		return multiBindVisitor;
	}
	
	public C applyMultiBindVisitor(INMultipleBind def, S arg)
	{
 		if (multiBindVisitor != null && def != null)
 		{
 			return def.apply(multiBindVisitor, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}
}
