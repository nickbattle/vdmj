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
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

public class DefaultRangeCreator extends TCTypeVisitor<String, Object>
{
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
	public String caseNumericType(TCNumericType node, Object arg)
	{
		return "{ 0, ..., 10 }";
	}
	
	@Override
	public String caseNaturalOneType(TCNaturalOneType node, Object arg)
	{
		return "{ 1, ..., 10 }";
	}
	
	@Override
	public String caseIntegerType(TCIntegerType node, Object arg)
	{
		return "{ -10, ..., 10 }";
	}
	
	@Override
	public String caseRealType(TCRealType node, Object arg)
	{
		return "{ a / b | a, b in set {1, ..., 10} }";
	}
	
	@Override
	public String caseRationalType(TCRationalType node, Object arg)
	{
		return "{ a / b | a, b in set {1, ..., 10} }";
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
		String type = node.seqof.apply(this, arg);
		
		return "{ [ e | e in set s ]\n" +
				" | s in set power " + type + " }";
	}
	
	@Override
	public String caseMapType(TCMapType node, Object arg)
	{
		String dom = node.from.apply(this, arg);
		String rng = node.to.apply(this, arg);
		
		return "{ { a |-> b | a in set d, b in set r }\n" +
				"| d in set power " + dom + ", r in set power " + rng + " }";
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
