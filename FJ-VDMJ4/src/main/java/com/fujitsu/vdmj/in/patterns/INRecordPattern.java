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
import java.util.Vector;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Permutor;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FieldMap;
import com.fujitsu.vdmj.values.FieldValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;

public class INRecordPattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken typename;
	public final INPatternList plist;
	public final TCType type;

	public INRecordPattern(TCNameToken typename, INPatternList list, TCType type)
	{
		super(typename.getLocation());
		this.plist = list;
		this.typename = typename;
		this.type = type;
	}

	@Override
	public String toString()
	{
		return "mk_" + type + "(" + Utils.listToString(plist) + ")";
	}
	
	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt) throws PatternMatchException
	{
		FieldMap fields = null;
		RecordValue exprec = null;

		try
		{
			exprec = expval.recordValue(ctxt);
			fields = exprec.fieldmap;
		}
		catch (ValueException e)
		{
			patternFail(e);
		}

		// if (!type.equals(exprec.type))
		if (!TypeComparator.compatible(type, exprec.type))
		{
			patternFail(4114, "Record type does not match pattern");
		}

		if (fields.size() != plist.size())
		{
			patternFail(4115, "Record expression does not match pattern");
		}

		Iterator<FieldValue> iter = fields.iterator();
		List<List<NameValuePairList>> nvplists = new Vector<List<NameValuePairList>>();
		int psize = plist.size();
		int[] counts = new int[psize];
		int i = 0;

		for (INPattern p: plist)
		{
			List<NameValuePairList> pnvps = p.getAllNamedValues(iter.next().value, ctxt);
			nvplists.add(pnvps);
			counts[i++] = pnvps.size();
		}

		Permutor permutor = new Permutor(counts);
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
								patternFail(4116, "Values do not match record pattern");
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
			patternFail(4116, "Values do not match record pattern");
		}

		return finalResults;
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
	protected TCType getPossibleType()
	{
		return type;
	}

	@Override
	public TCNameList getAllVariableNames()
	{
		return plist.getAllVariableNames();
	}
}
