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

package vdmj.commands;

import java.io.IOException;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.InitExecutor;

public class InitCommand extends Command
{
	public static final String USAGE = "Usage: init";
	public static final String[] HELP =	{ "init", "init - re-initialize the specification" };
	
	public InitCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length != 1)
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		InitExecutor exec = new InitExecutor("init", request, null)
		{
			@Override
			protected void head() throws IOException
			{
				// No header
			}
			
			@Override
			protected void tail(double time) throws IOException
			{
				server.stdout("Global context initialized in " + time + " secs.\n");
			}

			@Override
			protected void error(Exception e) throws IOException
			{
				server.writeMessage(new DAPResponse(request, false, e.getMessage(), null));
				server.stdout("Init terminated.");
			}
		};
		
		exec.start();
		return null;
	}
}
