/*******************************************************************************
 *
 *	Copyright (c) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.expressions;

import java.util.Collections;

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.MultiBindListDefinition;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.patterns.Bind;
import org.overturetool.vdmj.patterns.SetBind;
import org.overturetool.vdmj.pog.POForAllPredicateContext;
import org.overturetool.vdmj.pog.POForAllContext;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.PatternMatchException;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.FlatCheckedEnvironment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.BooleanType;
import org.overturetool.vdmj.types.SeqType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.values.NameValuePairList;
import org.overturetool.vdmj.values.SeqValue;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;
import org.overturetool.vdmj.values.ValueMap;
import org.overturetool.vdmj.values.ValueSet;


public class SeqCompExpression extends SeqExpression
{
	private static final long serialVersionUID = 1L;
	public final Expression first;
	public final Bind bind;
	public final Expression predicate;

	public SeqCompExpression(LexLocation start,
		Expression first, Bind bind, Expression predicate)
	{
		super(start);
		this.first = first;
		this.bind = bind;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "[" + first + " | " + bind +
			(predicate == null ? "]" : " & " + predicate + "]");
	}

	@Override
	public Type typeCheck(Environment base, TypeList qualifiers, NameScope scope, Type constraint)
	{
		Definition def = new MultiBindListDefinition(location, bind.getMultipleBindList());
		def.typeCheck(base, scope);

		if (bind instanceof SetBind &&
			(bind.pattern.getVariableNames().size() != 1 || !def.getType().isNumeric()))
		{
			report(3155, "List comprehension must define one numeric bind variable");
		}

		Environment local = new FlatCheckedEnvironment(def, base, scope);
		Type elemConstraint = null;
		
		if (constraint != null && constraint.isSeq())
		{
			elemConstraint = constraint.getSeq().seqof;
		}

		Type etype = first.typeCheck(local, null, scope, elemConstraint);

		if (predicate != null)
		{
			if (!predicate.typeCheck(local, null, scope, new BooleanType(location)).isType(BooleanType.class))
			{
				predicate.report(3156, "Predicate is not boolean");
			}
		}

		local.unusedCheck();
		return new SeqType(location, etype);
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		ValueList allValues = null;
		
		try
		{
			allValues = bind.getBindValues(ctxt, false);
		}
		catch (ValueException e)
		{
			abort(e);
		}
		
		if (bind instanceof SetBind)
		{
			return evalSetBind(allValues, ctxt);
		}
		else
		{
			return evalSeqBind(allValues, ctxt);
		}
	}
	
	private Value evalSetBind(ValueList allValues, Context ctxt)
	{
		ValueSet seq = new ValueSet();	// Bind variable values
		ValueMap map = new ValueMap();	// Map bind values to output values

		for (Value val: allValues)
		{
			try
			{
				Context evalContext = new Context(location, "seq comprehension", ctxt);
				NameValuePairList nvpl = bind.pattern.getNamedValues(val, ctxt);
				Value sortOn = nvpl.get(0).value;

				if (map.get(sortOn) == null)
				{
    				if (nvpl.size() != 1 || !sortOn.isNumeric())
    				{
    					abort(4029, "Sequence comprehension bindings must be one numeric value", ctxt);
    				}

    				evalContext.putList(nvpl);

    				if (predicate == null || predicate.eval(evalContext).boolValue(ctxt))
    				{
    					Value out = first.eval(evalContext);
   						seq.add(sortOn);
   						map.put(sortOn, out);
    				}
				}
			}
			catch (ValueException e)
			{
				abort(e);
			}
			catch (PatternMatchException e)
			{
				// Ignore mismatches
			}
		}

		Collections.sort(seq);	// Using compareTo
		ValueList sorted = new ValueList();

		for (Value bv: seq)
		{
			sorted.add(map.get(bv));
		}

		return new SeqValue(sorted);
	}

	private Value evalSeqBind(ValueList allValues, Context ctxt)
	{
		ValueList seq = new ValueList();	// Bind variable values

		for (Value val: allValues)
		{
			try
			{
				Context evalContext = new Context(location, "seq comprehension", ctxt);
				NameValuePairList nvpl = bind.pattern.getNamedValues(val, ctxt);

				evalContext.putList(nvpl);

				if (predicate == null || predicate.eval(evalContext).boolValue(ctxt))
				{
					seq.add(first.eval(evalContext));
				}
			}
			catch (ValueException e)
			{
				abort(e);
			}
			catch (PatternMatchException e)
			{
				// Ignore mismatches
			}
		}

		return new SeqValue(seq);
	}

	@Override
	public Expression findExpression(int lineno)
	{
		Expression found = super.findExpression(lineno);
		if (found != null) return found;

		found = first.findExpression(lineno);
		if (found != null) return found;

		return predicate == null ? null : predicate.findExpression(lineno);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();

		ctxt.push(new POForAllPredicateContext(this));
		obligations.addAll(first.getProofObligations(ctxt));
		ctxt.pop();

		obligations.addAll(bind.getProofObligations(ctxt));

		if (predicate != null)
		{
    		ctxt.push(new POForAllContext(this));
    		obligations.addAll(predicate.getProofObligations(ctxt));
    		ctxt.pop();
		}

		return obligations;
	}

	@Override
	public String kind()
	{
		return "seq comprehension";
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = first.getValues(ctxt);
		list.addAll(bind.getValues(ctxt));

		if (predicate != null)
		{
			list.addAll(predicate.getValues(ctxt));
		}

		return list;
	}

	@Override
	public LexNameList getOldNames()
	{
		LexNameList list = first.getOldNames();
		list.addAll(bind.getOldNames());

		if (predicate != null)
		{
			list.addAll(predicate.getOldNames());
		}

		return list;
	}
}
