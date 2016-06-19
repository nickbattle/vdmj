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

package org.overturetool.vdmj.types;

import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.values.SetValue;
import org.overturetool.vdmj.values.ValueList;

public class Set1Type extends SetType
{
	private static final long serialVersionUID = 1L;

	public Set1Type(LexLocation location, Type type)
	{
		super(location, type);
	}

	@Override
	public String toDisplay()
	{
		return "set1 of (" + setof + ")";
	}

	@Override
	public Type polymorph(LexNameToken pname, Type actualType)
	{
		return new Set1Type(location, setof.polymorph(pname, actualType));
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other.getClass().equals(Set1Type.class))
		{
			Set1Type os = (Set1Type)other;
			return setof.equals(os.setof);
		}

		return false;
	}

	@Override
	public ValueList getAllValues(Context ctxt) throws ValueException
	{
		ValueList all = super.getAllValues(ctxt);
		all.remove(new SetValue());  // Remove {}
		return all;
	}
}
