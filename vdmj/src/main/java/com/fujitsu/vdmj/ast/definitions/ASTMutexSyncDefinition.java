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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.definitions;

import com.fujitsu.vdmj.ast.definitions.visitors.ASTDefinitionVisitor;
import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.util.Utils;

public class ASTMutexSyncDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final LexNameList operations;

	public ASTMutexSyncDefinition(LexLocation location, LexNameList operations)
	{
		super(location, null);
		this.operations = operations;
	}

	@Override
	public String kind()
	{
		return "mutex predicate";
	}

	@Override
	public String toString()
	{
		return "mutex(" +
			(operations.isEmpty() ? "all)" :
				Utils.listToString("", operations, ", ", ")"));
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTMutexSyncDefinition)
		{
			return toString().equals(other.toString());
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public <R, S> R apply(ASTDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMutexSyncDefinition(this, arg);
	}
}
