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

package com.fujitsu.vdmj.ast.types.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.ast.ASTVisitorSet;
import com.fujitsu.vdmj.ast.types.ASTBracketType;
import com.fujitsu.vdmj.ast.types.ASTField;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.ast.types.ASTInMapType;
import com.fujitsu.vdmj.ast.types.ASTMapType;
import com.fujitsu.vdmj.ast.types.ASTNamedType;
import com.fujitsu.vdmj.ast.types.ASTOperationType;
import com.fujitsu.vdmj.ast.types.ASTOptionalType;
import com.fujitsu.vdmj.ast.types.ASTProductType;
import com.fujitsu.vdmj.ast.types.ASTRecordType;
import com.fujitsu.vdmj.ast.types.ASTSeq1Type;
import com.fujitsu.vdmj.ast.types.ASTSeqType;
import com.fujitsu.vdmj.ast.types.ASTSet1Type;
import com.fujitsu.vdmj.ast.types.ASTSetType;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTUnionType;

/**
 * This TCType visitor visits all of the leaves of a type tree and calls
 * the basic processing methods for the simple types.
 */
public abstract class ASTLeafTypeVisitor<E, C extends Collection<E>, S> extends ASTTypeVisitor<C, S>
{
	protected ASTVisitorSet<E, C, S> visitorSet = new ASTVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			typeVisitor = ASTLeafTypeVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return ASTLeafTypeVisitor.this.newCollection();
		}
	};

	@Override
	public C caseBracketType(ASTBracketType node, S arg)
	{
		return node.type.apply(this, arg);
	}
	
	@Override
	public C caseFunctionType(ASTFunctionType node, S arg)
	{
		C all = newCollection();
		
		for (ASTType param: node.parameters)
		{
			all.addAll(param.apply(this, arg));
		}
		
		all.addAll(node.result.apply(this, arg));
		return all;
	}

	@Override
	public C caseInMapType(ASTInMapType node, S arg)
	{
		return caseMapType(node, arg);
	}

	@Override
	public C caseMapType(ASTMapType node, S arg)
	{
		C all = node.from.apply(this, arg);
		all.addAll(node.to.apply(this, arg));
		return all;
	}

	@Override
	public C caseNamedType(ASTNamedType node, S arg)
	{
		// NB. This won't recurse, unlike TCType, because the node.type is unresolved
		return node.type.apply(this, arg);
	}

	@Override
	public C caseOperationType(ASTOperationType node, S arg)
	{
		C all = newCollection();
		
		for (ASTType param: node.parameters)
		{
			all.addAll(param.apply(this, arg));
		}
		
		all.addAll(node.result.apply(this, arg));
		return all;
	}

	@Override
	public C caseOptionalType(ASTOptionalType node, S arg)
	{
		return node.type.apply(this, arg);
	}

	@Override
	public C caseProductType(ASTProductType node, S arg)
	{
		C all = newCollection();
		
		for (ASTType param: node.types)
		{
			all.addAll(param.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseRecordType(ASTRecordType node, S arg)
	{
		C all = newCollection();
		
		for (ASTField field: node.fields)
		{
			// NB. This won't recurse, unlike TCType, because the field.type is unresolved
			all.addAll(field.type.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseSeq1Type(ASTSeq1Type node, S arg)
	{
		return caseSeqType(node, arg);
	}

	@Override
	public C caseSeqType(ASTSeqType node, S arg)
	{
		return node.seqof.apply(this, arg);
	}

	@Override
	public C caseSet1Type(ASTSet1Type node, S arg)
	{
		return caseSetType(node, arg);
	}

	@Override
	public C caseSetType(ASTSetType node, S arg)
	{
		return node.setof.apply(this, arg);
	}

	@Override
	public C caseUnionType(ASTUnionType node, S arg)
	{
		C all = newCollection();
		
		for (ASTType param: node.types)
		{
			all.addAll(param.apply(this, arg));
		}
		
		return all;
	}
	
	abstract protected C newCollection();
}
