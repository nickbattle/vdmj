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

import rpc.RPCRequest;
import rpc.RPCResponse;
import rpc.RPCErrors;
import rpc.RPCMessageList;

public class InitializeHandler extends LSPHandler
{
	public InitializeHandler(LSPServerState state)
	{
		super(state);
	}

	@Override
	public RPCMessageList run(RPCRequest request) throws IOException
	{
		if ("initialize".equals(request.getMethod()))
		{
			RPCMessageList resp = lspServerState.getManager().initialize(request);
			resp.add(new RPCResponse(request, new InitializeResponse()));
			return resp;
		}
		else if ("initialized".equals(request.getMethod()))
		{
			lspServerState.setInitialized(true);
			return lspServerState.getManager().initialized(request);
		}
		
		return new RPCMessageList(request, RPCErrors.InternalError, "Unexpected initialize message");
	}
}
