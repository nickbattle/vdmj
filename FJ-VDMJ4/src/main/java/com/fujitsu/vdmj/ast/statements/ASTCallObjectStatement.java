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

package com.fujitsu.vdmj.ast.statements;

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.statements.visitors.ASTStatementVisitor;
import com.fujitsu.vdmj.util.Utils;

public class ASTCallObjectStatement extends ASTStatement
{
	private static final long serialVersionUID = 1L;
	public final ASTObjectDesignator designator;
	public final LexNameToken classname;
	public final LexIdentifierToken fieldname;
	public final ASTExpressionList args;
	public final boolean explicit;

	public ASTCallObjectStatement(ASTObjectDesignator designator,
		LexNameToken classname, ASTExpressionList args)
	{
		super(designator.location);

		this.designator = designator;
		this.classname = classname;
		this.fieldname = null;
		this.args = args;
		this.explicit = (classname.module != null);
	}

	public ASTCallObjectStatement(ASTObjectDesignator designator,
		LexIdentifierToken fieldname, ASTExpressionList args)
	{
		super(designator.location);

		this.designator = designator;
		this.classname = null;
		this.fieldname = fieldname;
		this.args = args;
		this.explicit = false;
	}

	@Override
	public String toString()
	{
		return designator + "." +
			(classname != null ? classname : fieldname) +
			"(" + Utils.listToString(args) + ")";
	}

	@Override
	public String kind()
	{
		return "object call";
	}

	@Override
	public <R, S> R apply(ASTStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCallObjectStatement(this, arg);
	}
}
