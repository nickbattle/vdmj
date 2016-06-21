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

package org.overturetool.vdmj.patterns;

import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.SeqType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.UnknownType;
import org.overturetool.vdmj.values.ValueList;

public class MultipleSeqBind extends MultipleBind
{
	private static final long serialVersionUID = 1L;
	public final Expression sequence;

	public MultipleSeqBind(PatternList plist, Expression sequence)
	{
		super(plist);
		this.sequence = sequence;
	}

	@Override
	public String toString()
	{
		return plist + " in seq " + sequence;
	}

	@Override
	public Type typeCheck(Environment base, NameScope scope)
	{
		plist.typeResolve(base);
		Type type = sequence.typeCheck(base, null, scope, null);
		Type result = new UnknownType(location);

		if (!type.isSeq())
		{
			sequence.report(3197, "Expression matching seq bind is not a sequence");
			sequence.detail("Actual type", type);
		}
		else
		{
			SeqType st = type.getSeq();

			if (!st.empty)
			{
				result = st.seqof;
				Type ptype = getPossibleType();

				if (!TypeComparator.compatible(ptype, result))
				{
					sequence.report(3264, "At least one bind cannot match sequence");
					sequence.detail2("Binds", ptype, "Seq of", st);
				}
			}
			else
			{
				sequence.warning(3264, "Empty sequence used in bind");
			}
		}

		return result;
	}

	@Override
	public ValueList getBindValues(Context ctxt, boolean permuted)
	{
		try
		{
			return sequence.eval(ctxt).seqValue(ctxt);
		}
		catch (ValueException e)
		{
			abort(e);
			return null;
		}
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return sequence.getProofObligations(ctxt);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return sequence.getValues(ctxt);
	}

	@Override
	public LexNameList getOldNames()
	{
		return sequence.getOldNames();
	}
}
