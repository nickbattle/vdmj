/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package annotations.tc;

import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCDeadlineMetAnnotation extends TCConjectureAnnotation
{
	private static final long serialVersionUID = 1L;

	public TCDeadlineMetAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}
	
	@Override
	protected void typeCheck(Environment env)
	{
		if (args.size() != 5)
		{
			name.report(6008, "Expecting @DeadlineMet(e1, [c], e2, d, m)");
		}
		else
		{
			checkHistoryExpression(env, args.get(0));
			checkNumericExpression(env, args.get(1));
			checkHistoryExpression(env, args.get(2));
			checkNumericExpression(env, args.get(3));
			checkBooleanExpression(env, args.get(4));
		}
	}
}
