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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTixeStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCTixeStmtAlternativeList traps;
	public final TCStatement body;

	public TCTixeStatement(LexLocation location, TCTixeStmtAlternativeList traps, TCStatement body)
	{
		super(location);
		this.traps = traps;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "tixe {" + traps + "} in " + body;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCType rt = body.typeCheck(env, scope, constraint, mandatory);
		TCTypeSet extypes = body.exitCheck(env);

		if (extypes.isEmpty())
		{
			extypes.add(new TCUnknownType(location));	// Force TC of alts anyway
		}

		TCType union = extypes.getType(location);

		for (TCTixeStmtAlternative tsa: traps)
		{
			tsa.typeCheck(env, scope, union, constraint, mandatory);
		}

		return setType(rt);
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTixeStatement(this, arg);
	}
}
