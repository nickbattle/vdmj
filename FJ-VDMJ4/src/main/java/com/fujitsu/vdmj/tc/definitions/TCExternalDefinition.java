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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

/**
 * A class to hold an external state definition.
 */
public class TCExternalDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCDefinition state;
	public final boolean readOnly;
	public final TCNameToken oldname;	// For "wr" only

	public TCExternalDefinition(TCDefinition state, boolean readOnly)
	{
		super(Pass.DEFS, state.location, state.name, NameScope.STATE);
		this.state = state;
		this.readOnly = readOnly;
		this.oldname = readOnly ? null : state.name.getOldName();
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		if (sought.isOld() && scope == NameScope.NAMESANDANYSTATE)
		{
			return (sought.equals(oldname)) ? this : null;
		}

		return (sought.equals(state.name)) ? this : null;
	}

	@Override
	public String toString()
	{
		return (readOnly ? "ext rd " : "ext wr ") + state.name;
	}
	
	@Override
	public String kind()
	{
		return "ext def";
	}

	@Override
	public TCType getType()
	{
		return state.getType();
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		// Nothing to do - state is type checked separately
	}

	@Override
    public void markUsed()
	{
		used = true;
		state.markUsed();
	}

	@Override
    protected boolean isUsed()
	{
		return state.isUsed();
	}

	@Override
	public boolean isUpdatable()
	{
		return true;
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(state);
	}

	@Override
	public TCNameList getVariableNames()
	{
		return state.getVariableNames();
	}
}
