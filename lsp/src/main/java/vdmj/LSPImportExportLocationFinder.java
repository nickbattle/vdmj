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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmj;

import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCExport;
import com.fujitsu.vdmj.tc.modules.TCExportedFunction;
import com.fujitsu.vdmj.tc.modules.TCExportedOperation;
import com.fujitsu.vdmj.tc.modules.TCExportedType;
import com.fujitsu.vdmj.tc.modules.TCExportedValue;
import com.fujitsu.vdmj.tc.modules.TCImport;
import com.fujitsu.vdmj.tc.modules.TCImportedValue;
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
			if (arg.touches(name.getLocation()))
			{
				return name;
			}
		}
		
		Set<TCNode> set = node.unresolved.matchUnresolved(arg);
		
		if (set.isEmpty())
		{
			return null;
		}
		else
		{
			return set.iterator().next();
		}
	}
	
	@Override
	public TCNode caseExportedFunction(TCExportedFunction node, LexLocation arg)
	{
		for (TCNameToken name: node.nameList)
		{
			if (arg.touches(name.getLocation()))
			{
				return name;
			}
		}
		
		Set<TCNode> set = node.unresolved.matchUnresolved(arg);
		
		if (set.isEmpty())
		{
			return null;
		}
		else
		{
			return set.iterator().next();
		}
	}
	
	@Override
	public TCNode caseExportedOperation(TCExportedOperation node, LexLocation arg)
	{
		for (TCNameToken name: node.nameList)
		{
			if (arg.touches(name.getLocation()))
			{
				return name;
			}
		}
		
		Set<TCNode> set = node.unresolved.matchUnresolved(arg);
		
		if (set.isEmpty())
		{
			return null;
		}
		else
		{
			return set.iterator().next();
		}
	}
	
	@Override
	public TCNode caseExportedType(TCExportedType node, LexLocation arg)
	{
		if (arg.touches(node.name.getLocation()))
		{
			return node.name;
		}
		
		return null;
	}
	
	@Override
	public TCNode caseImport(TCImport node, LexLocation arg)
	{
		if (arg.touches(node.name.getLocation()))
		{
			return node.name;
		}
		else if (node.renamed != null && arg.touches(node.renamed.getLocation()))
		{
			return node.renamed;
		}
		else if (node instanceof TCImportedValue)
		{
			TCImportedValue imp = (TCImportedValue)node;
			Set<TCNode> set = imp.unresolved.matchUnresolved(arg);
			
			if (set.isEmpty())
			{
				return null;
			}
			else
			{
				return set.iterator().next();
			}
		}
		else
		{
			return null;
		}
	}
}
