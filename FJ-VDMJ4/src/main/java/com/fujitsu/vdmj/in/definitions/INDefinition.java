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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.definitions;

import java.io.Serializable;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.ValueList;

/**
 * The abstract parent of all definitions. A definition can represent a data
 * type, a value (constant), implicit or explicit functions, implicit or
 * explicit operations, module state, as well as various sorts of local variable
 * definition.
 */
public abstract class INDefinition extends INNode implements Serializable, Comparable<INDefinition>
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
	 * Return a list of external values that are read by the definition.
	 * @param ctxt The context in which to evaluate the expressions.
	 * @return A list of values read.
	 */
	public ValueList getValues(Context ctxt)
	{
		return new ValueList();
	}

	/**
	 * Return a list of old names used by the definition.
	 */
	public TCNameList getOldNames()
	{
		return new TCNameList();
	}

	/**
	 * Return the static type of the definition. For example, the type of a
	 * function or operation definition would be its parameter/result signature;
	 * the type of a value definition would be that value's type; the type of a
	 * type definition is the underlying type being defined.
	 * <p>
	 * Note that for Definitions which define multiple inner definitions (see
	 * {@link #getDefinitions}), this method returns the primary type - eg.
	 * the type of a function, not the types of its pre/post definitions.
	 *
	 * @return The primary type of this definition.
	 */
	abstract public TCType getType();

	/**
	 * Locate a {@link TCStatement} that starts on the line number indicated.
	 * This is used by the debugger to set breakpoints.
	 *
	 * @param lineno The line number to look for.
	 * @return A statement that starts on the line, or null if there is none.
	 */
	public INStatement findStatement(int lineno)
	{
		return null;
	}

	/**
	 * Locate an {@link TCExpression} that starts on the line number indicated.
	 * This is used by the debugger to set breakpoints.
	 *
	 * @param lineno The line number to look for.
	 * @return An expression that starts on the line, or null if there is none.
	 */
	public INExpression findExpression(int lineno)
	{
		return null;
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
		return new NameValuePairList();		// Overridden
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
