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
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.util.Utils;

public class ExportedFunction extends Export
{
	private static final long serialVersionUID = 1L;
	public final LexNameList nameList;
	public final Type type;
	public final LexNameList typeParams;

	public ExportedFunction(LexLocation location, LexNameList nameList, Type type, LexNameList typeParams)
	{
		super(location);
		this.nameList = nameList;
		this.type = type;
		this.typeParams = typeParams;
	}

	@Override
	public String toString()
	{
		return "export function " + Utils.listToString(nameList) + ":" + type;
	}

	@Override
	public DefinitionList getDefinition(DefinitionList actualDefs)
	{
		DefinitionList list = new DefinitionList();

		for (LexNameToken name: nameList)
		{
			Definition def = actualDefs.findName(name, NameScope.NAMES);

			if (def == null)
			{
				report(3183, "Exported function " + name + " not defined in module");
			}
			else
			{
				list.add(def);
			}
		}

		return list;
	}

	@Override
	public DefinitionList getDefinition()
	{
		DefinitionList list = new DefinitionList();

		for (LexNameToken name: nameList)
		{
			list.add(new LocalDefinition(name.location, name, NameScope.GLOBAL, type));
		}

		return list;
	}

	@Override
	public void typeCheck(Environment env, DefinitionList actualDefs)
	{
		Type resolved = type;
		
		if (type.toString().indexOf('@') < 0)	// Don't check polymorphic types here
		{
			resolved = type.typeResolve(env, null);
		}
		
		for (LexNameToken name: nameList)
		{
			Definition def = actualDefs.findName(name, NameScope.NAMES);

			if (def == null)
			{
				report(3183, "Exported function " + name + " not defined in module");
			}
			else
			{
				Type actualType = def.getType();

				if (typeParams != null)
				{
					if (def instanceof ExplicitFunctionDefinition)
					{
						ExplicitFunctionDefinition efd = (ExplicitFunctionDefinition)def;
						
						if (efd.typeParams == null)
						{
							report(3352, "Exported " + name + " function has no type paramaters");
						}
						else if (!efd.typeParams.equals(typeParams))
						{
							report(3353, "Exported " + name + " function type parameters incorrect");
							detail2("Exported", typeParams, "Actual", efd.typeParams);
						}
						
						if (actualType != null && !actualType.toString().equals(type.toString()))
						{
							report(3184, "Exported " + name + " function type incorrect");
							detail2("Exported", type, "Actual", actualType);
						}
					}
					else if (def instanceof ImplicitFunctionDefinition)
					{
						ImplicitFunctionDefinition ifd = (ImplicitFunctionDefinition)def;
						
						if (ifd.typeParams == null)
						{
							report(3352, "Exported " + name + " function has no type paramaters");
						}
						else if (!ifd.typeParams.equals(typeParams))
						{
							report(3353, "Exported " + name + " function type parameters incorrect");
							detail2("Exported", typeParams, "Actual", ifd.typeParams);
						}
						
						if (actualType != null && !actualType.toString().equals(type.toString()))
						{
							report(3184, "Exported " + name + " function type incorrect");
							detail2("Exported", type, "Actual", actualType);
						}
					}
				}
				else if (actualType != null && !TypeComparator.compatible(resolved, actualType))
				{
					report(3184, "Exported " + name + " function type incorrect");
					detail2("Exported", resolved, "Actual", actualType);
				}
			}
		}
	}
}
