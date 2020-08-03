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

package com.fujitsu.vdmj.po.patterns;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.visitors.POPatternVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

public class POIgnorePattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	private static int var = 1;		// Used in getMatchingExpression()
	private TCNameToken anyName = null;

	public POIgnorePattern(LexLocation location)
	{
		super(location);
	}

	@Override
	public String toString()
	{
		return "-";
	}

	@Override
	public POExpression getMatchingExpression()
	{
		// Generate a new "any" name for use during PO generation. The name
		// must be unique for the pattern instance.
		
		if (anyName == null)
		{
			anyName = new TCNameToken(location, "", "any" + var++);
		}
		
		return new POVariableExpression(anyName, null);
	}

	@Override
	public PODefinitionList getAllDefinitions(TCType type)
	{
		return new PODefinitionList();
	}

	@Override
	public int getLength()
	{
		return ANY;	// Special value meaning "any length"
	}

	@Override
	public boolean isSimple()
	{
		return false;
	}

	@Override
	public boolean alwaysMatches()
	{
		return true;
	}

	@Override
	public <R, S> R apply(POPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIgnorePattern(this, arg);
	}
}
