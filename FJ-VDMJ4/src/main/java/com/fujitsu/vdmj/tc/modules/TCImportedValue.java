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

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCImportedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCRenamedDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCImportedValue extends TCImport
{
	private static final long serialVersionUID = 1L;
	public TCType type;

	public TCImportedValue(TCNameToken name, TCType type, TCNameToken renamed)
	{
		super(name, renamed);
		this.type = type;
	}

	@Override
	public TCDefinitionList getDefinitions(TCModule module)
	{
		TCDefinitionList list = new TCDefinitionList();
		from = module;
		TCDefinition expdef = from.exportdefs.findName(name, NameScope.NAMES);

		if (expdef == null)
		{
			report(3193, "No export declared for import of value " + name + " from " + from.name);
		}
		else
		{
			if (renamed != null)
			{
				expdef = new TCRenamedDefinition(renamed, expdef);
			}
			else
			{
				expdef = new TCImportedDefinition(name.getLocation(), expdef);
			}

			list.add(expdef);
		}

		return list;
	}

	@Override
	public String toString()
	{
		return "import value " +
				(renamed == null ? "" : " renamed " + renamed.getName()) +
				(type == null ? "" : ":" + type);
	}

	@Override
	public void typeCheck(Environment env)
	{
		TCDefinition expdef = null;
		
		if (from != null)
		{
			expdef = from.exportdefs.findName(name, NameScope.NAMES);
			checkKind(expdef);
		}
		
		if (type != null)
		{
			type = type.typeResolve(env, null);
			TypeComparator.checkComposeTypes(type, env, false);
			
			if (from != null && expdef != null)
			{
    			expdef = from.exportdefs.findName(name, NameScope.NAMES);
    
    			if (expdef != null)
    			{
        			TCType exptype = expdef.getType().typeResolve(env, null);
    
        			if (!TypeComparator.compatible(type, exptype))
        			{
        				report(3194, "Type of value import " + name + " does not match export from " + from.name);
        				detail2("Import", type.toDetailedString(), "Export", exptype.toDetailedString());
        			}
    			}
			}
		}
	}

	@Override
	public boolean isExpectedKind(TCDefinition def)
	{
		return def.isValueDefinition();
	}

	@Override
	public String kind()
	{
		return "value";
	}
}
