/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.tc.annotations;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCAnnotatedStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;

	public final TCAnnotation annotation;

	public final TCStatement statement;
	
	public TCAnnotatedStatement(LexLocation location, TCAnnotation annotation, TCStatement statement)
	{
		super(location);
		this.annotation = annotation;
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return annotation + " " + statement;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint)
	{
		annotation.typeCheck(null, env, scope);
		return statement.typeCheck(env, scope, constraint);
	}

	public boolean hasSideEffects()
	{
		return statement.hasSideEffects();
	}

	public TCTypeSet exitCheck()
	{
		return statement.exitCheck();
	}
}
