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

package com.fujitsu.vdmj.tc.modules;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCExportedFunction extends TCExport
{
	private static final long serialVersionUID = 1L;
	public final TCNameList nameList;
	public final TCType type;
	public final TCNameList typeParams;

	public TCExportedFunction(LexLocation location, TCNameList nameList, TCType type, TCNameList typeParams)
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
	public TCDefinitionList getDefinition(TCDefinitionList actualDefs)
	{
		TCDefinitionList list = new TCDefinitionList();

		for (TCNameToken name: nameList)
		{
			TCDefinition def = actualDefs.findName(name, NameScope.NAMES);

			if (def != null)
			{
				list.add(def);
			}
		}

		return list;
	}

	@Override
	public TCDefinitionList getDefinition()
	{
		TCDefinitionList list = new TCDefinitionList();

		for (TCNameToken name: nameList)
		{
			list.add(new TCLocalDefinition(name.getLocation(), name, type));
		}

		return list;
	}

	@Override
	public void typeCheck(Environment env, TCDefinitionList actualDefs)
	{
		for (TCNameToken name: nameList)
		{
			TCDefinition def = actualDefs.findName(name, NameScope.NAMES);

			if (def == null)
			{
				report(3183, "Exported function " + name + " not defined in module");
			}
			else
			{
				TCType actualType = def.getType();

				if (typeParams != null)
				{
					if (def instanceof TCExplicitFunctionDefinition)
					{
						TCExplicitFunctionDefinition efd = (TCExplicitFunctionDefinition)def;
						FlatCheckedEnvironment params =	new FlatCheckedEnvironment(
							efd.getTypeParamDefinitions(), env, NameScope.NAMES);

						TCType resolved = type.typeResolve(params, null);
						
						if (efd.typeParams == null)
						{
							report(3352, "Exported " + name + " function has no type paramaters");
						}
						else if (!efd.typeParams.equals(typeParams))
						{
							report(3353, "Exported " + name + " function type parameters incorrect");
							detail2("Exported", typeParams, "Actual", efd.typeParams);
						}
						
						if (actualType != null && !actualType.toString().equals(resolved.toString()))
						{
							report(3184, "Exported " + name + " function type incorrect");
							detail2("Exported", resolved, "Actual", actualType);
						}
					}
					else if (def instanceof TCImplicitFunctionDefinition)
					{
						TCImplicitFunctionDefinition ifd = (TCImplicitFunctionDefinition)def;
						FlatCheckedEnvironment params =	new FlatCheckedEnvironment(
							ifd.getTypeParamDefinitions(), env, NameScope.NAMES);

						TCType resolved = type.typeResolve(params, null);
						
						if (ifd.typeParams == null)
						{
							report(3352, "Exported " + name + " function has no type paramaters");
						}
						else if (!ifd.typeParams.equals(typeParams))
						{
							report(3353, "Exported " + name + " function type parameters incorrect");
							detail2("Exported", typeParams, "Actual", ifd.typeParams);
						}
						
						if (actualType != null && !actualType.toString().equals(resolved.toString()))
						{
							report(3184, "Exported " + name + " function type incorrect");
							detail2("Exported", resolved, "Actual", actualType);
						}
					}
				}
				else
				{
					TCType resolved = type.typeResolve(env, null);
					
					// if (actualType != null && !TypeComparator.compatible(resolved, actualType))
					if (actualType != null && !actualType.equals(resolved))
					{
						report(3184, "Exported " + name + " function type incorrect");
						detail2("Exported", resolved, "Actual", actualType);
					}
				}
			}
		}
	}
}
