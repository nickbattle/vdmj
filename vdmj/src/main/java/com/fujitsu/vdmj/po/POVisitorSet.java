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

package com.fujitsu.vdmj.po;

import java.util.Collection;

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.POBind;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.visitors.POBindVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POMultipleBindVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POPatternVisitor;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
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
abstract public class POVisitorSet<E, C extends Collection<E>, S>
{
	protected PODefinitionVisitor<C, S> definitionVisitor = null;
	protected POExpressionVisitor<C, S> expressionVisitor = null;
	protected POStatementVisitor<C, S> statementVisitor = null;
	protected POPatternVisitor<C, S> patternVisitor = null;
	protected TCTypeVisitor<C, S> typeVisitor = null;		// NOTE uses TC visitor
	protected POBindVisitor<C, S> bindVisitor = null;
	protected POMultipleBindVisitor<C, S> multiBindVisitor = null;
	
	protected POVisitorSet()
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
	
	public PODefinitionVisitor<C, S> getDefinitionVisitor()
 	{
 		return definitionVisitor;
 	}
	
	public C applyDefinitionVisitor(PODefinition def, S arg)
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
	
	public POExpressionVisitor<C, S> getExpressionVisitor()
 	{
 		return expressionVisitor;
 	}
 	
	public C applyExpressionVisitor(POExpression def, S arg)
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
	
	public POStatementVisitor<C, S> getStatementVisitor()
 	{
 		return statementVisitor;
 	}

	public C applyStatementVisitor(POStatement def, S arg)
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
	
	public POPatternVisitor<C, S> getPatternVisitor()
 	{
 		return patternVisitor;
 	}
 	
	public C applyPatternVisitor(POPattern def, S arg)
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
	
	public POBindVisitor<C, S> getBindVisitor()
	{
		return bindVisitor;
	}

	public C applyBindVisitor(POBind def, S arg)
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
	
	public POMultipleBindVisitor<C, S> getMultiBindVisitor()
	{
		return multiBindVisitor;
	}
	
	public C applyMultiBindVisitor(POMultipleBind def, S arg)
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
