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

package com.fujitsu.vdmj.ast.types;

import com.fujitsu.vdmj.lex.LexLocation;

public class ASTSeqType extends ASTType
{
	private static final long serialVersionUID = 1L;
	public final ASTType seqof;

	public ASTSeqType(LexLocation location, ASTType type)
	{
		super(location);
		this.seqof = type;
	}

	@Override
	public String toDisplay()
	{
		return "seq of (" + seqof + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other.getClass().equals(ASTSeqType.class))
		{
			ASTSeqType os = (ASTSeqType)other;
			return seqof.equals(os.seqof);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return seqof.hashCode();
	}
}
