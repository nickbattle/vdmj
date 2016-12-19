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

package com.fujitsu.vdmj.ast.definitions;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.types.ASTFieldList;

/**
 * A class to hold a module's state definition.
 */
public class ASTStateDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final ASTFieldList fields;
	public final ASTPattern invPattern;
	public final ASTExpression invExpression;
	public final ASTPattern initPattern;
	public final ASTExpression initExpression;

	public ASTStateDefinition(LexNameToken name, ASTFieldList fields,
		ASTPattern invPattern, ASTExpression invExpression,
		ASTPattern initPattern, ASTExpression initExpression)
	{
		super(name.location, name);

		this.fields = fields;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		this.initPattern = initPattern;
		this.initExpression = initExpression;
	}

	@Override
	public String toString()
	{
		return "state " + name + "of\n" + fields +
			(invPattern == null ? "" : "\n\tinv " + invPattern + " == " + invExpression) +
    		(initPattern == null ? "" : "\n\tinit " + initPattern + " == " + initExpression);
	}

	@Override
	public String kind()
	{
		return "state";
	}
}
