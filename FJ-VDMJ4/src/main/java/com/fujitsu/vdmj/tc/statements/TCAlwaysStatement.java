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

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCAlwaysStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;

	public final TCStatement always;
	public final TCStatement body;

	public TCAlwaysStatement(LexLocation location, TCStatement always, TCStatement body)
	{
		super(location);
		this.always = always;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "always " + always + " in " + body;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint)
	{
		always.typeCheck(env, scope, constraint);
		return body.typeCheck(env, scope, constraint);
	}

	@Override
	public TCTypeSet exitCheck()
	{
		TCTypeSet types = new TCTypeSet();
		types.addAll(body.exitCheck());
		types.addAll(always.exitCheck());
		return types;
	}

	@Override
	public TCNameSet getFreeVariables(Environment env, AtomicBoolean returns)
	{
		TCNameSet names = new TCNameSet();
		names.addAll(always.getFreeVariables(env, returns));
		names.addAll(body.getFreeVariables(env, returns));
		return names;
	}
}
