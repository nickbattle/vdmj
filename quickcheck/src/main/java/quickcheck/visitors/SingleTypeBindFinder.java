/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package quickcheck.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.patterns.INBind;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.in.patterns.INTypeBind;
import com.fujitsu.vdmj.in.patterns.visitors.INLeafBindVisitor;

public class SingleTypeBindFinder extends INLeafBindVisitor<INBindingOverride, List<INBindingOverride>, Object>
{
	public SingleTypeBindFinder(INVisitorSet<INBindingOverride, List<INBindingOverride>, Object> inVisitorSet)
	{
		this.visitorSet = inVisitorSet;
	}

	@Override
	protected List<INBindingOverride> newCollection()
	{
		return new Vector<INBindingOverride>();
	}
	
	@Override
	public List<INBindingOverride> caseTypeBind(INTypeBind node, Object arg)
	{
		List<INBindingOverride> binds = newCollection();
		binds.add(node.setter);
		return binds;
	}

	@Override
	public List<INBindingOverride> caseBind(INBind node, Object arg)
	{
		return newCollection();
	}
}
