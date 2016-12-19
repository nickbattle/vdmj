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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCFieldDesignator extends TCStateDesignator
{
	private static final long serialVersionUID = 1L;
	public final TCStateDesignator object;
	public final TCIdentifierToken field;
	private TCNameToken objectfield = null;

	public TCFieldDesignator(TCStateDesignator object, TCIdentifierToken field)
	{
		super(object.location);
		this.object = object;
		this.field = field;
	}

	@Override
	public String toString()
	{
		return object + "." + field;
	}

	@Override
	public TCType typeCheck(Environment env)
	{
		TCType type = object.typeCheck(env);
		TCTypeSet result = new TCTypeSet();
		boolean unique = !type.isUnion(location);

		if (type.isRecord(location))
		{
    		TCRecordType rec = type.getRecord();
    		TCField rf = rec.findField(field.getName());

    		if (rf == null)
    		{
    			concern(unique, 3246, "Unknown field name, '" + field + "'");
    			result.add(new TCUnknownType(field.getLocation()));
    		}
    		else
    		{
    			result.add(rf.type);
    		}
		}

		if (type.isClass(env))
		{
			TCClassType ctype = type.getClassType(env);
			String cname = ctype.name.getName();

			objectfield = new TCNameToken(location, cname, field.getName(), false);
			TCDefinition fdef = ctype.classdef.findName(objectfield, NameScope.STATE);

			if (fdef == null)
			{
				field.concern(unique, 3260, "Unknown class field name, '" + field + "'");
				result.add(new TCUnknownType(location));
			}
			else if (TCClassDefinition.isAccessible(env, fdef, false))
   			{
				result.add(fdef.getType());
			}
			else
   			{
   				field.concern(unique,
   					3092, "Inaccessible member " + field.getName() + " of class " + cname);
   				result.add(new TCUnknownType(location));
   			}
		}

		if (result.isEmpty())
		{
			report(3245, "Field assignment is not of a record or object type");
			detail2("Expression", object, "Type", type);
			return new TCUnknownType(field.getLocation());
		}

		return result.getType(location);
	}
}
