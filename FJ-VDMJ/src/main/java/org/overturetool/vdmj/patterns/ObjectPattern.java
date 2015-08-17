/*******************************************************************************
 *
 *	Copyright (c) 2014 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.patterns;

import java.util.List;
import java.util.Vector;

import org.overturetool.vdmj.definitions.ClassDefinition;
import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.definitions.InstanceVariableDefinition;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.expressions.ExpressionList;
import org.overturetool.vdmj.expressions.NewExpression;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.PatternMatchException;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.traces.Permutor;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.TypeCheckException;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.ClassType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.UnresolvedType;
import org.overturetool.vdmj.util.Utils;
import org.overturetool.vdmj.values.NameValuePair;
import org.overturetool.vdmj.values.NameValuePairList;
import org.overturetool.vdmj.values.NameValuePairMap;
import org.overturetool.vdmj.values.ObjectValue;
import org.overturetool.vdmj.values.Value;


public class ObjectPattern extends Pattern
{
	private static final long serialVersionUID = 1L;
	public final LexNameToken classname;
	public final NamePatternPairList fieldlist;
	public Type type;

	public ObjectPattern(LexLocation location, LexNameToken classname, NamePatternPairList fieldlist)
	{
		super(location);
		this.classname = classname;
		this.fieldlist = fieldlist;
		this.type = new UnresolvedType(classname);
	}

	@Override
	public String toString()
	{
		return "obj_" + type + "(" + Utils.listToString(fieldlist) + ")";
	}

	@Override
	public Expression getMatchingExpression()
	{
		ExpressionList list = new ExpressionList();

		for (NamePatternPair npp: fieldlist)
		{
			list.add(npp.pattern.getMatchingExpression());
		}

		// Note... this may not actually match obj_C(...)
		return new NewExpression(location, classname.getIdentifier(), list);
	}

	@Override
	public void unResolve()
	{
		type.unResolve();
		resolved = false;
	}

	@Override
	public void typeResolve(Environment env)
	{
		if (resolved) return; else { resolved = true; }

		try
		{
			fieldlist.typeResolve(env);
			type = type.typeResolve(env, null);

			if (!type.isClass(env))
			{
				report(3331, "obj_ expression is not an object type");
				detail("Type", type);
			}
			else
			{
				typeCheck(env);		// Note checked from resolve for simplicity
			}
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	private void typeCheck(Environment base)
	{
		// Check whether the field access is permitted from here.
		ClassType cls = type.getClassType(base);

		for (NamePatternPair npp: fieldlist)
		{
			Definition fdef = cls.findName(npp.name, NameScope.STATE);

			if (fdef == null)
			{
				npp.name.report(3091, "Unknown member " + npp.name + " of class " + cls.name.name);
			}
			else if (!ClassDefinition.isAccessible(base, fdef, false))
			{
				npp.name.report(3092, "Inaccessible member " + npp.name + " of class " + cls.name.name);
			}
		}

		if (base.isFunctional())
		{
			report(3332, "Object pattern cannot be used from a function");
		}
	}

	@Override
	public DefinitionList getAllDefinitions(Type exptype, NameScope scope)
	{
		DefinitionList defs = new DefinitionList();
		ClassType pattype = type.getClassType(null);
		ClassType expctype = exptype.getClassType(null);

		if (expctype == null || !TypeComparator.isSubType(pattype, expctype))
		{
			report(3333, "Matching expression is not a compatible object type");
			detail2("Pattern type", type, "Expression type", exptype);
			return defs;
		}
		
		DefinitionList members = pattype.classdef.getDefinitions();

		for (NamePatternPair npp: fieldlist)
		{
			Definition d = members.findName(npp.name, NameScope.STATE);	// NB. state lookup
			
			if (d != null)
			{
				d = d.deref();
			}
			
			if (d instanceof InstanceVariableDefinition)
			{
				defs.addAll(npp.pattern.getAllDefinitions(d.getType(), scope));
			}
			else
			{
				report(3334, npp.name.name + " is not a matchable field of class " + pattype);
			}
		}

		return defs;
	}

	@Override
	public LexNameList getAllVariableNames()
	{
		LexNameList list = new LexNameList();

		for (NamePatternPair npp: fieldlist)
		{
			list.addAll(npp.pattern.getAllVariableNames());
		}

		return list;
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt)
		throws PatternMatchException
	{
		ObjectValue objval = null;

		try
		{
			objval = expval.objectValue(ctxt);
		}
		catch (ValueException e)
		{
			patternFail(e);
		}

		if (!TypeComparator.isSubType(objval.type, type))
		{
			patternFail(4114, "Object type does not match pattern");
		}

		List<List<NameValuePairList>> nvplists = new Vector<List<NameValuePairList>>();
		int psize = fieldlist.size();
		int[] counts = new int[psize];
		int i = 0;

		for (NamePatternPair npp: fieldlist)
		{
			Value fval = objval.get(npp.name, false);
			
			if (fval == null)	// Field does not exist in this object
			{
				patternFail(4114, "Object type does not match pattern");
			}
			
			List<NameValuePairList> pnvps = npp.pattern.getAllNamedValues(fval, ctxt);
			nvplists.add(pnvps);
			counts[i++] = pnvps.size();
		}

		Permutor permutor = new Permutor(counts);
		List<NameValuePairList> finalResults = new Vector<NameValuePairList>();

		if (fieldlist.isEmpty())
		{
			finalResults.add(new NameValuePairList());
			return finalResults;
		}

		while (permutor.hasNext())
		{
			try
			{
				NameValuePairMap results = new NameValuePairMap();
				int[] selection = permutor.next();

				for (int p=0; p<psize; p++)
				{
					for (NameValuePair nvp: nvplists.get(p).get(selection[p]))
					{
						Value v = results.get(nvp.name);

						if (v == null)
						{
							results.put(nvp);
						}
						else	// Names match, so values must also
						{
							if (!v.equals(nvp.value))
							{
								patternFail(4116, "Values do not match object pattern");
							}
						}
					}
				}

				finalResults.add(results.asList());		// Consistent set of nvps
			}
			catch (PatternMatchException pme)
			{
				// try next perm
			}
		}

		if (finalResults.isEmpty())
		{
			patternFail(4116, "Values do not match object pattern");
		}

		return finalResults;
	}

	@Override
	public Type getPossibleType()
	{
		return type;
	}

	@Override
	public boolean isConstrained()
	{
		return fieldlist.isConstrained();
	}

	@Override
	public boolean isSimple()
	{
		return fieldlist.isSimple();
	}

	@Override
	public boolean alwaysMatches()
	{
		return fieldlist.alwaysMatches();
	}

	@Override
	public List<IdentifierPattern> findIdentifiers()
	{
		List<IdentifierPattern> list = new Vector<IdentifierPattern>();

		for (NamePatternPair npp: fieldlist)
		{
			list.addAll(npp.pattern.findIdentifiers());
		}

		return list;
	}
}
