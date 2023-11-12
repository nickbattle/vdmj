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

package com.fujitsu.vdmj.po.patterns.visitors;

import com.fujitsu.vdmj.po.patterns.POBooleanPattern;
import com.fujitsu.vdmj.po.patterns.POCharacterPattern;
import com.fujitsu.vdmj.po.patterns.POConcatenationPattern;
import com.fujitsu.vdmj.po.patterns.POExpressionPattern;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POIgnorePattern;
import com.fujitsu.vdmj.po.patterns.POIntegerPattern;
import com.fujitsu.vdmj.po.patterns.POMapPattern;
import com.fujitsu.vdmj.po.patterns.POMapUnionPattern;
import com.fujitsu.vdmj.po.patterns.POMapletPattern;
import com.fujitsu.vdmj.po.patterns.PONilPattern;
import com.fujitsu.vdmj.po.patterns.POObjectPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POQuotePattern;
import com.fujitsu.vdmj.po.patterns.PORealPattern;
import com.fujitsu.vdmj.po.patterns.PORecordPattern;
import com.fujitsu.vdmj.po.patterns.POSeqPattern;
import com.fujitsu.vdmj.po.patterns.POSetPattern;
import com.fujitsu.vdmj.po.patterns.POStringPattern;
import com.fujitsu.vdmj.po.patterns.POTuplePattern;
import com.fujitsu.vdmj.po.patterns.POUnionPattern;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;

/**
 * This is used during the generation of PO counterexample launches. Given a pattern
 * like mk_(a, b) and a Context that contains a=1 and b={1,2,3}, the visitor produces
 * a String "mk_(1, {1,2,3})" and hasFailed returns false.
 * 
 * Some patterns cannot be matched or the Context may not contain values, then
 * hasFailed() is true.
 */
public class POGetMatchingConstantVisitor extends POPatternVisitor<String, Context>
{
	private boolean failed = false;
	
	public boolean hasFailed()
	{
		return failed;
	}

	@Override
	public String casePattern(POPattern node, Context arg)
	{
		throw new RuntimeException("Missing POGetMatchingConstantVisitor method!");
	}
	
	@Override
	public String caseBooleanPattern(POBooleanPattern node, Context arg)
	{
		return node.value.toString();
	}
	
	@Override
	public String caseCharacterPattern(POCharacterPattern node, Context arg)
	{
		return node.value.toString();
	}
	
	@Override
	public String caseConcatenationPattern(POConcatenationPattern node, Context arg)
	{
		return node.left.apply(this, arg) + " ^ " + node.right.apply(this, arg);
	}
	
	@Override
	public String caseExpressionPattern(POExpressionPattern node, Context arg)
	{
		return node.exp.toString();
	}
	
	@Override
	public String caseIdentifierPattern(POIdentifierPattern node, Context arg)
	{
		Value v = arg.get(node.name);
		
		if (v == null)
		{
			failed  = true;
			return "?";
		}
		else
		{
			return v.toString();
		}
	}
	
	@Override
	public String caseIgnorePattern(POIgnorePattern node, Context arg)
	{
		// Difficult - we have to discover the pattern's type.
		failed = true;
		return "?";
	}
	
	@Override
	public String caseIntegerPattern(POIntegerPattern node, Context arg)
	{
		return node.value.toString();
	}
	
	@Override
	public String caseMapPattern(POMapPattern node, Context arg)
	{
		StringBuilder sb = new StringBuilder("{");
		String sep = "";
		
		for (POMapletPattern p: node.maplets)
		{
			sb.append(sep);
			sb.append(p.from.apply(this, arg));
			sb.append(" |-> ");
			sb.append(p.to.apply(this, arg));
		}

		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public String caseMapUnionPattern(POMapUnionPattern node, Context arg)
	{
		return node.left.apply(this, arg) + " munion " + node.right.apply(this, arg);
	}
	
	@Override
	public String caseNilPattern(PONilPattern node, Context arg)
	{
		return "nil";
	}
	
	@Override
	public String caseObjectPattern(POObjectPattern node, Context arg)
	{
		failed = true;
		return "?";
	}
	
	@Override
	public String caseQuotePattern(POQuotePattern node, Context arg)
	{
		return node.value.toString();
	}
	
	@Override
	public String caseRealPattern(PORealPattern node, Context arg)
	{
		return node.value.toString();
	}
	
	@Override
	public String caseRecordPattern(PORecordPattern node, Context arg)
	{
		StringBuilder sb = new StringBuilder("mk_");
		sb.append(node.type.toExplicitString(node.location));
		sb.append("(");
		String sep = "";

		for (POPattern p: node.plist)
		{
			sb.append(sep);
			sb.append(p.apply(this, arg));
			sep = ", ";
		}

		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public String caseSeqPattern(POSeqPattern node, Context arg)
	{
		StringBuilder sb = new StringBuilder("[");
		String sep = "";

		for (POPattern p: node.plist)
		{
			sb.append(sep);
			sb.append(p.apply(this, arg));
			sep = ", ";
		}

		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public String caseSetPattern(POSetPattern node, Context arg)
	{
		StringBuilder sb = new StringBuilder("{");
		String sep = "";

		for (POPattern p: node.plist)
		{
			sb.append(sep);
			sb.append(p.apply(this, arg));
			sep = ", ";
		}

		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public String caseStringPattern(POStringPattern node, Context arg)
	{
		return node.value.toString();
	}
	
	@Override
	public String caseTuplePattern(POTuplePattern node, Context arg)
	{
		StringBuilder sb = new StringBuilder("mk_(");
		String sep = "";

		for (POPattern p: node.plist)
		{
			sb.append(sep);
			sb.append(p.apply(this, arg));
			sep = ", ";
		}

		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public String caseUnionPattern(POUnionPattern node, Context arg)
	{
		return node.left.apply(this, arg) + " union " + node.right.apply(this, arg);
	}
}
