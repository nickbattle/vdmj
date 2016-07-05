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

import org.overturetool.vdmj.definitions.AccessSpecifier;
import org.overturetool.vdmj.definitions.TypeDefinition;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.values.NilValue;
import org.overturetool.vdmj.values.ValueList;

public class OptionalType extends Type
{
	private static final long serialVersionUID = 1L;
	public Type type;

	public OptionalType(LexLocation location, Type type)
	{
		super(location);

		while (type instanceof OptionalType)
		{
			type = ((OptionalType)type).type;
		}

		this.type = type;
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }
		type.unResolve();
	}

	@Override
	public OptionalType typeResolve(Environment env, TypeDefinition root)
	{
		if (resolved) return this; else { resolved = true; }
		type = type.typeResolve(env, root);
		if (root != null) root.infinite = false;	// Could be nil
		return this;
	}

	@Override
	public Type polymorph(LexNameToken pname, Type actualType)
	{
		return new OptionalType(location, type.polymorph(pname, actualType));
	}

	@Override
	public boolean narrowerThan(AccessSpecifier accessSpecifier)
	{
		return type.narrowerThan(accessSpecifier);
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof OptionalType)
		{
			OptionalType oo = (OptionalType)other;
			return type.equals(oo.type);
		}
		
		return false;
	}

	@Override
	public int hashCode()
	{
		return type.hashCode();
	}

	@Override
	public Type isType(String typename, LexLocation from)
	{
		return type.isType(typename, location);
	}

	@Override
	public boolean isType(Class<? extends Type> typeclass, LexLocation from)
	{
		if (typeclass.equals(VoidType.class))
		{
			return false;	// Optionals are never void
		}
		
		return type.isType(typeclass, location);
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
	public boolean isClass(Environment env)
	{
		return type.isClass(env);
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
	public String toDisplay()
	{
		return "[" + type + "]";
	}

	@Override
	public ValueList getAllValues(Context ctxt) throws ValueException
	{
		ValueList list = type.getAllValues(ctxt);
		list.add(new NilValue());
		return list;
	}
	
	@Override
	public TypeList getComposeTypes()
	{
		return type.getComposeTypes();
	}
}
