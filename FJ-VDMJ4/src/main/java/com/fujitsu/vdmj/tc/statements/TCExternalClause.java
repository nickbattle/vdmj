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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCExternalClause extends TCNode
{
	private static final long serialVersionUID = 1L;
	public final LexToken mode;
	public final TCNameList identifiers;
	public TCType type;

	public TCExternalClause(LexToken mode, TCNameList names, TCType type)
	{
		this.mode = mode;
		this.identifiers = names;
		this.type = (type == null) ? new TCUnknownType(names.get(0).getLocation()) : type;
	}

	public void typeResolve(Environment base)
	{
		type = type.typeResolve(base, null);
	}

	@Override
	public String toString()
	{
		return mode.toString() + " " + identifiers + (type == null ? "" : ":" + type);
	}
}
