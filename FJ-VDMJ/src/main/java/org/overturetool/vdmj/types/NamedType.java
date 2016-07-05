/*******************************************************************************
 *
 *	Copyright (c) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.types;

import org.overturetool.vdmj.Settings;
import org.overturetool.vdmj.definitions.AccessSpecifier;
import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.TypeDefinition;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.TypeCheckException;
import org.overturetool.vdmj.values.InvariantValue;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;

public class NamedType extends InvariantType
{
	private static final long serialVersionUID = 1L;
	public final LexNameToken typename;
	public Type type;

	public NamedType(LexNameToken typename, Type type)
	{
		super(typename.location);

		this.typename = typename;
		this.type = type;
	}

	@Override
	public Type isType(String other, LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return null;
		return type.isType(other, location);
	}

	@Override
	public boolean isType(Class<? extends Type> typeclass, LexLocation from)
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
	public Type typeResolve(Environment env, TypeDefinition root)
	{
		if (resolved) return this; else resolved = true;

		try
		{
			type = type.typeResolve(env, root);
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
	public UnionType getUnion()
	{
		return type.getUnion();
	}

	@Override
	public SeqType getSeq()
	{
		return type.getSeq();
	}

	@Override
	public SetType getSet()
	{
		return type.getSet();
	}

	@Override
	public MapType getMap()
	{
		return type.getMap();
	}

	@Override
	public RecordType getRecord()
	{
		return type.getRecord();
	}

	@Override
	public ClassType getClassType(Environment env)
	{
		return type.getClassType(env);
	}

	@Override
	public NumericType getNumeric()
	{
		return type.getNumeric();
	}

	@Override
	public ProductType getProduct()
	{
		return type.getProduct();
	}

	@Override
	public ProductType getProduct(int n)
	{
		return type.getProduct(n);
	}

	@Override
	public FunctionType getFunction()
	{
		return type.getFunction();
	}

	@Override
	public OperationType getOperation()
	{
		return type.getOperation();
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other instanceof NamedType)
		{
			NamedType nother = (NamedType)other;
			return typename.equals(nother.typename);
		}

		return false;
	}

	@Override
	public int compareTo(Type other)
	{
		if (other instanceof NamedType)
		{
			NamedType nt = (NamedType)other;
    		String n1 = typename.getExplicit(true).toString();
    		String n2 = nt.typename.getExplicit(true).toString();
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
		return typename.toString();
	}

	@Override
	public ValueList getAllValues(Context ctxt) throws ValueException
	{
		ValueList raw = type.getAllValues(ctxt);
		boolean checks = Settings.invchecks;
		Settings.invchecks = true;

		ValueList result = new ValueList();
		for (Value v: raw)
		{
			try
			{
				result.add(new InvariantValue(this, v, ctxt));
			}
			catch (ValueException e)
			{
				// Raw value not in type because of invariant
			}
		}

		Settings.invchecks = checks;
		return result;
	}
	
	@Override
	public boolean narrowerThan(AccessSpecifier accessSpecifier)
	{
		if (inNarrower)
		{
			return false;
		}

		inNarrower = true;
		boolean result = false;
		
		if (definitions != null)
		{
			for (Definition d: definitions)
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
			for (Definition d: type.definitions)
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
}
