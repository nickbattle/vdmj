/*******************************************************************************
 *
 *	Copyright (c) 2008 Fujitsu Services Ltd.
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

public class Seq1Type extends SeqType
{
	private static final long serialVersionUID = 1L;

	public Seq1Type(LexLocation location, Type type)
	{
		super(location, type);
	}

	@Override
	public String toDisplay()
	{
		return "seq1 of (" + seqof + ")";
	}

	@Override
	public Type polymorph(LexNameToken pname, Type actualType)
	{
		return new Seq1Type(location, seqof.polymorph(pname, actualType));
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other.getClass().equals(Seq1Type.class))
		{
			Seq1Type os = (Seq1Type)other;
			return seqof.equals(os.seqof);
		}

		return false;
	}
}
