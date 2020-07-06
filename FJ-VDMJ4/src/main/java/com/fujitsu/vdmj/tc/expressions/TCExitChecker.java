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
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCExitChecker extends TCLeafExpressionVisitor<TCType, TCTypeSet, Environment>
{
	public TCExitChecker(TCVisitorSet<TCType, TCTypeSet, Environment> visitors)
	{
		visitorSet = visitors;
	}

	public TCExitChecker()
	{
		visitorSet = new TCVisitorSet<TCType, TCTypeSet, Environment>() {};
	}

	@Override
	public TCTypeSet caseExpression(TCExpression node, Environment base)
	{
		return new TCTypeSet();
	}
	
	@Override
	public TCTypeSet caseApplyExpression(TCApplyExpression node, Environment base)
	{
		TCTypeSet result = super.caseApplyExpression(node, base);

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
							return new TCTypeSet();
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
	protected TCTypeSet newCollection()
	{
		return new TCTypeSet();
	}
}
