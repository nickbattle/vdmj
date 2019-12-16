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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

public class TCSeqType extends TCType
{
	private static final long serialVersionUID = 1L;
	public TCType seqof;
	public final boolean empty;

	public TCSeqType(LexLocation location, TCType type)
	{
		super(location);
		this.seqof = type;
		this.empty = false;
	}

	public TCSeqType(LexLocation location)
	{
		super(location);
		this.seqof = new TCUnknownType(location);
		this.empty = true;
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		return seqof.narrowerThan(accessSpecifier);
	}

	@Override
	public boolean isSeq(LexLocation from)
	{
		return true;
	}

	@Override
	public TCSeqType getSeq()
	{
		return this;
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }
		seqof.unResolve();
	}

	@Override
	public TCType typeResolve(Environment env, TCTypeDefinition root)
	{
		if (resolved) return this; else { resolved = true; }

		try
		{
			seqof = seqof.typeResolve(env, root);
			if (root != null) root.infinite = false;	// Could be empty
			return this;
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	@Override
	public String toDisplay()
	{
		return empty ? "[]" : "seq of (" + seqof + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other.getClass().equals(TCSeqType.class))
		{
			TCSeqType os = (TCSeqType)other;
			// NB. Empty sequence is the same type as any sequence
			return empty || os.empty ||	seqof.equals(os.seqof);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return empty ? 0 : seqof.hashCode();
	}
	
	@Override
	public TCTypeList getComposeTypes()
	{
		return seqof.getComposeTypes();
	}

	@Override
	public TCNameSet getFreeVariables(Environment env)
	{
		return seqof.getFreeVariables(env);
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSeqType(this, arg);
	}
}
