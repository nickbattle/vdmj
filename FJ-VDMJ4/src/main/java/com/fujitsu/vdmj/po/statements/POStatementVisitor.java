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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.po.annotations.POAnnotatedStatement;

/**
 * The base type for all POStatement visitors. All methods, by default, call
 * the abstract caseStatement method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class POStatementVisitor<R, S>
{
 	abstract public R caseStatement(POStatement node, S arg);

	public R caseAnnotatedStatement(POAnnotatedStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

	public R caseAlwaysStatement(POAlwaysStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseAssignmentStatement(POAssignmentStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseAtomicStatement(POAtomicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseBlockStatement(POBlockStatement node, S arg)
	{
		return caseSimpleBlockStatement(node, arg);
	}

 	public R caseCallObjectStatement(POCallObjectStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCallStatement(POCallStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCasesStatement(POCasesStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseClassInvariantStatement(POClassInvariantStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCyclesStatement(POCyclesStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseDefStatement(PODefStatement node, S arg)
	{
		return caseLetDefStatement(node, arg);
	}

 	public R caseDurationStatement(PODurationStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseElseIfStatement(POElseIfStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseErrorStatement(POErrorStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseExitStatement(POExitStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForAllStatement(POForAllStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForIndexStatement(POForIndexStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForPatternBindStatement(POForPatternBindStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseIfStatement(POIfStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseLetBeStStatement(POLetBeStStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseLetDefStatement(POLetDefStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseNonDeterministicStatement(PONonDeterministicStatement node, S arg)
	{
		return caseSimpleBlockStatement(node, arg);
	}

 	public R caseNotYetSpecifiedStatement(PONotYetSpecifiedStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R casePeriodicStatement(POPeriodicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseReturnStatement(POReturnStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSimpleBlockStatement(POSimpleBlockStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSkipStatement(POSkipStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSpecificationStatement(POSpecificationStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSporadicStatement(POSporadicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseStartStatement(POStartStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseStopStatement(POStopStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSubclassResponsibilityStatement(POSubclassResponsibilityStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseTixeStatement(POTixeStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseTrapStatement(POTrapStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseWhileStatement(POWhileStatement node, S arg)
	{
		return caseStatement(node, arg);
	}
}