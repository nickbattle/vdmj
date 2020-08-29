/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.tc.types;

import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.util.Utils;

public class TCUnionType extends TCType
{
	private static final long serialVersionUID = 1L;

	public TCTypeSet types;

	private TCSetType setType = null;
	private TCSeqType seqType = null;
	private TCMapType mapType = null;
	private TCRecordType recType = null;
	private TCNumericType numType = null;
	private TCProductType prodType = null;
	private TCFunctionType funcType = null;
	private TCOperationType opType = null;
	private TCClassType classType = null;

	private boolean setDone = false;
	private boolean seqDone = false;
	private boolean mapDone = false;
	private boolean recDone = false;
	private boolean numDone = false;
	private boolean funDone = false;
	private boolean opDone = false;
	private boolean classDone = false;

	private int prodCard = -1;
	private boolean expanded = false;

	public TCUnionType(LexLocation location, TCType a, TCType b)
	{
		super(location);
		types = new TCTypeSet();
		types.add(a);
		types.add(b);
		expand();
	}

	public TCUnionType(LexLocation location, TCTypeSet types)
	{
		super(location);
		this.types = types;
		expand();
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		for (TCType t: types)
		{
			if (t.narrowerThan(accessSpecifier))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public TCType isType(String typename, LexLocation from)
	{
		for (TCType t: types)
		{
			TCType rt = t.isType(typename, location);

			if (rt != null)
			{
				return rt;
			}
		}

		return null;
	}

	@Override
	public boolean isType(Class<? extends TCType> typeclass, LexLocation from)
	{
		for (TCType t: types)
		{
			if (t.isType(typeclass, location))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isUnknown(LexLocation from)
	{
		for (TCType t: types)
		{
			if (t.isUnknown(location))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isVoid()
	{
		for (TCType t: types)
		{
			if (!t.isVoid())
			{
				return false;		// NB. Only true if ALL void, not ANY void (see hasVoid)
			}
		}

		return true;
	}

	@Override
	public boolean hasVoid()
	{
		for (TCType t: types)
		{
			if (t.isVoid())
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isUnion(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isSeq(LexLocation from)
	{
		return getSeq() != null;
	}

	@Override
	public boolean isSet(LexLocation from)
	{
		return getSet() != null;
	}

	@Override
	public boolean isMap(LexLocation from)
	{
		return getMap() != null;
	}

	@Override
	public boolean isRecord(LexLocation from)
	{
		return getRecord() != null;
	}

	@Override
	public boolean isTag()
	{
		return false;
	}

	@Override
	public boolean isClass(Environment env)
	{
		return getClassType(env) != null;
	}

	@Override
	public boolean isNumeric(LexLocation from)
	{
		return getNumeric() != null;
	}
	
	private boolean inOrdering = false;
	
	@Override
	public boolean isOrdered(LexLocation from)
	{
		if (inOrdering) return false; else inOrdering = true;
		
		for (TCType t: types)
		{
			if (t.isOrdered(from))
			{
				inOrdering = false;
				return true;
			}
		}

		inOrdering = false;
		return types.isEmpty();		// Empty => ordered
	}
	
	private boolean inEqing = false;
	
	@Override
	public boolean isEq(LexLocation from)
	{
		if (inEqing) return false; else inEqing = true;

		for (TCType t: types)
		{
			if (t.isEq(from))
			{
				inEqing = false;
				return true;
			}
		}

		inEqing = false;
		return false;
	}

	@Override
	public boolean isProduct(LexLocation from)
	{
		return getProduct() != null;
	}

	@Override
	public boolean isProduct(int n, LexLocation from)
	{
		return getProduct(n) != null;
	}

	@Override
	public boolean isFunction(LexLocation from)
	{
		return getFunction() != null;
	}

	@Override
	public boolean isOperation(LexLocation from)
	{
		return getOperation() != null;
	}

	@Override
	public TCUnionType getUnion()
	{
		return this;
	}

	@Override
	public TCSeqType getSeq()
	{
		if (!seqDone)
		{
	   		seqDone = true;		// Mark early to avoid recursion.
	   		seqType = new TCUnknownType(location).getSeq();

	   		TCTypeSet set = new TCTypeSet();
	   		boolean allSeq1 = true;

    		for (TCType t: types)
    		{
    			if (t.isSeq(location))
    			{
    				TCSeqType st = t.getSeq();
    				set.add(st.seqof);
    				allSeq1 = allSeq1 && (st instanceof TCSeq1Type);
    			}
    		}

    		seqType = set.isEmpty() ? null :
    			allSeq1 ?
   	    			new TCSeq1Type(location, set.getType(location)) :  					
   	    			new TCSeqType(location, set.getType(location));
 		}

		return seqType;
	}

	@Override
	public TCSetType getSet()
	{
		if (!setDone)
		{
    		setDone = true;		// Mark early to avoid recursion.
    		setType = new TCUnknownType(location).getSet();

    		TCTypeSet set = new TCTypeSet();
    		boolean allSet1 = true;

    		for (TCType t: types)
    		{
    			if (t.isSet(location))
    			{
    				TCSetType st = t.getSet();
    				set.add(st.setof);
    				allSet1 = allSet1 && (st instanceof TCSet1Type);
    			}
    		}

    		setType = set.isEmpty() ? null :
    			allSet1 ?
    				new TCSet1Type(location, set.getType(location)) :
    				new TCSetType(location, set.getType(location));
		}

		return setType;
	}

	@Override
	public TCMapType getMap()
	{
		if (!mapDone)
		{
    		mapDone = true;		// Mark early to avoid recursion.
    		mapType = new TCUnknownType(location).getMap();

    		TCTypeSet from = new TCTypeSet();
    		TCTypeSet to = new TCTypeSet();

    		for (TCType t: types)
    		{
    			if (t.isMap(location))
    			{
    				from.add(t.getMap().from);
    				to.add(t.getMap().to);
    			}
    		}

    		mapType = from.isEmpty() ? null :
    			new TCMapType(location, from.getType(location), to.getType(location));
		}

		return mapType;
	}

	@Override
	public TCRecordType getRecord()
	{
		if (!recDone)
		{
    		recDone = true;		// Mark early to avoid recursion.
    		recType = new TCUnknownType(location).getRecord();

    		// Build a record type with the common fields of the contained
    		// record types, making the field types the union of the original
    		// fields' types...

    		Map<String, TCTypeList> common = new HashMap<String, TCTypeList>();
    		int recordCount = 0;

    		for (TCType t: types)
    		{
    			if (t.isRecord(location))
    			{
    				recordCount++;
    				
    				for (TCField f: t.getRecord().fields)
    				{
    					TCTypeList current = common.get(f.tag);

    					if (current == null)
    					{
    						common.put(f.tag, new TCTypeList(f.type));
    					}
    					else
    					{
    						current.add(f.type);
    					}
    				}
    			}
    		}
    		
    		// If all fields were present in all records, the TypeLists will be the
    		// same size. But if not, the shorter ones have to have UnknownTypes added,
    		// because some of the records do not have that field.
    		
    		Map<String, TCTypeSet> typesets = new HashMap<String, TCTypeSet>();
    		
    		for (String field: common.keySet())
    		{
    			TCTypeList list = common.get(field);
    			
    			if (list.size() != recordCount)
    			{
    				// Both unknown and undefined types do not trigger isSubType, so we use
    				// an illegal quote type, <?>.
    				list.add(new TCQuoteType(location, "?"));
    			}
    			
    			TCTypeSet set = new TCTypeSet();
    			set.addAll(list);
    			typesets.put(field, set);
    		}

    		TCFieldList fields = new TCFieldList();

    		for (String tag: typesets.keySet())
    		{
				TCNameToken tagname = new TCNameToken(location, "?", tag, false);
				fields.add(new TCField(tagname, tag, typesets.get(tag).getType(location), false));
    		}

    		recType = fields.isEmpty() ? null : new TCRecordType(location, fields);
		}

		return recType;
	}

	@Override
	public TCClassType getClassType(Environment env)
	{
		if (!classDone)
		{
    		classDone = true;		// Mark early to avoid recursion.
    		classType = new TCUnknownType(location).getClassType(env);

    		// Build a class type with the common fields of the contained
    		// class types, making the field types the union of the original
    		// fields' types...

    		Map<TCNameToken, TCTypeSet> common = new HashMap<TCNameToken, TCTypeSet>();
    		Map<TCNameToken, TCAccessSpecifier> access = new HashMap<TCNameToken, TCAccessSpecifier>();
    		
    		// Derive the pseudoclass name for the combined union
    		String classString = "*union";	// NB, illegal class name
    		int count = 0;
    		TCClassType found = null;

    		for (TCType t: types)
    		{
    			if (t.isClass(env))
    			{
    				found = t.getClassType(env);
    				classString = classString + "_" + found.name.getName();	// eg. "*union_A_B"
    				count++;
    			}
    		}
    		
    		if (count == 1)		// Only one class in union, so just return this one
    		{
    			classType = found;
    			return classType;
    		}
    		else if (count == 0)
    		{
    			classType = null;
    			return null;
    		}

    		TCNameToken classname = new TCNameToken(new LexLocation(), "CLASS", classString, false, false);
    		
    		for (TCType t: types)
    		{
    			if (t.isClass(env))
    			{
    				TCClassType ct = t.getClassType(env);

    				for (TCDefinition f: ct.classdef.getDefinitions())
    				{
    					if (env != null && !TCClassDefinition.isAccessible(env, f, false))
    					{
    						// Omit inaccessible fields
    						continue;
    					}
    					
    					// TCTypeSet current = common.get(f.name);
    					TCNameToken synthname = f.name.getModifiedName(classname.getName());
    					TCTypeSet current = null;

    					for (TCNameToken n: common.keySet())
    					{
    						if (n.getName().equals(synthname.getName()))
    						{
    							current = common.get(n);
    							break;
    						}
    					}

    					TCType ftype = f.getType();

    					if (current == null)
    					{
    						common.put(synthname, new TCTypeSet(ftype));
    					}
    					else
    					{
    						current.add(ftype);
    					}

    					TCAccessSpecifier curracc = access.get(synthname);

    					if (curracc == null)
    					{
							TCAccessSpecifier acc = new TCAccessSpecifier(
								f.accessSpecifier.isStatic,
								f.accessSpecifier.isAsync,
								Token.PUBLIC,	// Guaranteed to be accessible
								f.accessSpecifier.isPure);

							access.put(synthname, acc);
    					}
    					else if (!curracc.isPure && f.accessSpecifier.isPure)
						{
							TCAccessSpecifier purified = new TCAccessSpecifier(
								f.accessSpecifier.isStatic,
								f.accessSpecifier.isAsync,
								Token.PUBLIC,
								curracc.isPure || f.accessSpecifier.isPure);

							access.put(synthname, purified);
						}
    				}
    			}
    		}

    		TCDefinitionList newdefs = new TCDefinitionList();

    		for (TCNameToken synthname: common.keySet())
    		{
    			TCType ptype = common.get(synthname).getType(location);
    			TCNameToken newname = null;
    			
    			if (ptype.isOperation(location))
    			{
    				TCOperationType optype = ptype.getOperation();
    				TCOperationType newtype = new TCOperationType(optype.location, optype.parameters, optype.result);
    				newtype.setPure(access.get(synthname).isPure);
    				ptype = newtype;
    				newname = synthname.getModifiedName(optype.parameters);
    			}
    			else if (ptype.isFunction(location))
    			{
    				TCFunctionType ftype = ptype.getFunction();
    				newname = synthname.getModifiedName(ftype.parameters);
    			}
    			
    			TCLocalDefinition def = new TCLocalDefinition(synthname.getLocation(), (newname == null ? synthname : newname),
					ptype);
    			
    			def.setAccessSpecifier(access.get(synthname));
				newdefs.add(def);
    		}

    		classType = (classname == null) ? null :
    			new TCClassType(location,
    				new TCClassDefinition(classname, new TCNameList(), newdefs));
		}

		return classType;
	}

	@Override
	public TCNumericType getNumeric()
	{
		if (!numDone)
		{
    		numDone = true;
			numType = new TCNaturalOneType(location);		// lightest default
			boolean found = false;

    		for (TCType t: types)
    		{
    			if (t.isNumeric(location))
    			{
    				TCNumericType nt = t.getNumeric();

    				if (nt.getWeight() > numType.getWeight())
    				{
    					numType = nt;
    				}

    				found = true;
    			}
    		}

    		if (!found) numType = null;
		}

		return numType;
	}

	@Override
	public TCProductType getProduct()
	{
		return getProduct(0);
	}

	@Override
	public TCProductType getProduct(int n)
	{
		if (prodCard != n)
		{
    		prodCard = n;
    		prodType = new TCUnknownType(location).getProduct(n);

    		// Build a N-ary product type, making the types the union of the
    		// original N-ary products' types...

    		Map<Integer, TCTypeSet> result = new HashMap<Integer, TCTypeSet>();

    		for (TCType t: types)
    		{
    			if ((n == 0 && t.isProduct(location)) || t.isProduct(n, location))
    			{
    				TCProductType pt = t.getProduct(n);
    				int i=0;

    				for (TCType member: pt.types)
    				{
    					TCTypeSet ts = result.get(i);

    					if (ts == null)
    					{
    						ts = new TCTypeSet();
    						result.put(i, ts);
    					}

    					ts.add(member);
    					i++;
    				}
    			}
    		}

    		TCTypeList list = new TCTypeList();

    		for (int i=0; i<result.size(); i++)
    		{
    			list.add(result.get(i).getType(location));
    		}

    		prodType = list.isEmpty() ? null : new TCProductType(location, list);
		}

		return prodType;
	}

	@Override
	public TCFunctionType getFunction()
	{
		if (!funDone)
		{
    		funDone = true;
    		funcType = new TCUnknownType(location).getFunction();

       		TCTypeSet result = new TCTypeSet();
       		Map<Integer, TCTypeSet> params = new HashMap<Integer, TCTypeSet>();
			TCDefinitionList defs = new TCDefinitionList();

    		for (TCType t: types)
    		{
    			if (t.isFunction(location))
    			{
    				if (t.definitions != null) defs.addAll(t.definitions);
    				TCFunctionType f = t.getFunction();
    				result.add(f.result);

    				for (int p=0; p < f.parameters.size(); p++)
    				{
    					TCType pt = f.parameters.get(p);
    					TCTypeSet pset = params.get(p);

    					if (pset == null)
    					{
    						pset = new TCTypeSet(pt);
    						params.put(p, pset);
    					}
    					else
    					{
    						pset.add(pt);
    					}
    				}
    			}
    		}

    		if (!result.isEmpty())
    		{
    			TCType rtype = result.getType(location);
    			TCTypeList plist = new TCTypeList();

    			for (int i=0; i<params.size(); i++)
    			{
    				TCType pt = params.get(i).getType(location);
    				plist.add(pt);
    			}

    			funcType = new TCFunctionType(location, plist, true, rtype);
    			funcType.definitions = defs;
    		}
    		else
    		{
    			funcType = null;
    		}
    	}

		return funcType;
	}

	@Override
	public TCOperationType getOperation()
	{
		if (!opDone)
		{
    		opDone = true;
    		opType = new TCUnknownType(location).getOperation();

       		TCTypeSet result = new TCTypeSet();
       		Map<Integer, TCTypeSet> params = new HashMap<Integer, TCTypeSet>();
			TCDefinitionList defs = new TCDefinitionList();

    		for (TCType t: types)
    		{
    			if (t.isOperation(location))
    			{
    				if (t.definitions != null) defs.addAll(t.definitions);
    				TCOperationType op = t.getOperation();
    				result.add(op.result);

    				for (int p=0; p < op.parameters.size(); p++)
    				{
    					TCType pt = op.parameters.get(p);
    					TCTypeSet pset = params.get(p);

    					if (pset == null)
    					{
    						pset = new TCTypeSet(pt);
    						params.put(p, pset);
    					}
    					else
    					{
    						pset.add(pt);
    					}
    				}
    			}
    		}

    		if (!result.isEmpty())
    		{
    			TCType rtype = result.getType(location);
       			TCTypeList plist = new TCTypeList();

    			for (int i=0; i<params.size(); i++)
    			{
    				TCType pt = params.get(i).getType(location);
    				plist.add(pt);
    			}

    			opType = new TCOperationType(location, plist, rtype);
    			opType.definitions = defs;
    		}
    		else
    		{
    			opType = null;
    		}
    	}

		return opType;
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other instanceof TCUnionType)
		{
			TCUnionType uother = (TCUnionType)other;

			for (TCType t: uother.types)
			{
				if (!types.contains(t))
				{
					return false;
				}
			}

			return true;
		}

		return types.contains(other);
	}

	@Override
	public int hashCode()
	{
		return types.hashCode();
	}

	private void expand()
	{
		if (expanded) return;
		TCTypeSet exptypes = new TCTypeSet();

		for (TCType t: types)
		{
    		if (t instanceof TCUnionType)
    		{
    			TCUnionType ut = (TCUnionType)t;
  				ut.expand();
   				exptypes.addAll(ut.types);
    		}
    		else
    		{
    			exptypes.add(t);
    		}
		}

		types = exptypes;
		expanded = true;
		definitions = new TCDefinitionList();

		for (TCType t: types)
		{
			if (t.definitions != null)
			{
				definitions.addAll(t.definitions);
			}
		}
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }

		for (TCType t: types)
		{
			t.unResolve();
		}
	}

	private boolean infinite = false;

	@Override
	public TCType typeResolve(Environment env, TCTypeDefinition root)
	{
		if (resolved)
		{
			return this;
		}
		else
		{
			resolved = true;
			infinite = true;
		}

		TCTypeSet fixed = new TCTypeSet();
		TypeCheckException problem = null;

		for (TCType t: types)
		{
			if (root != null)
				root.infinite = false;

			try
			{
				fixed.add(t.typeResolve(env, root));
			}
			catch (TypeCheckException e)
			{
				if (problem == null)
				{
					problem = e;
				}
				else
				{
					// Add extra messages to the exception for each union member
					problem.addExtra(e);
				}

				resolved = true;	// See bug #26
			}

			if (root != null)
				infinite = infinite && root.infinite;
		}
		
		if (problem != null)
		{
			unResolve();
			throw problem;
		}

		types = fixed;
		if (root != null) root.infinite = infinite;

		// Resolved types may be unions, so force a re-expand
		expanded = false;
		expand();

		return this;
	}

	@Override
	public String toDisplay()
	{
		if (types.size() == 1)
		{
			return types.iterator().next().toString();
		}
		else
		{
			return Utils.setToString(types, " | ");
		}
	}

	@Override
	public TCTypeList getComposeTypes()
	{
		return types.getComposeTypes();
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseUnionType(this, arg);
	}
}
