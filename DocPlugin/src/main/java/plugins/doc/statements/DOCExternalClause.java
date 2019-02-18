/*******************************************************************************
 *
 *	Copyright (c) 2019 Paul Chisholm
 *
 *	Author: Paul Chisholm
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
 
package plugins.doc.statements;

import plugins.doc.DOCNode;
import plugins.doc.lex.DOCNameList;
import plugins.doc.types.DOCType;

import com.fujitsu.vdmj.ast.lex.LexToken;

public class DOCExternalClause extends DOCNode
{
	private final LexToken mode;
	private final DOCNameList identifiers;
	private final DOCType type;

	public DOCExternalClause(LexToken mode, DOCNameList identifiers, DOCType type)
	{
		super();
		this.mode = mode;
		this.identifiers = identifiers;
		this.type = type;
	}

	@Override
	public void extent(int maxWidth)
	{
		return;
	}
	
	@Override
	public String toHTML(int indent)
	{
		return null;
	}
}
