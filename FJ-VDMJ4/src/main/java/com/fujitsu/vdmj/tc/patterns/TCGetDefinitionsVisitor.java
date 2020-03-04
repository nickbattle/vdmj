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

package com.fujitsu.vdmj.tc.patterns;

import java.util.Iterator;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCInstanceVariableDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * Local class to hold the parameters. Note that this can't be public :)
 */
class Pair
{
	public final TCType ptype;
	public final NameScope scope;
	
	public Pair(TCType type, NameScope scope)
	{
		this.ptype = type;
		this.scope = scope;
	}
}

public class TCGetDefinitionsVisitor extends TCLeafPatternVisitor<TCDefinition, TCDefinitionList, Pair>
{
	@Override
	protected TCDefinitionList newCollection()
	{
		return new TCDefinitionList();
	}

	@Override
	protected TCLeafExpressionVisitor<TCDefinition, TCDefinitionList, Pair> getExpressionVisitor()
	{
		return null;	// No variables in expression patterns :)
	}

	@Override
	public TCDefinitionList caseIdentifierPattern(TCIdentifierPattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();
		defs.add(new TCLocalDefinition(node.location, node.name, arg.ptype, arg.scope));
		return defs;
	}
	@Override
	public TCDefinitionList caseMapPattern(TCMapPattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();

		if (!arg.ptype.isMap(node.location))
		{
			TypeChecker.report(3314, "Map pattern is not matched against map type", node.location);
			TypeChecker.detail("Actual type", arg.ptype);
		}
		else
		{
			TCMapType map = arg.ptype.getMap();

			if (!map.empty)
			{
		 		for (TCMapletPattern maplet: node.maplets)
		 		{
		 			defs.addAll(maplet.from.apply(this, new Pair(map.from, arg.scope)));
		 			defs.addAll(maplet.to.apply(this, new Pair(map.to, arg.scope)));
		 		}
			}
		}

		return defs;
	}
	
	@Override
	public TCDefinitionList caseMapUnionPattern(TCMapUnionPattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();
		
		if (!arg.ptype.isMap(node.location))
		{
			TypeChecker.report(3315, "Matching expression is not a map type", node.location);
		}

		defs.addAll(super.caseMapUnionPattern(node, arg));

		return defs;
	}

	@Override
	public TCDefinitionList caseObjectPattern(TCObjectPattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();
		TCClassType pattype = node.type.getClassType(null);
		TCClassType expctype = arg.ptype.getClassType(null);

		if (expctype == null || !TypeComparator.isSubType(pattype, expctype))
		{
			TypeChecker.report(3333, "Matching expression is not a compatible object type", node.location);
			TypeChecker.detail2("Pattern type", node.type, "Expression type", arg.ptype);
			return defs;
		}
		
		TCDefinitionList members = pattype.classdef.getDefinitions();

		for (TCNamePatternPair npp: node.fieldlist)
		{
			TCDefinition d = members.findName(npp.name, NameScope.STATE);	// NB. state lookup
			
			if (d != null)
			{
				d = d.deref();
			}
			
			if (d instanceof TCInstanceVariableDefinition)
			{
				defs.addAll(npp.pattern.apply(this, new Pair(d.getType(), arg.scope)));
			}
			else
			{
				TypeChecker.report(3334, npp.name.getName() + " is not a matchable field of class " + pattype, node.location);
			}
		}

		return defs;
	}

	@Override
	public TCDefinitionList caseRecordPattern(TCRecordPattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();

		if (!node.type.isTag())
		{
			TypeChecker.report(3200, "Mk_ expression is not a record type", node.location);
			TypeChecker.detail("Type", arg.ptype);
			return defs;
		}

		TCRecordType pattype = node.type.getRecord();

		if (!arg.ptype.isType(pattype.getClass(), pattype.location))
		{
			TypeChecker.report(3201, "Matching expression is not a compatible record type", node.location);
			TypeChecker.detail2("Pattern type", node.type, "Expression type", arg.ptype);
			return defs;
		}

		if (pattype.fields.size() != node.plist.size())
		{
			TypeChecker.report(3202, "Record pattern argument/field count mismatch", node.location);
		}
		else
		{
			Iterator<TCField> patfi = pattype.fields.iterator();

    		for (TCPattern p: node.plist)
    		{
    			TCField pf = patfi.next();
    			defs.addAll(p.apply(this, new Pair(pf.type, arg.scope)));
    		}
		}

		return defs;
	}
	
	@Override
	public TCDefinitionList caseSeqPattern(TCSeqPattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();

		if (!arg.ptype.isSeq(node.location))
		{
			TypeChecker.report(3203, "Sequence pattern is matched against " + arg.ptype, node.location);
		}
		else
		{
			TCType elem = arg.ptype.getSeq().seqof;

    		for (TCPattern p: node.plist)
    		{
    			defs.addAll(p.apply(this, new Pair(elem, arg.scope)));
    		}
		}

		return defs;
	}
	
	@Override
	public TCDefinitionList caseSetPattern(TCSetPattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();

		if (!arg.ptype.isSet(node.location))
		{
			TypeChecker.report(3204, "Set pattern is not matched against set type", node.location);
			TypeChecker.detail("Actual type", arg.ptype);
		}
		else
		{
			TCSetType set = arg.ptype.getSet();

			if (!set.empty)
			{
        		for (TCPattern p: node.plist)
        		{
        			defs.addAll(p.apply(this, new Pair(set.setof, arg.scope)));
        		}
			}
		}

		return defs;
	}
	
	@Override
	public TCDefinitionList caseTuplePattern(TCTuplePattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();

		if (!arg.ptype.isProduct(node.plist.size(), node.location))
		{
			TypeChecker.report(3205, "Matching expression is not a product of cardinality " + node.plist.size(), node.location);
			TypeChecker.detail("Actual", arg.ptype);
			return defs;
		}

		TCProductType product = arg.ptype.getProduct(node.plist.size());
		Iterator<TCType> ti = product.types.iterator();

		for (TCPattern p: node.plist)
		{
			defs.addAll(p.apply(this, new Pair(ti.next(), arg.scope)));
		}

		return defs;
	}
	
	@Override
	public TCDefinitionList caseUnionPattern(TCUnionPattern node, Pair arg)
	{
		TCDefinitionList defs = newCollection();

		if (!arg.ptype.isSet(node.location))
		{
			TypeChecker.report(3206, "Matching expression is not a set type", node.location);
		}

		defs.addAll(node.left.apply(this, arg));
		defs.addAll(node.right.apply(this, arg));

		return defs;
	}
	
	@Override
	public TCDefinitionList casePattern(TCPattern node, Pair arg)
	{
		return newCollection();
	}
}
