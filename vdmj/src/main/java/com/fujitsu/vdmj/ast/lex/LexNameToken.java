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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.lex;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;

public class LexNameToken extends LexToken implements Serializable, Comparable<LexNameToken>
{
	private static final long serialVersionUID = 1L;

	public final String module;
	public final String name;
	public final boolean old;
	public final boolean explicit;

	public LexNameToken(String module, String name, LexLocation location, boolean old, boolean explicit)
	{
		super(location, Token.NAME);
		this.module = module;
		this.name = name;
		this.old = old;
		this.explicit = explicit;
	}

	public LexNameToken(String module, String name, LexLocation location)
	{
		this(module, name, location, false, false);
	}

	public LexNameToken(String module, LexIdentifierToken id)
	{
		super(id.location, Token.NAME);
		this.module = module;
		this.name = id.name;
		this.old = id.old;
		this.explicit = false;
	}

	public LexIdentifierToken getIdentifier()
	{
		return new LexIdentifierToken(name, old, location);
	}

	public LexNameToken getOldName()
	{
		return new LexNameToken(module,
			new LexIdentifierToken(name, true, location));
	}

	public LexNameToken getNewName()
	{
		return new LexNameToken(module,
			new LexIdentifierToken(name, false, location));
	}

	@Override
	public String toString()
	{
		return (explicit ? (module.length() > 0 ? module + "`" : "") : "") +
			name + (old ? "~" : "");	// NB. No qualifier
	}

	@Override
	public int hashCode()
	{
		return name.hashCode() + module.hashCode();
	}

	public String getName()
	{
		return name;
	}
	
	public String getModule()
	{
		return module;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof LexNameToken)
		{
			LexNameToken lother = (LexNameToken)other;
			return matches(lother);
		}
	
		return false;
	}

	public boolean matches(LexNameToken other)
	{
		return module.equals(other.module) &&
				name.equals(other.name) &&
				old == other.old;
	}

	@Override
	public int compareTo(LexNameToken o)
	{
		return toString().compareTo(o.toString());
	}

	public LexNameToken getModifiedName(String classname)
	{
		return new LexNameToken(classname, name, location, old, explicit);
	}

	public LexNameToken getExplicit(boolean explicit)
	{
		return new LexNameToken(module, name, location, old, explicit);
	}

	public LexNameToken getInvName(LexLocation location)
	{
		return new LexNameToken(module, "inv_" + name, location);
	}
}
