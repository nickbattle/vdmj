/*******************************************************************************
 *
 *	Copyright (c) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.modules;

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.definitions.ExplicitFunctionDefinition;
import org.overturetool.vdmj.definitions.ImplicitFunctionDefinition;
import org.overturetool.vdmj.definitions.LocalDefinition;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.FlatCheckedEnvironment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.ParameterType;
import org.overturetool.vdmj.types.Type;

public class ImportedFunction extends ImportedValue
{
	private static final long serialVersionUID = 1L;

	public final LexNameList typeParams;

	public ImportedFunction(
		LexNameToken name, Type type, LexNameList typeParams, LexNameToken renamed)
	{
		super(name, type, renamed);
		this.typeParams = typeParams;
	}

	@Override
	public void typeCheck(Environment env)
	{
		if (typeParams == null)
		{
			super.typeCheck(env);
		}
		else
		{
			if (type != null && from != null)
			{
	    		DefinitionList defs = new DefinitionList();

	    		for (LexNameToken pname: typeParams)
	    		{
	    			Definition p = new LocalDefinition(
	    				pname.location, pname, NameScope.NAMES, new ParameterType(pname));

	    			p.markUsed();
	    			defs.add(p);
	    		}

	    		FlatCheckedEnvironment params =	new FlatCheckedEnvironment(defs, env, NameScope.NAMES);
				type = type.typeResolve(params, null);
				TypeComparator.checkComposeTypes(type, params, false);
				
				Definition def = from.exportdefs.findName(name, NameScope.NAMES);
				Type act = def.getType();
				
				if (def instanceof ExplicitFunctionDefinition)
				{
					ExplicitFunctionDefinition efd = (ExplicitFunctionDefinition)def;
					
					if (efd.typeParams == null)
					{
						report(3352, "Imported " + name + " function has no type paramaters");
					}
					else if (!efd.typeParams.toString().equals(typeParams.toString()))
					{
						report(3353, "Imported " + name + " function type parameters incorrect");
						detail2("Imported", typeParams, "Actual", efd.typeParams);
					}
					
					if (act != null && !act.toString().equals(type.toString()))
					{
						report(3184, "Imported " + name + " function type incorrect");
						detail2("Imported", type, "Actual", act);
					}
				}
				else if (def instanceof ImplicitFunctionDefinition)
				{
					ImplicitFunctionDefinition ifd = (ImplicitFunctionDefinition)def;
					
					if (ifd.typeParams == null)
					{
						report(3352, "Imported " + name + " function has no type paramaters");
					}
					else if (!ifd.typeParams.toString().equals(typeParams.toString()))
					{
						report(3353, "Imported " + name + " function type parameters incorrect");
						detail2("Imported", typeParams, "Actual", ifd.typeParams);
					}
					
					if (act != null && !act.toString().equals(type.toString()))
					{
						report(3184, "Imported " + name + " function type incorrect");
						detail2("Imported", type, "Actual", act);
					}
				}
			}
		}
	}

	@Override
	public String toString()
	{
		return "import function " + name +
				(typeParams == null ? "" : "[" + typeParams + "]") +
				(renamed == null ? "" : " renamed " + renamed.name) +
				(type == null ? "" : ":" + type);
	}
}
