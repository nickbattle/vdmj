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

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.visitors.TCLeafMultipleBindVisitor;
import com.fujitsu.vdmj.typechecker.Environment;

import smtlib.ast.Command;
import smtlib.ast.Script;

public class SMTMultiBindAnalysis extends TCLeafMultipleBindVisitor<Command, Script, Environment>
{
	public SMTMultiBindAnalysis(TCVisitorSet<Command, Script, Environment> visitorSet)
	{
		this.visitorSet = visitorSet;
	}

	@Override
	protected Script newCollection()
	{
		return new Script();
	}

	@Override
	public Script caseMultipleBind(TCMultipleBind node, Environment arg)
	{
		return newCollection();
	}
}
