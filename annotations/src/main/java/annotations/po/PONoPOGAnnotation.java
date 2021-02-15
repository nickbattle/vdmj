/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package annotations.po;

import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.modules.POModule;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class PONoPOGAnnotation extends POAnnotation
{
	public PONoPOGAnnotation(TCIdentifierToken name, POExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void poAfter(PODefinition def, ProofObligationList obligations, POContextStack ctxt)
	{
		obligations.clear();
	}

	@Override
	public void poAfter(POStatement stmt, ProofObligationList obligations, POContextStack ctxt)
	{
		obligations.clear();
	}

	@Override
	public void poAfter(POExpression exp, ProofObligationList obligations, POContextStack ctxt)
	{
		obligations.clear();
	}

	@Override
	public void poAfter(POModule module, ProofObligationList obligations)
	{
		obligations.clear();
	}
}
