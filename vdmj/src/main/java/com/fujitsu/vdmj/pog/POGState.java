/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

package com.fujitsu.vdmj.pog;

import java.util.Collection;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to hold state information for POG of statements, which involve potentially
 * changing local variables.
 */
public class POGState
{
	private final TCNameList localNames;
	private final POGState outerState;
	
	private boolean ambiguousExpression = false;
	private POPattern resultPattern = null;
	private TCType resultType = null;
	
	public POGState()
	{
		this.localNames = new TCNameList();
		this.outerState = null;
	}
	
	/**
	 * Used by getLink.
	 */
	private POGState(POGState outerState)
	{
		this.localNames = new TCNameList();
		this.outerState = outerState;
	}
	
	@Override
	public String toString()
	{
		return "locals: " + localNames +
				(outerState != null ? " / " + outerState.toString() : "");
	}
	
	/**
	 * Create a new chained POGState, linked to the current one. This is used to process
	 * block statements that may contain "dcl" statements (ie. local state). Locals are
	 * added with addDclLocal.
	 */
	public POGState getLink()
	{
		return new POGState(this);
	}
	
	/**
	 * True if this POGState has a local variable of this name. Note, this doesn't look
	 * at the outer states.
	 */
	public boolean hasLocalName(TCNameToken name)
	{
		return localNames.contains(name);
	}

	/**
	 * Declare new "dcl" local(s).
	 */
	public void addDclLocal(TCNameToken name)
	{
		localNames.add(name);
	}
	
	public void addDclLocal(Collection<TCNameToken> names)
	{
		for (TCNameToken name: names)
		{
			localNames.add(name);
		}
	}
	
	/**
	 * Set the def/name/type of the RESULT variable. Null means "RESULT"
	 */
	public void setResult(POPattern result, TCType rtype)
	{
		if (outerState != null)
		{
			outerState.setResult(result, rtype);
		}
		else if (result == null)
		{
			TCNameToken R = TCNameToken.getResult(LexLocation.ANY);
			resultPattern = new POIdentifierPattern(R);
			resultType = rtype;
		}
		else
		{
			resultPattern = result;
			resultType = rtype;
		}
	}
	
	public POPattern getResultPattern()
	{
		if (outerState != null)
		{
			return outerState.getResultPattern();
		}
		else
		{
			return resultPattern;
		}
	}
	
	public TCType getResultType()
	{
		if (outerState != null)
		{
			return outerState.getResultType();
		}
		else
		{
			return resultType;
		}
	}
	
	/**
	 * Mark an expression as ambiguous - used in let def to mark locals as ambiguous.
	 */
	public void setAmbiguous(boolean ambiguous)
	{
		ambiguousExpression = ambiguous;
	}
	
	public boolean isAmbiguous()
	{
		return ambiguousExpression;
	}
}
