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
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.util.Utils;

public class TCOperationType extends TCType
{
	private static final long serialVersionUID = 1L;
	public TCTypeList parameters;
	public TCType result;
	public boolean pure;

	public TCOperationType(LexLocation location, TCTypeList parameters, TCType result)
	{
		super(location);
		this.parameters = parameters;
		this.result = result;
		this.pure = false;
	}

	public TCOperationType(LexLocation location)	// Create "() ==> ()"
	{
		super(location);
		this.parameters = new TCTypeList();
		this.result = new TCVoidType(location);
		this.pure = false;
	}

	public TCFunctionType getPreType(
		TCStateDefinition state, TCClassDefinition classname, boolean isStatic)
	{
		if (state != null)
		{
			TCTypeList params = new TCTypeList();
			params.addAll(parameters);
			params.add(new TCUnresolvedType(state.name));
			return new TCFunctionType(location, params, false, new TCBooleanType(location));
		}
		else if (classname != null && !isStatic)
		{
			TCTypeList params = new TCTypeList();
			params.addAll(parameters);
			params.add(new TCUnresolvedType(classname.name));
			return new TCFunctionType(location, params, false, new TCBooleanType(location));
		}
		else
		{
			return new TCFunctionType(location, parameters, false, new TCBooleanType(location));
		}
	}

	public TCFunctionType getPostType(
		TCStateDefinition state, TCClassDefinition classname, boolean isStatic)
	{
		TCTypeList params = new TCTypeList();
		params.addAll(parameters);

		if (!(result instanceof TCVoidType))
		{
			params.add(result);
		}

		if (state != null)
		{
			params.add(new TCUnresolvedType(state.name));
			params.add(new TCUnresolvedType(state.name));
		}
		else if (classname != null)
		{
			params.add(
				new TCMapType(location,
					new TCSeqType(location, new TCCharacterType(location)),
					new TCUnknownType(location)));
			
			if (!isStatic)
			{
				params.add(new TCUnresolvedType(classname.name));
			}
		}

		return new TCFunctionType(location, params, false, new TCBooleanType(location));
	}

	@Override
	public boolean isOperation(LexLocation from)
	{
		return true;
	}

	@Override
	public TCOperationType getOperation()
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

		for (TCType type: parameters)
		{
			type.unResolve();
		}

		result.unResolve();
	}

	@Override
	public TCOperationType typeResolve(Environment env, TCTypeDefinition root)
	{
		if (resolved) return this; else { resolved = true; }

		TCTypeList fixed = new TCTypeList();
		TypeCheckException problem = null;

		for (TCType type: parameters)
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

				fixed.add(new TCUnknownType(location));	// Parameter count must be right
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

		if (!(other instanceof TCOperationType))
		{
			return false;
		}

		TCOperationType oother = (TCOperationType)other;
		return (result.equals(oother.result) &&
				parameters.equals(oother.parameters));
	}

	@Override
	public int hashCode()
	{
		return parameters.hashCode() + result.hashCode();
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		for (TCType t: parameters)
		{
			if (t.narrowerThan(accessSpecifier))
			{
				return true;
			}
		}

		return result.narrowerThan(accessSpecifier);
	}
	
	@Override
	public TCTypeList getComposeTypes()
	{
		TCTypeList list = new TCTypeList();
		list.addAll(parameters.getComposeTypes());
		list.addAll(result.getComposeTypes());
		return list;
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseOperationType(this, arg);
	}

	@Override
	public TCType clone()
	{
		return new TCOperationType(location, parameters.clone(), result.clone());
	}
}
