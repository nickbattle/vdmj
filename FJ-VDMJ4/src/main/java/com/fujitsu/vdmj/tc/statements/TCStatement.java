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

package com.fujitsu.vdmj.tc.statements;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInheritedDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * The parent class of all statements.
 */
public abstract class TCStatement extends TCNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The location of the statement. */
	public final LexLocation location;
	
	/**
	 * Create a statement at the given location.
	 * @param location
	 */
	public TCStatement(LexLocation location)
	{
		this.location = location;
	}

	@Override
	abstract public String toString();

	/** Type check the statement and return its type. */ 
	abstract public TCType typeCheck(Environment env, NameScope scope, TCType constraint);

	/**
	 * Check that a return type meets a constraint. This is used in various statements
	 * to verify that a constraining type is met (see typeCheck methods).
	 */
	protected TCType checkReturnType(TCType constraint, TCType actual)
	{
		if (constraint != null && !(actual instanceof TCVoidType) && !(actual.isUnknown(location)))
		{
			if (actual.hasVoid() && !(constraint instanceof TCVoidType))
			{
				report(3328, "Statement may return void value");
				detail2("Actual", actual, "Expected", constraint);
			}
			else if (!TypeComparator.compatible(constraint, actual))
			{
				report(3327, "Value is not of the right type");
				detail2("Actual", actual, "Expected", constraint);
			}
		}

		return actual;
	}

	/**
	 * Whether the statement probably produces side effects. This is used during
	 * TC generation for operations. The method will produce false positives
	 * (saying that side effects are produced, when they are not), but no false
	 * negatives.
	 */
	public boolean hasSideEffects()
	{
		return true;
	}

	/**
	 * Return a list of exit types which can be thrown by the statement.
	 * @return A possibly empty list of exit types.
	 */
	public TCTypeSet exitCheck()
	{
		return new TCTypeSet();
	}

	/**
	 * Search the statement for its free variables, if any. The environment passed contains
	 * those names that are already defined in the scope - ie. which are not free variables.
	 * The returns boolean (which is mutable) is updated by return and exit statements,
	 * which indicate that statements that follow are conditional.
	 * @param globals TODO
	 * @param env
	 * @param returns
	 */
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		return new TCNameSet();
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
	 * @see org.TCDefinition.vdmj.definitions.Definition#detail
	 */
	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	/**
	 * @see org.TCDefinition.vdmj.definitions.Definition#detail2
	 */
	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}
	
	/**
	 * Test whether a definition is a class constructor.
	 */
	public static boolean isConstructor(TCDefinition def)
	{
		if (def instanceof TCExplicitOperationDefinition)
		{
			TCExplicitOperationDefinition op = (TCExplicitOperationDefinition)def;
			return op.isConstructor;
		}
		else if (def instanceof TCImplicitOperationDefinition)
		{
			TCImplicitOperationDefinition op = (TCImplicitOperationDefinition)def;
			return op.isConstructor;
		}
		else if (def instanceof TCInheritedDefinition)
		{
			TCInheritedDefinition op = (TCInheritedDefinition)def;
			return isConstructor(op.superdef);
		}
		
		return false;
	}

	/**
	 * Test whether the calling environment indicates that we are within a constructor.
	 */
	public static boolean inConstructor(Environment env)
	{
		TCDefinition encl = env.getEnclosingDefinition();
	
		if (encl != null)
		{
			return isConstructor(encl);
		}
		
		return false;
	}
}
