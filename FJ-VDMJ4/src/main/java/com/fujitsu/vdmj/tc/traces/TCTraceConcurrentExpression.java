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

package com.fujitsu.vdmj.tc.traces;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTraceConcurrentExpression extends TCTraceCoreDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCTraceDefinitionList defs;

	public TCTraceConcurrentExpression(LexLocation location, TCTraceDefinitionList defs)
	{
		super(location);
		this.defs = defs;
	}

	@Override
	public void typeCheck(Environment local, NameScope scope)
	{
		for (TCTraceDefinition def: defs)
		{
			def.typeCheck(local, scope);
		}
	}
}
