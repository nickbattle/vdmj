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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.traces.TCTraceDefinitionTerm;
import com.fujitsu.vdmj.tc.traces.TCTraceDefinitionTermList;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

public class TCNamedTraceDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCTraceDefinitionTermList terms;

	public TCNamedTraceDefinition(LexLocation location, TCNameToken name, TCTraceDefinitionTermList terms)
	{
		super(Pass.DEFS, location, name, NameScope.GLOBAL);

		this.terms = terms;
		setAccessSpecifier(new TCAccessSpecifier(false, false, Token.PUBLIC, false));
		markUsed();		// Traces are never called, so this makes sense
	}

	@Override
	public boolean isOperation()
	{
		return true;
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(this);
	}

	@Override
	public TCType getType()
	{
		return new TCOperationType(location);		// () ==> ()
	}

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList(name);
	}

	@Override
	public String toString()
	{
		return name + " = " + terms.toString();
	}
	
	@Override
	public String kind()
	{
		return "trace";
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (base.isVDMPP())
		{
			base = new FlatEnvironment(getSelfDefinition(), base);
		}

		for (TCTraceDefinitionTerm term: terms)
		{
			term.typeCheck(base, NameScope.NAMESANDSTATE);
		}
	}
}
