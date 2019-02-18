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
 
package plugins.doc.types;

import java.util.HashSet;

import plugins.doc.DOCNode;

import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;

public class DOCTypeSet extends HashSet<DOCType>
{
	private static final long serialVersionUID = 1L;

	public DOCTypeSet(TCTypeSet types) throws Exception
	{
		ClassMapper mapper = ClassMapper.getInstance(DOCNode.MAPPINGS);
		
		for (TCType type: types)
		{
			add((DOCType)mapper.convert(type));
		}
	}

	public void extent(int maxWidth)
	{
		return;
	}
	
	public String toHTML(int indent)
	{
		return null;
	}
}
