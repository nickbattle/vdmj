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

package json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import workspace.Log;

abstract public class JSONServer
{
	private final String prefix;
	private final InputStream inStream;
	private final OutputStream outStream;
	
	public JSONServer(String prefix, InputStream inStream, OutputStream outStream) throws IOException
	{
		this.prefix = prefix;
		this.inStream = inStream;
		this.outStream = outStream;
	}
	
	public JSONObject readMessage() throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
		String contentLength = br.readLine();
		String separator = br.readLine();	// blank separator
		
		if (contentLength == null || separator == null)	// EOF
		{
			return null;
		}
		
		String encoding = "cp1252";		// For VSCode?
		
		if (separator.startsWith("Content-Type:"))
		{
			int charset = separator.indexOf("charset=");
			
			if (charset > 0)
			{
				encoding = separator.substring(charset + "charset=".length());
				Log.printf("Encoding = %s", encoding);
			}
			
			separator = br.readLine();
		}
		else if (!separator.isEmpty())
		{
			Log.error("Input stream out of sync. Expected \\r\\n got [%s]", separator);
		}
		
		if (!contentLength.startsWith("Content-Length:"))
		{
			Log.error("Input stream out of sync. Expected Content-Length: got [%s]", contentLength);
		}
		
		int length = Integer.parseInt(contentLength.substring(16));	// Content-Length: NNNN
		byte[] bytes = new byte[length];
		
		for (int i=0; i<length; i++)
		{
			bytes[i] = (byte) br.read();
		}

		String message = new String(bytes, encoding);
		Log.printf(">>> %s %s", prefix, message);
		JSONReader jreader = new JSONReader(new StringReader(message));
		return jreader.readObject();
	}
	
	public void writeMessage(JSONObject response) throws IOException
	{
		StringWriter swout = new StringWriter();
		JSONWriter jwriter = new JSONWriter(new PrintWriter(swout));
		jwriter.writeObject(response);

		String jout = swout.toString();
		Log.printf("<<< %s %s", prefix, jout);
		PrintWriter pwout = new PrintWriter(new OutputStreamWriter(outStream, "UTF-8"));
		pwout.printf("Content-Length: %d\r\n\r\n%s", jout.getBytes("UTF-8").length, jout);
		pwout.flush();
	}
}
