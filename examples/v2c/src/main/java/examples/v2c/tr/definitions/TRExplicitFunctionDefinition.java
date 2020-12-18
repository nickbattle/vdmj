/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package examples.v2c.tr.definitions;

import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

import examples.v2c.tr.expressions.TRExpression;
import examples.v2c.tr.patterns.TRParameterList;
import examples.v2c.tr.types.TRFunctionType;
import examples.v2c.tr.types.TRTypeList;

public class TRExplicitFunctionDefinition extends TRDefinition
{
	private static final long serialVersionUID = 1L;
	private final TCNameToken name;
	private final TRFunctionType type;
	private final TRParameterList parameters;
	private final TRExpression body;
	
	public TRExplicitFunctionDefinition(LexCommentList comments, TCNameToken name, TRFunctionType type, TRParameterList parameters, TRExpression body)
	{
		super(comments);
		this.name = name;
		this.type = type;
		this.parameters = parameters;
		this.body = body;
	}

	@Override
	public String translate()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.translate());
		sb.append(type.getResult().translate() + " " + name.getName() + "(");
		TRTypeList ptypes = type.getParameters();
		
		for (int i=0; i<ptypes.size(); i++)
		{
			sb.append(ptypes.get(i).translate());
			sb.append(" ");
			sb.append(parameters.get(i));
		}
		
		sb.append(")\n");
		sb.append("{\n    return ");
		sb.append(body.translate());
		sb.append(";\n}\n");
		
		return sb.toString();
	}
}
