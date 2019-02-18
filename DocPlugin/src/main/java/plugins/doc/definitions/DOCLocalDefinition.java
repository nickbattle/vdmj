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
 
package plugins.doc.definitions;

import plugins.doc.definitions.DOCDefinition;
import plugins.doc.lex.DOCNameToken;
import plugins.doc.types.DOCType;

import com.fujitsu.vdmj.lex.LexLocation;

public class DOCLocalDefinition extends DOCDefinition
{
	private static final long serialVersionUID = 1L;
	private final DOCType type;

	public DOCLocalDefinition(LexLocation location, DOCNameToken name, DOCType type)
	{
		super(location, DOCAccessSpecifier.DEFAULT, name);
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
