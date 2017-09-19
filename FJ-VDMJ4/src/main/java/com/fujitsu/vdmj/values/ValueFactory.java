/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.values;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * Create Values from the arguments passed, which is useful from native Java implementations.
 */
public class ValueFactory
{
	public static RecordValue mkRecord(String module, String name, Value ...args) throws ValueException
	{
		TCType type = getType(module, name);
		
		if (type instanceof TCRecordType)
		{
    		TCRecordType r = (TCRecordType)type;
    		ValueList l = new ValueList();
    		
    		for (int a=0; a<args.length; a++)
    		{
    			l.add(args[a]);
    		}
    		
    		return new RecordValue(r, l, Interpreter.getInstance().getInitialContext());
		}
		else
		{
			throw new ValueException(69, "Definition " + module + "`" + name +
				" is " + type.getClass().getSimpleName() + " not TCRecordType", null);
		}
	}

	public static InvariantValue mkInvariant(String module, String name, Value x) throws ValueException
	{
		TCType type = getType(module, name);
		
		if (type instanceof TCNamedType)
		{
			TCNamedType r = (TCNamedType)type;
			return new InvariantValue(r, x, Interpreter.getInstance().getInitialContext());
		}
		else
		{
			throw new ValueException(69, "Definition " + module + "`" + name +
				" is " + type.getClass().getSimpleName() + " not TCNamedType", null);
		}
	}
	
	private static TCType getType(String module, String name) throws ValueException
	{
		Interpreter i = Interpreter.getInstance();
		TCNameToken tcname = new TCNameToken(new LexLocation(), module, name);
		TCDefinition def = i.getGlobalEnvironment().findType(tcname, i.getDefaultName());
		
		if (def == null)
		{
			throw new ValueException(70, "Definition " + tcname.getExplicit(true) + " not found", null);
		}
		
		return def.getType();
	}
}
