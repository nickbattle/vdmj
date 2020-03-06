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

package com.fujitsu.vdmj.tc.expressions;

import java.io.Serializable;

import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 *	The parent class of all VDM expressions.
 */
public abstract class TCExpression extends TCNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the expression. */
	public final LexLocation location;
	
	/** A list of comments preceding the expression */
	public LexCommentList comments;

	/**
	 * Generate an expression at the given location.
	 *
	 * @param location	The location of the new expression.
	 */
	public TCExpression(LexLocation location)
	{
		this.location = location;
	}

	/**
	 * Generate an expression at the same location as the expression passed.
	 * This is used when a compound expression, comprising several
	 * subexpressions, is being constructed. The expression passed "up" is
	 * considered the location of the overall expression. For example, a
	 * function application involves an expression for the function to apply,
	 * plus a list of expressions for the arguments. The location of the
	 * expression for the function (often just a variable name) is considered
	 * the location of the entire function application.
	 *
	 * @param exp The expression containing the location.
	 */
	public TCExpression(TCExpression exp)
	{
		this(exp.location);
	}

	@Override
	public abstract String toString();

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCExpression)
		{
			TCExpression oe = (TCExpression)other;
			return toString().equals(oe.toString());	// For now...
		}
		else
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	/**
	 * Perform a type check of the expression. The method returns the
	 * {@link TCType} of the expression it checked, and is used
	 * recursively across an expression tree. For example, a boolean "and"
	 * expression would type check its left and right hand sides, check that the
	 * types returned were both compatible with {@link org.INBooleanType.vdmj.types.BooleanType}
	 * and then return a TCBooleanType (regardless of any errors).
	 * <p>
	 * The qualifiers argument is passed when checking function and operation
	 * application. It contains the list of argument types of the application,
	 * and is used to choose between overloaded function/operation definitions.
	 *
	 * @param env The static environment for resolving names.
	 * @param qualifiers The argument type qualifiers for overloading.
	 * @param scope The scope of applicable names from the environment.
	 * @param constraint The type constraining the result, if any.
	 * @return The type of the expression.
	 */
	public abstract TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint);
	
	/**
	 * Check that a type meets a constraint. This is used in various value types to
	 * verify that a constraining type is met (see typeCheck methods).
	 */
	protected TCType checkConstraint(TCType constraint, TCType actual)
	{
		if (constraint != null)
		{
			if (!TypeComparator.isSubType(actual, constraint, true))
			{
				report(3327, "Value is not of the right type");
				detail2("Actual", actual, "Expected", constraint);
			}
		}

		return actual;
	}

	/**
	 * Check that a type possibly meets a constraint. This is used in various value types to
	 * verify that a constraining type could possibly met (see typeCheck methods).
	 */
	protected TCType possibleConstraint(TCType constraint, TCType actual)
	{
		if (constraint != null)
		{
			if (!TypeComparator.compatible(constraint, actual))
			{
				report(3327, "Value is not of the right type");
				detail2("Actual", actual, "Expected", constraint);
			}
		}

		return actual;
	}

	/**
	 * Search the expression for anything which qualifies the type of a definition,
	 * like "is_real(a)", and produce a list of QualifiedDefinitions to add to the
	 * environment.
	 * @param env 
	 */
	public TCDefinitionList getQualifiedDefs(Environment env)
	{
		return new TCDefinitionList();
	}
	
	/**
	 * Search the expression for its free variables, if any. The environment passed contains
	 * those names that are already defined in the scope - ie. which are not free variables.
	 */
	public final TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		return apply(new TCGetFreeVariablesVisitor(), new EnvTriple(globals, env, null));
	}

	/**
	 * Return a list of exit types which can be thrown by the expression's op calls.
	 * @return A possibly empty list of exit types.
	 */
	public TCTypeSet exitCheck(Environment base)
	{
		TCTypeSet result = new TCTypeSet();
		TCExitChecker checker = new TCExitChecker();
		result.addAll(this.apply(checker, base));
		return result;
	}

	/**
	 * @see org.TCDefinition.vdmj.definitions.Definition#report
	 */
	public void report(int number, String msg)
	{
		TypeChecker.report(number, msg, location);
	}

	/**
	 * @see org.TCDefinition.vdmj.definitions.Definition#warning
	 */
	public void warning(int number, String msg)
	{
		TypeChecker.warning(number, msg, location);
	}

	/**
	 * This is used when a problem would be an error if the type it applies
	 * to is unambiguous, or a warning otherwise.
	 * @param serious True if this should be an error
	 * @param number The error number.
	 * @param msg The problem.
	 */
	public void concern(boolean serious, int number, String msg)
	{
		if (serious)
		{
			TypeChecker.report(number, msg, location);
		}
		else
		{
			TypeChecker.warning(number, msg, location);
		}
	}

	/**
	 * @see org.TCDefinition.vdmj.definitions.Definition#detail
	 */

	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	public void detail(boolean serious, String tag, Object obj)
	{
		if (serious)
		{
			TypeChecker.detail(tag, obj);
		}
	}

	/**
	 * @see org.TCDefinition.vdmj.definitions.Definition#detail2
	 */

	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}

	public void detail2(boolean serious, String tag1, Object obj1, String tag2, Object obj2)
	{
		if (serious)
		{
			TypeChecker.detail2(tag1, obj1, tag2, obj2);
		}
	}
	
	/**
	 * Set the comments field.
	 */
	public void setComments(LexCommentList comments)
	{
		this.comments = comments;
	}

	/**
	 * Implemented by all expressions to allow visitor processing.
	 */
	abstract public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg);
}
