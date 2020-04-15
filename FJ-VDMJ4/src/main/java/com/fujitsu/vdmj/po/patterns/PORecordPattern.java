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

package com.fujitsu.vdmj.po.patterns;

import java.util.Iterator;

import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POMkTypeExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.util.Utils;

public class PORecordPattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken typename;
	public final POPatternList plist;
	public final TCType type;

	public PORecordPattern(TCNameToken typename, POPatternList list, TCType type)
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
	public POExpression getMatchingExpression()
	{
		POExpressionList list = new POExpressionList();

		for (POPattern p: plist)
		{
			list.add(p.getMatchingExpression());
		}

		return new POMkTypeExpression(typename, list, null, null);
	}

	@Override
	public PODefinitionList getAllDefinitions(TCType exptype)
	{
		PODefinitionList defs = new PODefinitionList();
		TCRecordType pattype = type.getRecord();
		Iterator<TCField> patfi = pattype.fields.iterator();

		for (POPattern p: plist)
		{
			TCField pf = patfi.next();
			defs.addAll(p.getAllDefinitions(pf.type));
		}

		return defs;
	}

	@Override
	public TCNameList getAllVariableNames()
	{
		TCNameList list = new TCNameList();

		for (POPattern p: plist)
		{
			list.addAll(p.getAllVariableNames());
		}

		return list;
	}

	@Override
	public TCType getPossibleType()
	{
		return type;
	}

	@Override
	public boolean isSimple()
	{
		return plist.isSimple();
	}

	@Override
	public boolean alwaysMatches()
	{
		return plist.alwaysMatches();
	}

	@Override
	public <R, S> R apply(POPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseRecordPattern(this, arg);
	}
}
