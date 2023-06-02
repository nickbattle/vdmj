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

package quickcheck.visitors;

import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

public class DefaultRangeCreator extends TCTypeVisitor<String, Integer>
{
	private final int NUMERIC_LIMIT;
	private final TCTypeSet done;
	
	public DefaultRangeCreator(int numeric_limit)
	{
		NUMERIC_LIMIT = numeric_limit;
		this.done = new TCTypeSet();
	}
	
	private String npower(String set, int maxsize, boolean withEmpty)
	{
		// A power set of card N is 2^N, so generate a subset such that 2^N <= maxsize
		int size = (int)(Math.log(maxsize) / Math.log(2));	// < maxsize
		if (size == 0) size = 1;
		
		StringBuilder vars = new StringBuilder();
		vars.append("{");
		String sep = "";
		
		for (int i=0; i<size; i++)
		{
			vars.append(sep);
			vars.append("s" + i);
			sep = ", ";
		}
		
		vars.append("}");
		
		String tail = withEmpty ? "" : " \\ {{}}";
		
		return "power (if card " + set + " < " + size + " then " + set +
				" else let " + vars + " union - = " + set + " in " + vars + ")" + tail;
	}
	
	@Override
	public String caseType(TCType node, Integer maxsize)
	{
		return "{ /* list of " + node.toString() + " */ }";
	}
	
	@Override
	public String caseBooleanType(TCBooleanType node, Integer maxsize)
	{
		switch (maxsize)
		{
			case 0:		return "{}";
			case 1:		return "{ true }";
			default:	return "{ true, false }";
		}
	}
	
	@Override
	public String caseCharacterType(TCCharacterType node, Integer maxsize)
	{
		switch (maxsize)
		{
			case 0:		return "{}";
			case 1:		return "{ 'a' }";
			case 2:		return "{ 'a', 'b' }";
			default:	return "{ 'a', 'b', 'c' }";
		}
	}
	
	@Override
	public String caseTokenType(TCTokenType node, Integer maxsize)
	{
		switch (maxsize)
		{
			case 0:		return "{}";
			case 1:		return "{ mk_token(1) }";
			case 2:		return "{ mk_token(1), mk_token(2) }";
			default:	return "{ mk_token(1), mk_token(2), mk_token(3) }";
		}
	}
	
	@Override
	public String caseOptionalType(TCOptionalType node, Integer maxsize)
	{
		switch (maxsize)
		{
			case 0:		return "{}";
			case 1:		return "{ nil }";
			default:	return node.type.apply(this, maxsize - 1) + " union { nil }";
		}
	}
	
	@Override
	public String caseBracketType(TCBracketType node, Integer maxsize)
	{
		return node.type.apply(this, maxsize);
	}

	@Override
	public String caseQuoteType(TCQuoteType node, Integer maxsize)
	{
		return "{ <" + node.value + "> }"; 
	}

	@Override
	public String caseNumericType(TCNumericType node, Integer maxsize)
	{
		if (maxsize < NUMERIC_LIMIT)
		{
			return String.format("{ 0, ..., %d }", maxsize);
		}
		else
		{
			return String.format("{ 0, ..., %d }", NUMERIC_LIMIT);
		}
	}
	
	@Override
	public String caseNaturalOneType(TCNaturalOneType node, Integer maxsize)
	{
		if (maxsize < NUMERIC_LIMIT)
		{
			return String.format("{ 1, ..., %d }", maxsize);
		}
		else
		{
			return String.format("{ 1, ..., %d }", NUMERIC_LIMIT);
		}
	}
	
	@Override
	public String caseIntegerType(TCIntegerType node, Integer maxsize)
	{
		if (maxsize < NUMERIC_LIMIT * 2 + 1)
		{
			int half = maxsize / 2;		// eg. 5/2=2 => {-2, -1, 0, 1, 2}
			if (half == 0) half = 1;
			return String.format("{ -%d, ..., %d }", half, half);
		}
		else
		{
			return String.format("{ -%d, ..., %d }", NUMERIC_LIMIT, NUMERIC_LIMIT);
		}
	}
	
	@Override
	public String caseRationalType(TCRationalType node, Integer maxsize)
	{
		if (maxsize < NUMERIC_LIMIT * NUMERIC_LIMIT)
		{
			int half = (int) Math.round(Math.sqrt(maxsize)) / 2;
			if (half == 0) half = 1;
			return String.format("{ a / b | a, b in set {-%d, ..., %d} & b <> 0 }", half, half);
		}
		else
		{
			return String.format("{ a / b | a, b in set {-%d, ..., %d} & b <> 0 }", NUMERIC_LIMIT, NUMERIC_LIMIT);
		}
	}

	@Override
	public String caseRealType(TCRealType node, Integer maxsize)
	{
		if (maxsize < NUMERIC_LIMIT * NUMERIC_LIMIT)
		{
			int half = (int) Math.round(Math.sqrt(maxsize)) / 2;
			if (half == 0) half = 1;
			return String.format("{ a / b | a, b in set {-%d, ..., %d} & b <> 0 }", half, half);
		}
		else
		{
			return String.format("{ a / b | a, b in set {-%d, ..., %d} & b <> 0 }", NUMERIC_LIMIT, NUMERIC_LIMIT);
		}
	}
	
	@Override
	public String caseFunctionType(TCFunctionType node, Integer maxsize)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		
		for (TCType ptype: node.parameters)
		{
			sb.append("-- Param ");
			sb.append(ptype);
			sb.append(": ");
			sb.append(ptype.apply(this, maxsize));
			sb.append("\n");			
		}
		
		sb.append("-- Result ");
		sb.append(node.result);
		sb.append(": ");
		sb.append(node.result.apply(this, maxsize));
		sb.append("\n");
		sb.append("{ /* Set of functions of the above, lambdas or symbols */ }");
		
		return sb.toString();
	}
	
	@Override
	public String caseNamedType(TCNamedType node, Integer maxsize)
	{
		if (done.contains(node))
		{
			return "{ /* recursing " + node.toString() + " */ }";
		}
		
		done.add(node);
		String rhs = node.type.apply(this, maxsize);
		done.remove(node);
		
		if (node.invdef != null)
		{
			return "{ x | x in set " + rhs + " & inv_" + node.typename + "(x) }";
		}
		else
		{
			return rhs;
		}
	}
	
	@Override
	public String caseRecordType(TCRecordType node, Integer maxsize)
	{
		if (done.contains(node))
		{
			return "{ /* recursing " + node.toString() + " */ }";
		}
		
		done.add(node);
		
		StringBuilder sb = new StringBuilder();
		sb.append("{ mk_" + node.name + "(");
		String sep = "";
		
		for (int f=1; f <= node.fields.size(); f++)
		{
			sb.append(sep);
			sb.append("f" + f);
			sep = ", ";
		}
		
		sb.append(") | ");
		
		int v = 1;
		sep = "";
		
		// Size will be the product of all fields, ie. maxsize ^ N. So we set root to the
		// Nth root of maxsize for each field (or 1, minimally).
		
		int root = (int) Math.floor(Math.pow(maxsize, 1.0D/node.fields.size()));
		if (root == 0) root = 1;
		
		for (TCField field: node.fields)
		{
			sb.append(sep);
			sb.append("f" + v++);
			sb.append(" in set ");
			sb.append(field.type.apply(this, root));
			sep = ", ";
		}
		
		if (node.invdef != null)
		{
			sb.append(" /* & " + node.invdef.body + " */");
		}
		
		sb.append(" }");
		done.remove(node);
		
		return sb.toString(); 
	}
	
	@Override
	public String caseSetType(TCSetType node, Integer maxsize)
	{
		return npower(node.setof.apply(this, maxsize), maxsize, true);
	}
	
	@Override
	public String caseSet1Type(TCSet1Type node, Integer maxsize)
	{
		return npower(node.setof.apply(this, maxsize), maxsize, false);
	}
	
	@Override
	public String caseSeqType(TCSeqType node, Integer maxsize)
	{
		if (node.seqof.isOrdered(node.location) && !node.seqof.isUnion(node.location))
		{
			String type = node.seqof.apply(this, maxsize);
			return "{ [ e | e in set s ] | s in set " + npower(type, maxsize, true) + " }";
		}
		else
		{
			return "{ if s = {} then [] else [ let e in set s in e ] | s in set " + npower(node.seqof.apply(this, maxsize), maxsize, true) + " }";
		}
	}
	
	@Override
	public String caseSeq1Type(TCSeq1Type node, Integer maxsize)
	{
		if (node.seqof.isOrdered(node.location) && !node.seqof.isUnion(node.location))
		{
			String type = node.seqof.apply(this, maxsize);
			return "{ [ e | e in set s ] | s in set " + npower(type, maxsize, false) + " }";
		}
		else
		{
			return "{ if s = {} then [] else [ let e in set s in e ] | s in set " + npower(node.seqof.apply(this, maxsize), maxsize, false) + " }";
		}
	}
	
	@Override
	public String caseMapType(TCMapType node, Integer maxsize)
	{
		String dom = node.from.apply(this, maxsize);
		String rng = node.to.apply(this, maxsize);
		
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("-- Domain: ");
		sb.append(dom);
		sb.append("\n");
		sb.append("-- Range: ");
		sb.append(rng);
		sb.append("\n");
		sb.append("{ /* Set of maps of the above */ }");
		
		return sb.toString();
	}
	
	@Override
	public String caseProductType(TCProductType node, Integer maxsize)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{ mk_(");
		String sep = "";
		
		for (int v=1; v <= node.types.size(); v++)
		{
			sb.append(sep);
			sb.append("v" + v);
			sep = ", ";
		}
		
		sb.append(") | ");
		
		// Size will be the product of all fields, ie. maxsize ^ N. So we set root to the
		// Nth root of maxsize for each field (or 1, minimally).
		
		int root = (int) Math.floor(Math.pow(maxsize, 1.0D/node.types.size()));
		if (root == 0) root = 1;

		int v = 1;
		sep = "";
		
		for (TCType type: node.types)
		{
			sb.append(sep);
			sb.append("v" + v++);
			sb.append(" in set ");
			sb.append(type.apply(this, root));
			sep = ", ";
		}
		
		sb.append(" }");
		
		return sb.toString(); 
	}
	
	@Override
	public String caseUnionType(TCUnionType node, Integer maxsize)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("dunion { ");
		String sep = "";
		
		int root = maxsize / node.types.size();		// ie. the sum is < maxsize
		
		for (TCType type: node.types)
		{
			sb.append(sep);
			sb.append(type.apply(this, root));
			sep = ", ";
		}
		
		sb.append(" }");
		return sb.toString();
	}
}
