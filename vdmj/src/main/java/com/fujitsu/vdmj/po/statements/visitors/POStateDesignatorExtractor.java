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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.statements.visitors;

import com.fujitsu.vdmj.po.statements.POFieldDesignator;
import com.fujitsu.vdmj.po.statements.POIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POMapSeqDesignator;
import com.fujitsu.vdmj.po.statements.POStateDesignator;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POStateDesignatorExtractor extends POStateDesignatorVisitor<POStateDesignator, Object>
{
	private POStatement stmt;
	private ProofObligationList obligations;
	private POGState pogState;
	private POContextStack ctxt;
	private Environment env;

	public POStateDesignatorExtractor(POStatement stmt,
		ProofObligationList obligations, POGState pogState, POContextStack ctxt, Environment env)
	{
		this.stmt = stmt;
		this.obligations = obligations;
		this.pogState = pogState;
		this.ctxt = ctxt;
		this.env = env;
	}

	public POStateDesignator caseIdentifierDesignator(POIdentifierDesignator node, Object arg)
	{
		return node;
	};

	public POStateDesignator caseFieldDesignator(POFieldDesignator node, Object arg)
	{
		return new POFieldDesignator(
			node.object.apply(this, arg),
			node.field,
			node.recType,
			node.clsType
		);
	}

	public POStateDesignator caseMapSeqDesignator(POMapSeqDesignator node, Object arg)
	{
		return new POMapSeqDesignator(
			node.mapseq.apply(this, arg),
			stmt.extractOpCalls(node.exp, obligations, pogState, ctxt, env),
			node.seqType
		);
	}

	@Override
	POStateDesignator caseStateDesignator(POStateDesignator node)
	{
		return node;	// Not processed yet?
	}
}
