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
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.types.ASTInvariantType;

/**
 * A class to hold a type definition.
 */
public class ASTTypeDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final ASTInvariantType type;
	public final ASTPattern invPattern;
	public final ASTExpression invExpression;
	public final ASTPattern eqPattern1;
	public final ASTPattern eqPattern2;
	public final ASTExpression eqExpression;
	public final ASTPattern ordPattern1;
	public final ASTPattern ordPattern2;
	public final ASTExpression ordExpression;

	public ASTTypeDefinition(LexNameToken name, ASTInvariantType type, ASTPattern invPattern,
		ASTExpression invExpression, ASTPattern eqPattern1, ASTPattern eqPattern2, ASTExpression eqExpression,
		ASTPattern ordPattern1, ASTPattern ordPattern2, ASTExpression ordExpression)
	{
		super(name.location, name);

		this.type = type;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		
		this.eqPattern1 = eqPattern1;
		this.eqPattern2 = eqPattern2;
		this.eqExpression = eqExpression;
		
		this.ordPattern1 = ordPattern1;
		this.ordPattern2 = ordPattern2;
		this.ordExpression = ordExpression;
	}

	@Override
	public String toString()
	{
		return accessSpecifier.ifSet(" ") +
				name.name + " = " + type.toDetailedString() +
				(invPattern == null ? "" :
					"\n\tinv " + invPattern + " == " + invExpression) +
        		(eqPattern1 == null ? "" :
        			"\n\teq " + eqPattern1 + " = " + eqPattern2 + " == " + eqExpression) +
        		(ordPattern1 == null ? "" :
        			"\n\tord " + ordPattern1 + " < " + ordPattern2 + " == " + ordExpression);
	}

	@Override
	public String kind()
	{
		return "type";
	}

	@Override
	public <R, S> R apply(ASTDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTypeDefinition(this, arg);
	}
}
