/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.po.patterns.visitors;

import com.fujitsu.vdmj.lex.LexLocation;
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
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POQuotePattern;
import com.fujitsu.vdmj.po.patterns.PORealPattern;
import com.fujitsu.vdmj.po.patterns.PORecordPattern;
import com.fujitsu.vdmj.po.patterns.POSeqPattern;
import com.fujitsu.vdmj.po.patterns.POSetPattern;
import com.fujitsu.vdmj.po.patterns.POStringPattern;
import com.fujitsu.vdmj.po.patterns.POTuplePattern;
import com.fujitsu.vdmj.po.patterns.POUnionPattern;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;

public class POGetPossibleTypeVisitor extends POPatternVisitor<TCType, Object>
{
	private TCType apply(POPatternList list, LexLocation location)
	{
		switch (list.size())
		{
			case 0:
				return new TCUnknownType(location);

			case 1:
				return list.get(0).apply(this, null);

			default:
        		TCTypeSet set = new TCTypeSet();

        		for (POPattern p: list)
        		{
        			set.add(p.apply(this, null));
        		}

        		return set.getType(location);		// NB. a union of types
		}
	}
	
	@Override
	public TCType casePattern(POPattern node, Object arg)
	{
		throw new RuntimeException("Missing POGetPossibleTypeVisitor method!");
	}

	@Override
	public TCType caseBooleanPattern(POBooleanPattern node, Object arg)
	{
		return new TCBooleanType(node.location);
	}

	@Override
	public TCType caseCharacterPattern(POCharacterPattern node, Object arg)
	{
		return new TCCharacterType(node.location);
	}
	
	@Override
	public TCType caseConcatenationPattern(POConcatenationPattern node, Object arg)
	{
		POPatternList plist = new POPatternList();
		plist.add(node.left);
		plist.add(node.right);
		
		TCType type = apply(plist, node.location);
		
		return type.isUnknown(node.location) ? 
			new TCSeqType(node.location, new TCUnknownType(node.location)) : type;
	}
	
	@Override
	public TCType caseExpressionPattern(POExpressionPattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}
	
	@Override
	public TCType caseIdentifierPattern(POIdentifierPattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}
	
	@Override
	public TCType caseIgnorePattern(POIgnorePattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}
	
	@Override
	public TCType caseIntegerPattern(POIntegerPattern node, Object arg)
	{
		return TCNumericType.typeOf(node.value.value, node.location);
	}
	
	@Override
	public TCType caseMapPattern(POMapPattern node, Object arg)
	{
		TCTypeSet types = new TCTypeSet();
		
		for (POMapletPattern p: node.maplets)
		{
			TCType m = new TCMapType(p.location, p.from.apply(this, arg), p.to.apply(this, arg));
			types.add(m);
		}
		
		return types.isEmpty() ? new TCMapType(node.location) : types.getType(node.location);
	}
	
	@Override
	public TCType caseMapUnionPattern(POMapUnionPattern node, Object arg)
	{
		TCTypeSet list = new TCTypeSet();

		list.add(node.left.apply(this, arg));
		list.add(node.right.apply(this, arg));

		TCType s = list.getType(node.location);

		return s.isUnknown(node.location) ?
			new TCMapType(node.location, new TCUnknownType(node.location), new TCUnknownType(node.location)) : s;
	}
	
	@Override
	public TCType caseNilPattern(PONilPattern node, Object arg)
	{
		return new TCOptionalType(node.location, new TCUnknownType(node.location));
	}
	
	@Override
	public TCType caseObjectPattern(POObjectPattern node, Object arg)
	{
		return node.type;
	}
	
	@Override
	public TCType caseQuotePattern(POQuotePattern node, Object arg)
	{
		return new TCQuoteType(node.location, node.value.value);
	}
	
	@Override
	public TCType caseRealPattern(PORealPattern node, Object arg)
	{
		return new TCRealType(node.location);
	}
	
	@Override
	public TCType caseRecordPattern(PORecordPattern node, Object arg)
	{
		return node.type;
	}
	
	@Override
	public TCType caseSeqPattern(POSeqPattern node, Object arg)
	{
		return new TCSeqType(node.location, apply(node.plist, node.location));
	}
	
	@Override
	public TCType caseSetPattern(POSetPattern node, Object arg)
	{
		return new TCSetType(node.location, apply(node.plist, node.location));
	}
	
	@Override
	public TCType caseStringPattern(POStringPattern node, Object arg)
	{
		return new TCSeqType(node.location, new TCCharacterType(node.location));
	}
	
	@Override
	public TCType caseTuplePattern(POTuplePattern node, Object arg)
	{
		TCTypeList list = new TCTypeList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return list.getType(node.location);
	}
	
	@Override
	public TCType caseUnionPattern(POUnionPattern node, Object arg)
	{
		TCTypeSet list = new TCTypeSet();

		list.add(node.left.apply(this, arg));
		list.add(node.right.apply(this, arg));

		TCType s = list.getType(node.location);

		return s.isUnknown(node.location) ?
			new TCSetType(node.location, new TCUnknownType(node.location)) : s;
	}
}
