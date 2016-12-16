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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCUnknownType extends TCType
{
	private static final long serialVersionUID = 1L;

	public TCUnknownType(LexLocation location)
	{
		super(location);
	}

	@Override
	public TCType isType(String typename, LexLocation from)
	{
		return null;	// Isn't any particular type?
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		return false;
	}

	@Override
	public boolean isType(Class<? extends TCType> typeclass, LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isUnknown(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isSeq(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isSet(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isMap(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isRecord(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isTag()
	{
		return true;
	}

	@Override
	public boolean isClass(Environment env)
	{
		return true;		// Too much trouble?
	}

	@Override
	public boolean isNumeric(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isProduct(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isProduct(int n, LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isFunction(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isOperation(LexLocation from)
	{
		return true;
	}

	@Override
	public TCSeqType getSeq()
	{
		return new TCSeqType(location, new TCUnknownType(location));	// seq of ?
	}

	@Override
	public TCSetType getSet()
	{
		return new TCSetType(location, new TCUnknownType(location));	// set of ?
	}

	@Override
	public TCMapType getMap()
	{
		return new TCMapType(location);	// Unknown |-> Unknown
	}

	@Override
	public TCRecordType getRecord()
	{
		return new TCRecordType(location, new TCFieldList());
	}

	@Override
	public TCClassType getClassType(Environment env)
	{
		return new TCClassType(location, new TCClassDefinition());
	}

	@Override
	public TCNumericType getNumeric()
	{
		return new TCRealType(location);
	}

	@Override
	public TCProductType getProduct()
	{
		return new TCProductType(location, new TCTypeList());
	}

	@Override
	public TCProductType getProduct(int n)
	{
		TCTypeList tl = new TCTypeList();

		for (int i=0; i<n; i++)
		{
			tl.add(new TCUnknownType(location));
		}

		return new TCProductType(location, tl);
	}

	@Override
	public TCFunctionType getFunction()
	{
		return new TCFunctionType(location, new TCTypeList(), true, new TCUnknownType(location));
	}

	@Override
	public TCOperationType getOperation()
	{
		return new TCOperationType(
			location, new TCTypeList(), new TCUnknownType(location));
	}

	@Override
	public boolean equals(Object other)
	{
		return true;	// Assume OK to avoid error explosions
	}

	@Override
	public String toDisplay()
	{
		return "?";
	}
}
