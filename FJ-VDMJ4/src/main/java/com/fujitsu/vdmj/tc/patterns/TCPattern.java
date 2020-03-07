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

package com.fujitsu.vdmj.tc.patterns;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * The parent type of all patterns.
 */
public abstract class TCPattern extends TCNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the pattern. */
	public final LexLocation location;

	/** A flag to prevent recursive type resolution problems. */
	boolean resolved = false;

	/**
	 * Create a pattern at the given location.
	 */
	public TCPattern(LexLocation location)
	{
		this.location = location;
	}

	@Override
	abstract public String toString();

	/**
	 * Resolve any types that the pattern may use by looking up the type
	 * names in the environment passed.
	 *
	 * @param env The environment to resolve types.
	 */
	public void typeResolve(Environment env)
	{
		resolved = true;
	}

	/**
	 * Clear the recursive type resolution flag. This is a deep clear,
	 * used when recovering from type resolution errors.
	 */
	public void unResolve()
	{
		resolved = false;
	}

	/**
	 * Get a set of definitions for the pattern's variables. Note that if the
	 * pattern includes duplicate variable names, these are collapse into one.
	 */
	public final TCDefinitionList getDefinitions(TCType type, NameScope scope)
	{
		TCDefinitionSet set = new TCDefinitionSet();
		set.addAll(apply(new TCGetDefinitionsVisitor(), new Pair(type, scope)));
		return set.asList();
	}

	/**
	 * Get a complete list of all definitions, including duplicates.
	 */
	public final TCDefinitionList getAllDefinitions(TCType type, NameScope scope)
	{
		return apply(new TCGetDefinitionsVisitor(), new Pair(type, scope));
	}

	/**
	 * Get the type(s) that match this pattern.
	 */
	public final TCType getPossibleType()
	{
		return apply(new TCPossibleTypeVisitor(), null);
	}
	
	/** Test whether the pattern can match the type passed */
	public boolean matches(TCType type)
	{
		return TypeComparator.compatible(getPossibleType(), type);
	}
	
	/** Test whether the pattern will always match the type passed */
	public boolean alwaysMatches(TCType type)
	{
		return TypeComparator.isSubType(type, getPossibleType()) && alwaysMatches();
	}

	/**
	 * Get a set of names for the pattern's variables. Note that if the
	 * pattern includes duplicate variable names, these are collapsed into one.
	 */
	public final TCNameList getVariableNames()
	{
		TCNameList list = new TCNameList();
		list.addAll(apply(new TCGetVariableNamesVisitor(), null));
		return list;
	}

	/**
	 * Report type checking errors for patterns. 
	 */
	public void report(int number, String msg)
	{
		TypeChecker.report(number, msg, location);
	}

	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}

	/**
	 * @return True if the pattern will always match a value of the corresponding
	 * type. For example, an identifier will always match a value, but a
	 * sequence pattern [a,b] will only match sequences of two values, and a
	 * constant pattern line "123" will only match the value 123.
	 */
	public final boolean alwaysMatches()
	{
		return apply(new TCAlwaysMatchesVisitor(), null);
	}

	/**
	 * Implemented by all patterns to allow visitor processing.
	 */
	abstract public <R, S> R apply(TCPatternVisitor<R, S> visitor, S arg);
}
