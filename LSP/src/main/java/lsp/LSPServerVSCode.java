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

package lsp;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

import com.fujitsu.vdmj.lex.Dialect;

import dap.DAPServerSocket;
import workspace.Log;

public class LSPServerVSCode implements Runnable
{
	private Dialect dialect;
	private int port;

	public LSPServerVSCode(Dialect dialect, int port)
	{
		this.dialect = dialect;
		this.port = port;
	}

	public static void main(String[] args)
	{
		Dialect dialect = Dialect.VDM_SL;
		int dapPort = -1;
		int lspPort = -1;
		
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
			else if (args[a].equals("-lsp"))
			{
				lspPort = Integer.parseInt(args[++a]);
			}
			else if (args[a].equals("-dap"))
			{
				dapPort = Integer.parseInt(args[++a]);
			}
			else
			{
				usage();
			}
		}
		
		if (lspPort < 0)
		{
			usage();
		}
		
		if (dapPort > 0)
		{
			new Thread(new DAPServerSocket(dialect, dapPort), "DAP Listener").start();
		}
		
		new LSPServerVSCode(dialect, lspPort).run();
	}
	
	private static void usage()
	{
		System.err.println("Usage: LSPServerVSCode [-vdmsl | -vdmpp] -lsp [-dap]");
		System.exit(1);
	}
	
	@Override
	public void run()
	{
		try
		{
			Socket socket = new Socket("localhost", port);
			Log.printf("LSP %s Server listening on port %d", dialect, port);

			while (true)
			{
			   new LSPServer(dialect, socket.getInputStream(), socket.getOutputStream()).run();
			   socket.close();
			}
		}
		catch (ConnectException e)
		{
			System.err.println("Connection exception: you have to start VSCode first?");
			Log.error(e);
		}
		catch (IOException e)
		{
			Log.error(e);
		}
	}
}
