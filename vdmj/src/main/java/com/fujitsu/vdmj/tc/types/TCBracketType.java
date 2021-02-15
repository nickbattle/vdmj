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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

public class TCBracketType extends TCType
{
	private static final long serialVersionUID = 1L;

	public TCType type;

	public TCBracketType(TCType type)
	{
		super(type.location);
		this.type = type;
	}

	@Override
	public TCType isType(String typename, LexLocation from)
	{
		return type.isType(typename, location);
	}

	@Override
	public boolean isType(Class<? extends TCType> typeclass, LexLocation from)
	{
		return type.isType(typeclass, location);
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }
		type.unResolve();
	}

	@Override
	public TCType typeResolve(Environment env, TCTypeDefinition root)
	{
		if (resolved) return type; else { resolved = true; }

		try
		{
			do
			{
				type = type.typeResolve(env, root);
			}
			while (type instanceof TCBracketType);

			type = type.typeResolve(env, root);
			return type;
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		return type.narrowerThan(accessSpecifier);
	}

	@Override
	public String toDisplay()
	{
		return "(" + type + ")";
	}

	@Override
	public boolean isUnion(LexLocation from)
	{
		return type.isUnion(location);
	}

	@Override
	public boolean isSeq(LexLocation from)
	{
		return type.isSeq(location);
	}

	@Override
	public boolean isSet(LexLocation from)
	{
		return type.isSet(location);
	}

	@Override
	public boolean isMap(LexLocation from)
	{
		return type.isMap(location);
	}

	@Override
	public boolean isRecord(LexLocation from)
	{
		return type.isRecord(from);
	}

	@Override
	public boolean isTag()
	{
		return type.isTag();
	}

	@Override
	public boolean isNumeric(LexLocation from)
	{
		return type.isNumeric(location);
	}

	@Override
	public boolean isProduct(LexLocation from)
	{
		return type.isProduct(location);
	}

	@Override
	public boolean isProduct(int n, LexLocation from)
	{
		return type.isProduct(n, location);
	}

	@Override
	public boolean isFunction(LexLocation from)
	{
		return type.isFunction(location);
	}

	@Override
	public boolean isOperation(LexLocation from)
	{
		return type.isOperation(location);
	}

	@Override
	public TCUnionType getUnion()
	{
		return type.getUnion();
	}

	@Override
	public TCSeqType getSeq()
	{
		return type.getSeq();
	}

	@Override
	public TCSetType getSet()
	{
		return type.getSet();
	}

	@Override
	public TCMapType getMap()
	{
		return type.getMap();
	}

	@Override
	public TCRecordType getRecord()
	{
		return type.getRecord();
	}

	@Override
	public TCNumericType getNumeric()
	{
		return type.getNumeric();
	}

	@Override
	public TCProductType getProduct()
	{
		return type.getProduct();
	}

	@Override
	public TCProductType getProduct(int n)
	{
		return type.getProduct(n);
	}

	@Override
	public TCFunctionType getFunction()
	{
		return type.getFunction();
	}

	@Override
	public TCOperationType getOperation()
	{
		return type.getOperation();
	}

	@Override
	public boolean equals(Object other)
	{
		return type.equals(other);
	}

	@Override
	public int hashCode()
	{
		return type.hashCode();
	}
	
	@Override
	public TCTypeList getComposeTypes()
	{
		return type.getComposeTypes();
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBracketType(this, arg);
	}
}
