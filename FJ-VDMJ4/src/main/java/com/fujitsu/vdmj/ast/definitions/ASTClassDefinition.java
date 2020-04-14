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

import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;

/**
 * A class to represent a VDM++ class definition.
 */
public class ASTClassDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;

	/** The names of the superclasses of this class. */
	public final LexNameList supernames;
	/** The definitions in this class (excludes superclasses). */
	public final ASTDefinitionList definitions;

	/**
	 * Create a class definition with the given name, list of superclass names,
	 * and list of local definitions.
	 *
	 * @param className
	 * @param supernames
	 * @param definitions
	 */
	public ASTClassDefinition(LexNameToken className,
		LexNameList supernames, ASTDefinitionList definitions)
	{
		super(className.location, className);

		this.supernames = supernames;
		this.definitions = definitions;

		// Classes are all effectively public types
		this.setAccessSpecifier(new ASTAccessSpecifier(false, false, Token.PUBLIC, false));
	}

	/**
	 * Create a dummy class for the interpreter.
	 */
	public ASTClassDefinition()
	{
		this(new LexNameToken("CLASS", "DEFAULT", new LexLocation()),
			new LexNameList(), new ASTDefinitionList());
	}

	@Override
	public String toString()
	{
		return	"class " + name.name +
				(supernames.isEmpty() ? "" : " is subclass of " + supernames) + "\n" +
				definitions.toString() +
				"end " + name.name + "\n";
	}

	@Override
	public String kind()
	{
		return "class";
	}

	@Override
	public <R, S> R apply(ASTDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseClassDefinition(this, arg);
	}
}
