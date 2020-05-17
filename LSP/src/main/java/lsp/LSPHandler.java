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

import rpc.RPCHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import workspace.Log;

abstract public class LSPHandler implements RPCHandler
{
	protected LSPServerState lspServerState;

	public LSPHandler(LSPServerState state)
	{
		this.lspServerState = state;
	}

	@Override
	abstract public RPCMessageList request(RPCRequest request) throws IOException;
	
	@Override
	public void response(RPCResponse message)
	{
		Log.printf("Response to id %d ignored", (Long)message.get("id"));
	}
}
