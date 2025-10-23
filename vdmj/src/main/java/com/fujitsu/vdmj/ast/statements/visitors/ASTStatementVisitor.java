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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.statements.visitors;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotatedStatement;
import com.fujitsu.vdmj.ast.statements.ASTAlwaysStatement;
import com.fujitsu.vdmj.ast.statements.ASTAssignmentStatement;
import com.fujitsu.vdmj.ast.statements.ASTAtomicStatement;
import com.fujitsu.vdmj.ast.statements.ASTBlockStatement;
import com.fujitsu.vdmj.ast.statements.ASTCallObjectStatement;
import com.fujitsu.vdmj.ast.statements.ASTCallStatement;
import com.fujitsu.vdmj.ast.statements.ASTCasesStatement;
import com.fujitsu.vdmj.ast.statements.ASTClassInvariantStatement;
import com.fujitsu.vdmj.ast.statements.ASTCyclesStatement;
import com.fujitsu.vdmj.ast.statements.ASTDefStatement;
import com.fujitsu.vdmj.ast.statements.ASTDurationStatement;
import com.fujitsu.vdmj.ast.statements.ASTElseIfStatement;
import com.fujitsu.vdmj.ast.statements.ASTErrorStatement;
import com.fujitsu.vdmj.ast.statements.ASTExitStatement;
import com.fujitsu.vdmj.ast.statements.ASTForAllStatement;
import com.fujitsu.vdmj.ast.statements.ASTForIndexStatement;
import com.fujitsu.vdmj.ast.statements.ASTForPatternBindStatement;
import com.fujitsu.vdmj.ast.statements.ASTIfStatement;
import com.fujitsu.vdmj.ast.statements.ASTLetBeStStatement;
import com.fujitsu.vdmj.ast.statements.ASTLetDefStatement;
import com.fujitsu.vdmj.ast.statements.ASTNonDeterministicStatement;
import com.fujitsu.vdmj.ast.statements.ASTNotYetSpecifiedStatement;
import com.fujitsu.vdmj.ast.statements.ASTPeriodicStatement;
import com.fujitsu.vdmj.ast.statements.ASTReturnStatement;
import com.fujitsu.vdmj.ast.statements.ASTSimpleBlockStatement;
import com.fujitsu.vdmj.ast.statements.ASTSkipStatement;
import com.fujitsu.vdmj.ast.statements.ASTSpecificationStatement;
import com.fujitsu.vdmj.ast.statements.ASTSporadicStatement;
import com.fujitsu.vdmj.ast.statements.ASTStartStatement;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.ast.statements.ASTStopStatement;
import com.fujitsu.vdmj.ast.statements.ASTSubclassResponsibilityStatement;
import com.fujitsu.vdmj.ast.statements.ASTTixeStatement;
import com.fujitsu.vdmj.ast.statements.ASTTrapStatement;
import com.fujitsu.vdmj.ast.statements.ASTWhileStatement;

/**
 * The base type for all ASTStatement visitors. All methods, by default, call
 * the abstract caseStatement method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class ASTStatementVisitor<R, S>
{
 	abstract public R caseStatement(ASTStatement node, S arg);

	public R caseAnnotatedStatement(ASTAnnotatedStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

	public R caseAlwaysStatement(ASTAlwaysStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseAssignmentStatement(ASTAssignmentStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseAtomicStatement(ASTAtomicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseBlockStatement(ASTBlockStatement node, S arg)
	{
		return caseSimpleBlockStatement(node, arg);
	}

 	public R caseCallObjectStatement(ASTCallObjectStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCallStatement(ASTCallStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCasesStatement(ASTCasesStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseClassInvariantStatement(ASTClassInvariantStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCyclesStatement(ASTCyclesStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseDefStatement(ASTDefStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseDurationStatement(ASTDurationStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseElseIfStatement(ASTElseIfStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseErrorStatement(ASTErrorStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseExitStatement(ASTExitStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForAllStatement(ASTForAllStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForIndexStatement(ASTForIndexStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForPatternBindStatement(ASTForPatternBindStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseIfStatement(ASTIfStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseLetBeStStatement(ASTLetBeStStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseLetDefStatement(ASTLetDefStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseNonDeterministicStatement(ASTNonDeterministicStatement node, S arg)
	{
		return caseSimpleBlockStatement(node, arg);
	}

 	public R caseNotYetSpecifiedStatement(ASTNotYetSpecifiedStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R casePeriodicStatement(ASTPeriodicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseReturnStatement(ASTReturnStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSimpleBlockStatement(ASTSimpleBlockStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSkipStatement(ASTSkipStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSpecificationStatement(ASTSpecificationStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSporadicStatement(ASTSporadicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseStartStatement(ASTStartStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseStopStatement(ASTStopStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSubclassResponsibilityStatement(ASTSubclassResponsibilityStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseTixeStatement(ASTTixeStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseTrapStatement(ASTTrapStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseWhileStatement(ASTWhileStatement node, S arg)
	{
		return caseStatement(node, arg);
	}
}