package workspace.inlays;

import com.fujitsu.vdmj.lex.LexLocation;

import json.JSONObject;

public class TCImplicitTypeInlayHint extends TCInlayHint
{
	private final LexLocation location;
	private final String label;
	private final String markup;
	
	public TCImplicitTypeInlayHint(LexLocation location, String type)
	{
		this.location = location;
		this.label = ":" + type;
		this.markup = "Implicit type created";
	}

	@Override
	public JSONObject getInlayHint()
	{
		return makeInlay(location, label, markup);
	}
}
