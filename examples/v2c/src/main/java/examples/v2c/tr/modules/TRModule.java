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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package examples.v2c.tr.modules;

import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

import examples.v2c.tr.TRNode;
import examples.v2c.tr.definitions.TRDefinitionList;

public class TRModule extends TRNode
{
	private static final long serialVersionUID = 1L;
	private final TCIdentifierToken name;
	private final TRDefinitionList definitions;
	
	public TRModule(TCIdentifierToken name, TRDefinitionList definitions)
	{
		this.name = name;
		this.definitions = definitions;
	}

	public String translate()
	{
		return "// Module " + name + "\n" + definitions.translate();
	}
}
