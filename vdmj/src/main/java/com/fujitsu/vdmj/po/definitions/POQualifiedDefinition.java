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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

public class POQualifiedDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final PODefinition def;
	private final TCType type;

	public POQualifiedDefinition(LexLocation location, TCNameToken name, PODefinition qualifies, TCType type)
	{
		super(location, name);
		this.def = qualifies;
		this.type = type;
	}

	@Override
	public String toString()
	{
		return def.toString();
	}

	@Override
	public boolean equals(Object other)
	{
		return def.equals(other);
	}

	@Override
	public int hashCode()
	{
		return def.hashCode();
	}

	@Override
	public TCType getType()
	{
		return type; // NB. Not delegated!
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		return def.getProofObligations(ctxt, pogState, env);
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseQualifiedDefinition(this, arg);
	}
}
