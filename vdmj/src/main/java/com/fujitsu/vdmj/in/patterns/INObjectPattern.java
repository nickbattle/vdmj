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
import java.util.Vector;

import com.fujitsu.vdmj.in.patterns.visitors.INPatternVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Selector;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

public class INObjectPattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken classname;
	public final INNamePatternPairList fieldlist;
	public final TCType type;

	public INObjectPattern(LexLocation location, TCNameToken classname, INNamePatternPairList fieldlist, TCType type)
	{
		super(location);
		this.classname = classname;
		this.fieldlist = fieldlist;
		this.type = type;
	}

	@Override
	public String toString()
	{
		return "obj_" + type + "(" + Utils.listToString(fieldlist) + ")";
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt) throws PatternMatchException
	{
		ObjectValue objval = null;

		try
		{
			objval = expval.objectValue(ctxt);
		}
		catch (ValueException e)
		{
			patternFail(e);
		}

		if (!TypeComparator.isSubType(objval.type, type))
		{
			patternFail(4114, "Object type does not match pattern");
		}

		List<List<NameValuePairList>> nvplists = new Vector<List<NameValuePairList>>();
		int psize = fieldlist.size();
		int[] counts = new int[psize];
		int i = 0;

		for (INNamePatternPair npp: fieldlist)
		{
			Value fval = objval.get(npp.name, false);
			
			if (fval == null)	// INField does not exist in this object
			{
				patternFail(4114, "Object type does not match pattern");
			}
			
			List<NameValuePairList> pnvps = npp.pattern.getAllNamedValues(fval, ctxt);
			nvplists.add(pnvps);
			counts[i++] = pnvps.size();
		}

		Selector permutor = new Selector(counts);
		List<NameValuePairList> finalResults = new Vector<NameValuePairList>();

		if (fieldlist.isEmpty())
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
								patternFail(4116, "Values do not match object pattern");
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
			patternFail(4116, "Values do not match object pattern");
		}

		return finalResults;
	}

	@Override
	public boolean isConstrained()
	{
		return fieldlist.isConstrained();
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseObjectPattern(this, arg);
	}
}
