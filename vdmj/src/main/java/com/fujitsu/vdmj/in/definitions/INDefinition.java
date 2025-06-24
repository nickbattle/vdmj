/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionExpressionFinder;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionStatementFinder;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.NameValuePairList;

/**
 * The abstract parent of all definitions. A definition can represent a data
 * type, a value (constant), implicit or explicit functions, implicit or
 * explicit operations, module state, as well as various sorts of local variable
 * definition.
 */
public abstract class INDefinition extends INNode implements Comparable<INDefinition>
{
	private static final long serialVersionUID = 1L;

	/** The access specifier of the definition */
	public final INAccessSpecifier accessSpecifier;
	/** The name of the object being defined. */
	public final TCNameToken name;

	/** A pointer to the enclosing class definition, if any. */
	public INClassDefinition classDefinition = null;
	
	/** A list of annotations, if any */
	public INAnnotationList annotations = null;

	/**
	 * Create a new definition of a particular name and location.
	 */
	public INDefinition(LexLocation location, INAccessSpecifier accessSpecifier, TCNameToken name)
	{
		super(location);
		this.accessSpecifier = accessSpecifier;
		this.name = name;
	}

	@Override
	abstract public String toString();

	@Override
	public boolean equals(Object other)		// Used for sets of definitions.
	{
		if (other instanceof INDefinition)
		{
			INDefinition odef = (INDefinition)other;
			return name != null && odef.name != null && name.equals(odef.name);
		}

		return false;
	}
	
	@Override
	public int compareTo(INDefinition o)
	{
		return name == null ? 0 : name.compareTo(o.name); 
	};

	@Override
	public int hashCode()
	{
		return name.hashCode();		// Used for sets of definitions (see equals).
	}

	/**
	 * Return the static type of the definition. For example, the type of a
	 * function or operation definition would be its parameter/result signature;
	 * the type of a value definition would be that value's type; the type of a
	 * type definition is the underlying type being defined.
	 * p
	 * Note that for Definitions which define multiple inner definitions,
	 * this method returns the primary type - eg.
	 * the type of a function, not the types of its pre/post definitions.
	 *
	 * @return The primary type of this definition.
	 */
	abstract public TCType getType();

	/**
	 * Locate all {@link TCStatement} that starts on the line number indicated.
	 * This is used by the debugger to set breakpoints.
	 *
	 * @param lineno The line number to look for.
	 * @return A list of statements that start on the line, or null if there is none.
	 */
	public final INStatementList findStatements(int lineno)
	{
		return this.apply(new INDefinitionStatementFinder(), lineno);
	}

	/**
	 * Locate all {@link TCExpression} that start on the line number indicated.
	 * This is used by the debugger to set breakpoints.
	 *
	 * @param lineno The line number to look for.
	 * @return An expression that starts on the line, or null if there is none.
	 */
	public final INExpressionList findExpressions(int lineno)
	{
		return this.apply(new INDefinitionExpressionFinder(), lineno);
	}

	/**
	 * Locate an INDefinition by name, starting with this definition.
	 * @param sought
	 * @return An INDefinition or null.
	 */
	public final INDefinition findName(TCNameToken sought)
	{
		return this.apply(new INNameFinder(), sought);
	}

	/**
	 * Return the names and values in the runtime environment created by this
	 * definition. For example, a value definition would return its name(s),
	 * coupled with the value(s) derived from the evaluation of the value of
	 * the definition using the Context object passed in.
	 *
	 * In many definition types, the method has to deal with pattern
	 * substitution which may yield several variables, which is why it returns a
	 * list of name/values.
	 *
	 * @param ctxt The execution context in which to evaluate the definition.
	 * @return A possibly empty list of names and values.
	 */
	public NameValuePairList getNamedValues(Context ctxt)
	{
		return new NameValuePairList();
	}

	public boolean isOperation()
	{
		return false;
	}

	public boolean isFunction()
	{
		return false;
	}

	public boolean isCallableFunction()
	{
		return false;
	}

	public boolean isSubclassResponsibility()
	{
		return false;
	}

	public boolean isCallableOperation()
	{
		return false;
	}

	public boolean isUpdatable()
	{
		return false;
	}

	public boolean isInstanceVariable()
	{
		return false;
	}

	public boolean isTypeDefinition()
	{
		return false;
	}

	public boolean isValueDefinition()
	{
		return false;
	}

	public boolean isRuntime()
	{
		return true;	// Most are
	}

	public boolean isStatic()
	{
		return accessSpecifier != null && accessSpecifier.isStatic;
	}

	public boolean isFunctionOrOperation()
	{
		return isFunction() || isOperation();
	}

	public boolean isAccess(Token token)
	{
		return accessSpecifier != null && accessSpecifier.access.equals(token);
	}

	/**
	 * Implemented by all definitions to allow visitor processing.
	 */
	abstract public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg);
}
