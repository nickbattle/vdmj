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
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
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

	/** A value for getLength meaning "any length" */
	protected static int ANY = -1;

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
	public TCDefinitionList getDefinitions(TCType type, NameScope scope)
	{
		TCDefinitionSet set = new TCDefinitionSet();
		set.addAll(getAllDefinitions(type, scope));
		return set.asList();
	}

	/**
	 * Get a complete list of all definitions, including duplicates.
	 */
	abstract protected TCDefinitionList getAllDefinitions(TCType type, NameScope scope);

	/** Return a list of the contained IdentifierPatterns */
	protected List<TCIdentifierPattern> findIdentifiers()
	{
		return new Vector<TCIdentifierPattern>();		// Most have none
	}

	/** Get the type(s) that could match this pattern. */
	abstract public TCType getPossibleType();

	/** Test whether the pattern can match the type passed */
	public boolean matches(TCType type)
	{
		return TypeComparator.compatible(getPossibleType(), type);
	}

	/**
	 * Get a set of names for the pattern's variables. Note that if the
	 * pattern includes duplicate variable names, these are collapse into one.
	 */
	public TCNameList getVariableNames()
	{
		TCNameSet set = new TCNameSet();
		set.addAll(getAllVariableNames());
		TCNameList list = new TCNameList();
		list.addAll(set);
		return list;
	}

	/** Get a complete list of the pattern's variable names, including duplicates. */
	protected TCNameList getAllVariableNames()
	{
		return new TCNameList();	// Most are empty
	}

	/**
	 * @return The "length" of the pattern (eg. sequence and set patterns).
	 */
	public int getLength()
	{
		return 1;	// Most only identify one member
	}

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
}
