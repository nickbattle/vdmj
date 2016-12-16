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
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCObjectFieldDesignator extends TCObjectDesignator
{
	private static final long serialVersionUID = 1L;
	public final TCObjectDesignator object;
	public final TCNameToken classname;
	public final TCIdentifierToken fieldname;

	private TCNameToken field = null;

	public TCObjectFieldDesignator(TCObjectDesignator object, TCNameToken classname, TCIdentifierToken fieldname)
	{
		super(object.location);
		this.object = object;
		this.classname = classname;
		this.fieldname = fieldname;
	}

	@Override
	public String toString()
	{
		return object + "." + (classname == null ? fieldname : classname);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers)
	{
		TCType type = object.typeCheck(env, qualifiers);
		TCTypeSet result = new TCTypeSet();
		boolean unique = !type.isUnion(location);

		if (type.isClass(env))
		{
			TCClassType ctype = type.getClassType(env);
			
			if (classname == null)
			{
				field = new TCNameToken(
					fieldname.getLocation(), ctype.name.getName(), fieldname.getName(), false);
			}
			else
			{
				field = classname;
			}

			field.setTypeQualifier(qualifiers);
			TCDefinition fdef = ctype.classdef.findName(field, NameScope.NAMESANDSTATE);

			if (fdef == null)
			{
				concern(unique, 3260, "Unknown class member name, '" + field + "'");
				result.add(new TCUnknownType(location));
			}
			else if (!TCClassDefinition.isAccessible(env, fdef, false))
			{
				concern(unique, 3260, "Inaccessible class member name, '" + field + "'");
				result.add(new TCUnknownType(location));
			}
			else
			{
				result.add(fdef.getType());
			}
		}

		if (type.isRecord(location))
		{
			String sname = (fieldname != null) ? fieldname.getName() : classname.toString();
			TCRecordType rec = type.getRecord();
			TCField rf = rec.findField(sname);

			if (rf == null)
			{
				concern(unique, 3261, "Unknown field name, '" + sname + "'");
				result.add(new TCUnknownType(location));
			}
			else
			{
				result.add(rf.type);
			}
		}

		if (result.isEmpty())
		{
			report(3262, "Field assignment is not of a class or record type");
			detail2("Expression", object, "Type", type);
			return new TCUnknownType(location);
		}

		return result.getType(location);
	}
}
