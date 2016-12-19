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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCVariableExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;

	public TCNameToken name;
	public final String original;

	private TCDefinition vardef = null;

	public TCVariableExpression(LexLocation location, TCNameToken name, String original)
	{
		super(location);
		this.name = name;
		this.original = original;
	}

	@Override
	public String toString()
	{
		return original;
	}

	public void setExplicit(boolean explicit)
	{
		name = name.getExplicit(explicit);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		if (env.isVDMPP())
		{
			name.setTypeQualifier(qualifiers);
    		vardef = env.findName(name, scope);

    		if (vardef != null)
    		{
    			if (vardef.classDefinition != null)
    			{
        			if (!TCClassDefinition.isAccessible(env, vardef, true))
        			{
        				report(3180, "Inaccessible member " + name + " of class " +
        					vardef.classDefinition.name.getName());
        				return new TCUnknownType(location);
        			}
        			else if (!vardef.isStatic() && env.isStatic())
            		{
            			report(3181, "Cannot access " + name + " from a static context");
            			return new TCUnknownType(location);
            		}
    			}
    		}
    		else if (qualifiers != null)
    		{
    			// It may be an apply of a map or sequence, which would not
    			// have the type qualifier of its arguments in the name. Or
    			// it might be an apply of a function via a function variable
    			// which would not be qualified.

    			name.setTypeQualifier(null);
    			vardef = env.findName(name, scope);

    			if (vardef == null)
    			{
    				name.setTypeQualifier(qualifiers);	// Just for error text!
    			}
    		}
    		else
    		{
    			// We may be looking for a bare function/op "x", when in fact
    			// there is one with a qualified name "x(args)". So we check
    			// the TCssible matches - if there is precisely one, we pick it,
    			// else we raise an ambiguity error.

				for (TCDefinition TCssible: env.findMatches(name))
				{
					if (TCssible.isFunctionOrOperation())
					{
						if (vardef != null)
						{
							report(3269, "Ambiguous function/operation name: " + name.getName());
							env.listAlternatives(name);
							break;
						}

						vardef = TCssible;

						// Set the qualifier so that it will find it at runtime.

						TCType pt = TCssible.getType();

						if (pt instanceof TCFunctionType)
						{
							TCFunctionType ft = (TCFunctionType)pt;
							name.setTypeQualifier(ft.parameters);
						}
						else
						{
							TCOperationType ot = (TCOperationType)pt;
							name.setTypeQualifier(ot.parameters);
						}
					}
				}
    		}
    	}
    	else
    	{
    		vardef = env.findName(name, scope);
    	}

		if (vardef == null)
		{
			report(3182, "Name '" + name + "' is not in scope");
			env.listAlternatives(name);
			return new TCUnknownType(location);
		}
		else
		{
			TCType result = vardef.getType();
			
			if (result instanceof TCParameterType)
			{
				TCParameterType ptype = (TCParameterType)result;
				
				if (ptype.name.equals(name))	// Referring to "T" of @T
				{
					report(3351, "Type parameter '" + name.getName() + "' cannot be used here");
					return new TCUnknownType(location);
				}
			}
			
			// Note that we perform an extra typeResolve here. This is
			// how forward referenced types are resolved, and is the reason
			// we don't need to retry at the top level (assuming all names
			// are in the environment).

			result = result.typeResolve(env, null);
			
			// If a constraint is passed in, we can raise an error if it is
			// not TCssible for the type to match the constraint (rather than
			// certain, as checkConstraint would).
			
			return possibleConstraint(constraint, result);
		}
	}
}
