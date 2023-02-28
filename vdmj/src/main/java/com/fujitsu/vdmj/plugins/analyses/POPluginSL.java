/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.plugins.analyses;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.modules.POModuleList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.util.Utils;

/**
 * VDM-SL PO plugin
 */
public class POPluginSL extends POPlugin
{
	protected POModuleList poModuleList = null;
	
	@Override
	protected List<VDMMessage> pogPrepare()
	{
		poModuleList = null;
		return null;
	}

	@Override
	protected List<VDMMessage> pogGenerate()
	{
		TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
		TCModuleList checkedModules = tc.getTC();
		List<VDMMessage> messages = new Vector<VDMMessage>();

		try
   		{
			long before = System.currentTimeMillis();
   			poModuleList = new POModuleList(checkedModules);
   			Utils.mapperStats(before, PONode.MAPPINGS);
   		}
		catch (InternalException e)
		{
			messages.addAll(errsOf(e));
		}
		catch (Throwable e)
		{
			messages.addAll(errsOf(e));
		}

		return messages;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Collection<?>> T getPO()
	{
		return (T)poModuleList;
	}

	@Override
	public ProofObligationList getProofObligations()
	{
		POAnnotation.init();
		ProofObligationList list = poModuleList.getProofObligations();
		POAnnotation.close();
		return list;
	}
}
