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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj;

import java.io.File;
import java.util.List;

import com.fujitsu.vdmj.ast.definitions.ASTBUSClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTCPUClassDefinition;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.InternalException;

public class VDMRT extends VDMPP
{
	public VDMRT()
	{
		Settings.dialect = Dialect.VDM_RT;
	}
	
	@Override
	public ExitStatus parse(List<File> files)
	{
		ExitStatus e = super.parse(files);
		
		RemoteSimulation rs = RemoteSimulation.getInstance();
		
		if (rs != null)
		{
			try
			{
				rs.setup(parsedClasses);
			}
			catch (Exception ex)
			{
				println("Simulation: " + ex.getMessage());
				e = ExitStatus.EXIT_ERRORS;
			}
		}
		
		return e;
	}

	@Override
	public ExitStatus typeCheck()
	{
		try
		{
			parsedClasses.add(new ASTCPUClassDefinition());
			parsedClasses.add(new ASTBUSClassDefinition());
		}
		catch (Exception e)
		{
			throw new InternalException(11, "CPU or BUS creation failure");
		}

		return super.typeCheck();
	}
}
