/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package smtlib.visitors;

import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

/**
 * TypeCast an expression to a given type. The "caseXXX" type is the target type, and
 * the arg pass is the expression/type being passed.
 */
public class TypeCast extends TCTypeVisitor<TCExpression, TCExpression> implements ASTHelper
{
	@Override
	public TCExpression caseType(TCType node, TCExpression arg)
	{
		return null;
	}

	@Override
	public TCExpression caseNaturalType(TCNaturalType node, TCExpression arg)
	{
		TCType atype = arg.getType();

		if (atype instanceof TCNaturalType ||
			atype instanceof TCNaturalOneType)
		{
			return null;
		}
		else if (atype instanceof TCIntegerType)
		{
			return new TCApplyExpression(
				Globals.getInstance().getInteralVariable("inv_nat"),
				new TCExpressionList(arg));
		}
		else if (atype instanceof TCRealType ||
				 atype instanceof TCRationalType)
		{
			return new TCIsExpression(ANYWHERE, null, node, arg);
		}
		else
		{
			throw new UnsupportedOperationException("Argument is not a nat: " + arg);
		}
	}

	@Override
	public TCExpression caseNaturalOneType(TCNaturalOneType node, TCExpression arg)
	{
		TCType atype = arg.getType();

		if (atype instanceof TCNaturalOneType)
		{
			return null;
		}
		else if (atype instanceof TCNaturalType ||
				 atype instanceof TCIntegerType)
		{
			return new TCApplyExpression(
				Globals.getInstance().getInteralVariable("inv_nat1"),
				new TCExpressionList(arg));
		}
		else if (atype instanceof TCRealType ||
				 atype instanceof TCRationalType)
		{
			return new TCIsExpression(ANYWHERE, null, node, arg);
		}
		else
		{
			throw new UnsupportedOperationException("Argument is not a nat1: " + arg);
		}
	}
}
