/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.statements.visitors;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCEqualsDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionExitChecker;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindExitChecker;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindExitChecker;
import com.fujitsu.vdmj.tc.statements.TCBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCCallObjectStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCExitStatement;
import com.fujitsu.vdmj.tc.statements.TCLetBeStStatement;
import com.fujitsu.vdmj.tc.statements.TCLetDefStatement;
import com.fujitsu.vdmj.tc.statements.TCNotYetSpecifiedStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCSubclassResponsibilityStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCStatementExitChecker extends TCLeafStatementVisitor<TCType, TCTypeSet, Environment>
{
	public TCStatementExitChecker()
	{
		visitorSet = new TCVisitorSet<TCType, TCTypeSet, Environment>()
		{
			@Override
			protected void setVisitors()
			{
				expressionVisitor = new TCExpressionExitChecker(this);
				bindVisitor = new TCBindExitChecker(this);
				multiBindVisitor = new TCMultipleBindExitChecker(this);
				statementVisitor = TCStatementExitChecker.this;
			}

			@Override
			protected TCTypeSet newCollection()
			{
				return TCStatementExitChecker.this.newCollection();
			}
		};
	}
	
	public TCStatementExitChecker(TCVisitorSet<TCType, TCTypeSet, Environment> visitors)
	{
		this.visitorSet = visitors;
	}

	@Override
	protected TCTypeSet newCollection()
	{
		return new TCTypeSet();
	}

	@Override
	public TCTypeSet caseStatement(TCStatement node, Environment base)
	{
		return new TCTypeSet();
	}
	
	@Override
	public TCTypeSet caseBlockStatement(TCBlockStatement node, Environment base)
	{
		TCTypeSet types = caseSimpleBlockStatement(node, base);
		
		for (TCDefinition d: node.assignmentDefs)
		{
			if (d instanceof TCAssignmentDefinition)
			{
				TCAssignmentDefinition ad = (TCAssignmentDefinition)d;
				types.addAll(visitorSet.applyExpressionVisitor(ad.expression, base));
			}
		}
		
		return types;
	}
	
	@Override
	public TCTypeSet caseCallObjectStatement(TCCallObjectStatement node, Environment base)
	{
		TCTypeSet result = super.caseCallObjectStatement(node, base);
		
		boolean overridable = Settings.dialect != Dialect.VDM_SL &&
				node.fdef != null && !node.fdef.accessSpecifier.access.equals(Token.PRIVATE);

		if (node.fdef != null && !overridable)
		{
			if (node.fdef instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition explop = (TCExplicitOperationDefinition)node.fdef;
				
				if (explop.possibleExceptions == null)
				{
					explop.possibleExceptions = TCDefinition.IN_PROGRESS;
					explop.possibleExceptions = visitorSet.applyStatementVisitor(explop.body, base);
				}
				
				result.addAll(explop.possibleExceptions);
				return result;
			}
			else if (node.fdef instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition implop = (TCImplicitOperationDefinition)node.fdef;
				
				if (implop.possibleExceptions == null)
				{
					if (implop.body != null)
					{
						implop.possibleExceptions = TCDefinition.IN_PROGRESS;
						implop.possibleExceptions = visitorSet.applyStatementVisitor(implop.body, base);
					}
					else
					{
						return new TCTypeSet();
					}
				}
				
				result.addAll(implop.possibleExceptions);
				return result;
			}
		}

		result.add(new TCUnknownType(node.location));
		return result;
	}
	
	@Override
	public TCTypeSet caseCallStatement(TCCallStatement node, Environment base)
	{
		TCTypeSet result = super.caseCallStatement(node, base);

		TCDefinition opdef = base.findName(node.name, NameScope.GLOBAL);
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
					explop.possibleExceptions = visitorSet.applyStatementVisitor(explop.body, base);
				}
				
				result.addAll(explop.possibleExceptions);
				return result;
			}
			else if (opdef instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition implop = (TCImplicitOperationDefinition)opdef;
				
				if (implop.possibleExceptions == null)
				{
					if (implop.body != null)
					{
						implop.possibleExceptions = TCDefinition.IN_PROGRESS;
						implop.possibleExceptions = visitorSet.applyStatementVisitor(implop.body, base);
					}
					else
					{
						return new TCTypeSet();
					}
				}
				
				result.addAll(implop.possibleExceptions);
				return result;
			}
		}

		result.add(new TCUnknownType(node.location));
		return result;
	}
	
	@Override
	public TCTypeSet caseExitStatement(TCExitStatement node, Environment arg)
	{
		TCTypeSet types = new TCTypeSet();

		if (node.expression == null)
		{
			types.add(new TCVoidType(node.location));
		}
		else if (node.exptype == null)	// Not yet checked
		{
			types.add(new TCUnknownType(node.location));
		}
		else
		{
			types.add(node.exptype);
		}

		return types;
	}
	
	@Override
	public TCTypeSet caseLetBeStStatement(TCLetBeStStatement node, Environment base)
	{
		TCTypeSet result = visitorSet.applyMultiBindVisitor(node.bind, base);
		
		if (node.suchThat != null)
		{
			result.addAll(visitorSet.applyExpressionVisitor(node.suchThat, base));
		}
		
		if (node.def != null)
		{
			for (TCDefinition d: node.def.getDefinitions())
			{
				if (d instanceof TCValueDefinition)
				{
					TCValueDefinition vd = (TCValueDefinition)d;
					result.addAll(visitorSet.applyExpressionVisitor(vd.exp, base));
				}
			}
		}
		
		result.addAll(node.statement.apply(this, base));
		return result;
	}
	
	@Override
	public TCTypeSet caseLetDefStatement(TCLetDefStatement node, Environment base)
	{
		TCTypeSet result = new TCTypeSet();
		
		for (TCDefinition d: node.localDefs)
		{
			if (d instanceof TCEqualsDefinition)
			{
				TCEqualsDefinition ed = (TCEqualsDefinition)d;
				result.addAll(visitorSet.applyExpressionVisitor(ed.test, base));
			}
			else if (d instanceof TCValueDefinition)
			{
				TCValueDefinition vd = (TCValueDefinition)d;
				result.addAll(visitorSet.applyExpressionVisitor(vd.exp, base));
			}
		}
		
		result.addAll(node.statement.apply(this, base));
		return result;
	}
	
	@Override
	public TCTypeSet caseNotYetSpecifiedStatement(TCNotYetSpecifiedStatement node, Environment arg)
	{
		return new TCTypeSet(new TCUnknownType(node.location));
	}
	
	@Override
	public TCTypeSet caseSubclassResponsibilityStatement(TCSubclassResponsibilityStatement node, Environment arg)
	{
		return new TCTypeSet(new TCUnknownType(node.location));
	}
}
