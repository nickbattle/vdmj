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

package lsp.textdocument;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;

public enum SymbolKind
{
	File(1),
	Module(2),
	Namespace(3),
	Package(4),
	Class(5),
	Method(6),
	Property(7),
	Field(8),
	Constructor(9),
	Enum(10),
	Interface(11),
	Function(12),
	Variable(13),
	Constant(14),
	String(15),
	Number(16),
	Boolean(17),
	Array(18),
	Object(19),
	Key(20),
	Null(21),
	EnumMember(22),
	Struct(23),
	Event(24),
	Operator(25),
	TypeParameter(26);
	
	private final long value;
	
	private SymbolKind(long value)
	{
		this.value = value;
	}
	
	public Long getValue()
	{
		return value;
	}

	public static SymbolKind kindOf(TCDefinition def)
	{
		return kindOf(def.kind());
	}

	public static SymbolKind kindOf(ASTDefinition def)
	{
		return kindOf(def.kind());
	}
	
	public static SymbolKind kindOf(String kind)
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
				return Object;	// Null is "blank" on the Outline!
		}
	}
}
