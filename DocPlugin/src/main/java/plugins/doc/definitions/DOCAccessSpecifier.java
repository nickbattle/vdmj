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

import plugins.doc.DOCNode;

import com.fujitsu.vdmj.lex.Token;

public class DOCAccessSpecifier extends DOCNode
{
	private static final long serialVersionUID = 1L;
	public final static DOCAccessSpecifier DEFAULT = new DOCAccessSpecifier(false, false, Token.PRIVATE, false);
	
	private final boolean isStatic;
	private final boolean isAsync;
	private final Token access;
	private final boolean isPure;

	public DOCAccessSpecifier(boolean isStatic, boolean isAsync, Token access, boolean isPure)
	{
		super();
		this.isStatic = isStatic;
		this.isAsync = isAsync;
		this.access = access;
		this.isPure = isPure;
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
