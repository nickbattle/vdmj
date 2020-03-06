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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.patterns.TCPatternBind;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTrapStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCPatternBind patternBind;
	public final TCStatement with;
	public final TCStatement body;

	public TCTrapStatement(LexLocation location,
		TCPatternBind patternBind, TCStatement with, TCStatement body)
	{
		super(location);
		this.patternBind = patternBind;
		this.with = with;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "trap " + patternBind + " with " + with + " in " + body;
	}

	@Override
	public TCType typeCheck(Environment base, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCTypeSet rtypes = new TCTypeSet();

		TCType bt = body.typeCheck(base, scope, constraint, mandatory);
		rtypes.add(bt);

		TCTypeSet extype = body.exitCheck(base);
		TCType ptype = null;

		if (extype.isEmpty())
		{
			if (!Settings.exceptions)
			{
				report(3241, "Body of trap statement does not throw exceptions");
			}
			
			ptype = new TCUnknownType(body.location);
		}
		else
		{
			// Make a union with "?" so that pattern always matches
			extype.add(new TCUnknownType(body.location));
			ptype = extype.getType(body.location);
		}

		patternBind.typeCheck(base, scope, ptype);
		TCDefinitionList defs = patternBind.getDefinitions();
		defs.typeCheck(base, scope);
		Environment local = new FlatCheckedEnvironment(defs, base, scope);
		rtypes.add(with.typeCheck(local, scope, constraint, mandatory));

		return rtypes.getType(location);
	}

	@Override
	public TCTypeSet exitCheck(Environment base)
	{
		TCTypeSet types = patternBind.exitCheck(base);
		types.addAll(body.exitCheck(base));
		types.addAll(with.exitCheck(base));
		return types;
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTrapStatement(this, arg);
	}
}
