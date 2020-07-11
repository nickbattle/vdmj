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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBind;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.INSeqBind;
import com.fujitsu.vdmj.in.patterns.INSetBind;
import com.fujitsu.vdmj.in.patterns.INTypeBind;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

/**
 * A class to hold an equals definition.
 */
public class INEqualsDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INPattern pattern;
	public final INTypeBind typebind;
	public final INBind bind;
	public final INExpression test;
	public final TCType defType;
	public final INDefinitionList defs;

	public INEqualsDefinition(LexLocation location, INPattern pattern,
		INTypeBind typebind, INBind bind, INExpression test, TCType defType, INDefinitionList defs)
	{
		super(location, null, null);
		this.pattern = pattern;
		this.typebind = typebind;
		this.bind = bind;
		this.test = test;
		this.defType = defType;
		this.defs = defs;
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
	public boolean equals(Object other)
	{
		if (other instanceof INEqualsDefinition)
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
	public INExpression findExpression(int lineno)
	{
		return test.findExpression(lineno);
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
		else if (bind instanceof INSetBind)
		{
			try
			{
				ValueSet set = ((INSetBind)bind).set.eval(ctxt).setValue(ctxt);

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
		else if (bind instanceof INSeqBind)
		{
			try
			{
				ValueList seq = ((INSeqBind)bind).sequence.eval(ctxt).seqValue(ctxt);

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
	public boolean isUpdatable()
	{
		return true;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseEqualsDefinition(this, arg);
	}
}
