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

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.Token;

/**
 * A class to hold an external state definition.
 */
public class ASTExternalDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final ASTDefinition state;
	public final boolean readOnly;
	public final LexNameToken oldname;	// For "wr" only

	public ASTExternalDefinition(ASTDefinition state, LexToken mode)
	{
		super(state.location, state.name);
		this.state = state;
		this.readOnly = mode.is(Token.READ);
		this.oldname = readOnly ? null : state.name.getOldName();
	}

	@Override
	public String toString()
	{
		return (readOnly ? "ext rd " : "ext wr ") + state.name;
	}

	@Override
	public String kind()
	{
		return "external";
	}
}
