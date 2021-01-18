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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import workspace.Log;

abstract public class JSONServer
{
	private final String prefix;
	private final InputStream inStream;
	private final OutputStream outStream;

	private final String CONTENT_LENGTH = "Content-Length:";
	private final String CONTENT_TYPE = "Content-Type:";
	private final char EOF = (char) -1;
	
	public JSONServer(String prefix, InputStream inStream, OutputStream outStream) throws IOException
	{
		this.prefix = prefix;
		this.inStream = inStream;
		this.outStream = outStream;
	}
	
	private String readLine() throws IOException
	{
		StringBuilder sb = new StringBuilder();
		char c = (char) inStream.read();
		
		while (c != '\n' && c != EOF)
		{
			sb.append(c);
			c = (char) inStream.read();
		}
		
		if (c == EOF) return null;
		
		return sb.toString().trim();
	}
	
	public JSONObject readMessage() throws IOException
	{
		String contentLength = readLine();
		String separator = readLine();
		
		if (contentLength == null || separator == null)	// EOF
		{
			return null;
		}
		
		String encoding = "UTF-8";
		
		if (separator.startsWith(CONTENT_TYPE))
		{
			int charset = separator.indexOf("charset=");
			
			if (charset > 0)
			{
				encoding = separator.substring(charset + "charset=".length());
			}
			else
			{
				Log.error("Malformed header: %s", separator);
			}
			
			separator = readLine();
		}
		else if (!separator.isEmpty())
		{
			Log.error("Input stream out of sync. Expected \\r\\n got [%s]", separator);
		}
		
		if (!contentLength.startsWith(CONTENT_LENGTH))
		{
			Log.error("Input stream out of sync. Expected Content-Length: got [%s]", contentLength);
			throw new IOException("Input stream out of sync");
		}
		else
		{
			int length = Integer.parseInt(contentLength.substring(CONTENT_LENGTH.length()).trim());
			byte[] bytes = new byte[length];
			int size = 0;
			
			while (size < length)
			{
				size += inStream.read(bytes, size, length-size);
			}
			
			String message = new String(bytes, encoding);
			Log.printf(">>> %s %s", prefix, message);
			JSONReader jreader = new JSONReader(new StringReader(message));
			return jreader.readObject();
		}
	}
	
	public synchronized void writeMessage(JSONObject response) throws IOException
	{
		StringWriter swout = new StringWriter();
		JSONWriter jwriter = new JSONWriter(new PrintWriter(swout));
		jwriter.writeObject(response);
		byte[] jout = swout.toString().getBytes("UTF-8");
		
		ByteArrayOutputStream message = new ByteArrayOutputStream(); 
		message.write(CONTENT_LENGTH.getBytes("UTF-8"));
		message.write(' ');
		message.write(Integer.toString(jout.length).getBytes("UTF-8"));
		message.write('\r');
		message.write('\n');
		message.write('\r');
		message.write('\n');
		message.write(jout);
		
		outStream.write(message.toByteArray());		// Avoid multiple packets
		outStream.flush();

		Log.printf("<<< %s %s", prefix, swout);
	}
}
