/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.tc.types.visitors;

import com.fujitsu.vdmj.tc.types.TCBasicType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;

/**
 * Explore the tree of a type and generate a string, with any named types from
 * external module/classes as explicit M`T names.
 */
public class TCExplicitTypeVisitor extends TCTypeVisitor<String, String>
{
	/**
	 * We have to collect the nodes that have already been visited since types can be recursive,
	 * and the visitor will otherwise blow the stack. Note that this means you need a new visitor
	 * instance for every use (or only re-use with care!). This is tested and modified in the
	 * NamedType and RecordType entries.
	 */
	private TCTypeSet done = new TCTypeSet();
	
	@Override
	public String caseType(TCType node, String arg)
	{
		return node.toString();		// Like "int" or "char" etc.
	}
	
	@Override
	public String caseBracketType(TCBracketType node, String from)
	{
		return "(" + node.type.apply(this, from) + ")";
	}
	
	@Override
	public String caseFunctionType(TCFunctionType node, String from)
	{
		StringBuilder all = newBuilder();
		String prefix = "";
		
		for (TCType param: node.parameters)
		{
			all.append(prefix);
			all.append(wrap(param, from));
			prefix = " * ";
		}
		
		all.append(node.partial ? " -> " : " +> ");
		all.append(wrap(node.result, from));
		return all.toString();
	}

	@Override
	public String caseInMapType(TCInMapType node, String from)
	{
		StringBuilder all = newBuilder();
		
		all.append("inmap ");
		all.append(wrap(node.from, from));
		all.append(" to ");
		all.append(wrap(node.to, from));
		
		return all.toString();
	}

	@Override
	public String caseMapType(TCMapType node, String from)
	{
		StringBuilder all = newBuilder();
		
		all.append("map ");
		all.append(wrap(node.from, from));
		all.append(" to ");
		all.append(wrap(node.to, from));
		
		return all.toString();
	}

	@Override
	public String caseNamedType(TCNamedType node, String from)
	{
		if (done.contains(node))
		{
			return "";
		}
		else
		{
			done.add(node);
			String s = null;
			
			if (node.location.module.equals(from))
			{
				s = node.typename.toString();
			}
			else
			{
				s = node.typename.getExplicit(true).toString();
			}
			
			done.remove(node);
			return s;
		}
	}

	@Override
	public String caseOperationType(TCOperationType node, String from)
	{
		StringBuilder all = newBuilder();
		String prefix = "";
		
		for (TCType param: node.parameters)
		{
			all.append(prefix);
			all.append(wrap(param, from));
			prefix = " * ";
		}
		
		all.append(" ==> ");
		all.append(wrap(node.result, from));
		return all.toString();
	}

	@Override
	public String caseOptionalType(TCOptionalType node, String from)
	{
		return "[" + node.type.apply(this, from) + "]";
	}

	@Override
	public String caseProductType(TCProductType node, String from)
	{
		StringBuilder all = newBuilder();
		String prefix = "";
		
		for (TCType param: node.types)
		{
			all.append(prefix);
			all.append("(");
			all.append(wrap(param, from));
			all.append(")");
			prefix = " * ";
		}
		
		return all.toString();
	}

	@Override
	public String caseRecordType(TCRecordType node, String from)
	{
		if (done.contains(node))
		{
			return "";
		}
		else
		{
			done.add(node);
			String s = null;
			
			if (node.location.module.equals(from))
			{
				s = node.name.toString();
			}
			else
			{
				s = node.name.getExplicit(true).toString();
			}
			
			done.remove(node);
			return s;
		}
	}

	@Override
	public String caseSeq1Type(TCSeq1Type node, String from)
	{
		return "seq1 of " + wrap(node.seqof, from);
	}

	@Override
	public String caseSeqType(TCSeqType node, String from)
	{
		return "seq of " + wrap(node.seqof, from);
	}

	@Override
	public String caseSet1Type(TCSet1Type node, String from)
	{
		return "set1 of " + wrap(node.setof, from);
	}

	@Override
	public String caseSetType(TCSetType node, String from)
	{
		return "set of " + wrap(node.setof, from);
	}

	@Override
	public String caseUnionType(TCUnionType node, String from)
	{
		StringBuilder all = newBuilder();
		String prefix = "";
		
		for (TCType param: node.types)
		{
			all.append(prefix);
			all.append(wrap(param, from));
			prefix = " | ";
		}
		
		return all.toString();
	}

	private StringBuilder newBuilder()
	{
		return new StringBuilder();
	}
	
	/**
	 * Don't bother wrapping simple types in brackets. 
	 */
	private String wrap(TCType type, String from)
	{
		if (type instanceof TCBasicType ||
			type instanceof TCInvariantType)
		{
			return type.apply(this, from);
		}
		else
		{
			return "(" + type.apply(this, from) + ")";
		}
	}
}
