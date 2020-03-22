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
 *
 ******************************************************************************/

package dap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.fujitsu.vdmj.lex.Dialect;

import workspace.Log;

public class DAPServerSocket implements Runnable
{
	private Dialect dialect;
	private int port;

	public DAPServerSocket(Dialect dialect, int port)
	{
		this.dialect = dialect;
		this.port = port;
	}

	public static void main(String[] args)
	{
		Dialect dialect = Dialect.VDM_SL;
		int dapPort = -1;
		
		Log.init();

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
			else if (args[a].equals("-dap"))
			{
				dapPort = Integer.parseInt(args[++a]);
			}
			else
			{
				Log.error("Usage: DAPSocketServer [-vdmsl | -vdmpp] -dap");
			}
		}
		
		new DAPServerSocket(dialect, dapPort).run();
	}
	
	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				ServerSocket socket = new ServerSocket(port, 10);
				Log.printf("DAP %s Server listening on port %d", dialect, port);
	
				Socket conn = socket.accept();
				new DAPServer(dialect, conn.getInputStream(), conn.getOutputStream()).run();
				socket.close();
			}
		}
		catch (IOException e)
		{
			Log.error("DAP Server stopped: %s", e.getMessage());
		}
	}
}
