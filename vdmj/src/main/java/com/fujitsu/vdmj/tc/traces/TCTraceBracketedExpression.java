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

package com.fujitsu.vdmj.tc.traces;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A class representing a core trace bracketed expression.
 */
public class TCTraceBracketedExpression extends TCTraceCoreDefinition
{
    private static final long serialVersionUID = 1L;
	public final TCTraceDefinitionTermList terms;

	public TCTraceBracketedExpression(LexLocation location, TCTraceDefinitionTermList terms)
	{
		super(location);
		this.terms = terms;
	}

	@Override
	public String toString()
	{
		return terms.toString();
	}

	@Override
	public void typeCheck(Environment env, NameScope scope)
	{
		for (TCTraceDefinitionTerm term: terms)
		{
			term.typeCheck(env, scope);
		}
	}
}
