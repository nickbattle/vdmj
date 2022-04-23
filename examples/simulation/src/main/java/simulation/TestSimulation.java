/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package simulation;

import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;

public class TestSimulation extends RemoteSimulation
{
	@Override
	public void setup(ASTClassList classes)
	{
		setParameter(classes, "SETTINGS", "PARAM", 1.23);
		setParameter(classes, "zz", "ss", 1.23);
	}
}
