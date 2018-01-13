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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.PrivateClassEnvironment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

public class TCClassType extends TCType
{
	private static final long serialVersionUID = 1L;

	public final TCClassDefinition classdef;
	public final TCNameToken name;		// "CLASS`<name>"

	public TCClassType(LexLocation location, TCClassDefinition classdef)
	{
		super(location);

		this.classdef = classdef;
		this.name = classdef.name;
	}

	public TCNameToken getMemberName(TCIdentifierToken field)
	{
		return new TCNameToken(field.getLocation(), classdef.name.getName(), field.getName(), false, false);
	}

	@Override
	public void unResolve()
	{
		if (resolved)
		{
    		resolved = false;

    		for (TCDefinition d: classdef.getDefinitions())
    		{
    			d.getType().unResolve();
    		}
		}
	}

	@Override
	public TCType typeResolve(Environment env, TCTypeDefinition root)
	{
		if (resolved) return this; else resolved = true;

		try
		{
			// We have to add a private class environment here because the
			// one passed in may be from a class that contains a reference
			// to this class. We need the private environment to see all
			// the definitions that are available to us while resolving...

			Environment self = new PrivateClassEnvironment(classdef, env);

			for (TCDefinition d: classdef.getDefinitions())
			{
				// There is a problem resolving ParameterTypes via a TCFunctionType
				// when this is not being done via TCExplicitFunctionDefinition
				// which extends the environment with the type names that
				// are in scope. So we skip these here.

				if (d instanceof TCExplicitFunctionDefinition)
				{
					TCExplicitFunctionDefinition fd = (TCExplicitFunctionDefinition)d;

					if (fd.typeParams != null)
					{
						continue;	// Skip polymorphic functions
					}
				}

				d.getType().typeResolve(self, root);
			}

			return this;
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	public TCDefinition findName(TCNameToken tag, NameScope scope)
	{
		return classdef.findName(tag, scope);
	}

	@Override
	public boolean isClass(Environment env)
	{
		return true;
	}

	@Override
	public TCClassType getClassType(Environment env)
	{
		return this;
	}

	public boolean hasSupertype(TCType other)
	{
		return classdef.hasSupertype(other);
	}

	@Override
	protected String toDisplay()
	{
		return classdef.name.getName();
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other instanceof TCClassType)
		{
			TCClassType oc = (TCClassType)other;
			return name.equals(oc.name);		// NB. name only
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
}
