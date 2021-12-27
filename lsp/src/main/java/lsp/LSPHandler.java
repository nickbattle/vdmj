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

import rpc.RPCHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import workspace.Diag;

abstract public class LSPHandler implements RPCHandler
{
	public LSPHandler()
	{
	}

	@Override
	abstract public RPCMessageList request(RPCRequest request);
	
	@Override
	public void response(RPCResponse message)
	{
		if (message.isError())
		{
			Diag.error("Error response to id %d received: %s", (Long)message.get("id"), message.getError());
			return;
		}
		else
		{
			Diag.info("Successful response to id %d ignored", (Long)message.get("id"));
		}
	}
}
