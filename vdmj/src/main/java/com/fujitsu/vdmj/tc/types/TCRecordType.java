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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.util.Utils;

public class TCRecordType extends TCInvariantType
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	public final TCFieldList fields;
	public final boolean composed;	// Created via "compose R of ... end"

	public TCRecordType(TCNameToken name, TCFieldList fields, boolean composed)
	{
		super(name.getLocation());
		this.name = name;
		this.fields = fields;
		this.composed = composed;
	}

	public TCRecordType(LexLocation location, TCFieldList fields)
	{
		super(location);
		this.name = new TCNameToken(location, "?", "?", false, false);
		this.fields = fields;
		this.composed = false;
	}
	
	@Override
	public TCRecordType copy(boolean maximal)
	{
		TCRecordType recordType = new TCRecordType(name, fields, composed);
		recordType.setInvariant(invdef);
		recordType.setEquality(eqdef);
		recordType.setOrder(orddef);
		recordType.setMaximal(maximal);
		return recordType;
	}

	public TCField findField(String tag)
	{
		for (TCField f: fields)
		{
			if (f.tag.equals(tag))
			{
				return f;
			}
		}

		return null;
	}

	@Override
	public boolean isRecord(LexLocation from)
	{
		if (opaque && !from.module.equals(location.module)) return false;
		return true;
	}

	@Override
	public boolean isTag()
	{
		return true;
	}

	@Override
	public TCRecordType getRecord()
	{
		return this;
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }

		for (TCField f: fields)
		{
			f.unResolve();
		}
	}

	@Override
	public TCType typeResolve(Environment env)
	{
		if (resolved) return this; else resolved = true;
		TypeCheckException problem = null;

		for (TCField f: fields)
		{
			try
			{
				f.typeResolve(env);
			}
			catch (TypeCheckException e)
			{
				if (problem == null)
				{
					problem = e;
				}
				else
				{
					// Add extra messages to the exception for each field
					problem.addExtra(e);
				}

				resolved = true;	// See bug #26
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
		return name.toString() + (maximal ? "!" : "") + (opaque ? " /* opaque */" : "");
	}

	@Override
	public String toDetailedString()
	{
		return "compose " + name + " of " + Utils.listToString(fields) + " end";
	}
	
	@Override
	public String toExplicitString(LexLocation from)
	{
		if (name.getLocation().module.equals(from.module))
		{
			return toString();
		}
		else
		{
			return name.getExplicit(true).toString();
		}
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other instanceof TCRecordType)
		{
			TCRecordType rother = (TCRecordType)other;
			return name.equals(rother.name);
		}

		return false;
	}

	@Override
	public int compareTo(TCType other)
	{
		if (other instanceof TCRecordType)
		{
			TCRecordType rt = (TCRecordType)other;
    		String n1 = name.getExplicit(true).toString();
    		String n2 = rt.name.getExplicit(true).toString();
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
		return name.hashCode();
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		if (inNarrower)
		{
			return false;
		}
		else
		{
			inNarrower = true;
		}
		
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
		else
		{
			for (TCField field: fields)
			{
				if (field.type.narrowerThan(accessSpecifier))
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
	public TCTypeList getComposeTypes()
	{
		if (composed)
		{
			TCTypeList types = new TCTypeList(this);

			for (TCField f: fields)
			{
				types.addAll(f.type.getComposeTypes());
			}
			
			return types;
		}
		else
		{
			return new TCTypeList();
		}
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseRecordType(this, arg);
	}
}
