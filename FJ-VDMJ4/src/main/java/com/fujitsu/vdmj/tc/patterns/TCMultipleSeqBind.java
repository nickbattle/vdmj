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

package com.fujitsu.vdmj.tc.patterns;

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCMultipleSeqBind extends TCMultipleBind
{
	private static final long serialVersionUID = 1L;
	public final TCExpression sequence;

	public TCMultipleSeqBind(TCPatternList plist, TCExpression sequence)
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
	public TCType typeCheck(Environment base, NameScope scope)
	{
		plist.typeResolve(base);
		TCType type = sequence.typeCheck(base, null, scope, null);
		TCType result = new TCUnknownType(location);

		if (!type.isSeq(location))
		{
			sequence.report(3197, "Expression matching seq bind is not a sequence");
			sequence.detail("Actual type", type);
		}
		else
		{
			TCSeqType st = type.getSeq();

			if (!st.empty)
			{
				result = st.seqof;
				TCType ptype = getPossibleType();

				if (!TypeComparator.compatible(ptype, result))
				{
					sequence.report(3264, "At least one bind cannot match sequence");
					sequence.detail2("Binds", ptype, "Seq of", st);
				}
			}
			else
			{
				sequence.warning(5009, "Empty sequence used in bind");
			}
		}

		return result;
	}

	@Override
	public TCNameSet getFreeVariables(Environment env)
	{
		return sequence.getFreeVariables(env);
	}
}
