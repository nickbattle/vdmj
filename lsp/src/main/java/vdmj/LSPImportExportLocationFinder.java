/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package vdmj;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCExport;
import com.fujitsu.vdmj.tc.modules.TCExportedFunction;
import com.fujitsu.vdmj.tc.modules.TCExportedOperation;
import com.fujitsu.vdmj.tc.modules.TCExportedType;
import com.fujitsu.vdmj.tc.modules.TCExportedValue;
import com.fujitsu.vdmj.tc.modules.TCImport;
import com.fujitsu.vdmj.tc.modules.visitors.TCImportExportVisitor;

public class LSPImportExportLocationFinder extends TCImportExportVisitor<TCNode, LexLocation>
{
	@Override
	public TCNode caseExport(TCExport node, LexLocation arg)
	{
		return null;
	}
	
	@Override
	public TCNode caseExportedValue(TCExportedValue node, LexLocation arg)
	{
		for (TCNameToken name: node.nameList)
		{
			if (arg.within(name.getLocation()))
			{
				return name;
			}
		}
		
		return null;
	}
	
	@Override
	public TCNode caseExportedFunction(TCExportedFunction node, LexLocation arg)
	{
		for (TCNameToken name: node.nameList)
		{
			if (arg.within(name.getLocation()))
			{
				return name;
			}
		}
		
		return null;
	}
	
	@Override
	public TCNode caseExportedOperation(TCExportedOperation node, LexLocation arg)
	{
		for (TCNameToken name: node.nameList)
		{
			if (arg.within(name.getLocation()))
			{
				return name;
			}
		}
		
		return null;
	}
	
	@Override
	public TCNode caseExportedType(TCExportedType node, LexLocation arg)
	{
		if (arg.within(node.name.getLocation()))
		{
			return node.name;
		}
		
		return null;
	}

	@Override
	public TCNode caseImport(TCImport node, LexLocation arg)
	{
		if (arg.within(node.name.getLocation()))
		{
			return node;
		}
		else if (node.renamed != null && arg.within(node.renamed.getLocation()))
		{
			return node;
		}
		else
		{
			return null;
		}
	}
}
