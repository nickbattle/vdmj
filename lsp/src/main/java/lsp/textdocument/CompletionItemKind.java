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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package lsp.textdocument;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;

public enum CompletionItemKind
{
	Text(1),
	Method(2),
	Function(3),
	Constructor(4),
	Field(5),
	Variable(6),
	Class(7),
	Interface(8),
	Module(9),
	Property(10),
	Unit(11),
	Value(12),
	Enum(13),
	Keyword(14),
	Snippet(15),
	Color(16),
	File(17),
	Reference(18),
	Folder(19),
	EnumMember(20),
	Constant(21),
	Struct(22),
	Event(23),
	Operator(24),
	TypeParameter(25);
	
	private final long value;
	
	private CompletionItemKind(long value)
	{
		this.value = value;
	}
	
	public Long getValue()
	{
		return value;
	}

	public static CompletionItemKind kindOf(TCDefinition def)
	{
		return kindOf(def.kind());
	}

	public static CompletionItemKind kindOf(ASTDefinition def)
	{
		return kindOf(def.kind());
	}
	
	public static CompletionItemKind kindOf(String kind)
	{	
		switch (kind)
		{
			case "module":
				return Module;
			
			case "class":
				return Class;
				
			case "explicit function":
			case "implicit function":
				return Function;

			case "explicit operation":
			case "implicit operation":
				return Method;
				
			case "instance variable":
				return Field;
				
			case "value":
				return Constant;
				
			case "type":
				return Struct;
				
			default:
				return Text;
		}
	}
}
