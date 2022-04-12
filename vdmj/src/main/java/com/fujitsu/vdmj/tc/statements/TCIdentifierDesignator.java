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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCExternalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCIdentifierDesignator extends TCStateDesignator
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	private TCDefinition vardef = null;

	public TCIdentifierDesignator(TCNameToken name)
	{
		super(name.getLocation());
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name.getName();
	}

	@Override
	public TCType typeCheck(Environment env)
	{
		TCDefinition encl = env.getEnclosingDefinition();

		if (env.isVDMPP())
		{
			// We generate an explicit name because accessing a variable
			// by name in VDM++ does not "inherit" values from a superclass.

			TCNameToken exname = name.getExplicit(true);
			vardef = env.findName(exname, NameScope.STATE);

			if (vardef == null)
			{
				TCDefinitionSet matches = env.findMatches(exname);
				
				if (!matches.isEmpty())
				{
					TCDefinition match = matches.iterator().next();	// Just take first
					
					if (match.isFunction())
					{
						report(3247, "Function apply not allowed in state designator");
					}
					else
					{
						report(3247, "Operation call not allowed in state designator");
					}
					
					return match.getType();
				}
				else
				{
					report(3247, "Symbol '" + name + "' is not an updatable variable");
				}
				
				return new TCUnknownType(location);
			}
			else if (!vardef.isUpdatable())
			{
				report(3301, "Variable '" + name + "' in scope is not updatable");
				return vardef.getType();
			}
			else if (encl != null && encl.isPure() && vardef.isInstanceVariable())
			{
				report(3338, "Cannot update state in a pure operation");
			}
			else if (vardef.classDefinition != null)
			{
    			if (!TCClassDefinition.isAccessible(env, vardef, true))
    			{
    				report(3180, "Inaccessible member '" + name + "' of class " +
    					vardef.classDefinition.name.getName());
    				return new TCUnknownType(location);
    			}
    			else if (!vardef.isStatic() && env.isStatic())
    			{
    				report(3181, "Cannot access " + name + " from a static context");
    				return new TCUnknownType(location);
    			}
			}
			else if (vardef instanceof TCExternalDefinition)
			{
				TCExternalDefinition d = (TCExternalDefinition)vardef;

				if (d.readOnly)
				{
					report(3248, "Cannot assign to 'ext rd' state " + name);
				}
			}
			// else just state access in (say) an explicit operation

			return vardef.getType();
		}
		else
		{
			TCDefinition def = env.findName(name, NameScope.STATE);

			if (def == null)
			{
				report(3247, "Unknown state variable '" + name + "' in assignment");
				return new TCUnknownType(name.getLocation());
			}
			else if (def.isFunction())
			{
				report(3247, "Function apply not allowed in state designator");
				return new TCUnknownType(name.getLocation());
			}
			else if (def.isOperation())
			{
				report(3247, "Operation call not allowed in state designator");
				return new TCUnknownType(name.getLocation());
			}
			else if (!def.isUpdatable())
			{
				report(3301, "Variable '" + name + "' in scope is not updatable");
				return new TCUnknownType(name.getLocation());
			}
			else if (encl != null && encl.isPure() && !(def instanceof TCAssignmentDefinition))
			{
				report(3338, "Cannot update state in a pure operation");
			}
			else if (def instanceof TCExternalDefinition)
			{
				TCExternalDefinition d = (TCExternalDefinition)def;

				if (d.readOnly)
				{
					report(3248, "Cannot assign to 'ext rd' state " + name);
				}
			}
			// else just state access in (say) an explicit operation

			return def.getType();
		}
	}
	
	public TCDefinition getDefinition()
	{
		return vardef;
	}

	@Override
	public TCDefinition targetDefinition(Environment env)
	{
		return env.findName(name, NameScope.STATE);
	}
}
