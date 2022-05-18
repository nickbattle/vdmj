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

package dap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

import workspace.Diag;

public class DAPServerSocket implements Runnable
{
	private Dialect dialect;
	private static int port;

	public DAPServerSocket(Dialect dialect, int port)
	{
		this.dialect = dialect;
		Settings.dialect = dialect;		// Race with LSP that does this too
		DAPServerSocket.port = port;
	}

	public static void main(String[] args)
	{
		Dialect dialect = Dialect.VDM_SL;
		int dapPort = -1;
		
		for (int a=0; a<args.length; a++)
		{
			if (args[a].equals("-vdmsl"))
			{
				dialect = Dialect.VDM_SL;
			}
			else if (args[a].equals("-vdmpp"))
			{
				dialect = Dialect.VDM_PP;
			}
			else if (args[a].equals("-vdmrt"))
			{
				dialect = Dialect.VDM_RT;
			}
			else if (args[a].equals("-dap"))
			{
				dapPort = Integer.parseInt(args[++a]);
			}
			else
			{
				Diag.error("Usage: DAPSocketServer [-vdmsl | -vdmpp | -vdmrt] -dap");
			}
		}
		
		new DAPServerSocket(dialect, dapPort).run();
	}
	
	@Override
	public void run()
	{
		ServerSocket socket = null;

		try
		{
			socket = new ServerSocket(port, 10);
			port = socket.getLocalPort();	// In case of zero allocated
			
			while (true)
			{
				Diag.info("DAP %s Server listening on port %d", dialect, port);
				Socket conn = socket.accept();
				
				try
				{
					new DAPServer(dialect, conn).run();
				}
				catch (IOException e)
				{
					Diag.error("DAP Server stopped: %s", e.getMessage());
				}

				Diag.info("DAP %s Server closing port %d", dialect, port);
				conn.close();
			}
		}
		catch (IOException e)
		{
			try
			{
				Diag.error("DAP Server socket error: %s", e.getMessage());
				if (socket != null) socket.close();
			}
			catch (IOException e1)
			{
				// Ignore
			}
		}
	}
	
	public static int getPort()
	{
		return port;	// Allocated port
	}
}
