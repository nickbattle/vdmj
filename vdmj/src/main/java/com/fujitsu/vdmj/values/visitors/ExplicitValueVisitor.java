/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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
package com.fujitsu.vdmj.values.visitors;

import java.util.Iterator;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.ReferenceValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;

/**
 * This visitor produces a toString of a value, with any record values expanded
 * to be explicit (like mk_A`R) if they are not within the "from" module.
 */
public class ExplicitValueVisitor extends ValueVisitor<String, LexLocation>
{
	@Override
	public String caseValue(Value node, LexLocation arg)
	{
		return node.toString();
	}
	
	@Override
	public String caseReferenceValue(ReferenceValue node, LexLocation from)
	{
		return node.getValue().apply(this, from);
	}
	
 	@Override
	public String caseMapValue(MapValue node, LexLocation from)
	{
 		if (node.values.isEmpty())
 		{
 			return "{|->}";
 		}
 		
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		String prefix = "";

		for (Value key: node.values.keySet())
		{
			sb.append(prefix);
			sb.append(key.apply(this, from));
			sb.append(" |-> ");
			sb.append(node.values.get(key).apply(this, from));
			prefix = ", ";
		}

		sb.append("}");
		return sb.toString();
	}
 	
 	@Override
 	public String caseRecordValue(RecordValue node, LexLocation from)
 	{
		StringBuilder sb = new StringBuilder();
		sb.append("mk_" + node.type.toExplicitString(from) + "(");

		Iterator<TCField> fi = node.type.fields.iterator();

		if (fi.hasNext())
		{
    		String ftag = fi.next().tag;
    		sb.append(node.fieldmap.get(ftag).apply(this, from));

    		while (fi.hasNext())
    		{
    			ftag = fi.next().tag;
    			sb.append(", " + node.fieldmap.get(ftag).apply(this, from));
    		}
		}

		sb.append(")");
		return sb.toString();
 	}

 	@Override
	public String caseSeqValue(SeqValue node, LexLocation from)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");

		if (!node.values.isEmpty())
		{
			sb.append(node.values.get(0).apply(this, from));

			for (int i=1; i<node.values.size(); i++)
			{
				sb.append(", ");
				sb.append(node.values.get(i).apply(this, from));
			}
		}

		sb.append("]");
		return sb.toString();
	}

 	@Override
	public String caseSetValue(SetValue node, LexLocation from)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{");

		if (!node.values.isEmpty())
		{
			sb.append(node.values.get(0).apply(this, from));

			for (int i=1; i<node.values.size(); i++)
			{
				sb.append(", ");
				sb.append(node.values.get(i).apply(this, from));
			}
		}

		sb.append("}");
		return sb.toString();
	}

 	@Override
	public String caseTupleValue(TupleValue node, LexLocation from)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("mk_(");

		if (!node.values.isEmpty())
		{
			sb.append(node.values.get(0).apply(this, from));

			for (int i=1; i<node.values.size(); i++)
			{
				sb.append(", ");
				sb.append(node.values.get(i).apply(this, from));
			}
		}

		sb.append(")");
		return sb.toString();
	}
}
