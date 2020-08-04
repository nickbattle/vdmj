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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.fujitsu.vdmj.in.patterns.visitors.INPatternVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.util.Permutor;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueMap;

public class INMapPattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final INMapletPatternList maplets;

	public INMapPattern(LexLocation location, INMapletPatternList maplets)
	{
		super(location);
		this.maplets = maplets;
	}

	@Override
	public String toString()
	{
		if (maplets.isEmpty())
		{
			return "{|->}";
		}
		else
		{
			return Utils.listToString("{", maplets, ", ", "}");
		}
	}

	@Override
	public int getLength()
	{
		return maplets.size();
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt)
		throws PatternMatchException
	{
		ValueMap values = null;

		try
		{
			values = expval.mapValue(ctxt);
		}
		catch (ValueException e)
		{
			patternFail(e);
		}

		if (values.size() != maplets.size())
		{
			patternFail(4152, "Wrong number of elements for map pattern");
		}

		// Since the member patterns may indicate specific map members, we
		// have to permute through the various map orderings to see
		// whether there are any which match both sides. If the members
		// are not constrained however, the initial ordering will be
		// fine.

		List<Map<Value, Value>> allMaps;

		if (isConstrained())
		{
			allMaps = values.permutedMaps();
		}
		else
		{
			allMaps = new Vector<Map<Value, Value>>();
			allMaps.add(values);
		}

		List<NameValuePairList> finalResults = new Vector<NameValuePairList>();
		int psize = maplets.size();

		if (maplets.isEmpty())
		{
			finalResults.add(new NameValuePairList());
			return finalResults;
		}

		for (Map<Value, Value> mapPerm: allMaps)
		{
			Iterator<Entry<Value, Value>> iter = mapPerm.entrySet().iterator();

			List<List<NameValuePairList>> nvplists = new Vector<List<NameValuePairList>>();
			int[] counts = new int[psize];
			int i = 0;

			try
			{
				for (INMapletPattern p: maplets)
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

			Permutor permutor = new Permutor(counts);

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
									patternFail(4153, "Values do not match map pattern");
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
			patternFail(4154, "Cannot match map pattern");
		}

		return finalResults;
	}

	@Override
	public boolean isConstrained()
	{
		for (INMapletPattern p: maplets)
		{
			if (p.isConstrained()) return true;
		}

		return false;
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapPattern(this, arg);
	}
}
