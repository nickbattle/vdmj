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

package com.fujitsu.vdmj.tc.definitions;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to hold an equals definition.
 */
public class TCEqualsDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCPattern pattern;
	public final TCTypeBind typebind;
	public final TCBind bind;
	public final TCExpression test;

	public TCType expType = null;
	private TCType defType = null;
	private TCDefinitionList defs = null;

	public TCEqualsDefinition(LexLocation location, TCPattern pattern,
		TCTypeBind typebind, TCBind bind, TCExpression test)
	{
		super(Pass.DEFS, location, null, NameScope.LOCAL);
		this.pattern = pattern;
		this.typebind = typebind;
		this.bind = bind;
		this.test = test;
	}

	@Override
	public TCType getType()
	{
		return defType != null ? defType : new TCUnknownType(location);
	}

	@Override
	public String toString()
	{
		return (pattern != null ? pattern :
				typebind != null ? typebind : bind) + " = " + test;
	}
	
	@Override
	public String kind()
	{
		return "equ def";
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCEqualsDefinition)
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
		else if (bind instanceof TCSetBind)
		{
			TCType st = ((TCSetBind)bind).set.typeCheck(base, null, scope, null);

			if (!st.isSet(location))
			{
				report(3015, "Set bind is not a set type?");
				defType = expType;
			}
			else
			{
    			TCType setof = st.getSet().setof;

    			if (!TypeComparator.compatible(expType, setof))
    			{
    				bind.report(3016, "Expression is not compatible with set bind");
    			}

    			defType = setof;	// Effectively a cast
			}

			bind.pattern.typeResolve(base);
			defs = bind.pattern.getDefinitions(defType, nameScope);
		}
		else if (bind instanceof TCSeqBind)
		{
			TCType st = ((TCSeqBind)bind).sequence.typeCheck(base, null, scope, null);

			if (!st.isSeq(location))
			{
				report(3015, "Seq bind is not a sequence type?");
				defType = expType;
			}
			else
			{
    			TCType seqof = st.getSeq().seqof;

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
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		if (defs != null)
		{
			TCDefinition def = defs.findName(sought, scope);

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
	public TCDefinitionList getDefinitions()
	{
		return defs == null ? new TCDefinitionList() : defs;
	}

	@Override
	public TCNameList getVariableNames()
	{
		return defs == null ? new TCNameList() : defs.getVariableNames();
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		Environment local = new FlatEnvironment(defs, env);
		return test.getFreeVariables(globals, local);
	}
}
