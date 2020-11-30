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
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCImportedFunction extends TCImportedValue
{
	private static final long serialVersionUID = 1L;

	public final TCNameList typeParams;

	public TCImportedFunction(
		TCNameToken name, TCType type, TCNameList typeParams, TCNameToken renamed)
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
			TCDefinition expdef = null;
			
			if (from != null)
			{
				expdef = from.exportdefs.findName(name, NameScope.NAMES);
				checkKind(expdef);
			}
			
			if (type != null)
			{
	    		TCDefinitionList defs = new TCDefinitionList();

	    		for (TCNameToken pname: typeParams)
	    		{
	    			TCDefinition p = new TCLocalDefinition(pname.getLocation(),
	    				pname, new TCParameterType(pname));

	    			p.markUsed();
	    			defs.add(p);
	    		}

	    		FlatCheckedEnvironment params =	new FlatCheckedEnvironment(defs, env, NameScope.NAMES);
				type = type.typeResolve(params, null);
				TypeComparator.checkComposeTypes(type, params, false);
				
				if (expdef != null)
				{
    				TCType act = expdef.getType();
    				
    				if (expdef instanceof TCExplicitFunctionDefinition)
    				{
    					TCExplicitFunctionDefinition efd = (TCExplicitFunctionDefinition)expdef;
    					
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
    				else if (expdef instanceof TCImplicitFunctionDefinition)
    				{
    					TCImplicitFunctionDefinition ifd = (TCImplicitFunctionDefinition)expdef;
    					
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
	}

	@Override
	public String toString()
	{
		return "import function " + name +
				(typeParams == null ? "" : "[" + typeParams + "]") +
				(renamed == null ? "" : " renamed " + renamed.getName()) +
				(type == null ? "" : ":" + type);
	}

	@Override
	public boolean isExpectedKind(TCDefinition def)
	{
		return def.isFunction();
	}

	@Override
	public String kind()
	{
		return "function";
	}
}
