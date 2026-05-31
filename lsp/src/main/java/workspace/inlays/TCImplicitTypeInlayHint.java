package workspace.inlays;

import com.fujitsu.vdmj.lex.LexLocation;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;

public class TCImplicitTypeInlayHint extends TCInlayHint
{
	private final LexLocation location;
	private final String label;
	private final String markup;
	
	public TCImplicitTypeInlayHint(LexLocation location, String type)
	{
		this.location = location;
		this.label = ":" + type;
		this.markup = "Implicit type added";
	}

	@Override
	public JSONObject getInlayHint()
	{
		JSONObject inlay = makeInlay(location, label, markup, false, false);

		JSONArray edits = new JSONArray(
			new JSONObject(
				"range", Utils.lexLocationToZeroRange(location),
				"newText", label));

		inlay.put("textEdits", edits);
		return inlay;
	}

	@Override
	public boolean whenDirty()
	{
		return false;	// Too confusing otherwise
	}
}
