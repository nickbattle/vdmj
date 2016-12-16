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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.traces;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * A class representing a let-definition trace binding.
 */
public class ASTTraceLetDefBinding extends ASTTraceDefinition
{
    private static final long serialVersionUID = 1L;
	public final ASTDefinitionList localDefs;
	public final ASTTraceDefinition body;

	public ASTTraceLetDefBinding(
		LexLocation location, ASTDefinitionList localDefs, ASTTraceDefinition body)
	{
		super(location);
		this.localDefs = localDefs;
		this.body = body;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder("let ");

		for (ASTDefinition d: localDefs)
		{
			result.append(d.toString());
			result.append(" ");
		}

		result.append("in ");
		result.append(body);

		return result.toString();
	}
}
