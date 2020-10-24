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

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.po.definitions.POClassList;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.LSPDefinitionFinder;
import vdmj.LSPDefinitionFinder.Found;
import workspace.plugins.ASTPluginPR;
import workspace.plugins.INPlugin;
import workspace.plugins.INPluginPR;
import workspace.plugins.POPlugin;
import workspace.plugins.POPluginPR;
import workspace.plugins.TCPlugin;
import workspace.plugins.TCPluginPR;

public class WorkspaceManagerPP extends WorkspaceManager
{
	public WorkspaceManagerPP()
	{
		Settings.dialect = Dialect.VDM_PP;
		registerPlugin(new ASTPluginPR(this));
		registerPlugin(new TCPluginPR(this));
		registerPlugin(new INPluginPR(this));
		registerPlugin(new POPluginPR(this));
	}
	
	protected ASTClassList extras()
	{
		return new ASTClassList();		// Overridden in RT to add CPU and BUS
	}

	@Override
	protected TCNode findLocation(File file, int zline, int zcol)
	{
		TCPlugin plugin = getPlugin("TC");
		TCClassList tcClassList = plugin.getTC();
		
		if (tcClassList != null && !tcClassList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			Found found = finder.findLocation(tcClassList, file, zline + 1, zcol + 1);
			
			if (found != null)
			{
				return found.node;
			}
		}

		return null;
	}

	@Override
	protected TCDefinition findDefinition(File file, int zline, int zcol)
	{
		TCPlugin plugin = getPlugin("TC");
		TCClassList tcClassList = plugin.getTC();
		
		if (tcClassList != null && !tcClassList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			return finder.findDefinition(tcClassList, file, zline + 1, zcol + 1);		// Convert from zero-relative
		}
		else
		{
			return null;
		}
	}

	@Override
	public RPCMessageList findDefinition(RPCRequest request, File file, int zline, int zcol)
	{
		TCDefinition def = findDefinition(file, zline, zcol);
		
		if (def == null)
		{
			return new RPCMessageList(request, null);
		}
		else
		{
			URI defuri = def.location.file.toURI();
			
			return new RPCMessageList(request,
				System.getProperty("lsp.lsp4e") != null ?
					new JSONArray(
						new JSONObject(
							"targetUri", defuri.toString(),
							"targetRange", Utils.lexLocationToRange(def.location),
							"targetSelectionRange", Utils.lexLocationToPoint(def.location)))
					:
					new JSONObject(
						"uri", defuri.toString(),
						"range", Utils.lexLocationToRange(def.location)));
		}
	}

	@Override
	protected TCDefinitionList lookupDefinition(String startsWith)
	{
		TCPlugin plugin = getPlugin("TC");
		TCClassList tcClassList = plugin.getTC();
		TCDefinitionList results = new TCDefinitionList();
		
		for (TCClassDefinition cdef: tcClassList)
		{
			if (cdef.name.getName().startsWith(startsWith))
			{
				results.add(cdef);	// Add classes as well
			}
			
			for (TCDefinition def: cdef.definitions)
			{
				if (def.name != null && def.name.getName().startsWith(startsWith))
				{
					results.add(def);
				}
			}
		}
		
		return results;
	}

	@Override
	protected FilenameFilter getFilenameFilter()
	{
		return Dialect.VDM_PP.getFilter();
	}
	
	@Override
	protected String[] getFilenameFilters()
	{
		return new String[] { "**/*.vpp", "**/*.vdmpp" }; 
	}

	@Override
	public ClassInterpreter getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				TCPlugin plugin = getPlugin("TC");
				TCClassList tcClassList = plugin.getTC();
				INPlugin plugin2 = getPlugin("IN");
				INClassList inClassList = plugin2.getIN();
				interpreter = new ClassInterpreter(inClassList, tcClassList);
			}
			catch (Exception e)
			{
				Log.error(e);
				interpreter = null;
			}
		}
		
		return (ClassInterpreter) interpreter;
	}

	@Override
	protected boolean canExecute()
	{
		return getInterpreter() != null;	// inClassList != null;
	}
	
	@Override
	protected boolean hasChanged()
	{
		INPlugin plugin = getPlugin("IN");
		INClassList inClassList = plugin.getIN();
		return getInterpreter() != null && getInterpreter().getIN() != inClassList;		// TOTO hasn't changed??
	}

	@Override
	public DAPMessageList threads(DAPRequest request)
	{
		return new DAPMessageList(request, new JSONObject("threads", new JSONArray()));	// empty?
	}

	@Override
	public RPCMessageList pogGenerate(RPCRequest request, File file, JSONObject range)
	{
		TCPlugin plugin = getPlugin("TC");
		TCClassList tcClassList = plugin.getTC();
		
		if (!plugin.getErrs().isEmpty())	// No type clean tree
		{
			return new RPCMessageList(request, RPCErrors.InternalError, "Type checking errors found");
		}
		
		try
		{
			POPlugin plugin2 = getPlugin("PO");
			POClassList poClassList = plugin2.getPO();

			if (poClassList == null)
			{
				plugin2.checkLoadedFiles(tcClassList);
				poClassList = plugin2.getPO();
			}
			
			ProofObligationList poGeneratedList = poClassList.getProofObligations();
			poGeneratedList.renumber();
			JSONArray results = new JSONArray();
			
			for (ProofObligation po: poGeneratedList)
			{
				if (file != null &&
					!po.location.file.equals(file) &&
					!po.location.file.getParentFile().equals(file))		// folder
				{
					continue;
				}
				
				JSONArray name = new JSONArray(po.location.module);
				
				for (String part: po.name.split(";\\s+"))
				{
					name.add(part);
				}

				results.add(
					new JSONObject(
						"id",		new Long(po.number),
						"kind", 	po.kind.toString(),
						"name",		name,
						"location",	Utils.lexLocationToLocation(po.location),
						"source",	po.value,
						"proved",	po.status != POStatus.UNPROVED));
			}
			
			return new RPCMessageList(request, results);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
