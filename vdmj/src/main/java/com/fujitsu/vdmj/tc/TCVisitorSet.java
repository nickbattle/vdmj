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

package com.fujitsu.vdmj.tc;

import java.util.Collection;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCPatternVisitor;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
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
abstract public class TCVisitorSet<E, C extends Collection<E>, S>
{
	protected TCDefinitionVisitor<C, S> definitionVisitor = null;
	protected TCExpressionVisitor<C, S> expressionVisitor = null;
	protected TCStatementVisitor<C, S> statementVisitor = null;
	protected TCPatternVisitor<C, S> patternVisitor = null;
	protected TCTypeVisitor<C, S> typeVisitor = null;
	protected TCBindVisitor<C, S> bindVisitor = null;
	protected TCMultipleBindVisitor<C, S> multiBindVisitor = null;
	
	protected TCVisitorSet()
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
	
	public TCDefinitionVisitor<C, S> getDefinitionVisitor()
 	{
 		return definitionVisitor;
 	}
	
	public C applyDefinitionVisitor(TCDefinition def, S arg)
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
	
	public TCExpressionVisitor<C, S> getExpressionVisitor()
 	{
 		return expressionVisitor;
 	}
 	
	public C applyExpressionVisitor(TCExpression def, S arg)
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
	
	public TCStatementVisitor<C, S> getStatementVisitor()
 	{
 		return statementVisitor;
 	}

	public C applyStatementVisitor(TCStatement def, S arg)
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
	
	public TCPatternVisitor<C, S> getPatternVisitor()
 	{
 		return patternVisitor;
 	}
 	
	public C applyPatternVisitor(TCPattern def, S arg)
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
	
	public TCTypeVisitor<C, S> getTypeVisitor()
 	{
 		return typeVisitor;
 	}

	public C applyTypeVisitor(TCType def, S arg)
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
	
	public TCBindVisitor<C, S> getBindVisitor()
	{
		return bindVisitor;
	}

	public C applyBindVisitor(TCBind def, S arg)
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
	
	public TCMultipleBindVisitor<C, S> getMultiBindVisitor()
	{
		return multiBindVisitor;
	}
	
	public C applyMultiBindVisitor(TCMultipleBind def, S arg)
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
