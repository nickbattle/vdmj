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
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

public class DefaultRangeCreator extends TCTypeVisitor<String, Object>
{
	private static final int NUMERIC_LIMIT = 10;	
	
	@Override
	public String caseType(TCType node, Object arg)
	{
		return "{ /* list of " + node.toString() + " */ }";
	}
	
	@Override
	public String caseBooleanType(TCBooleanType node, Object arg)
	{
		return "{ true, false }";
	}
	
	@Override
	public String caseCharacterType(TCCharacterType node, Object arg)
	{
		return "{ 'a', 'b', 'c' }";
	}
	
	@Override
	public String caseTokenType(TCTokenType node, Object arg)
	{
		return "{ mk_token(1), mk_token(2), mk_token(3) }";
	}
	
	@Override
	public String caseOptionalType(TCOptionalType node, Object arg)
	{
		return node.type.apply(this, arg) + " union { nil }";
	}
	
	@Override
	public String caseBracketType(TCBracketType node, Object arg)
	{
		return node.type.apply(this, arg);
	}

	@Override
	public String caseNumericType(TCNumericType node, Object arg)
	{
		return String.format("{ 0, ..., %d }", NUMERIC_LIMIT);
	}
	
	@Override
	public String caseNaturalOneType(TCNaturalOneType node, Object arg)
	{
		return String.format("{ 1, ..., %d }", NUMERIC_LIMIT);
	}
	
	@Override
	public String caseIntegerType(TCIntegerType node, Object arg)
	{
		return String.format("{ -%d, ..., %d }", NUMERIC_LIMIT, NUMERIC_LIMIT);
	}
	
	@Override
	public String caseRealType(TCRealType node, Object arg)
	{
		return String.format("{ a / b | a, b in set {-%d, ..., %d} & b <> 0 }", NUMERIC_LIMIT, NUMERIC_LIMIT);
	}
	
	@Override
	public String caseRationalType(TCRationalType node, Object arg)
	{
		return String.format("{ a / b | a, b in set {-%d, ..., %d} & b <> 0 }", NUMERIC_LIMIT, NUMERIC_LIMIT);
	}
	
	@Override
	public String caseFunctionType(TCFunctionType node, Object arg)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		
		for (TCType ptype: node.parameters)
		{
			sb.append("-- Param ");
			sb.append(ptype);
			sb.append(": ");
			sb.append(ptype.apply(this, arg));
			sb.append("\n");			
		}
		
		sb.append("-- Result ");
		sb.append(node.result);
		sb.append(": ");
		sb.append(node.result.apply(this, arg));
		sb.append("\n");
		sb.append("{ /* Set of functions of the above, lambdas or symbols */ }");
		
		return sb.toString();
	}
	
	@Override
	public String caseNamedType(TCNamedType node, Object arg)
	{
		if (node.invdef != null)
		{
			return "{ x | x in set " + node.type.apply(this, arg) + " & inv_" + node.typename + "(x) }";
		}
		else
		{
			return node.type.apply(this, arg);
		}
	}
	
	@Override
	public String caseRecordType(TCRecordType node, Object arg)
	{
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
		
		for (TCField field: node.fields)
		{
			sb.append(sep);
			sb.append("f" + v++);
			sb.append(" in set ");
			sb.append(field.type.apply(this, arg));
			sep = ", ";
		}
		
		if (node.invdef != null)
		{
			sb.append(" /* & " + node.invdef.body + " */");
		}
		
		sb.append(" }");
		
		return sb.toString(); 
	}
	
	@Override
	public String caseSetType(TCSetType node, Object arg)
	{
		return "power " + node.setof.apply(this, arg);
	}
	
	@Override
	public String caseSet1Type(TCSet1Type node, Object arg)
	{
		return "power " + node.setof.apply(this, arg) + " \\ {}";
	}
	
	@Override
	public String caseSeqType(TCSeqType node, Object arg)
	{
		if (node.seqof.isOrdered(node.location))
		{
			String type = node.seqof.apply(this, arg);
			return "{ [ e | e in set s ] | s in set power " + type + " }";
		}
		else
		{
			return "{ [a, b, c] | a, b, c in set " + node.seqof.apply(this, arg) + " }";
		}
	}
	
	@Override
	public String caseMapType(TCMapType node, Object arg)
	{
		String dom = node.from.apply(this, arg);
		String rng = node.to.apply(this, arg);
		
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
	public String caseProductType(TCProductType node, Object arg)
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
		
		int v = 1;
		sep = "";
		
		for (TCType type: node.types)
		{
			sb.append(sep);
			sb.append("v" + v++);
			sb.append(" in set ");
			sb.append(type.apply(this, arg));
			sep = ", ";
		}
		
		sb.append(" }");
		
		return sb.toString(); 
	}
	
	@Override
	public String caseUnionType(TCUnionType node, Object arg)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("dunion { ");
		String sep = "";
		
		for (TCType type: node.types)
		{
			sb.append(sep);
			sb.append(type.apply(this, arg));
			sep = ", ";
		}
		
		sb.append(" }");
		return sb.toString();
	}
	
	@Override
	public String caseQuoteType(TCQuoteType node, Object arg)
	{
		return "{ <" + node.value + "> }"; 
	}
}
