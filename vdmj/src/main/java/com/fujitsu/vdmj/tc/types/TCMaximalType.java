/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

public class TCMaximalType extends TCType
{
	private static final long serialVersionUID = 1L;
	public TCInvariantType maxtype;
	
	public TCMaximalType(TCInvariantType base)
	{
		super(base.location);
		maxtype = base.copy();
		maxtype.setMaximal(true);
	}
	
	@Override
	protected String toDisplay()
	{
		return maxtype.toDisplay();
	}

	@Override
	public boolean isType(Class<? extends TCType> typeclass, LexLocation from)
	{
		return maxtype.isType(typeclass, location);
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }
		maxtype.unResolve();
	}

	@Override
	public TCType typeResolve(Environment env)
	{
		if (resolved) return maxtype; else { resolved = true; }

		try
		{
			maxtype = (TCInvariantType) maxtype.typeResolve(env);
			return this;
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
		return maxtype.narrowerThan(accessSpecifier);
	}
	
	@Override
	public boolean isMaximal()
	{
		return true;
	}

	@Override
	public boolean isUnion(LexLocation from)
	{
		return maxtype.isUnion(location);
	}

	@Override
	public boolean isSeq(LexLocation from)
	{
		return maxtype.isSeq(location);
	}

	@Override
	public boolean isSet(LexLocation from)
	{
		return maxtype.isSet(location);
	}

	@Override
	public boolean isMap(LexLocation from)
	{
		return maxtype.isMap(location);
	}

	@Override
	public boolean isRecord(LexLocation from)
	{
		return maxtype.isRecord(from);
	}

	@Override
	public boolean isTag()
	{
		return maxtype.isTag();
	}

	@Override
	public boolean isNumeric(LexLocation from)
	{
		return maxtype.isNumeric(location);
	}

	@Override
	public boolean isProduct(LexLocation from)
	{
		return maxtype.isProduct(location);
	}

	@Override
	public boolean isProduct(int n, LexLocation from)
	{
		return maxtype.isProduct(n, location);
	}

	@Override
	public boolean isFunction(LexLocation from)
	{
		return maxtype.isFunction(location);
	}

	@Override
	public boolean isOperation(LexLocation from)
	{
		return maxtype.isOperation(location);
	}

	@Override
	public TCUnionType getUnion()
	{
		return maxtype.getUnion();
	}

	@Override
	public TCSeqType getSeq()
	{
		return maxtype.getSeq();
	}

	@Override
	public TCSetType getSet()
	{
		return maxtype.getSet();
	}

	@Override
	public TCMapType getMap()
	{
		return maxtype.getMap();
	}

	@Override
	public TCRecordType getRecord()
	{
		return maxtype.getRecord();
	}

	@Override
	public TCNumericType getNumeric()
	{
		return maxtype.getNumeric();
	}

	@Override
	public TCProductType getProduct()
	{
		return maxtype.getProduct();
	}

	@Override
	public TCProductType getProduct(int n)
	{
		return maxtype.getProduct(n);
	}

	@Override
	public TCFunctionType getFunction()
	{
		return maxtype.getFunction();
	}

	@Override
	public TCOperationType getOperation()
	{
		return maxtype.getOperation();
	}

	@Override
	public boolean equals(Object other)
	{
		if (maxtype instanceof TCNamedType && other instanceof TCNamedType)
		{
			TCNamedType nt = (TCNamedType)maxtype;
			TCNamedType ot = (TCNamedType)other;
			
			return nt.type.equals(ot.type) &&
				(
					ot.maximal ||

					(nt.invdef != null) == (ot.invdef != null) &&
					(nt.eqdef != null) == (ot.eqdef != null) &&
					(nt.orddef != null) == (ot.orddef != null)
				);
		}
		else if (maxtype instanceof TCRecordType && other instanceof TCNamedType)
		{
			TCRecordType rt = (TCRecordType)maxtype;
			TCRecordType ot = (TCRecordType)other;
			
			return rt.name.equals(ot.name) &&
				(
					ot.maximal ||

					(rt.invdef != null) == (ot.invdef != null) &&
					(rt.eqdef != null) == (ot.eqdef != null) &&
					(rt.orddef != null) == (ot.orddef != null)
				);
		}
		else
		{
			return maxtype.equals(other);
		}
	}

	@Override
	public int hashCode()
	{
		return maxtype.hashCode();
	}
	
	@Override
	public TCTypeList getComposeTypes()
	{
		return maxtype.getComposeTypes();
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMaximalType(this, arg);
	}
}
