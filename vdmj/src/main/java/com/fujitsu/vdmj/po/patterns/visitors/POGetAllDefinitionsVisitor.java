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

import java.util.Iterator;

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POLocalDefinition;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.PONamePatternPair;
import com.fujitsu.vdmj.po.patterns.POObjectPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.PORecordPattern;
import com.fujitsu.vdmj.po.patterns.POSeqPattern;
import com.fujitsu.vdmj.po.patterns.POSetPattern;
import com.fujitsu.vdmj.po.patterns.POTuplePattern;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCInstanceVariableDefinition;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.NameScope;

public class POGetAllDefinitionsVisitor extends POLeafPatternVisitor<PODefinition, PODefinitionList, TCType>
{
	public POGetAllDefinitionsVisitor()
	{
		visitorSet = new POVisitorSet<PODefinition, PODefinitionList, TCType>() {};
	}
	
	@Override
	protected PODefinitionList newCollection()
	{
		return new PODefinitionList();
	}

	@Override
	public PODefinitionList casePattern(POPattern node, TCType arg)
	{
		return newCollection();
	}

	@Override
	public PODefinitionList caseIdentifierPattern(POIdentifierPattern node, TCType arg)
	{
		PODefinitionList defs = newCollection();
		defs.add(new POLocalDefinition(node.location, node.name, arg));
		return defs;
	}
	
	@Override
	public PODefinitionList caseObjectPattern(POObjectPattern node, TCType arg)
	{
		PODefinitionList defs = new PODefinitionList();
		TCClassType pattype = node.type.getClassType(null);
		TCDefinitionList members = pattype.classdef.getDefinitions();

		for (PONamePatternPair npp: node.fieldlist)
		{
			TCDefinition d = members.findName(npp.name, NameScope.STATE);	// NB. state lookup
			
			if (d != null)
			{
				d = d.deref();
			}
			
			if (d instanceof TCInstanceVariableDefinition)
			{
				defs.addAll(npp.pattern.apply(this, d.getType()));
			}
		}

		return defs;
	}
	
	@Override
	public PODefinitionList caseRecordPattern(PORecordPattern node, TCType arg)
	{
		PODefinitionList defs = new PODefinitionList();
		TCRecordType pattype = node.type.getRecord();
		Iterator<TCField> patfi = pattype.fields.iterator();

		for (POPattern p: node.plist)
		{
			TCField pf = patfi.next();
			defs.addAll(p.apply(this, pf.type));
		}

		return defs;
	}
	
	@Override
	public PODefinitionList caseSeqPattern(POSeqPattern node, TCType arg)
	{
		PODefinitionList defs = new PODefinitionList();
		TCType elem = arg.getSeq().seqof;

		for (POPattern p: node.plist)
		{
			defs.addAll(p.apply(this, elem));
		}

		return defs;
	}
	
	@Override
	public PODefinitionList caseSetPattern(POSetPattern node, TCType arg)
	{
		PODefinitionList defs = new PODefinitionList();

		TCSetType set = arg.getSet();

		if (!set.empty)
		{
    		for (POPattern p: node.plist)
    		{
    			defs.addAll(p.apply(this, set.setof));
    		}
		}

		return defs;
	}
	
	@Override
	public PODefinitionList caseTuplePattern(POTuplePattern node, TCType arg)
	{
		PODefinitionList defs = new PODefinitionList();
		TCProductType product = arg.getProduct(node.plist.size());
		Iterator<TCType> ti = product.types.iterator();

		for (POPattern p: node.plist)
		{
			defs.addAll(p.apply(this, ti.next()));
		}

		return defs;
	}
}
