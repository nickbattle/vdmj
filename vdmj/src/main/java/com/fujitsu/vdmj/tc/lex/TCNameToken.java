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

package com.fujitsu.vdmj.tc.lex;

import java.io.Serializable;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A name token for the purpose of type checking.
 */
public class TCNameToken extends TCToken implements Serializable, Comparable<TCNameToken>
{
	private static final long serialVersionUID = 1L;

	private final LexNameToken lexname;

	private TCTypeList parameters = null;
	
	public TCNameToken(LexNameToken name)
	{
		this.lexname = name;
	}

	public TCNameToken(LexLocation location, String module, String name)
	{
		this(location, module, name, false, false);	// NB NOT explicit!
	}

	public TCNameToken(LexLocation location, String module, String name, boolean old)
	{
		this(location, module, name, old, true);	// NB explicit!
	}

	public TCNameToken(LexLocation location, String module, String name, boolean old, boolean explicit)
	{
		this.lexname = new LexNameToken(module, name, location, old, explicit);
	}

	public void setTypeQualifier(TCTypeList parameters)
	{
		this.parameters = parameters;
	}

	public TCTypeList getTypeQualifier()
	{
		return parameters;
	}

	@Override
	public int compareTo(TCNameToken other)
	{
		return toString().compareTo(other.toString());
	}
	
	public boolean matches(TCNameToken name)
	{
		return name.lexname.matches(lexname);
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof TCNameToken))
		{
			return false;
		}

		TCNameToken lother = (TCNameToken)other;
		
		if (!lexname.matches(lother.lexname))
		{
			return false;
		}
		else if (parameters != null && lother.parameters != null)
		{
			return TypeComparator.compatible(parameters, lother.parameters);
		}
		else 
		{
			return (parameters == null && lother.parameters == null);
		}
		
//		if (parameters != null && lother.parameters != null)
//		{
//			if (!TypeComparator.compatible(parameters, lother.parameters))
//			{
//				return false;
//			}
//		}
//		else if ((parameters != null && lother.parameters == null) ||
//				 (parameters == null && lother.parameters != null))
//		{
//			return false;
//		}
//
//		return matches(lother);
	}
	
	@Override
	public int hashCode()
	{
		return lexname.hashCode();
	}

	@Override
	public String toString()
	{
		return  lexname.toString() + (parameters == null ? "" : parameters);
	}

	public String getName()		// Simple name, never explicit
	{
		return lexname.name;
	}

	public String getModule()	// Module name
	{
		return lexname.getModule();
	}
	
	public LexNameToken getLex()
	{
		return lexname;
	}

	@Override
	public LexLocation getLocation()
	{
		return lexname.location;
	}

	public boolean isOld()
	{
		return lexname.old;
	}
	
	public boolean isExplicit()
	{
		return lexname.explicit;
	}

	public TCNameToken getPreName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "pre_" + getName(), false, false);
	}

	public TCNameToken getPostName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "post_" + getName(), false, false);
	}

	public TCNameToken getInvName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "inv_" + getName(), false, false);
	}

	public TCNameToken getEqName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "eq_" + getName(), false, false);
	}

	public TCNameToken getOrdName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "ord_" + getName(), false, false);
	}

	public TCNameToken getMaxName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "max_" + getName(), false, false);
	}

	public TCNameToken getMinName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "min_" + getName(), false, false);
	}

	public TCNameToken getInitName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "init_" + getName(), false, false);
	}

	public TCNameToken getMeasureName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "measure_" + getName(), false, false);
	}
	
	public TCNameToken getResultName(LexLocation l)
	{
		return new TCNameToken(l, getModule(), "RESULT", false, false);
	}
	
	public boolean isReserved()
	{
		String name = getName();
		
		return	name.startsWith("pre_") ||
				name.startsWith("post_") ||
				name.startsWith("inv_") ||
				name.startsWith("init_") ||
				name.startsWith("measure_") ||
				Settings.release == Release.VDM_10 &&
				(
					name.startsWith("eq_") ||
					name.startsWith("ord_") ||
					name.startsWith("min_") ||
					name.startsWith("max_")
				);
	}

	public TCNameToken getExplicit(boolean explicit)
	{
		TCNameToken ex = new TCNameToken(getLocation(), getModule(), getName(), isOld(), explicit);
		ex.setTypeQualifier(parameters);
		return ex;
	}

	public TCNameToken getOldName()
	{
		return new TCNameToken(lexname.getOldName());
	}

	public TCNameToken getNewName()
	{
		return new TCNameToken(lexname.getNewName());
	}

	public TCNameToken getModifiedName(String classname)	// Just change module
	{
		TCNameToken mod = new TCNameToken(getLocation(), classname, getName(), isOld());
		mod.setTypeQualifier(parameters);
		return mod;
	}

	public TCNameToken getModifiedName(TCTypeList parameters)
	{
		TCNameToken copy = copy();
		copy.setTypeQualifier(parameters);
		return copy;
	}

	public TCNameToken getSelfName()
	{
		if (getModule().equals("CLASS"))	// eg. CLASS`A
		{
			return new TCNameToken(getLocation(), getName(), "self", false);
		}
		else
		{
			return new TCNameToken(getLocation(), getModule(), "self", false);
		}
	}

	public TCNameToken getThreadName()
	{
		if (getModule().equals("CLASS"))	// eg. CLASS`A
		{
			TCNameToken thread =  new TCNameToken(getLocation(), getName(), "thread", false);
			thread.setTypeQualifier(new TCTypeList());
			return thread;
		}
		else
		{
			TCNameToken thread =  new TCNameToken(getLocation(), getModule(), "thread", false);
			thread.setTypeQualifier(new TCTypeList());
			return thread;
		}
	}

	public static TCNameToken getThreadName(LexLocation loc)
	{
		TCNameToken thread = new TCNameToken(loc, loc.module, "thread", false);
		thread.setTypeQualifier(new TCTypeList());
		return thread;
	}

	public TCNameToken getPerName(LexLocation loc)
	{
		return new TCNameToken(loc, getModule(), "per_" + getName(), false);
	}

	public TCNameToken getClassName()
	{
		return new TCNameToken(getLocation(), "CLASS", getName(), false);
	}

	public TCNameToken copy()
	{
		return new TCNameToken(getLocation(), getModule(), getName(), isOld());
	}
}
