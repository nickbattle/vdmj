/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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
package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCExitChecker extends TCLeafExpressionVisitor<TCType, TCTypeList, Environment>
{
	@Override
	public TCTypeList caseExpression(TCExpression node, Environment base)
	{
		return new TCTypeList();
	}
	
	@Override
	public TCTypeList caseApplyExpression(TCApplyExpression node, Environment base)
	{
		TCTypeList result = super.caseApplyExpression(node, base);

		if (node.root instanceof TCVariableExpression)
		{
			TCVariableExpression exp = (TCVariableExpression)node.root;
			TCDefinition opdef = base.findName(exp.name, NameScope.NAMESANDSTATE);

			boolean overridable = Settings.dialect != Dialect.VDM_SL &&
					opdef != null && !opdef.accessSpecifier.access.equals(Token.PRIVATE);

			if (opdef != null && !overridable)
			{
				if (opdef instanceof TCExplicitOperationDefinition)
				{
					TCExplicitOperationDefinition explop = (TCExplicitOperationDefinition)opdef;
					
					if (explop.possibleExceptions == null)
					{
						explop.possibleExceptions = TCDefinition.IN_PROGRESS;
						explop.possibleExceptions = explop.body.exitCheck(base);
					}
					
					result.addAll(explop.possibleExceptions);
				}
				else if (opdef instanceof TCImplicitOperationDefinition)
				{
					TCImplicitOperationDefinition implop = (TCImplicitOperationDefinition)opdef;
					
					if (implop.possibleExceptions == null)
					{
						if (implop.body != null)
						{
							implop.possibleExceptions = TCDefinition.IN_PROGRESS;
							implop.possibleExceptions = implop.body.exitCheck(base);
						}
						else
						{
							return new TCTypeList();
						}
					}
					
					result.addAll(implop.possibleExceptions);
				}
				
				return result;
			}

			result.add(new TCUnknownType(node.root.location));
		}
		else
		{
			result.add(new TCUnknownType(node.root.location));
		}

		return result;
	}

	@Override
	protected TCTypeList newCollection()
	{
		return new TCTypeList();
	}
}
