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
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class TCExportedOperation extends TCExport
{
	private static final long serialVersionUID = 1L;
	public final TCNameList nameList;
	public TCType type;

	public TCExportedOperation(LexLocation location, TCNameList nameList, TCType type)
	{
		super(location);
		this.nameList = nameList;
		this.type = type;
	}

	@Override
	public String toString()
	{
		return "export operation " + Utils.listToString(nameList) + ":" + type;
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
		type = type.typeResolve(env, null);
		
		for (TCNameToken name: nameList)
		{
			TCDefinition actual = actualDefs.findName(name, NameScope.GLOBAL);

			if (actual == null)
			{
				report(3185, "Exported operation " + name + " not defined in module");
			}
			else
			{
    			TCType actualType = actual.getType();
    			
				if (actualType != null && !TypeComparator.compatible(type, actualType))
				{
					report(3186, "Exported operation type does not match actual type");
					detail2("Exported", type, "Actual", actualType);
				}
			}
		}
	}
}
