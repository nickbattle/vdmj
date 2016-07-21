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

package org.overturetool.vdmj.definitions;

import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.patterns.Bind;
import org.overturetool.vdmj.patterns.IdentifierPattern;
import org.overturetool.vdmj.patterns.IgnorePattern;
import org.overturetool.vdmj.patterns.Pattern;
import org.overturetool.vdmj.patterns.SeqBind;
import org.overturetool.vdmj.patterns.SetBind;
import org.overturetool.vdmj.patterns.TypeBind;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.pog.SeqMemberObligation;
import org.overturetool.vdmj.pog.SetMemberObligation;
import org.overturetool.vdmj.pog.SubTypeObligation;
import org.overturetool.vdmj.pog.ValueBindingObligation;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.PatternMatchException;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.Pass;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeSet;
import org.overturetool.vdmj.types.UnionType;
import org.overturetool.vdmj.types.UnknownType;
import org.overturetool.vdmj.values.NameValuePairList;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;
import org.overturetool.vdmj.values.ValueSet;

/**
 * A class to hold an equals definition.
 */

public class EqualsDefinition extends Definition
{
	private static final long serialVersionUID = 1L;
	public final Pattern pattern;
	public final TypeBind typebind;
	public final Bind bind;
	public final Expression test;

	public Type expType = null;
	private Type defType = null;
	private DefinitionList defs = null;

	public EqualsDefinition(LexLocation location, Pattern pattern, Expression test)
	{
		super(Pass.DEFS, location, null, NameScope.LOCAL);
		this.pattern = pattern;
		this.typebind = null;
		this.bind = null;
		this.test = test;
	}

	public EqualsDefinition(LexLocation location, TypeBind typebind, Expression test)
	{
		super(Pass.DEFS, location, null, NameScope.LOCAL);
		this.pattern = null;
		this.typebind = typebind;
		this.bind = null;
		this.test = test;
	}

	public EqualsDefinition(LexLocation location, Bind setbind, Expression test)
	{
		super(Pass.DEFS, location, null, NameScope.LOCAL);
		this.pattern = null;
		this.typebind = null;
		this.bind = setbind;
		this.test = test;
	}

	@Override
	public Type getType()
	{
		return defType != null ? defType : new UnknownType(location);
	}

	@Override
	public String toString()
	{
		return (pattern != null ? pattern :
				typebind != null ? typebind : bind) + " = " + test;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof EqualsDefinition)
		{
			return toString().equals(other.toString());
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		expType = test.typeCheck(base, null, scope, null);

		if (pattern != null)
		{
			pattern.typeResolve(base);
			defs = pattern.getDefinitions(expType, nameScope);
			defType = expType;
		}
		else if (typebind != null)
		{
			typebind.typeResolve(base);

			if (!TypeComparator.compatible(typebind.type, expType))
			{
				typebind.report(3014, "Expression is not compatible with type bind");
			}

			defType = typebind.type;	// Effectively a cast
			defs = typebind.pattern.getDefinitions(defType, nameScope);
		}
		else if (bind instanceof SetBind)
		{
			Type st = ((SetBind)bind).set.typeCheck(base, null, scope, null);

			if (!st.isSet(location))
			{
				report(3015, "Set bind is not a set type?");
				defType = expType;
			}
			else
			{
    			Type setof = st.getSet().setof;

    			if (!TypeComparator.compatible(expType, setof))
    			{
    				bind.report(3016, "Expression is not compatible with set bind");
    			}

    			defType = setof;	// Effectively a cast
			}

			bind.pattern.typeResolve(base);
			defs = bind.pattern.getDefinitions(defType, nameScope);
		}
		else if (bind instanceof SeqBind)
		{
			Type st = ((SeqBind)bind).sequence.typeCheck(base, null, scope, null);

			if (!st.isSeq(location))
			{
				report(3015, "Seq bind is not a sequence type?");
				defType = expType;
			}
			else
			{
    			Type seqof = st.getSeq().seqof;

    			if (!TypeComparator.compatible(expType, seqof))
    			{
    				bind.report(3016, "Expression is not compatible with seq bind");
    			}

    			defType = seqof;	// Effectively a cast
			}

			bind.pattern.typeResolve(base);
			defs = bind.pattern.getDefinitions(defType, nameScope);
		}

		defs.typeCheck(base, scope);
	}

	@Override
	public Expression findExpression(int lineno)
	{
		return test.findExpression(lineno);
	}

	@Override
	public Definition findName(LexNameToken sought, NameScope scope)
	{
		if (defs != null)
		{
			Definition def = defs.findName(sought, scope);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	@Override
	public void unusedCheck()
	{
		if (defs != null)
		{
			defs.unusedCheck();
		}
	}

	@Override
	public DefinitionList getDefinitions()
	{
		return defs == null ? new DefinitionList() : defs;
	}

	@Override
	public LexNameList getVariableNames()
	{
		return defs == null ? new LexNameList() : defs.getVariableNames();
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		Value v = test.eval(ctxt);
		NameValuePairList nvpl = null;

		if (pattern != null)
		{
			try
			{
				nvpl = pattern.getNamedValues(v, ctxt);
			}
			catch (PatternMatchException e)
			{
				abort(e, ctxt);
			}
		}
		else if (typebind != null)
		{
			try
			{
				Value converted = v.convertTo(typebind.type, ctxt);
				nvpl = typebind.pattern.getNamedValues(converted, ctxt);
			}
			catch (PatternMatchException e)
			{
				abort(e, ctxt);
			}
			catch (ValueException e)
			{
				abort(e);
			}
		}
		else if (bind instanceof SetBind)
		{
			try
			{
				ValueSet set = ((SetBind)bind).set.eval(ctxt).setValue(ctxt);

				if (!set.contains(v))
				{
					abort(4002, "Expression value " + v + " is not in set bind", ctxt);
				}

				nvpl = bind.pattern.getNamedValues(v, ctxt);
			}
			catch (PatternMatchException e)
			{
				abort(e, ctxt);
			}
			catch (ValueException e)
			{
				abort(e);
			}
		}
		else if (bind instanceof SeqBind)
		{
			try
			{
				ValueList seq = ((SeqBind)bind).sequence.eval(ctxt).seqValue(ctxt);

				if (!seq.contains(v))
				{
					abort(4002, "Expression value " + v + " is not in seq bind", ctxt);
				}

				nvpl = bind.pattern.getNamedValues(v, ctxt);
			}
			catch (PatternMatchException e)
			{
				abort(e, ctxt);
			}
			catch (ValueException e)
			{
				abort(e);
			}
		}

		return nvpl;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = new ProofObligationList();

		if (pattern != null)
		{
			if (!(pattern instanceof IdentifierPattern) &&
				!(pattern instanceof IgnorePattern) &&
				expType.isUnion(location))
			{
				Type patternType = pattern.getPossibleType();	// With unknowns
				UnionType ut = expType.getUnion();
				TypeSet set = new TypeSet();

				for (Type u: ut.types)
				{
					if (TypeComparator.compatible(u, patternType))
					{
						set.add(u);
					}
				}

				if (!set.isEmpty())
				{
	    			Type compatible = set.getType(location);

	    			if (!TypeComparator.isSubType(
	    				ctxt.checkType(test, expType), compatible))
	    			{
	    				list.add(new ValueBindingObligation(this, ctxt));
	    				list.add(new SubTypeObligation(test, compatible, expType, ctxt));
	    			}
				}
			}
		}
		else if (typebind != null)
		{
			if (!TypeComparator.isSubType(ctxt.checkType(test, expType), defType))
			{
				list.add(new SubTypeObligation(test, defType, expType, ctxt));
			}
		}
		else if (bind instanceof SetBind)
		{
			list.addAll(((SetBind)bind).set.getProofObligations(ctxt));
			list.add(new SetMemberObligation(test, ((SetBind)bind).set, ctxt));
		}
		else if (bind instanceof SeqBind)
		{
			list.addAll(((SeqBind)bind).sequence.getProofObligations(ctxt));
			list.add(new SeqMemberObligation(test, ((SeqBind)bind).sequence, ctxt));
		}

		list.addAll(test.getProofObligations(ctxt));
		return list;
	}

	@Override
	public String kind()
	{
		return "equals";
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = test.getValues(ctxt);

		if (bind != null)
		{
			list.addAll(bind.getValues(ctxt));
		}

		return list;
	}

	@Override
	public LexNameList getOldNames()
	{
		LexNameList list = test.getOldNames();

		if (bind != null)
		{
			list.addAll(bind.getOldNames());
		}

		return list;
	}
}
