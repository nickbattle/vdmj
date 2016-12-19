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

package com.fujitsu.vdmj.ast.definitions;

import java.util.List;

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.traces.ASTTraceDefinitionTermList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.util.Utils;

public class ASTNamedTraceDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final List<String> pathname;
	public final ASTTraceDefinitionTermList terms;

	public ASTNamedTraceDefinition(
		LexLocation location, List<String> pathname, ASTTraceDefinitionTermList terms)
	{
		super(location, new LexNameToken(
			location.module, Utils.listToString(pathname, "_"), location));

		this.pathname = pathname;
		this.terms = terms;

		setAccessSpecifier(new ASTAccessSpecifier(false, false, Token.PUBLIC, false));
	}

	@Override
	public String kind()
	{
		return "trace";
	}

	@Override
	public String toString()
	{
		return pathname + " = " + terms.toString();
	}
}
