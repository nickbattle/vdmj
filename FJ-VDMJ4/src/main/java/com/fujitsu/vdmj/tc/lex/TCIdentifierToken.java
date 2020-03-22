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

package com.fujitsu.vdmj.tc.lex;

import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.lex.LexLocation;

public class TCIdentifierToken extends TCToken
{
	private static final long serialVersionUID = 1L;
	private final LexIdentifierToken identifier;
	
	public TCIdentifierToken(LexIdentifierToken identifier)
	{
		this.identifier = identifier;
	}

	public TCIdentifierToken(LexLocation location, String name, boolean old)
	{
		this.identifier = new LexIdentifierToken(name, old, location);
	}

	@Override
	public LexLocation getLocation()
	{
		return identifier.location;
	}

	public String getName()
	{
		return identifier.name;
	}

	public LexIdentifierToken getLex()
	{
		return identifier;
	}
	
	public TCNameToken getClassName()
	{
		// We don't know the class name of the name of a class until we've
		// read the name. So create a new location with the right module.

		LexLocation loc = new LexLocation(
			identifier.location.file,
			identifier.name,
			identifier.location.startLine,
			identifier.location.startPos,
			identifier.location.endLine,
			identifier.location.endPos);

		return new TCNameToken(loc, "CLASS", identifier.name, false);
	}
	
	@Override
	public String toString()
	{
		return identifier.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCIdentifierToken)
		{
			TCIdentifierToken tother = (TCIdentifierToken)other;
			return this.identifier.equals(tother.identifier);
		}

		return false;
	}
}
