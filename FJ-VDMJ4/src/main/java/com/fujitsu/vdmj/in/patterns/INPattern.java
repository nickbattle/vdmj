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

package com.fujitsu.vdmj.in.patterns;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

/**
 * The parent type of all patterns.
 */
public abstract class INPattern extends INNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** A value for getLength meaning "any length" */
	protected static int ANY = -1;

	/**
	 * Create a pattern at the given location.
	 */
	public INPattern(LexLocation location)
	{
		super(location);
	}

	@Override
	abstract public String toString();

	/**
	 * Get a name/value pair list for the pattern's variables.
	 */
	public NameValuePairList getNamedValues(Value expval, Context ctxt)	throws PatternMatchException
	{
		List<INIdentifierPattern> ids = findIdentifiers();

		// Go through the list of IDs, marking duplicate names as constrained. This is
		// because we have to permute sets that contain duplicate variables, so that
		// we catch permutations that match constrained values of the variable from
		// elsewhere in the pattern.

		int count = ids.size();

		for (int i=0; i<count; i++)
		{
			TCNameToken iname = ids.get(i).name;

			for (int j=i+1; j<count; j++)
			{
				if (iname.equals(ids.get(j).name))
				{
					ids.get(i).setConstrained(true);
					ids.get(j).setConstrained(true);
				}
			}
		}

		List<NameValuePairList> all = getAllNamedValues(expval, ctxt);
		return all.get(0);		// loose choice here!
	}
	
	/**
	 * Get the type(s) that can possibly match this pattern.
	 */
	protected abstract TCType getPossibleType();	// TODO as a LeafPatternVisitor?

	/**
	 * Return a list of the contained IdentifierPatterns
	 */
	protected List<INIdentifierPattern> findIdentifiers()
	{
		return new Vector<INIdentifierPattern>();		// TODO as a LeafPatternVisitor
	}

	/**
	 * Get a set of names for the pattern's variables. Note that if the
	 * pattern includes duplicate variable names, these are collapse into one.
	 */
	public final TCNameList getVariableNames()
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
	protected TCNameList getAllVariableNames()
	{
		return new TCNameList();	// TODO as a LeafPatternVisitor?
	}

	/**
	 * Get a name/value pair list for the pattern's variables.
	 */
	public abstract List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt) throws PatternMatchException;

	/**
	 * @return The "length" of the pattern (eg. sequence and set patterns).
	 */
	public int getLength()
	{
		return 1;	// Most only identify one member
	}

	/**
	 * @return True if the pattern has constraints, such that matching
	 * values should be permuted, where necessary, to find a match.
	 */
	public boolean isConstrained()
	{
		return true;
	}

	/**
	 * Throw a PatternMatchException with the given message.
	 * @throws PatternMatchException
	 */
	public void patternFail(int number, String msg) throws PatternMatchException
	{
		throw new PatternMatchException(number, msg, location);
	}

	/**
	 * Throw a PatternMatchException with a message from the ValueException.
	 * @throws PatternMatchException
	 */
	public Value patternFail(ValueException ve) throws PatternMatchException
	{
		throw new PatternMatchException(ve.number, ve.getMessage(), location);
	}

	/**
	 * Implemented by all patterns to allow visitor processing.
	 */
	abstract public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg);
}
