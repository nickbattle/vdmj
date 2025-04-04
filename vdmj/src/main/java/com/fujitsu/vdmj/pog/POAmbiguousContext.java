/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

package com.fujitsu.vdmj.pog;

import java.util.Collection;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A context to represent ambiguous variables
 */
public class POAmbiguousContext extends POContext
{
	private final String reason;
	private final TCNameSet variables;
	private final LexLocation location;
	
	public POAmbiguousContext(String reason, Collection<? extends TCNameToken> variables, LexLocation location)
	{
		this.reason = reason;
		this.variables = new TCNameSet();
		this.variables.addAll(variables);
		this.location = location;
	}
	
	@Override
	public TCNameSet ambiguousVariables()
	{
		return variables;
	}

	@Override
	public String getSource()
	{
		return "-- Ambiguous " + reason + ", affects " + variables +
			"? at " + location.startLine + ":" + location.startPos;
	}
}
