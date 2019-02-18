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
 
package plugins.doc.modules;

import plugins.doc.lex.DOCNameList;
import plugins.doc.modules.DOCExport;
import plugins.doc.types.DOCType;

import com.fujitsu.vdmj.lex.LexLocation;

public class DOCExportedFunction extends DOCExport
{
	private final DOCNameList nameList;
	private final DOCType type;
	private final DOCNameList typeParams;

	public DOCExportedFunction(LexLocation location, DOCNameList nameList, DOCType type, DOCNameList typeParams)
	{
		super(location);
		this.nameList = nameList;
		this.type = type;
		this.typeParams = typeParams;
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
