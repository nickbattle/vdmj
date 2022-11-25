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

import java.util.Iterator;
import java.util.List;
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
import com.fujitsu.vdmj.values.ValueSet;

public class INSetPattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final INPatternList plist;

	public INSetPattern(LexLocation location, INPatternList set)
	{
		super(location);
		this.plist = set;
	}

	@Override
	public String toString()
	{
		return "{" + plist.toString() + "}";
	}

	@Override
	public int getLength()
	{
		return plist.size();
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt) throws PatternMatchException
	{
		ValueSet values = null;

		try
		{
			values = expval.setValue(ctxt);
		}
		catch (ValueException e)
		{
			patternFail(e);
		}

		if (values.size() != plist.size())
		{
			patternFail(4119, "Wrong number of elements for set pattern");
		}

		// Since the member patterns may indicate specific set members, we
		// have to permute through the various set orderings to see
		// whether there are any which match both sides. If the members
		// are not constrained however, the initial ordering will be
		// fine.

		List<ValueSet> allSets;

		if (isConstrained())
		{
			allSets = values.permutedSets();
		}
		else
		{
			allSets = new Vector<ValueSet>();
			allSets.add(values);
		}

		List<NameValuePairList> finalResults = new Vector<NameValuePairList>();
		int psize = plist.size();

		if (plist.isEmpty())
		{
			finalResults.add(new NameValuePairList());
			return finalResults;
		}

		for (ValueSet setPerm: allSets)
		{
			Iterator<Value> iter = setPerm.iterator();

			List<List<NameValuePairList>> nvplists = new Vector<List<NameValuePairList>>();
			int[] counts = new int[psize];
			int i = 0;

			try
			{
				for (INPattern p: plist)
				{
					List<NameValuePairList> pnvps = p.getAllNamedValues(iter.next(), ctxt);
					nvplists.add(pnvps);
					counts[i++] = pnvps.size();
				}
			}
			catch (Exception e)
			{
				continue;
			}

			Selector permutor = new Selector(counts);

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
									patternFail(4120, "Values do not match set pattern");
								}
							}
						}
					}

					finalResults.add(results.asList());
				}
				catch (PatternMatchException pme)
				{
					// Try next perm then...
				}
			}
		}

		if (finalResults.isEmpty())
		{
			patternFail(4121, "Cannot match set pattern");
		}

		return finalResults;
	}

	@Override
	public boolean isConstrained()
	{
		if (plist.getPossibleType(location).isUnion(location))
		{
			return true;	// Set types are various, so we must permute
		}
		
		// Check that lengths of the members are the same
		int length = 0;
		
		for (INPattern p: plist)
		{
			if (length == 0)
			{
				length = p.getLength();
			}
			else
			{
				if (p.getLength() != length)
				{
					return true;	// Patterns are different sizes, so permute them
				}
			}
		}

		return plist.isConstrained();
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetPattern(this, arg);
	}
}
