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

package dap;

import java.io.IOException;

import com.fujitsu.vdmj.runtime.Breakpoint;

import lsp.CancellableThread;
import vdmj.DAPDebugReader;
import workspace.DAPWorkspaceManager;
import workspace.Log;

public abstract class AsyncExecutor extends CancellableThread
{
	protected final DAPServer server = DAPServer.getInstance();
	protected final DAPWorkspaceManager manager = DAPWorkspaceManager.getInstance();
	protected final DAPRequest request;

	public AsyncExecutor(String id, DAPRequest request)
	{
		super(id);
		this.request = request;
	}
	
	@Override
	public void body()
	{
		DAPDebugReader dbg = null;
		
		try
		{
			dbg = new DAPDebugReader();
			manager.setDebugReader(dbg);
			dbg.start();
			
			head();
			
			long before = System.currentTimeMillis();
			exec();
			long after = System.currentTimeMillis();
			
			tail((double)(after-before)/1000);
		}
		catch (Exception e)
		{
			try
			{
				Log.error(e);
				error(e);
			}
			catch (IOException io)
			{
				Log.error(io);
			}
		}
		finally
		{
			try
			{
				clean();
			}
			catch (IOException e)
			{
				// ignore
			}
			
			if (dbg != null)
			{
				dbg.interrupt();	// Stop the debugger reader.
			}
			
			manager.setDebugReader(null);
		}
	}
	
	protected abstract void head() throws IOException;

	protected abstract void exec() throws Exception;

	protected abstract void tail(double time) throws Exception;

	protected abstract void error(Exception e) throws IOException;

	protected abstract void clean() throws IOException;

	@Override
	public void setCancelled()
	{
		super.setCancelled();
		Breakpoint.setExecCancelled();
	}
}
