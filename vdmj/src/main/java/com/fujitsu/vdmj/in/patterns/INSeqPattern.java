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

package com.fujitsu.vdmj.in.patterns;

import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import com.fujitsu.vdmj.in.patterns.visitors.INPatternVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.util.Selector;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INSeqPattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final INPatternList plist;

	public INSeqPattern(LexLocation location, INPatternList list)
	{
		super(location);
		this.plist = list;
	}
	@Override
	public String toString()
	{
		return "[" + plist.toString() + "]";
	}

	@Override
	public int getLength()
	{
		return plist.size();
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt) throws PatternMatchException
	{
		ValueList values = null;

		try
		{
			values = expval.seqValue(ctxt);
		}
		catch (ValueException e)
		{
			patternFail(e);
		}

		if (values.size() != plist.size())
		{
			patternFail(4117, "Wrong number of elements for sequence pattern");
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

		Selector permutor = new Selector(counts);
		List<NameValuePairList> finalResults = new Vector<NameValuePairList>();

		if (plist.isEmpty())
		{
			finalResults.add(new NameValuePairList());
			return finalResults;
		}

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
								patternFail(4118, "Values do not match sequence pattern");
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
			patternFail(4118, "Values do not match sequence pattern");
		}

		return finalResults;
	}

	@Override
	public boolean isConstrained()
	{
		return plist.isConstrained();
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSeqPattern(this, arg);
	}
}
