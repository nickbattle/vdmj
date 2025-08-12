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

import java.util.LinkedHashMap;

import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POOperationExtractor;
import com.fujitsu.vdmj.po.statements.POFieldDesignator;
import com.fujitsu.vdmj.po.statements.POIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POMapSeqDesignator;
import com.fujitsu.vdmj.po.statements.POStateDesignator;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class POStateDesignatorExtractor extends POStateDesignatorVisitor<POStateDesignator, Object>
{
	private final POOperationExtractor extractor;
	private final LinkedHashMap<TCNameToken, POApplyExpression> substitutions;

	public POStateDesignatorExtractor()
	{
		this.extractor = new POOperationExtractor("$");
		this.substitutions = new LinkedHashMap<TCNameToken, POApplyExpression>();
	}

	public LinkedHashMap<TCNameToken, POApplyExpression> getSubstitutions()
	{
		return substitutions;
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
		POStateDesignator mapseq = node.mapseq.apply(this, arg);	// depth first

		POExpression subs = node.exp.apply(extractor, null);
		substitutions.putAll(extractor.getSubstitutions());

		return new POMapSeqDesignator(
			mapseq,
			subs,
			node.seqType
		);
	}

	@Override
	POStateDesignator caseStateDesignator(POStateDesignator node)
	{
		return node;	// Not expected?
	}
}
