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

package workspace;

import lsp.LSPMessageUtils;
import rpc.RPCErrors;
import rpc.RPCMessageList;

abstract public class WorkspacePlugin
{
	protected final WorkspaceManager manager;
	protected final LSPMessageUtils messages;
	
	public WorkspacePlugin(WorkspaceManager manager)
	{
		this.manager = manager;
		messages = new LSPMessageUtils();
	}
	
	protected RPCMessageList errorResult()
	{
		return new RPCMessageList(null, RPCErrors.InternalError, "?");
	}
	
	abstract public void init();

	abstract protected RPCMessageList processEvent(String event, Object... args) throws Exception;
}
