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
import com.fujitsu.vdmj.po.patterns.POMapletPatternList;
import com.fujitsu.vdmj.po.patterns.PONamePatternPair;
import com.fujitsu.vdmj.po.patterns.PONamePatternPairList;
import com.fujitsu.vdmj.po.patterns.PONilPattern;
import com.fujitsu.vdmj.po.patterns.POObjectPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POQuotePattern;
import com.fujitsu.vdmj.po.patterns.PORealPattern;
import com.fujitsu.vdmj.po.patterns.PORecordPattern;
import com.fujitsu.vdmj.po.patterns.POSeqPattern;
import com.fujitsu.vdmj.po.patterns.POSetPattern;
import com.fujitsu.vdmj.po.patterns.POStringPattern;
import com.fujitsu.vdmj.po.patterns.POTuplePattern;
import com.fujitsu.vdmj.po.patterns.POUnionPattern;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class PORemoveIgnoresVisitor extends POPatternVisitor<POPattern, Object>
{
	private static int var = 1;		// Used in caseIgnorePattern()

	public static void init()
	{
		var = 1;	// reset on each getProofObligations run.
	}

	@Override
	public POPattern casePattern(POPattern node, Object arg)
	{
		throw new RuntimeException("Missing POGetMatchingPatternVisitor method!");
	}
	
	@Override
	public POPattern caseBooleanPattern(POBooleanPattern node, Object arg)
	{
		return node;
	}
	
	@Override
	public POPattern caseCharacterPattern(POCharacterPattern node, Object arg)
	{
		return node;
	}
	
	@Override
	public POPattern caseConcatenationPattern(POConcatenationPattern node, Object arg)
	{
		return new POConcatenationPattern(
			node.left.apply(this, arg), node.location, node.right.apply(this, arg));
	}
	
	@Override
	public POPattern caseExpressionPattern(POExpressionPattern node, Object arg)
	{
		return node;
	}
	
	@Override
	public POPattern caseIdentifierPattern(POIdentifierPattern node, Object arg)
	{
		// If we encounter any "old" state names, like "Sigma~", we rename to
		// "oldSigma" to allow POs to work as simple expressions.
		
		if (node.name.isOld())
		{
			TCNameToken oldName = new TCNameToken(node.location, node.location.module, "old" + node.name.getName());
			return new POIdentifierPattern(oldName);
		}
		
		return node;
	}
	
	@Override
	public POPattern caseIgnorePattern(POIgnorePattern node, Object arg)
	{
		// Generate a new "any" name for use during PO generation. The name
		// must be unique for the pattern instance.
		
		TCNameToken anyName = new TCNameToken(node.location, node.location.module, "$any" + var++);
		return new POIdentifierPattern(anyName);
	}
	
	@Override
	public POPattern caseIntegerPattern(POIntegerPattern node, Object arg)
	{
		return node;
	}
	
	@Override
	public POPattern caseMapPattern(POMapPattern node, Object arg)
	{
		POMapletPatternList list = new POMapletPatternList();

		for (POMapletPattern p: node.maplets)
		{
			list.add((POMapletPattern) p.apply(this, arg));
		}

		return new POMapPattern(node.location, list);
	}
	
	@Override
	public POPattern caseMapletPattern(POMapletPattern node, Object arg)
	{
		return new POMapletPattern(node.from.apply(this, arg), node.to.apply(this, arg));
	}
	
	@Override
	public POPattern caseMapUnionPattern(POMapUnionPattern node, Object arg)
	{
		return new POMapUnionPattern(
			node.left.apply(this, arg), node.location, node.right.apply(this, arg));
	}
	
	@Override
	public POPattern caseNilPattern(PONilPattern node, Object arg)
	{
		return node;
	}
	
	@Override
	public POPattern caseObjectPattern(POObjectPattern node, Object arg)
	{
		PONamePatternPairList list = new PONamePatternPairList();

		for (PONamePatternPair npp: node.fieldlist)
		{
			list.add(new PONamePatternPair(npp.name, npp.pattern.apply(this, arg)));
		}

		// Note... this may not actually match obj_C(...)
		return new POObjectPattern(node.location, node.classname, list);
	}
	
	@Override
	public POPattern caseQuotePattern(POQuotePattern node, Object arg)
	{
		return node;
	}
	
	@Override
	public POPattern caseRealPattern(PORealPattern node, Object arg)
	{
		return node;
	}
	
	@Override
	public POPattern caseRecordPattern(PORecordPattern node, Object arg)
	{
		POPatternList list = new POPatternList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new PORecordPattern(node.typename, list, node.type);
	}
	
	@Override
	public POPattern caseSeqPattern(POSeqPattern node, Object arg)
	{
		POPatternList list = new POPatternList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new POSeqPattern(node.location, list);
	}
	
	@Override
	public POPattern caseSetPattern(POSetPattern node, Object arg)
	{
		POPatternList list = new POPatternList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new POSetPattern(node.location, list);
	}
	
	@Override
	public POPattern caseStringPattern(POStringPattern node, Object arg)
	{
		return node;
	}
	
	@Override
	public POPattern caseTuplePattern(POTuplePattern node, Object arg)
	{
		POPatternList list = new POPatternList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new POTuplePattern(node.location, list);
	}
	
	@Override
	public POPattern caseUnionPattern(POUnionPattern node, Object arg)
	{
		return new POUnionPattern(
			node.left.apply(this, arg), node.location, node.right.apply(this, arg));
	}
}
