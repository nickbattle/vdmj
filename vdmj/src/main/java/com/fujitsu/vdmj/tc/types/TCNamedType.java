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
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.values.FunctionValue;

public class TCNamedType extends TCInvariantType
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken typename;
	public TCType type;

	public TCNamedType(TCNameToken typename, TCType type)
	{
		super(typename.getLocation());

		this.typename = typename;
		this.type = type;
	}
	
	@Override
	public TCNamedType copy(boolean maximal)
	{
		TCNamedType namedType = new TCNamedType(typename, type);
		namedType.setInvariant(invdef);
		namedType.setEquality(eqdef);
		namedType.setOrder(orddef);
		namedType.setMaximal(maximal);
		return namedType;
	}

	@Override
	public boolean isType(Class<? extends TCType> typeclass, LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isType(typeclass, location);
	}

	@Override
	public boolean isUnion(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isUnion(location);
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }
		type.unResolve();
	}

	@Override
	public TCType typeResolve(Environment env)
	{
		if (resolved) return this; else resolved = true;

		try
		{
			type = type.typeResolve(env);
			return this;
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	@Override
	public boolean isSeq(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isSeq(location);
	}

	@Override
	public boolean isSet(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isSet(location);
	}

	@Override
	public boolean isMap(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isMap(location);
	}

	@Override
	public boolean isRecord(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isRecord(from);
	}

	@Override
	public boolean isTag()
	{
		return false;
	}

	@Override
	public boolean isClass(Environment env)
	{
		if (opaque) return false;
		return type.isClass(env);
	}

	@Override
	public boolean isNumeric(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isNumeric(location);
	}

	@Override
	public boolean isOrdered(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		
		if (orddef != null && !maximal)
		{
			return true;
		}
		else
		{
			return type.isOrdered(location);
		}
	}
	
	@Override
	public boolean isEq(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		
		if (eqdef != null && !maximal)
		{
			return true;
		}
		else
		{
			return type.isEq(location);
		}
	}

	@Override
	public boolean isProduct(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isProduct(location);
	}

	@Override
	public boolean isProduct(int n, LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isProduct(n, location);
	}

	@Override
	public boolean isFunction(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return type.isFunction(location);
	}

	@Override
	public boolean isOperation(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
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
	public TCClassType getClassType(Environment env)
	{
		return type.getClassType(env);
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
	public FunctionValue getEquality(Context ctxt)
	{
		if (eqdef != null)
		{
			return findFunction(eqdef, ctxt);
		}
		else if (type instanceof TCInvariantType)
		{
			TCInvariantType it = (TCInvariantType)type;
			return it.getEquality(ctxt);
		}
		
		return null;
	}

	@Override
	public FunctionValue getOrder(Context ctxt)
	{
		if (orddef != null)
		{
			return findFunction(orddef, ctxt);
		}
		else if (type instanceof TCInvariantType)
		{
			TCInvariantType it = (TCInvariantType)type;
			return it.getOrder(ctxt);
		}
		
		return null;
	}

	@Override
	public TCOperationType getOperation()
	{
		return type.getOperation();
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other instanceof TCNamedType)
		{
			TCNamedType nother = (TCNamedType)other;
			return typename.equals(nother.typename);
		}

		return false;
	}

	@Override
	public int compareTo(TCType other)
	{
		if (other instanceof TCNamedType)
		{
			TCNamedType nt = (TCNamedType)other;
    		String n1 = typename.getModule() + typename.getName();
    		String n2 = nt.typename.getModule() + nt.typename.getName();
    		return n1.compareTo(n2);
		}
		else
		{
			return super.compareTo(other);
		}
	}

	@Override
	public int hashCode()
	{
		return typename.hashCode();
	}

	@Override
	public String toDetailedString()
	{
		return type.toString();
	}

	@Override
	public String toDisplay()
	{
		return typename.toString() + (maximal ? "!" : "") + (opaque ? " /* opaque */" : "");
	}
	
	@Override
	public String toExplicitString(LexLocation from)
	{
		if (typename.getLocation().module.equals(from.module))
		{
			return toString();
		}
		else
		{
			return typename.getExplicit(true).toString();
		}
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		if (inNarrower)
		{
			return false;
		}

		inNarrower = true;
		boolean result = false;
		
		if (definitions != null)
		{
			for (TCDefinition d: definitions)
			{
				if (d.accessSpecifier.narrowerThan(accessSpecifier))
				{
					result = true;
					break;
				}
			}
		}
		else if (type.definitions == null)
		{
			result = type.narrowerThan(accessSpecifier) || super.narrowerThan(accessSpecifier);
		}
		else
		{
			for (TCDefinition d: type.definitions)
			{
				if (d.accessSpecifier.narrowerThan(accessSpecifier))
				{
					result = true;
					break;
				}
			}
		}
		
		inNarrower = false;
		return result;
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNamedType(this, arg);
	}
}
