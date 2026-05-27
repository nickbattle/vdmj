/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

 package workspace.inlays;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.LexLocation;

import json.JSONObject;

public class POMissingPOInlayHint extends POInlayHint
{
	private final LexLocation location;
	private final String label;
	private final String markup;
	
	public POMissingPOInlayHint(LexLocation location, String label, long paths, long missing)
	{
		this.location = location;
		this.label = label;
		this.markup = getMissingPOMarkup(paths, missing);
	}

	private String getMissingPOMarkup(long paths, long missing)
	{
		return
			"### This definition is too complex for POG\n" +
			"There are " +
			paths +
			" possible execution paths through this definition, " +
			"which exceeds the configured limit in the Java property vdmj.pog.max_alt_paths (" +
			Properties.pog_max_alt_paths +
			")\n\n" +
			"The proof obligation generation (POG) has therefore omitted " + missing +
			" POs. Try to simplify the definition, or increase the property value.";
	}

	@Override
	public JSONObject getInlayHint()
	{
		return makeInlay(location, label, markup);
	}
}
