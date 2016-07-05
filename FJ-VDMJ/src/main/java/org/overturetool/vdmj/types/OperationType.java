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
import org.overturetool.vdmj.definitions.ClassDefinition;
import org.overturetool.vdmj.definitions.StateDefinition;
import org.overturetool.vdmj.definitions.TypeDefinition;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.TypeCheckException;
import org.overturetool.vdmj.util.Utils;

public class OperationType extends Type
{
	private static final long serialVersionUID = 1L;
	public TypeList parameters;
	public Type result;
	public boolean pure;

	public OperationType(LexLocation location, TypeList parameters, Type result)
	{
		super(location);
		this.parameters = parameters;
		this.result = result;
		this.pure = false;
	}

	public OperationType(LexLocation location)	// Create "() ==> ()"
	{
		super(location);
		this.parameters = new TypeList();
		this.result = new VoidType(location);
		this.pure = false;
	}

	public FunctionType getPreType(
		StateDefinition state, ClassDefinition classname, boolean isStatic)
	{
		if (state != null)
		{
			TypeList params = new TypeList();
			params.addAll(parameters);
			params.add(new UnresolvedType(state.name));
			return new FunctionType(location, false, params, new BooleanType(location));
		}
		else if (classname != null && !isStatic)
		{
			TypeList params = new TypeList();
			params.addAll(parameters);
			params.add(new UnresolvedType(classname.name));
			return new FunctionType(location, false, params, new BooleanType(location));
		}
		else
		{
			return new FunctionType(location, false, parameters, new BooleanType(location));
		}
	}

	public FunctionType getPostType(
		StateDefinition state, ClassDefinition classname, boolean isStatic)
	{
		TypeList params = new TypeList();
		params.addAll(parameters);

		if (!(result instanceof VoidType))
		{
			params.add(result);
		}

		if (state != null)
		{
			params.add(new UnresolvedType(state.name));
			params.add(new UnresolvedType(state.name));
		}
		else if (classname != null)
		{
			params.add(
				new MapType(location,
					new SeqType(location, new CharacterType(location)),
					new UnknownType(location)));
			
			if (!isStatic)
			{
				params.add(new UnresolvedType(classname.name));
			}
		}

		return new FunctionType(location, false, params, new BooleanType(location));
	}

	@Override
	public boolean isOperation(LexLocation from)
	{
		return true;
	}

	@Override
	public OperationType getOperation()
	{
		return this;
	}
	
	public boolean isPure()
	{
		return pure;
	}
	
	public void setPure(boolean p)
	{
		this.pure = p;
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }

		for (Type type: parameters)
		{
			type.unResolve();
		}

		result.unResolve();
	}

	@Override
	public OperationType typeResolve(Environment env, TypeDefinition root)
	{
		if (resolved) return this; else { resolved = true; }

		TypeList fixed = new TypeList();
		TypeCheckException problem = null;

		for (Type type: parameters)
		{
			try
			{
				fixed.add(type.typeResolve(env, root));
			}
			catch (TypeCheckException e)
			{
				if (problem == null)
				{
					problem = e;
				}
				else
				{
					// Add extra messages to the exception for each parameter
					problem.addExtra(e);
				}

				fixed.add(new UnknownType(location));	// Parameter count must be right
			}
		}

		try
		{
			parameters = fixed;
			result = result.typeResolve(env, root);
		}
		catch (TypeCheckException e)
		{
			if (problem == null)
			{
				problem = e;
			}
			else
			{
				problem.addExtra(e);
			}
		}
		
		if (problem != null)
		{
			unResolve();
			throw problem;
		}

		return this;
	}

	@Override
	public String toDisplay()
	{
		String params = (parameters.isEmpty() ?
						"()" : Utils.listToString(parameters, " * "));
		return "(" + params + " ==> " + result + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (!(other instanceof OperationType))
		{
			return false;
		}

		OperationType oother = (OperationType)other;
		return (result.equals(oother.result) &&
				parameters.equals(oother.parameters));
	}

	@Override
	public int hashCode()
	{
		return parameters.hashCode() + result.hashCode();
	}

	@Override
	public boolean narrowerThan(AccessSpecifier accessSpecifier)
	{
		for (Type t: parameters)
		{
			if (t.narrowerThan(accessSpecifier))
			{
				return true;
			}
		}

		return result.narrowerThan(accessSpecifier);
	}
	
	@Override
	public TypeList getComposeTypes()
	{
		TypeList list = new TypeList();
		list.addAll(parameters.getComposeTypes());
		list.addAll(result.getComposeTypes());
		return list;
	}
}
