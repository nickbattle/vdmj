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
import com.fujitsu.vdmj.tc.definitions.TCBUSClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCCPUClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCImportedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInheritedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCRenamedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

public class TCUnresolvedType extends TCType
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken typename;

	public TCUnresolvedType(TCNameToken typename)
	{
		super(typename.getLocation());
		this.typename = typename;
	}

	@Override
	public TCType isType(String other, LexLocation from)
	{
		return typename.getName().equals(other) ? this : null;
	}

	@Override
	public TCType typeResolve(Environment env, TCTypeDefinition root)
	{
		TCType deref = dereference(env, root);

		if (!(deref instanceof TCClassType))
		{
			deref = deref.typeResolve(env, root);
		}

		return deref;
	}

	private TCType dereference(Environment env, TCTypeDefinition root)
	{
		TCDefinition def = env.findType(typename, location.module);

		if (def == null)
		{
			throw new TypeCheckException(
				"Unable to resolve type name '" + typename + "'", location);
		}

		if (def instanceof TCImportedDefinition)
		{
			TCImportedDefinition idef = (TCImportedDefinition)def;
			def = idef.def;
		}

		if (def instanceof TCRenamedDefinition)
		{
			TCRenamedDefinition rdef = (TCRenamedDefinition)def;
			def = rdef.def;
		}

		if (!(def instanceof TCTypeDefinition) &&
			!(def instanceof TCStateDefinition) &&
			!(def instanceof TCClassDefinition) &&
			!(def instanceof TCInheritedDefinition))
		{
			report(3434, "'" + typename + "' is not the name of a type definition");
		}

		if (def instanceof TCTypeDefinition)
		{
			if (def == root)
			{
				root.infinite = true;
			}
		}

		if ((def instanceof TCCPUClassDefinition ||
			 def instanceof TCBUSClassDefinition) && !env.isSystem())
		{
			report(3296, "Cannot use '" + typename + "' outside system class");
		}

		TCType r = def.getType();
		r.definitions = new TCDefinitionList(def);
		return r;
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other instanceof TCUnresolvedType)
		{
			TCUnresolvedType nother = (TCUnresolvedType)other;
			return typename.equals(nother.typename);
		}

		if (other instanceof TCNamedType)
		{
			TCNamedType nother = (TCNamedType)other;
			return typename.equals(nother.typename);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return typename.hashCode();
	}

	@Override
	public String toDisplay()
	{
		return "(unresolved " + typename.getExplicit(true) + ")";
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseUnresolvedType(this, arg);
	}
}
