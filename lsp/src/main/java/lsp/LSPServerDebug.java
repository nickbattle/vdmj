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

package lsp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.VDMJMain;
import com.fujitsu.vdmj.lex.Dialect;

import dap.DAPServerSocket;
import workspace.Diag;

public class LSPServerDebug implements Runnable, VDMJMain
{
	private Dialect dialect;
	private int port;

	public LSPServerDebug(Dialect dialect, int port)
	{
		this.dialect = dialect;
		this.port = port;
	}
	
	public static String getMainName()
	{
		return LSP_MAIN;
	}

	public static void main(String[] args)
	{
		Settings.mainClass = LSPServerDebug.class;
		Dialect dialect = Dialect.VDM_SL;
		int dapPort = -1;
		int lspPort = -1;
		
		Diag.init(true);

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
		
		if (dapPort >= 0)	// Zero selects any free port
		{
			new Thread(new DAPServerSocket(dialect, dapPort), "DAP Listener").start();
		}
		
		Thread.currentThread().setName("LSP Listener");
		new LSPServerDebug(dialect, lspPort).run();
	}
	
	private static void usage()
	{
		System.err.println("Usage: LSPServerDebug [-vdmsl | -vdmpp | -vdmrt] -lsp <port> [-dap <port>]");
		System.exit(1);
	}
	
	@Override
	public void run()
	{
		ServerSocket socket = null;

		try
		{
			socket = new ServerSocket(port, 10);

			while (true)
			{
				Diag.info("LSP %s Server listening on port %d", dialect, socket.getLocalPort());
				Socket conn = socket.accept();
				
				try
				{
					new LSPServer(dialect, conn.getInputStream(), conn.getOutputStream()).run();
				}
				catch (IOException e)
				{
					Diag.severe("LSP Server stopped: %s", e.getMessage());
					Diag.error(e);
				}
				
				Diag.info("LSP %s Server closing port %d", dialect, socket.getLocalPort());
				conn.close();
			}
		}
		catch (IOException e)
		{
			try
			{
				Diag.info("LSP Server socket error: %s", e.getMessage());
				if (socket != null) socket.close();
			}
			catch (IOException e1)
			{
				// Ignore
			}
		}
	}
}
