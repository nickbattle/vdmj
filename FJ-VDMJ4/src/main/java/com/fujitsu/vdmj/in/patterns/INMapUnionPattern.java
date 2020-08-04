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
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.util.Permutor;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueMap;

public class INMapUnionPattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final INPattern left;
	public final INPattern right;

	public INMapUnionPattern(INPattern left, LexLocation location, INPattern right)
	{
		super(location);
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString()
	{
		return left + " union " + right;
	}

	@Override
	public int getLength()
	{
		int llen = left.getLength();
		int rlen = right.getLength();
		return (llen == ANY || rlen == ANY) ? ANY : llen + rlen;
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt) throws PatternMatchException
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

		int llen = left.getLength();
		int rlen = right.getLength();
		int size = values.size();

		if ((llen == ANY && rlen > size) ||
			(rlen == ANY && llen > size) ||
			(rlen != ANY && llen != ANY && size != llen + rlen))
		{
			patternFail(4155, "Map union pattern does not match expression");
		}

		// If the left and right sizes are zero (ie. flexible) then we have to
		// generate a set of splits of the values, and offer these to sub-matches
		// to see whether they fit. Otherwise, there is just one split at this level.

		List<Integer> leftSizes = new Vector<Integer>();

		if (llen == ANY)
		{
			if (rlen == ANY)
			{
				if (size == 0)
				{
					// Can't match a munion b with {|->}
				}
				else if (size % 2 == 1)
				{
					// Odd => add the middle, then those either side
					int half = size/2 + 1;
					if (half > 0) leftSizes.add(half);

					for (int delta=1; half - delta > 0; delta++)
					{
						leftSizes.add(half + delta);
						leftSizes.add(half - delta);
					}

					leftSizes.add(0);
				}
				else
				{
					// Even => add those either side of the middle
					int half = size/2;
					if (half > 0) leftSizes.add(half);

					for (int delta=1; half - delta > 0; delta++)
					{
						leftSizes.add(half + delta);
						leftSizes.add(half - delta);
					}
					
					leftSizes.add(size);
					leftSizes.add(0);
				}
			}
			else
			{
				leftSizes.add(size - rlen);
			}
		}
		else
		{
			leftSizes.add(llen);
		}

		// Since the left and right may have specific element members, we
		// have to permute through the various map orderings to see
		// whether there are any which match both sides. If the patterns
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

		// Now loop through the various splits and attempt to match the l/r
		// sub-patterns to the split map value.

		List<NameValuePairList> finalResults = new Vector<NameValuePairList>();

		for (Integer lsize: leftSizes)
		{
			for (Map<Value, Value> setPerm: allMaps)
			{
				Iterator<Entry<Value, Value>> iter = setPerm.entrySet().iterator();
				ValueMap first = new ValueMap();

				for (int i=0; i<lsize; i++)
				{
					Entry<Value, Value> e = iter.next();
					first.put(e.getKey(), e.getValue());
				}

				ValueMap second = new ValueMap();

				while (iter.hasNext())	// Everything else in second
				{
					Entry<Value, Value> e = iter.next();
					second.put(e.getKey(), e.getValue());
				}

				List<List<NameValuePairList>> nvplists = new Vector<List<NameValuePairList>>();
				int psize = 2;
				int[] counts = new int[psize];

				try
				{
					List<NameValuePairList> lnvps = left.getAllNamedValues(new MapValue(first), ctxt);
					nvplists.add(lnvps);
					counts[0] = lnvps.size();

					List<NameValuePairList> rnvps = right.getAllNamedValues(new MapValue(second), ctxt);
					nvplists.add(rnvps);
					counts[1] = rnvps.size();
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
										patternFail(4126, "Values do not match union pattern");
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
		}

		if (finalResults.isEmpty())
		{
			patternFail(4156, "Cannot match map pattern");
		}

		return finalResults;
	}

	@Override
	public boolean isConstrained()
	{
		return left.isConstrained() || right.isConstrained();
	}

	@Override
	public List<INIdentifierPattern> findIdentifiers()
	{
		List<INIdentifierPattern> list = new Vector<INIdentifierPattern>();
		list.addAll(left.findIdentifiers());
		list.addAll(right.findIdentifiers());
		return list;
	}

	@Override
	protected TCType getPossibleType()
	{
		TCTypeSet list = new TCTypeSet();

		list.add(left.getPossibleType());
		list.add(right.getPossibleType());

		TCType s = list.getType(location);

		return s.isUnknown(location) ?
			new TCMapType(location, new TCUnknownType(location), new TCUnknownType(location)) : s;
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapUnionPattern(this, arg);
	}
}
