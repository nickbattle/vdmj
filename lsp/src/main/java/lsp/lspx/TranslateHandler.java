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

package lsp.lspx;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import json.JSONObject;
import lsp.LSPHandler;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.EventHub;
import workspace.MessageHub;
import workspace.events.UnknownTranslationEvent;
import workspace.plugins.LSPPlugin;
import workspace.plugins.TRPlugin;

public class TranslateHandler extends LSPHandler
{
	public TranslateHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		if (!LSPPlugin.getInstance().hasClientCapability("experimental.translateProvider"))
		{
			return new RPCMessageList(request, RPCErrors.MethodNotFound, "Translate capability is not enabled by client");
		}

		switch (request.getMethod())
		{
			case "slsp/TR/translate":
				return translate(request);

			default:
				return new RPCMessageList(request, RPCErrors.MethodNotFound, "Unexpected slsp/translate method");
		}
	}

	private RPCMessageList translate(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			File file = Utils.uriToFile(params.get("uri"));
			File saveUri = Utils.uriToFile(params.get("saveUri"));
			String language = params.get("languageId");
			JSONObject options = params.get("options");
			
			if (saveUri.exists())
			{
				if (saveUri.isDirectory())
				{
					if (saveUri.list().length != 0)
					{
						return new RPCMessageList(request, RPCErrors.InvalidParams, "saveUri is not empty");		
					}
				}
				else
				{
					return new RPCMessageList(request, RPCErrors.InvalidParams, "saveUri is not a folder");
				}
			}
			else
			{
				return new RPCMessageList(request, RPCErrors.InvalidParams, "saveUri does not exist");
			}
			
			TRPlugin tr = registry.getPlugin("TR");
			
			switch (language)
			{
				case "latex":
					return tr.translateLaTeX(request, file, saveUri, options);
				
				case "word":
					return tr.translateWord(request, file, saveUri, options);
				
				case "coverage":
					return tr.translateCoverage(request, file, saveUri, options);
				
				case "graphviz":
					return tr.translateGraphviz(request, file, saveUri, options);
				
				default:
					RPCMessageList result = EventHub.getInstance().publish(new UnknownTranslationEvent(request, language));
					
					if (result.isEmpty())	// Not handled
					{
						Diag.error("No external plugin registered for " + language);
						return new RPCMessageList(request, RPCErrors.MethodNotFound, language);
					}
					else
					{
						// Allow translations to raise errors
						result.addAll(MessageHub.getInstance().getDiagnosticResponses());
					}
					
					return result;
			}
			
		}
		catch (URISyntaxException e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InvalidParams, "URI syntax error");
		}
		catch (IOException e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
