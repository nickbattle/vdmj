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
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

/**
 * A class to hold a definition of, as yet, an unknown type. See TCValueDefinition
 * and TCExportedValue for the purpose of this class.
 */
public class TCUntypedDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;

	public TCUntypedDefinition(LexLocation location, TCNameToken name)
	{
		super(Pass.DEFS, location, name, NameScope.GLOBAL);
	}

	@Override
	public TCType getType()
	{
		return new TCUnknownType(location);
	}

	@Override
	public String toString()
	{
		return "Unknown type definition " + name;
	}
	
	@Override
	public String kind()
	{
		return "unknown";
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		assert false: "Can't type check untyped definition?";
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(this);
	}

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList(name);
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseUntypedDefinition(this, arg);
	}
}
