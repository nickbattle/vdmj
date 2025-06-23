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

package com.fujitsu.vdmj.po.patterns;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.PODefinitionSet;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.visitors.POGetAllDefinitionsVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POGetAllVarNamesVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POGetPossibleTypeVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POHiddenVariablesVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.POPatternVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * The parent type of all patterns.
 */
public abstract class POPattern extends PONode
{
	private static final long serialVersionUID = 1L;

	/** A value for getLength meaning "any length" */
	protected static int ANY = -1;

	/**
	 * Create a pattern at the given location.
	 */
	public POPattern(LexLocation location)
	{
		super(location);
	}

	@Override
	abstract public String toString();

	/**
	 * Get a set of definitions for the pattern's variables. Note that if the
	 * pattern includes duplicate variable names, these are collapse into one.
	 */
	public PODefinitionList getDefinitions(TCType type)
	{
		PODefinitionSet set = new PODefinitionSet();
		set.addAll(getAllDefinitions(type));
		return set.asList();
	}

	/**
	 * Get a complete list of all definitions, including duplicates.
	 */
	protected final PODefinitionList getAllDefinitions(TCType type)
	{
		return apply(new POGetAllDefinitionsVisitor(), type);
	}

	/**
	 * Get the type(s) that could match this pattern.
	 */
	public final TCType getPossibleType()
	{
		return apply(new POGetPossibleTypeVisitor(), null);
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

	/**
	 * Get a complete list of the pattern's variable names, including duplicates.
	 */
	protected final TCNameList getAllVariableNames()
	{
		return apply(new POGetAllVarNamesVisitor(), null);
	}

	/**
	 * @return The "length" of the pattern (eg. sequence and set patterns).
	 */
	public int getLength()
	{
		return 1;	// Most only identify one member
	}

	/**
	 * @return True if the pattern is a simple value that can match only
	 * one value for certain. Most pattern types are like this, but any
	 * that include variables or ignore patterns are not. 
	 */
	public boolean isSimple()
	{
		return true;
	}
	
	/**
	 * @return True if the pattern will always match a value of the corresponding
	 * type. For example, an identifier will always match a value, but a
	 * sequence pattern [a,b] will only match sequences of two values, and a
	 * constant pattern line "123" will only match the value 123.
	 */
	public boolean alwaysMatches()
	{
		return false;
	}

	/**
	 * An expression that matches the pattern. This is used in
	 * PO generation when parameter patterns have to be passed to pre/post
	 * conditions as arguments. The result is almost the same as toString(),
	 * except for IgnorePatterns, which produce "don't care" variables.
	 *
	 * @return An expression, being a value that matches the pattern.
	 */
	public final POExpression getMatchingExpression()
	{
		return apply(new POGetMatchingExpressionVisitor(), null);
	}
	
	/**
	 * Convert a pattern's ignore patterns into "any" identifier patterns.
	 * This is used in PO generation.
	 */
	public final POPattern removeIgnorePatterns()
	{
		return apply(new PORemoveIgnoresVisitor(), null);
	}

	/**
	 * Indicates that the pattern is associated with a maximal type, which may
	 * affect the string representation - eg. mk_R!(...).
	 */
	public void setMaximal(boolean maximal)
	{
		return;		// Only used in PORecordPattern
	}
	
	/**
	 * Search the pattern for identifiers that hide other definitions. This is
	 * used during PO generation to avoid TC errors.
	 */
	public TCNameList getHiddenVariables()
	{
		return apply(new POHiddenVariablesVisitor(), null);
	}

	/**
	 * Implemented by all patterns to allow visitor processing.
	 */
	abstract public <R, S> R apply(POPatternVisitor<R, S> visitor, S arg);
}
