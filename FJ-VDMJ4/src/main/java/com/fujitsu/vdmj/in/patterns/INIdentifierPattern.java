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

package com.fujitsu.vdmj.in.patterns;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.patterns.visitors.INPatternVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

public class INIdentifierPattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	private boolean constrained;

	public INIdentifierPattern(TCNameToken token)
	{
		super(token.getLocation());
		this.name = token;
	}

	@Override
	public int getLength()
	{
		return ANY;	// Special value meaning "any length"
	}

	@Override
	public String toString()
	{
		return name.toString();
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt)
	{
		List<NameValuePairList> result = new Vector<NameValuePairList>();
		NameValuePairList list = new NameValuePairList();
		list.add(new NameValuePair(name, expval));
		result.add(list);
		return result;
	}

	@Override
	public boolean isConstrained()
	{
		return constrained;	// The variable may be constrained to be the same as another occurrence
	}

	public void setConstrained(boolean c)
	{
		constrained = c;
	}

	@Override
	public List<INIdentifierPattern> findIdentifiers()
	{
		List<INIdentifierPattern> list = new Vector<INIdentifierPattern>();
		list.add(this);
		return list;
	}

	@Override
	protected TCType getPossibleType()
	{
		return new TCUnknownType(location);
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIdentifierPattern(this, arg);
	}
}
