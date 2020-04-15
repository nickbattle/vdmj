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
import java.util.ListIterator;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.util.Permutor;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INTuplePattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final INPatternList plist;

	public INTuplePattern(LexLocation location, INPatternList list)
	{
		super(location);
		this.plist = list;
	}

	@Override
	public String toString()
	{
		return "mk_" + "(" + Utils.listToString(plist) + ")";
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt) throws PatternMatchException
	{
		ValueList values = null;

		try
		{
			values = expval.tupleValue(ctxt);
		}
		catch (ValueException e)
		{
			patternFail(e);
		}

		if (values.size() != plist.size())
		{
			patternFail(4123, "Tuple expression does not match pattern");
		}

		ListIterator<Value> iter = values.listIterator();
		List<List<NameValuePairList>> nvplists = new Vector<List<NameValuePairList>>();
		int psize = plist.size();
		int[] counts = new int[psize];
		int i = 0;

		for (INPattern p: plist)
		{
			List<NameValuePairList> pnvps = p.getAllNamedValues(iter.next(), ctxt);
			nvplists.add(pnvps);
			counts[i++] = pnvps.size();
		}

		Permutor permutor = new Permutor(counts);
		List<NameValuePairList> finalResults = new Vector<NameValuePairList>();

		while (permutor.hasNext())
		{
			try
			{
				NameValuePairMap results = new NameValuePairMap();
				int[] selection = permutor.next();

				for (int p=0; p<psize; p++)
				{
					for (NameValuePair nvp: nvplists.get(p).get(selection[p]))
					{
						Value v = results.get(nvp.name);

						if (v == null)
						{
							results.put(nvp);
						}
						else	// Names match, so values must also
						{
							if (!v.equals(nvp.value))
							{
								patternFail(4124, "Values do not match tuple pattern");
							}
						}
					}
				}

				finalResults.add(results.asList());		// Consistent set of nvps
			}
			catch (PatternMatchException pme)
			{
				// try next perm
			}
		}

		if (finalResults.isEmpty())
		{
			patternFail(4124, "Values do not match tuple pattern");
		}

		return finalResults;
	}

	@Override
	public TCType getPossibleType()
	{
		TCTypeList list = new TCTypeList();

		for (INPattern p: plist)
		{
			list.add(p.getPossibleType());
		}

		return list.getType(location);
	}

	@Override
	public boolean isConstrained()
	{
		return plist.isConstrained();
	}

	@Override
	public List<INIdentifierPattern> findIdentifiers()
	{
		List<INIdentifierPattern> list = new Vector<INIdentifierPattern>();

		for (INPattern p: plist)
		{
			list.addAll(p.findIdentifiers());
		}

		return list;
	}

	@Override
	public TCNameList getAllVariableNames()
	{
		return plist.getAllVariableNames();
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTuplePattern(this, arg);
	}
}
