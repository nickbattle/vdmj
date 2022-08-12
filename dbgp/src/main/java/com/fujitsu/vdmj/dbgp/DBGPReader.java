/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.dbgp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteInterpreter;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.VDMJ;
import com.fujitsu.vdmj.VDMPP;
import com.fujitsu.vdmj.VDMRT;
import com.fujitsu.vdmj.VDMSL;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.debug.DebugExecutor;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.debug.DebugReason;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INHistoryExpression;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsolePrintWriter;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.ClassContext;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.SourceFile;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.runtime.Tracepoint;
import com.fujitsu.vdmj.scheduler.MainThread;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Base64;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TransactionValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;

public class DBGPReader extends DebugLink
{
	private static Map<String, DBGPReader> threadInstances = new HashMap<String, DBGPReader>();
	private static DBGPReader mainInstance;
	
	private static String host;
	private static int port;
	private static String ideKey;
	private static Interpreter interpreter;

	private final String expression;
	
	private boolean connected = false;
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	private CPUValue cpu;

	private int sessionId = 0;
	private DBGPStatus status = null;
	private DebugReason statusReason = null;
	private DBGPCommandType command = null;
	private String transaction = "";
	private DBGPFeatures features;
	private byte separator = '\0';

	private Context breakContext = null;
	private Breakpoint breakpoint = null;
	private Value theAnswer = null;
	private static boolean breaksSuspended = false;
	private RemoteControl remoteControl = null;
	private boolean stopped = false;

	private static final int SOURCE_LINES = 5;

	@SuppressWarnings("unchecked")
	public static void main(String[] args)
	{
		// Identify this class as the debug link - See DebugLink
		System.setProperty("vdmj.debug.link_class", DBGPReader.class.getName());
		
		String host = null;
		int port = -1;
		String ideKey = null;
		Settings.dialect = null;
		String expression = null;
		List<File> files = new Vector<File>();
		List<String> largs = Arrays.asList(args);
		VDMJ controller = null;
		boolean warnings = true;
		boolean quiet = false;
		String logfile = null;
		boolean expBase64 = false;
		File coverage = null;
		String defaultName = null;
		String remoteName = null;
		Class<RemoteControl> remoteClass = null;

		Properties.init();				// Read properties file, if any
		Redirector.initRedirectors();	// Allow stdio redirection to client

		for (Iterator<String> i = largs.iterator(); i.hasNext();)
		{
			String arg = i.next();

    		if (arg.equals("-vdmsl"))
    		{
    			controller = new VDMSL();
    		}
    		else if (arg.equals("-vdmpp"))
    		{
    			controller = new VDMPP();
    		}
    		else if (arg.equals("-vdmrt"))
    		{
    			controller = new VDMRT();
    		}
    		else if (arg.equals("-h"))
    		{
    			if (i.hasNext())
    			{
    				host = i.next();
    			}
    			else
    			{
    				usage("-h option requires a hostname");
    			}
    		}
    		else if (arg.equals("-p"))
    		{
    			try
    			{
    				port = Integer.parseInt(i.next());
    			}
    			catch (Exception e)
    			{
    				usage("-p option requires a port");
    			}
    		}
    		else if (arg.equals("-k"))
    		{
    			if (i.hasNext())
    			{
    				ideKey = i.next();
    			}
    			else
    			{
    				usage("-k option requires a key");
    			}
    		}
    		else if (arg.equals("-e"))
    		{
    			if (i.hasNext())
    			{
    				expression = i.next();
    			}
    			else
    			{
    				usage("-e option requires an expression");
    			}
    		}
    		else if (arg.equals("-e64"))
    		{
    			if (i.hasNext())
    			{
    				expression = i.next();
    				expBase64 = true;
    			}
    			else
    			{
    				usage("-e64 option requires an expression");
    			}
    		}
    		else if (arg.equals("-c"))
    		{
    			if (i.hasNext())
    			{
    				if (controller == null)
    				{
    					usage("-c must come after <-vdmpp|-vdmsl|-vdmrt>");
    				}

    				controller.setCharset(validateCharset(i.next()));
    			}
    			else
    			{
    				usage("-c option requires a charset name");
    			}
    		}
    		else if (arg.equals("-r"))
    		{
    			if (i.hasNext())
    			{
    				Settings.release = Release.lookup(i.next());

    				if (Settings.release == null)
    				{
    					usage("-r option must be " + Release.list());
    				}
    			}
    			else
    			{
    				usage("-r option requires a VDM release");
    			}
    		}
			else if (arg.equals("-pre"))
			{
				Settings.prechecks = false;
			}
			else if (arg.equals("-post"))
			{
				Settings.postchecks = false;
			}
			else if (arg.equals("-inv"))
			{
				Settings.invchecks = false;
			}
			else if (arg.equals("-dtc"))
			{
				// NB. Turn off both when no DTC
				Settings.invchecks = false;
				Settings.dynamictypechecks = false;
			}
			else if (arg.equals("-measures"))
			{
				Settings.measureChecks = false;
			}
    		else if (arg.equals("-log"))
    		{
    			if (i.hasNext())
    			{
        			try
        			{
        				logfile = new URI(i.next()).getPath();
        			}
        			catch (URISyntaxException e)
        			{
        				usage(e.getMessage() + ": " + arg);
        			}
        			catch (IllegalArgumentException e)
        			{
        				usage(e.getMessage() + ": " + arg);
        			}
    			}
    			else
    			{
    				usage("-log option requires a filename");
    			}
    		}
    		else if (arg.equals("-w"))
    		{
    			warnings = false;
    		}
    		else if (arg.equals("-q"))
    		{
    			quiet = true;
    		}
    		else if (arg.equals("-coverage"))
    		{
    			if (i.hasNext())
    			{
        			try
        			{
        				coverage = new File(new URI(i.next()));

        				if (!coverage.isDirectory())
        				{
        					usage("Coverage location is not a directory");
        				}
        			}
        			catch (URISyntaxException e)
        			{
        				usage(e.getMessage() + ": " + arg);
        			}
        			catch (IllegalArgumentException e)
        			{
        				usage(e.getMessage() + ": " + arg);
        			}
    			}
    			else
    			{
    				usage("-coverage option requires a directory name");
    			}
    		}
    		else if (arg.equals("-default64"))
    		{
    			if (i.hasNext())
    			{
       				defaultName = i.next();
    			}
    			else
    			{
    				usage("-default64 option requires a name");
    			}
    		}
    		else if (arg.equals("-remote"))
    		{
    			if (i.hasNext())
    			{
       				remoteName = i.next();
    			}
    			else
    			{
    				usage("-remote option requires a Java classname");
    			}
    		}
    		else if (arg.equals("-consoleName"))	// Overture compatibility
    		{
    			if (i.hasNext())
    			{
       				i.next();	// Ignored
    			}
    			else
    			{
    				usage("-consoleName option requires a name");
    			}
    		}
    		else if (arg.startsWith("-baseDir"))	// Overture compatibility
    		{
    			if (i.hasNext())
    			{
       				i.next();	// Ignored
    			}
    			else
    			{
    				usage("-baseDir option requires a directory name");
    			}
    		}
    		else if (arg.equals("-timeinv"))		// Overture compatibility
			{
				if (i.hasNext())
				{
       				i.next();	// Ignored
				}
				else
				{
					usage("-timeinv option requires a filename");
				}
			}
    		else if (arg.equals("-strict"))
    		{
    			Settings.strict = true;
    		}
    		else if (arg.startsWith("-"))
    		{
    			usage("Unknown option " + arg);
    		}
    		else
    		{
    			try
    			{
    				File dir = new File(new URI(arg));

    				if (dir.isDirectory())
    				{
     					for (File file: dir.listFiles(Settings.dialect.getFilter()))
    					{
    						if (file.isFile())
    						{
    							files.add(file);
    						}
    					}
    				}
        			else
        			{
        				files.add(dir);
        			}
    			}
    			catch (URISyntaxException e)
    			{
    				usage(e.getMessage() + ": " + arg);
    			}
    			catch (IllegalArgumentException e)
    			{
    				usage(e.getMessage() + ": " + arg);
    			}
    		}
		}

		if (host == null || port == -1 || controller == null ||
			ideKey == null || (expression == null && remoteName == null) || Settings.dialect == null ||
			files.isEmpty())
		{
			usage("Missing mandatory arguments");
		}
		else
		{
			System.setProperty(Settings.dialect.name(), "1");
		}

		if (Settings.dialect != Dialect.VDM_RT && logfile != null)
		{
			usage("-log can only be used with -vdmrt");
		}

		if (expBase64)
		{
			try
			{
				byte[] bytes = Base64.decode(expression);
				expression = new String(bytes, VDMJ.filecharset);
			}
			catch (Exception e)
			{
				usage("Malformed -e64 base64 expression");
			}
		}

		if (defaultName != null)
		{
			try
			{
				byte[] bytes = Base64.decode(defaultName);
				defaultName = new String(bytes, VDMJ.filecharset);
			}
			catch (Exception e)
			{
				usage("Malformed -default64 base64 name");
			}
		}

		if (remoteName != null)
		{
			try
			{
				Class<?> cls = ClassLoader.getSystemClassLoader().loadClass(remoteName);
				remoteClass = (Class<RemoteControl>)cls;
			}
			catch (ClassNotFoundException e)
			{
				usage("Cannot locate " + remoteName + " on the CLASSPATH");
			}
		}

		controller.setWarnings(warnings);
		controller.setQuiet(quiet);

		if (controller.parse(files) == ExitStatus.EXIT_OK)
		{
    		if (controller.typeCheck() == ExitStatus.EXIT_OK)
    		{
				try
				{
					if (logfile != null)
					{
		    			RTLogger.setLogfileName(new File(logfile));
					}

					Interpreter i = controller.getInterpreter();

					if (defaultName != null)
					{
						i.setDefaultName(defaultName);
					}

					RemoteControl remote =
						(remoteClass == null) ? null : remoteClass.getDeclaredConstructor().newInstance();

					mainInstance = new DBGPReader(host, port, ideKey, i, expression, null);
					mainInstance.startup(remote);

					if (coverage != null)
					{
						writeCoverage(i, coverage);
					}

					RTLogger.dump(true);
	    			System.exit(0);
				}
				catch (ContextException e)
				{
					System.out.println("Initialization: " + e);
					
					if (e.isStackOverflow())
					{
						e.ctxt.printStackFrames(Console.out);
					}
					else
					{
						e.ctxt.printStackTrace(Console.out, true);
					}

					RTLogger.dump(true);
					System.exit(3);
				}
				catch (Exception e)
				{
					System.out.println("Initialization: " + e);
					RTLogger.dump(true);
					System.exit(3);
				}
    		}
    		else
    		{
    			System.exit(2);
    		}
		}
		else
		{
			System.exit(1);
		}
	}
	
	public static synchronized DBGPReader getInstance()
	{
		String name = Thread.currentThread().getName();
		DBGPReader reader = threadInstances.get(name);
		
		if (name.equals("MainThread") || name.equals("CTMainThread"))
		{
			reader = mainInstance;
		}
		else if (reader == null)
		{
			reader = newDBGPReader();
			threadInstances.put(name, reader);
		}
		
		return reader;
	}

	private static void usage(String string)
	{
		System.err.println(string);
		System.err.println(
			"Usage: -h <host> -p <port> -k <ide key> <-vdmpp|-vdmsl|-vdmrt>" +
			" -e <expression> | -e64 <base64 expression>" +
			" [-w] [-q] [-log <logfile URL>] [-c <charset>] [-r <release>]" +
			" [-pre] [-post] [-inv] [-dtc] [-measures]" +
			" [-coverage <dir URL>] [-default64 <base64 name>]" +
			" [-remote <class>] [-strict] {<filename URLs>}");

		System.exit(1);
	}

	private static String validateCharset(String cs)
	{
		if (!Charset.isSupported(cs))
		{
			System.err.println("Charset " + cs + " is not supported\n");
			System.err.println("Available charsets:");
			System.err.println("Default = " + Charset.defaultCharset());
			Map<String,Charset> available = Charset.availableCharsets();

			for (Entry<String, Charset> name: available.entrySet())
			{
				System.err.println(name.getKey() + " " + name.getValue().aliases());
			}

			System.err.println("");
			usage("Charset " + cs + " is not supported");
		}

		return cs;
	}

	public DBGPReader(
		String host, int port, String ideKey,
		Interpreter interpreter, String expression, CPUValue cpu)
	{
		DBGPReader.host = host;
		DBGPReader.port = port;
		DBGPReader.ideKey = ideKey;
		DBGPReader.interpreter = interpreter;
		
		this.expression = expression;
		this.cpu = cpu;
	}

	private static DBGPReader newDBGPReader()
	{
		DBGPReader r = new DBGPReader(host, port, ideKey, interpreter, null, null);
		r.command = DBGPCommandType.UNKNOWN;
		r.transaction = "?";
		return r;
	}

	@Override
	public void newThread(CPUValue _cpu)
	{
		this.cpu = _cpu;
	}

	private void connect() throws IOException
	{
		if (!connected)
		{
			if (port > 0)
			{
				InetAddress server = InetAddress.getByName(host);
				socket = new Socket(server, port);
				input = socket.getInputStream();
				output = socket.getOutputStream();
			}
			else
			{
				socket = null;
				input = System.in;
				output = System.out;
				separator = ' ';
			}

			connected = true;
			init();
			run();			// New threads wait for a "run -i"
		}
	}

	private void startup(RemoteControl remote) throws IOException
	{
		remoteControl = remote;
		interpreter.init();
		connect();
	}

	private void init() throws IOException
	{
		sessionId = Math.abs(new Random().nextInt(1000000));
		status = DBGPStatus.STARTING;
		statusReason = DebugReason.OK;
		features = new DBGPFeatures();

		StringBuilder sb = new StringBuilder();

		sb.append("<init ");
		sb.append("appid=\"");
		sb.append(features.getProperty("language_name"));
		sb.append("\" ");
		sb.append("idekey=\"" + ideKey + "\" ");
		sb.append("session=\"" + sessionId + "\" ");
		sb.append("thread=\"");
		sb.append(Thread.currentThread().getId());

		if (cpu != null)
		{
			sb.append(" on ");
			sb.append(cpu.getName());
		}

		sb.append("\" ");
		sb.append("parent=\"");
		sb.append(features.getProperty("language_name"));
		sb.append("\" ");
		sb.append("language=\"");
		sb.append(features.getProperty("language_name"));
		sb.append("\" ");
		sb.append("protocol_version=\"");
		sb.append(features.getProperty("protocol_version"));
		sb.append("\"");

		Set<File> files = interpreter.getSourceFiles();
		sb.append(" fileuri=\"");
		sb.append(files.iterator().next().toURI());		// Any old one...
		sb.append("\"");

		sb.append("/>\n");

		write(sb);
	}

	private String readLine() throws IOException
	{
		try
		{
    		StringBuilder line = new StringBuilder();
    		int c = input.read();
    
    		while (c != '\n' && c > 0)
    		{
    			if (c != '\r') line.append((char)c);		// Ignore CRs
    			c = input.read();
    		}
    
    		return (line.length() == 0 && c == -1) ? null : line.toString();
    	}
		catch (SocketException e)
    	{
    		if (stopped)
    		{
    			return null;
    		}
    		else
    		{
    			throw e;
    		}
    	}
	}

	private void write(StringBuilder data) throws IOException
	{
		byte[] header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes("UTF-8");
		byte[] body = data.toString().getBytes("UTF-8");
		byte[] size = Integer.toString(header.length + body.length).getBytes("UTF-8");

		output.write(size);
		output.write(separator);
		output.write(header);
		output.write(body);
		output.write(separator);

		output.flush();
	}

	private void response(StringBuilder hdr, StringBuilder body) throws IOException
	{
		StringBuilder sb = new StringBuilder();

		sb.append("<response command=\"");
		sb.append(command);
		sb.append("\"");

		if (hdr != null)
		{
			sb.append(" ");
			sb.append(hdr);
		}

		sb.append(" transaction_id=\"");
		sb.append(transaction);
		sb.append("\"");

		if (body != null)
		{
			sb.append(">");
			sb.append(body);
			sb.append("</response>\n");
		}
		else
		{
			sb.append("/>\n");
		}

		write(sb);
	}

	private void errorResponse(DBGPErrorCode errorCode, String reason)
	{
		try
		{
			StringBuilder sb = new StringBuilder();

			sb.append("<error code=\"");
			sb.append(errorCode.value);
			sb.append("\" apperr=\"\"><message>");
			sb.append(quote(reason));
			sb.append("</message></error>");

			response(null, sb);
		}
		catch (SocketException e)
		{
			// Do not report these since the socket connection is down.
		}
		catch (IOException e)
		{
			throw new InternalException(29, "DBGP: " + reason);
		}
	}

	private void statusResponse() throws IOException
	{
		statusResponse(status, statusReason);
	}

	private void statusResponse(DBGPStatus s, DebugReason reason)
		throws IOException
	{
		StringBuilder sb = new StringBuilder();

		if (s == DBGPStatus.STOPPED)
		{
			stopped = true;
		}

		status = s;
		statusReason = reason;

		sb.append("status=\"");
		sb.append(status);
		sb.append("\"");
		sb.append(" reason=\"");
		sb.append(statusReason);
		sb.append("\"");

		StringBuilder body = new StringBuilder();
		body.append("<internal ");

		if (Thread.currentThread() instanceof SchedulableThread)
		{
			SchedulableThread th = (SchedulableThread)Thread.currentThread();

			body.append("threadId=\"");
			body.append(th.getId());
			body.append("\" ");

			body.append("threadName=\"");
			body.append(th.getName());
			body.append("\" ");

			body.append("threadState=\"");
			body.append(th.getRunState().toString());
			body.append("\" ");

		}
		else	// The init thread?
		{
			body.append("threadId=\"" + Thread.currentThread().getId() + "\" ");
			body.append("threadName=\"" + Thread.currentThread().getName() +"\" ");
			body.append("threadState=\"RUNNING\" ");
		}

		body.append("/>");

		response(sb, body);
	}

	private StringBuilder breakpointResponse(Breakpoint bp)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("<breakpoint id=\"" + bp.number + "\"");
		sb.append(" type=\"line\"");
		sb.append(" state=\"enabled\"");
		sb.append(" filename=\"" + bp.location.file.toURI() + "\"");
		sb.append(" lineno=\"" + bp.location.startLine + "\"");
		sb.append(">");

		if (bp.trace != null)
		{
			sb.append("<expression>" + quote(bp.trace) + "</expression>");
		}

		sb.append("</breakpoint>");

		return sb;
	}

	private StringBuilder stackResponse(LexLocation location, int level)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("<stack level=\"" + level + "\"");
		sb.append(" type=\"file\"");
		sb.append(" filename=\"" + location.file.toURI() + "\"");
		sb.append(" lineno=\"" + location.startLine + "\"");
		sb.append(" cmdbegin=\"" + location.startLine + ":" + location.startPos + "\"");
		sb.append("/>");

		return sb;
	}

	private void overtureResponse(String overtureCmd, StringBuilder hdr, StringBuilder body) throws IOException
    {
    	StringBuilder sb = new StringBuilder();
    
    	sb.append("<xcmd_overture_response command=\"");
    	sb.append(command);
    	sb.append("\"");
    
    	sb.append(" overtureCmd=\"");
    	sb.append(overtureCmd);
    	sb.append("\"");
    
    	if (hdr != null)
    	{
    		sb.append(" ");
    		sb.append(hdr);
    	}
    
    	sb.append(" transaction_id=\"");
    	sb.append(transaction);
    	sb.append("\"");
    
    	if (body != null)
    	{
    		sb.append(">");
    		sb.append(body);
    		sb.append("</xcmd_overture_response>\n");
    	}
    	else
    	{
    		sb.append("/>\n");
    	}
    
    	write(sb);
    }

	private StringBuilder propertyResponse(NameValuePairMap vars)
		throws UnsupportedEncodingException
	{
		StringBuilder sb = new StringBuilder();

		for (Entry<TCNameToken, Value> e: vars.entrySet())
		{
			sb.append(propertyResponse(e.getKey(), e.getValue()));
		}

		return sb;
	}

	private StringBuilder propertyResponse(TCNameToken name, Value value)
		throws UnsupportedEncodingException
	{
		return propertyResponse(
			name.getName(), name.toString(),
			name.getModule(), value);
	}

	private StringBuilder propertyResponse(
		String name, String fullname, String clazz, Value value)
		throws UnsupportedEncodingException
    {
    	StringBuilder sb = new StringBuilder();
    	
    	int children = childCount(value);

    	sb.append("<property");
    	sb.append(" name=\"" + quote(name) + "\"");
    	sb.append(" fullname=\"" + quote(fullname) + "\"");
    	sb.append(" type=\"" + value.kind() + "\"");
    	sb.append(" classname=\"" + clazz + "\"");
    	sb.append(" constant=\"0\"");
    	sb.append(" children=\"" + children + "\"");
    	sb.append(" size=\"" + (children > 0 ? "0" : value.toString().length()) + "\"");
    	sb.append(" encoding=\"base64\">");
    	
    	if (children == 0)
    	{
        	sb.append("<![CDATA[");
        	sb.append(Base64.encode(value.toString().getBytes("UTF-8")));
        	sb.append("]]>");
    	}
    	else
    	{
    		int index = 0;
    		
    		for (Value child: getChildren(value))
    		{
    			sb.append(propertyResponse(name + "[" + index++ + "]", "", clazz, child));
    		}
    	}
    	
    	sb.append("</property>");

    	return sb;
    }

	private ValueList getChildren(Value value)
	{
		ValueList results = new ValueList();
		value = value.deref();
		
		if (value instanceof SetValue)
		{
			results.addAll(((SetValue)value).values);
		}
		else if (value instanceof SeqValue)
		{
			SeqValue seq = (SeqValue)value;
			boolean string = !seq.values.isEmpty();		// So it's [] and not ""
			
			for (Value e: seq.values)
			{
				if (!(e instanceof CharacterValue))
				{
					string = false;
					break;
				}
			}
			
			if (string)
			{
				results.add(new SeqValue(value.toString()));
			}
			else
			{
				results.addAll(seq.values);
			}
		}
		else if (value instanceof MapValue)
		{
			ValueMap map = ((MapValue)value).values;
			
			for (Value dom: map.keySet())
			{
				Value rng = map.get(dom);
				results.add(new SeqValue("{" + dom + " |-> " + rng + "}"));
			}
		}
		else
		{
			results.add(value);
		}
		
		return results;
	}

	private int childCount(Value value)
	{
		value = value.deref();
		
		if (value instanceof SetValue)
		{
			return ((SetValue)value).values.size();
		}
		else if (value instanceof SeqValue)
		{
			SeqValue seq = (SeqValue)value;
			boolean string = !seq.values.isEmpty();		// So it's [] and not ""
			
			for (Value e: seq.values)
			{
				if (!(e instanceof CharacterValue))
				{
					string = false;
					break;
				}
			}
			
			if (string)
			{
				return 0;
			}
			else
			{
				return ((SeqValue)value).values.size();
			}
		}
		else if (value instanceof MapValue)
		{
			return ((MapValue)value).values.size();
		}
		else
		{
			return 0;
		}
	}

	private void cdataResponse(String msg) throws IOException
	{
		// Send back a CDATA response with a plain message
		response(null, new StringBuilder("<![CDATA[" + quote(msg) + "]]>"));
	}

	private static String quote(String in)
	{
		return in
    		.replace("&", "&amp;")
    		.replace("<", "&lt;")
    		.replace(">", "&gt;")
    		.replace("\"", "&quot;");
	}

	private void run() throws IOException
	{
		String line = null;

		do
		{
			line = readLine();
		}
		while (line != null && process(line));
	}

	@Override
	public void stopped(Context ctxt, LexLocation location, Exception ex)
	{
		if (location != null && ctxt != null)	// ie. thread has started
		{
			breakpoint(ctxt, new Breakpoint(location));
		}	
	}

	@Override
	public void breakpoint(Context ctxt, Breakpoint bp)
	{
		if (breaksSuspended)
		{
			return;		// We're inside an eval command or runtrace, so don't stop
		}

		try
		{
			connect();

			breakContext = ctxt;
			breakpoint = bp;
			statusResponse(DBGPStatus.BREAK, DebugReason.OK);

			run();

			breakContext = null;
			breakpoint = null;
		}
		catch (Exception e)
		{
			errorResponse(DBGPErrorCode.INTERNAL_ERROR, e.getMessage());
		}
	}

	@Override
	public void tracepoint(Context ctxt, Tracepoint tp)
	{
    	try
    	{
    		String display = null;
    		
    		if (tp.condition == null)
    		{
    			display = "Reached trace point [" + tp.number + "]";
    		}
    		else
    		{
    			String result = null;
    			
    			try
    			{
    				result = tp.condition.eval(ctxt).toString();
    			}
    			catch (Exception e)
    			{
    				result = e.getMessage();
    			}
    			
    			display = tp.trace + " = " + result + " at trace point [" + tp.number + "]";
    		}

    		connect();
    		cdataResponse(display);
    	}
    	catch (Exception e)
    	{
    		errorResponse(DBGPErrorCode.INTERNAL_ERROR, e.getMessage());
    	}
	}

	@Override
	public void complete(DebugReason reason, ContextException exception)
	{
		try
		{
			if (reason == DebugReason.OK && !connected)
			{
				// We never connected and there's no problem so just complete...
			}
			else
			{
				connect();

				if (reason == DebugReason.EXCEPTION && exception != null)
    			{
    				dyingThread(exception);
    			}
    			else
    			{
    				statusResponse(DBGPStatus.STOPPED, reason);
    			}
			}
		}
		catch (IOException e)
		{
			try
			{
				errorResponse(DBGPErrorCode.INTERNAL_ERROR, e.getMessage());
			}
			catch (Throwable th)
			{
				// Probably a shutdown race...
			}
		}
		finally
		{
			threadInstances.remove(Thread.currentThread().getName());
			
			if (!(Thread.currentThread() instanceof MainThread))	// Don't close main link to client
			{
				try
				{
					if (socket != null)
					{
						socket.close();
					}
				}
				catch (IOException e)
				{
					// ?
				}
			}
		}
	}

	private boolean process(String line)
	{
		boolean carryOn = true;

		try
		{
			command = DBGPCommandType.UNKNOWN;
			transaction = "?";

    		String[] parts = line.split("\\s+");
    		DBGPCommand c = parse(parts);

    		switch (c.type)
    		{
    			case STATUS:
    				processStatus(c);
    				break;

    			case FEATURE_GET:
    				processFeatureGet(c);
    				break;

    			case FEATURE_SET:
    				processFeatureSet(c);
    				break;

    			case RUN:
    				carryOn = processRun(c);
    				break;

    			case EVAL:
    				carryOn = processEval(c);
    				break;

    			case EXPR:
    				carryOn = processExpr(c);
    				break;

				case EXEC:
					carryOn = processExec(c);
					break;

    			case STEP_INTO:
    				processStepInto(c);
    				carryOn = false;
    				break;

    			case STEP_OVER:
    				processStepOver(c);
    				carryOn = false;
    				break;

    			case STEP_OUT:
    				processStepOut(c);
    				carryOn = false;
    				break;

    			case STOP:
    				processStop(c);
    				break;

    			case BREAKPOINT_GET:
    				breakpointGet(c);
    				break;

    			case BREAKPOINT_SET:
    				breakpointSet(c);
    				break;

    			case BREAKPOINT_UPDATE:
    				breakpointUpdate(c);
    				break;

    			case BREAKPOINT_REMOVE:
    				breakpointRemove(c);
    				break;

    			case BREAKPOINT_LIST:
    				breakpointList(c);
    				break;

    			case STACK_DEPTH:
    				stackDepth(c);
    				break;

    			case STACK_GET:
    				stackGet(c);
    				break;

    			case CONTEXT_NAMES:
    				contextNames(c);
    				break;

    			case CONTEXT_GET:
    				contextGet(c);
    				break;

    			case PROPERTY_GET:
    				propertyGet(c);
    				break;

    			case SOURCE:
    				processSource(c);
    				break;

    			case STDOUT:
       				processStdout(c);
    				break;

       			case STDERR:
       				processStderr(c);
    				break;

    			case DETACH:
    				carryOn = false;
    				break;

    			case XCMD_OVERTURE_CMD:
    				processOvertureCmd(c);
    				break;

    			case PROPERTY_SET:
    			default:
    				errorResponse(DBGPErrorCode.NOT_AVAILABLE, c.type.value);
    		}
		}
		catch (DBGPException e)
		{
			System.err.printf("DBGPException %s %s\n", e.code, e.reason);
			e.printStackTrace(System.err);
			errorResponse(e.code, e.reason);
		}
		catch (Throwable e)
		{
			errorResponse(DBGPErrorCode.INTERNAL_ERROR, "" + e.getMessage());
		}

		return carryOn;
	}

	private DBGPCommand parse(String[] parts) throws Exception
	{
		// "<type> [<options>] [-- <base64 args>]"

		List<DBGPOption> options = new Vector<DBGPOption>();
		String args = null;
		boolean doneOpts = false;
		boolean gotXID = false;

		try
		{
			command = DBGPCommandType.lookup(parts[0]);

			for (int i=1; i<parts.length; i++)
			{
				if (doneOpts)
				{
					if (args != null)
					{
						throw new Exception("Expecting one base64 arg after '--'");
					}
					else
					{
						args = parts[i];
					}
				}
				else
				{
	    			if (parts[i].equals("--"))
	    			{
	    				doneOpts = true;
	    			}
	     			else
	    			{
	    				DBGPOptionType opt = DBGPOptionType.lookup(parts[i++]);

	    				if (opt == DBGPOptionType.TRANSACTION_ID)
	    				{
	    					gotXID = true;
	    					transaction = parts[i];
	    				}

						options.add(new DBGPOption(opt, parts[i]));
	     			}
				}
			}

			if (!gotXID)
			{
				throw new Exception("No transaction_id");
			}
		}
		catch (DBGPException e)
		{
			throw e;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			throw new DBGPException(
				DBGPErrorCode.INVALID_OPTIONS, "Option arg missing");
		}
		catch (Exception e)
		{
			if (doneOpts)
			{
				throw new DBGPException(DBGPErrorCode.PARSE, e.getMessage());
			}
			else
			{
				throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, e.getMessage());
			}
		}

		return new DBGPCommand(command, options, args);
	}

	private void checkArgs(DBGPCommand c, int n, boolean data) throws DBGPException
	{
		if (data && c.data == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		if (c.options.size() != n)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}
	}

	private void processStatus(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 1, false);
		statusResponse();
	}

	private void processFeatureGet(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 2, false);
		DBGPOption option = c.getOption(DBGPOptionType.N);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		String feature = features.getProperty(option.value);
		StringBuilder hdr = new StringBuilder();
   		StringBuilder body = new StringBuilder();

		if (feature == null)
		{
			// Unknown feature - unsupported in header; nothing in body
    		hdr.append("feature_name=\"");
    		hdr.append(option.value);
    		hdr.append("\" supported=\"0\"");
		}
		else
		{
			// Known feature - supported in header; body reflects actual support
    		hdr.append("feature_name=\"");
    		hdr.append(option.value);
    		hdr.append("\" supported=\"1\"");
    		body.append(feature);
		}

		response(hdr, body);
	}

	private void processFeatureSet(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 3, false);
		DBGPOption option = c.getOption(DBGPOptionType.N);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		String feature = features.getProperty(option.value);

		if (feature == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		DBGPOption newval = c.getOption(DBGPOptionType.V);

		if (newval == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		features.setProperty(option.value, newval.value);

		StringBuilder hdr = new StringBuilder();

		hdr.append("feature_name=\"");
		hdr.append(option.value);
		hdr.append("\" success=\"1\"");

		response(hdr, null);
	}

	private void dyingThread(ContextException ex)
	{
		try
		{
			breakContext = ex.ctxt;
			breakpoint = new Breakpoint(ex.ctxt.location);
			status = DBGPStatus.STOPPING;
			statusReason = DebugReason.EXCEPTION;
			errorResponse(DBGPErrorCode.EVALUATION_ERROR, ex.getMessage());

			run();

			breakContext = null;
			breakpoint = null;
			statusResponse(DBGPStatus.STOPPED, DebugReason.EXCEPTION);
		}
		catch (Exception e)
		{
			errorResponse(DBGPErrorCode.INTERNAL_ERROR, e.getMessage());
		}
	}

	private boolean processRun(DBGPCommand c) throws DBGPException
	{
		checkArgs(c, 1, false);

		if (status == DBGPStatus.BREAK || status == DBGPStatus.STOPPING)
		{
			if (breakContext != null)
			{
				breakContext.threadState.setBreaks(null, null, null);
				status = DBGPStatus.RUNNING;
				statusReason = DebugReason.OK;
				return false;	// run means continue
			}
			else
			{
				throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
			}
		}

		if (status == DBGPStatus.STARTING && expression == null && remoteControl == null)
		{
			status = DBGPStatus.RUNNING;
			statusReason = DebugReason.OK;
			return false;	// a run for a new thread, means continue
		}

		if (status != DBGPStatus.STARTING)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		if (c.data != null)	// data is in "expression"
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		if (remoteControl != null)
		{
			try
			{
				status = DBGPStatus.RUNNING;
				statusReason = DebugReason.OK;
				remoteControl.run(new RemoteInterpreter(interpreter));
				stdout("Remote control completed");
				statusResponse(DBGPStatus.STOPPED, DebugReason.OK);
			}
			catch (Exception e)
			{
				status = DBGPStatus.STOPPED;
				statusReason = DebugReason.ERROR;
				errorResponse(DBGPErrorCode.INTERNAL_ERROR, e.getMessage());
			}

			return false;	// Do not continue after remote session
		}
		else
		{
    		try
    		{
    			status = DBGPStatus.RUNNING;
    			statusReason = DebugReason.OK;
    			long before = System.currentTimeMillis();
    			theAnswer = interpreter.execute(expression);
    			stdout(expression + " = " + theAnswer.toString() + "\n");
    			long after = System.currentTimeMillis();
    			stdout("Executed in " + (double)(after-before)/1000 + " secs. ");
    			statusResponse(DBGPStatus.STOPPED, DebugReason.OK);
    		}
    		catch (ContextException e)
    		{
    			dyingThread(e);
    		}
    		catch (Exception e)
    		{
    			status = DBGPStatus.STOPPED;
    			statusReason = DebugReason.ERROR;
    			errorResponse(DBGPErrorCode.EVALUATION_ERROR, e.getMessage());
    		}

    		return true;
		}
	}

	private boolean processEval(DBGPCommand c) throws DBGPException
	{
		checkArgs(c, 1, true);

		if ((status != DBGPStatus.BREAK && status != DBGPStatus.STOPPING)
			|| breakpoint == null)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		breaksSuspended = true;

		try
		{
			String exp = c.data;	// Already base64 decoded by the parser
			interpreter.setDefaultName(breakpoint.location.module);
			theAnswer = interpreter.evaluate(exp, breakContext);
			StringBuilder property = propertyResponse(exp, exp, interpreter.getDefaultName(), theAnswer);
			StringBuilder hdr = new StringBuilder("success=\"1\"");
			response(hdr, property);
		}
		catch (Exception e)
		{
			errorResponse(DBGPErrorCode.EVALUATION_ERROR, e.getMessage());
		}
		finally
		{
			breaksSuspended = false;
		}

		return true;
	}

	private boolean processExpr(DBGPCommand c) throws DBGPException
	{
		checkArgs(c, 1, true);

		if (status == DBGPStatus.BREAK || status == DBGPStatus.STOPPING)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		try
		{
			status = DBGPStatus.RUNNING;
			statusReason = DebugReason.OK;
			String exp = c.data;	// Already base64 decoded by the parser
			theAnswer = interpreter.execute(exp);
			StringBuilder property = propertyResponse(exp, exp, interpreter.getDefaultName(), theAnswer);
			StringBuilder hdr = new StringBuilder("success=\"1\"");
			status = DBGPStatus.STOPPED;
			statusReason = DebugReason.OK;
			response(hdr, property);
		}
		catch (ContextException e)
		{
			dyingThread(e);
		}
		catch (Exception e)
		{
			status = DBGPStatus.STOPPED;
			statusReason = DebugReason.ERROR;
			errorResponse(DBGPErrorCode.EVALUATION_ERROR, e.getMessage());
		}

		return true;
	}

	private boolean processExec(DBGPCommand c) throws DBGPException
	{
		return processEval(c);	// For now
	}

	private void processStepInto(DBGPCommand c) throws DBGPException
	{
		checkArgs(c, 1, false);

		if (breakpoint != null)
		{
	   		breakContext.threadState.setBreaks(breakpoint.location, null, null);
		}

		status = DBGPStatus.RUNNING;
		statusReason = DebugReason.OK;
	}

	private void processStepOver(DBGPCommand c) throws DBGPException
	{
		checkArgs(c, 1, false);

		if (breakpoint != null)
		{
			breakContext.threadState.setBreaks(
				breakpoint.location, breakContext.getRoot(), null);
		}

		status = DBGPStatus.RUNNING;
		statusReason = DebugReason.OK;
	}

	private void processStepOut(DBGPCommand c) throws DBGPException
	{
		checkArgs(c, 1, false);

		if (breakpoint != null)
		{
			breakContext.threadState.setBreaks(
				breakpoint.location, null, breakContext.getRoot().outer);
		}

		status = DBGPStatus.RUNNING;
		statusReason = DebugReason.OK;
	}

	private void processStop(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 1, false);
		statusResponse(DBGPStatus.STOPPED, DebugReason.OK);
		TransactionValue.commitAll();
	}

	private void breakpointGet(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 2, false);

		DBGPOption option = c.getOption(DBGPOptionType.D);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		Breakpoint bp = interpreter.getBreakpoints().get(Integer.parseInt(option.value));

		if (bp == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_BREAKPOINT, c.toString());
		}

		response(null, breakpointResponse(bp));
	}

	private void breakpointSet(DBGPCommand c)
		throws DBGPException, IOException, URISyntaxException
	{
		DBGPOption option = c.getOption(DBGPOptionType.T);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		DBGPBreakpointType type = DBGPBreakpointType.lookup(option.value);

		if (type == null)
		{
			throw new DBGPException(DBGPErrorCode.BREAKPOINT_TYPE_UNSUPPORTED, option.value);
		}

		option = c.getOption(DBGPOptionType.F);
		File filename = null;

		if (option != null)
		{
			filename = new File(new URI(option.value));
		}
		else
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		option = c.getOption(DBGPOptionType.S);

		if (option != null)
		{
    		if (!option.value.equalsIgnoreCase("enabled"))
    		{
    			throw new DBGPException(DBGPErrorCode.INVALID_BREAKPOINT, option.value);
    		}
		}

		option = c.getOption(DBGPOptionType.N);
		int lineno = 0;

		if (option != null)
		{
			lineno = Integer.parseInt(option.value);
		}

		String condition = null;

		if (c.data != null)
		{
			condition = c.data;
		}
		else
		{
			DBGPOption cond = c.getOption(DBGPOptionType.O);
			DBGPOption hits = c.getOption(DBGPOptionType.H);

			if (cond != null || hits != null)
			{
				String cs = (cond == null) ? ">=" : cond.value;
				String hs = (hits == null) ? "0"  : hits.value;

				if (hs.equals("0"))
				{
					condition = "= 0";		// impossible (disabled)
				}
				else if (cs.equals("=="))
    			{
    				condition = "= " + hs;
    			}
    			else if (cs.equals(">="))
    			{
    				condition = ">= " + hs;
    			}
    			else if (cs.equals("%"))
    			{
    				condition = "mod " + hs;
    			}
    			else
    			{
    				throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
    			}
			}
		}

		Breakpoint bp = null;
		INStatement stmt = interpreter.findStatement(filename, lineno);

		if (stmt == null)
		{
			INExpression exp = interpreter.findExpression(filename, lineno);

			if (exp == null)
			{
				throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT, filename + ":" + lineno);
			}
			else
			{
				try
				{
					if (exp.breakpoint.number != 0)
					{
						// Multiple threads set BPs multiple times, so...
						bp = exp.breakpoint;	// Re-use the existing one
					}
					else
					{
						bp = interpreter.setBreakpoint(exp, condition);
					}
				}
				catch (ParserException e)
				{
					throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT,
						filename + ":" + lineno + ", " + e.getMessage());
				}
				catch (LexException e)
				{
					throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT,
						filename + ":" + lineno + ", " + e.getMessage());
				}
				catch (Exception e)
				{
					throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT,
						filename + ":" + lineno + ", " + e.getMessage());
				}
			}
		}
		else
		{
			try
			{
				if (stmt.breakpoint.number != 0)
				{
					// Multiple threads set BPs multiple times, so...
					bp = stmt.breakpoint;	// Re-use the existing one
				}
				else
				{
					bp = interpreter.setBreakpoint(stmt, condition);
				}
			}
			catch (ParserException e)
			{
				throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT,
					filename + ":" + lineno + ", " + e.getMessage());
			}
			catch (LexException e)
			{
				throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT,
					filename + ":" + lineno + ", " + e.getMessage());
			}
			catch (Exception e)
			{
				throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT,
					filename + ":" + lineno + ", " + e.getMessage());
			}
		}

		StringBuilder hdr = new StringBuilder(
			"state=\"enabled\" id=\"" + bp.number + "\"");
		response(hdr, null);
	}

	private void breakpointUpdate(DBGPCommand c) throws DBGPException
	{
		checkArgs(c, 2, false);

		DBGPOption option = c.getOption(DBGPOptionType.D);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		Breakpoint bp = interpreter.getBreakpoints().get(Integer.parseInt(option.value));

		if (bp == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_BREAKPOINT, c.toString());
		}

		throw new DBGPException(DBGPErrorCode.UNIMPLEMENTED, c.toString());
	}

	private void breakpointRemove(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 2, false);

		DBGPOption option = c.getOption(DBGPOptionType.D);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		if (interpreter.clearBreakpoint(Integer.parseInt(option.value)) == null)
		{
			// Multiple threads remove BPs multiple times
			// throw new DBGPException(DBGPErrorCode.INVALID_BREAKPOINT, c.toString());
		}

		response(null, null);
	}

	private void breakpointList(DBGPCommand c) throws IOException, DBGPException
	{
		checkArgs(c, 1, false);
		StringBuilder bps = new StringBuilder();

		for (Integer key: interpreter.getBreakpoints().keySet())
		{
			Breakpoint bp = interpreter.getBreakpoints().get(key);
			bps.append(breakpointResponse(bp));
		}

		response(null, bps);
	}

	private void stackDepth(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 1, false);

		if (status != DBGPStatus.BREAK && status != DBGPStatus.STOPPING)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		StringBuilder sb = new StringBuilder();
		sb.append(breakContext.getDepth());

		response(null, sb);
	}

	private void stackGet(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 1, false);

		if ((status != DBGPStatus.BREAK && status != DBGPStatus.STOPPING)
			|| breakpoint == null)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		DBGPOption option = c.getOption(DBGPOptionType.D);
		int depth = -1;

		if (option != null)
		{
			depth = Integer.parseInt(option.value);	// 0 to n-1
		}

		// We omit the last frame, as this is unhelpful (globals),

		int actualDepth = breakContext.getDepth() - 1;

		if (depth >= actualDepth)
		{
			System.err.println("depth = " + depth);
			System.err.println("actualDepth = " + actualDepth);
			throw new DBGPException(DBGPErrorCode.INVALID_STACK_DEPTH, c.toString());
		}

		if (depth == 0)
		{
			response(null, stackResponse(breakpoint.location, 0));
		}
		else if (depth > 0)
		{
			Context ctxt = breakContext.getFrame(depth);
			response(null, stackResponse(ctxt.location, depth));
		}
		else
		{
			// The location of a context is where it was called from, so
			// to build the stack locations, we take the location of the
			// level above, and the first level is the BP's location,
			// assuming we have one which is different to the ctxt location.

			StringBuilder sb = new StringBuilder();
			int d = 0;

			if (!breakpoint.location.equals(breakContext.location))		// BP is different
			{
				sb.append(stackResponse(breakpoint.location, d++));
				actualDepth--;
			}

			for (int f=0; f < actualDepth; f++)
			{
				Context ctxt = breakContext.getFrame(f);
				sb.append(stackResponse(ctxt.location, d++));
			}

			response(null, sb);
		}
	}

	private void contextNames(DBGPCommand c) throws DBGPException, IOException
	{
		if (c.data != null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		DBGPOption option = c.getOption(DBGPOptionType.D);

		if (c.options.size() > ((option == null) ? 1 : 2))
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		StringBuilder names = new StringBuilder();
		String dialect = Settings.dialect == Dialect.VDM_SL ? "Module" : "Class";

		names.append("<context name=\"Local\" id=\"0\"/>");
		names.append("<context name=\"" + dialect + "\" id=\"1\"/>");
		names.append("<context name=\"Global\" id=\"2\"/>");

		response(null, names);
	}

	private NameValuePairMap getContextValues(DBGPContextType context, int depth) throws Exception
	{
		NameValuePairMap vars = new NameValuePairMap();

		switch (context)
		{
			case LOCAL:
				if (depth == 0)
				{
					vars.putAll(breakContext.getVisibleVariables());
				}
				else
				{
					Context frame = breakContext.getFrame(depth - 1).outer;

					if (frame != null)
					{
						vars.putAll(frame.getVisibleVariables());
					}
				}

				if (breakContext instanceof ObjectContext)
				{
					ObjectContext octxt = (ObjectContext)breakContext;
					int line = breakpoint.location.startLine;
					String opname = breakContext.guardOp == null ? "" : breakContext.guardOp.name.getName();

					for (TCDefinition d: octxt.self.type.classdef.definitions)
					{
						if (d instanceof TCPerSyncDefinition)
						{
							TCPerSyncDefinition pdef = (TCPerSyncDefinition)d;
							INExpression guard = ClassMapper.getInstance(INNode.MAPPINGS).convert(pdef.guard);

							if (pdef.opname.getName().equals(opname) ||
								pdef.location.startLine == line ||
								guard.findExpression(line) != null)
							{
	            				for (INExpression sub: guard.getHistoryExpressions())
	            				{
            						INHistoryExpression hexp = (INHistoryExpression)sub;
            						Value v = hexp.eval(octxt);
            						TCNameToken name =
            							new TCNameToken(pdef.location, octxt.self.type.name.getModule(),
            								hexp.toString());
            						vars.put(name, v);
	            				}
							}
						}
						else if (d instanceof TCMutexSyncDefinition)
						{
							TCMutexSyncDefinition mdef = (TCMutexSyncDefinition)d;

            				for (TCNameToken mop: mdef.operations)
            				{
            					if (mop.getName().equals(opname))
            					{
                    				for (TCNameToken op: mdef.operations)
                    				{
                    					TCNameList ops = new TCNameList(op);
                    					INHistoryExpression hexp = new INHistoryExpression(mdef.location, Token.ACTIVE, ops);
                    					Value v = hexp.eval(octxt);
                						TCNameToken name =
                							new TCNameToken(mdef.location, octxt.self.type.name.getModule(),
                								hexp.toString());
                						vars.put(name, v);
                    				}

                    				break;
            					}
            				}
						}
					}
				}
				break;

			case CLASS:		// Includes modules
				Context root = breakContext.getFrame(depth);

				if (root instanceof ObjectContext)
				{
					ObjectContext octxt = (ObjectContext)root;
					vars.putAll(octxt.self.members);
				}
				else if (root instanceof ClassContext)
				{
					ClassContext cctxt = (ClassContext)root;
					vars.putAll(cctxt.classdef.getStatics());
				}
				else if (root instanceof StateContext)
				{
					StateContext sctxt = (StateContext)root;

					if (sctxt.stateCtxt != null)
					{
						vars.putAll(sctxt.stateCtxt);
					}
				}
				break;

			case GLOBAL:
				vars.putAll(interpreter.getInitialContext());
				break;
		}

		return vars;
	}

	private void contextGet(DBGPCommand c) throws Exception
	{
		if (c.data != null || c.options.size() > 3)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		if (status != DBGPStatus.BREAK && status != DBGPStatus.STOPPING)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		DBGPOption option = c.getOption(DBGPOptionType.C);
		int type = 0;

		if (option != null)
		{
			type = Integer.parseInt(option.value);
		}

		DBGPContextType context = DBGPContextType.lookup(type);

		option = c.getOption(DBGPOptionType.D);
		int depth = 0;

		if (option != null)
		{
			depth = Integer.parseInt(option.value);
		}

		int actualDepth = breakContext.getDepth() - 1;

		if (depth >= actualDepth)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_STACK_DEPTH, c.toString());
		}

		NameValuePairMap vars = getContextValues(context, depth);

		response(null, propertyResponse(vars));
	}

	private void propertyGet(DBGPCommand c) throws Exception
	{
		if (c.data != null || c.options.size() > 4)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		if (status != DBGPStatus.BREAK && status != DBGPStatus.STOPPING)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		DBGPOption option = c.getOption(DBGPOptionType.C);
		int type = 0;

		if (option != null)
		{
			type = Integer.parseInt(option.value);
		}

		DBGPContextType context = DBGPContextType.lookup(type);

		option = c.getOption(DBGPOptionType.D);
		int depth = -1;

		if (option != null)
		{
			depth = Integer.parseInt(option.value);
		}

		option = c.getOption(DBGPOptionType.N);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.CANT_GET_PROPERTY, c.toString());
		}

		LexTokenReader ltr = new LexTokenReader(option.value, Dialect.VDM_PP);
		LexToken token = null;

		try
		{
			token = ltr.nextToken();
		}
		catch (LexException e)
		{
			throw new DBGPException(DBGPErrorCode.CANT_GET_PROPERTY, option.value);
		}
		finally
		{
			ltr.close();
		}

		if (token.isNot(Token.NAME))
		{
			if (token.is(Token.IDENTIFIER))
			{
				LexIdentifierToken id = (LexIdentifierToken)token;
				token = new LexNameToken("DEFAULT", id);
			}
			else
			{
				throw new DBGPException(DBGPErrorCode.CANT_GET_PROPERTY, token.toString());
			}
		}

		NameValuePairMap vars = getContextValues(context, depth);
		TCNameToken longname = new TCNameToken((LexNameToken)token);
		Value value = vars.get(longname);

		if (value == null)
		{
			throw new DBGPException(
				DBGPErrorCode.CANT_GET_PROPERTY, longname.toString());
		}

		response(null, propertyResponse(longname, value));
	}

	private void processSource(DBGPCommand c) throws DBGPException, IOException
	{
		if (c.data != null || c.options.size() > 4)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		DBGPOption option = c.getOption(DBGPOptionType.B);
		int begin = 1;

		if (option != null)
		{
			begin = Integer.parseInt(option.value);
		}

		option = c.getOption(DBGPOptionType.E);
		int end = 0;

		if (option != null)
		{
			end = Integer.parseInt(option.value);
		}

		option = c.getOption(DBGPOptionType.F);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		File file = null;

		try
		{
			file = new File(new URI(option.value));
		}
		catch (URISyntaxException e)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		SourceFile s = interpreter.getSourceFile(file);
		StringBuilder sb = new StringBuilder();

		if (end == 0)
		{
			end = s.getCount();
		}

		sb.append("<![CDATA[");

		for (int n = begin; n <= end; n++)
		{
			sb.append(quote(s.getLine(n)));
			sb.append("\n");
		}

		sb.append("]]>");
		response(null, sb);
	}

	private void processStdout(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 2, false);
		DBGPOption option = c.getOption(DBGPOptionType.C);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		DBGPRedirect redirect = DBGPRedirect.lookup(option.value);
		StdoutRedirector.directStdout(this, redirect);

		response(new StringBuilder("success=\"1\""), null);
	}

	private void processStderr(DBGPCommand c) throws DBGPException, IOException
	{
		checkArgs(c, 2, false);
		DBGPOption option = c.getOption(DBGPOptionType.C);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		DBGPRedirect redirect = DBGPRedirect.lookup(option.value);
		StderrRedirector.directStderr(this, redirect);

		response(new StringBuilder("success=\"1\""), null);
	}

	public synchronized void stdout(String line) throws IOException
	{
		StringBuilder sb = new StringBuilder("<stream type=\"stdout\"><![CDATA[");
		sb.append(Base64.encode(line.getBytes("UTF-8")));
		sb.append("]]></stream>\n");
		write(sb);
	}

	public synchronized void stderr(String line) throws IOException
	{
		StringBuilder sb = new StringBuilder("<stream type=\"stderr\"><![CDATA[");
		sb.append(Base64.encode(line.getBytes("UTF-8")));
		sb.append("]]></stream>\n");
		write(sb);
	}

	private void processOvertureCmd(DBGPCommand c) throws Exception
	{
		checkArgs(c, 2, false);
		DBGPOption option = c.getOption(DBGPOptionType.C);

		if (option == null)
		{
			throw new DBGPException(DBGPErrorCode.INVALID_OPTIONS, c.toString());
		}

		if (option.value.equals("init"))
		{
			processInit(c);
		}
		else if (option.value.equals("create"))
		{
			processCreate(c);
		}
		else if (option.value.equals("currentline"))
		{
			processCurrentLine(c);
		}
		else if (option.value.equals("source"))
		{
			processCurrentSource(c);
		}
		else if (option.value.equals("coverage"))
		{
			processCoverage(c);
		}
		else if (option.value.equals("write_complete_coverage"))
		{
			processCompleteCoverage(c);
		}
		else if (option.value.equals("runtrace"))
		{
			processRuntrace(c);
		}
		else if (option.value.startsWith("latex"))
		{
			processLatex(c);
		}
		else if (option.value.equals("word"))
		{
			processWord(c);
		}
		else if (option.value.equals("pog"))
		{
			processPOG(c);
		}
		else if (option.value.equals("stack"))
		{
			processStack(c);
		}
		else if (option.value.equals("trace"))
		{
			processTrace(c);
		}
		else if (option.value.equals("list"))
		{
			processList();
		}
		else if (option.value.equals("files"))
		{
			processFiles();
		}
		else if (option.value.equals("classes"))
		{
			processClasses();
		}
		else if (option.value.equals("modules"))
		{
			processModules();
		}
		else if (option.value.equals("default"))
		{
			processDefault(c);
		}
		else if (option.value.equals("log"))
		{
			processLog(c);
		}
		else
		{
			throw new DBGPException(DBGPErrorCode.UNIMPLEMENTED, c.toString());
		}
	}

	private void processInit(DBGPCommand c) throws IOException, DBGPException
	{
		if (status == DBGPStatus.BREAK || status == DBGPStatus.STOPPING)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		LexLocation.clearLocations();
		interpreter.init();
		statusResponse(DBGPStatus.STOPPED, DebugReason.OK);
		cdataResponse("Global context and test coverage initialized");
	}

	private void processLog(DBGPCommand c) throws IOException
	{
		StringBuilder out = new StringBuilder();

		try
		{
			if (c.data == null)
			{
				if (RTLogger.getLogSize() > 0)
				{
					out.append("Flushing " + RTLogger.getLogSize() + " RT events\n");
				}

				RTLogger.setLogfileName(null);
				out.append("RT events now logged to the console");
			}
			else if (c.data.equals("off"))
			{
				RTLogger.enable(false);
				out.append("RT event logging disabled");
			}
			else
			{
				RTLogger.setLogfileName(new File(c.data));
				out.append("RT events now logged to " + c.data);
			}
		}
		catch (FileNotFoundException e)
		{
			out.append("Cannot create RT event log: " + e.getMessage());
		}

		cdataResponse(out.toString());
	}

	private void processCreate(DBGPCommand c) throws DBGPException
	{
		if (!(interpreter instanceof ClassInterpreter))
		{
			throw new DBGPException(DBGPErrorCode.INTERNAL_ERROR, "Not available for VDM-SL");
		}

		try
		{
			int i = c.data.indexOf(' ');
			String var = c.data.substring(0, i);
			String exp = c.data.substring(i + 1);

			((ClassInterpreter)interpreter).create(var, exp);
		}
		catch (Exception e)
		{
			throw new DBGPException(DBGPErrorCode.INTERNAL_ERROR, e.getMessage());
		}
	}

	private void processStack(DBGPCommand c) throws IOException, DBGPException
	{
		if ((status != DBGPStatus.BREAK && status != DBGPStatus.STOPPING)
			|| breakpoint == null)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		OutputStream out = new ByteArrayOutputStream();
		ConsolePrintWriter pw = new ConsolePrintWriter(out);
		pw.println("Stopped [" + Thread.currentThread().getName() + "] " + breakpoint.location);
		breakContext.printStackTrace(pw, true);
		pw.close();
		cdataResponse(out.toString());
	}

	private void processTrace(DBGPCommand c) throws DBGPException
	{
		File file = null;
		int line = 0;
		String trace = null;

		try
		{
    		int i = c.data.indexOf(' ');
    		int j = c.data.indexOf(' ', i+1);
    		file = new File(new URI(c.data.substring(0, i)));
    		line = Integer.parseInt(c.data.substring(i+1, j));
    		trace = c.data.substring(j+1);

    		if (trace.length() == 0) trace = null;

    		OutputStream out = new ByteArrayOutputStream();
    		PrintWriter pw = new PrintWriter(out);

    		INStatement stmt = interpreter.findStatement(file, line);

    		if (stmt == null)
    		{
    			INExpression exp = interpreter.findExpression(file, line);

    			if (exp == null)
    			{
    				throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT,
    					"No breakable expressions or statements at " + file + ":" + line);
    			}
    			else
    			{
    				interpreter.clearBreakpoint(exp.breakpoint.number);
    				Breakpoint bp = interpreter.setTracepoint(exp, trace);
    				pw.println("Created " + bp);
    				pw.println(interpreter.getSourceLine(bp.location));
    			}
    		}
    		else
    		{
    			interpreter.clearBreakpoint(stmt.breakpoint.number);
    			Breakpoint bp = interpreter.setTracepoint(stmt, trace);
    			pw.println("Created " + bp);
    			pw.println(interpreter.getSourceLine(bp.location));
    		}

    		pw.close();
    		cdataResponse(out.toString());
		}
		catch (Exception e)
		{
			throw new DBGPException(DBGPErrorCode.CANT_SET_BREAKPOINT, e.getMessage());
		}
	}

	private void processList() throws IOException
	{
		Map<Integer, Breakpoint> map = interpreter.getBreakpoints();
		OutputStream out = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(out);

		for (Entry<Integer, Breakpoint> entry: map.entrySet())
		{
			Breakpoint bp = entry.getValue();
			pw.println(bp.toString());
			pw.println(interpreter.getSourceLine(bp.location));
		}

		pw.close();
		cdataResponse(out.toString());
	}

	private void processFiles() throws IOException
	{
		Set<File> filenames = interpreter.getSourceFiles();
		OutputStream out = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(out);

		for (File file: filenames)
		{
			pw.println(file.getPath());
		}

		pw.close();
		cdataResponse(out.toString());
	}

	private void processClasses() throws IOException, DBGPException
	{
		if (!(interpreter instanceof ClassInterpreter))
		{
			throw new DBGPException(
				DBGPErrorCode.INTERNAL_ERROR, "Not available for VDM-SL");
		}

		ClassInterpreter cinterpreter = (ClassInterpreter)interpreter;
		String def = cinterpreter.getDefaultName();
		INClassList classes = cinterpreter.getClasses();
		OutputStream out = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(out);

		for (INClassDefinition cls: classes)
		{
			if (cls.name.getName().equals(def))
			{
				pw.println(cls.name.getName() + " (default)");
			}
			else
			{
				pw.println(cls.name.getName());
			}
		}

		pw.close();
		cdataResponse(out.toString());
	}

	private void processModules() throws DBGPException, IOException
	{
		if (!(interpreter instanceof ModuleInterpreter))
		{
			throw new DBGPException(
				DBGPErrorCode.INTERNAL_ERROR, "Only available for VDM-SL");
		}

		ModuleInterpreter minterpreter = (ModuleInterpreter)interpreter;
		String def = minterpreter.getDefaultName();
		List<INModule> modules = minterpreter.getModules();
		OutputStream out = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(out);

		for (INModule m: modules)
		{
			if (m.name.getName().equals(def))
			{
				pw.println(m.name.getName() + " (default)");
			}
			else
			{
				pw.println(m.name.getName());
			}
		}

		pw.close();
		cdataResponse(out.toString());
	}

	private void processDefault(DBGPCommand c) throws DBGPException
	{
		try
		{
			interpreter.setDefaultName(c.data);
			cdataResponse("Default set to " + interpreter.getDefaultName());
		}
		catch (Exception e)
		{
			throw new DBGPException(DBGPErrorCode.INTERNAL_ERROR, e.getMessage());
		}
	}

	private void processCoverage(DBGPCommand c)
		throws DBGPException, IOException, URISyntaxException
	{
		if (status == DBGPStatus.BREAK)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		File file = new File(new URI(c.data));
		SourceFile source = interpreter.getSourceFile(file);

		if (source == null)
		{
			cdataResponse(file + ": file not found");
		}
		else
		{
			OutputStream out = new ByteArrayOutputStream();
			ConsolePrintWriter pw = new ConsolePrintWriter(out);
			source.printCoverage(pw);
			pw.close();
			cdataResponse(out.toString());
		}
	}

	private void processCompleteCoverage(DBGPCommand c)
		throws DBGPException, IOException, URISyntaxException
	{
		File file = new File(new URI(c.data));
		file.mkdirs();
		writeCoverage(interpreter, file);
		StringBuilder sb = new StringBuilder();
		sb.append("Coverage written to: " + file.toURI().toASCIIString());
		overtureResponse("write_complete_coverage", null, sb);
	}

	private void processRuntrace(DBGPCommand c) throws DBGPException
	{
		if (status == DBGPStatus.BREAK)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		try
		{
			String[] parts = c.data.split("\\s+");
			int startTest = Integer.parseInt(parts[1]);
			int endTest = Integer.parseInt(parts[2]);
			boolean debug = Boolean.parseBoolean(parts[3]);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ConsolePrintWriter pw = new ConsolePrintWriter(out);
			Interpreter.setTraceOutput(pw);
			breaksSuspended = !debug;
			interpreter.runtrace(parts[0], startTest, endTest, debug);
			pw.close();

			cdataResponse(out.toString());
			statusResponse(DBGPStatus.STOPPED, DebugReason.OK);
		}
		catch (Exception e)
		{
			throw new DBGPException(DBGPErrorCode.INTERNAL_ERROR, e.getMessage());
		}
		finally
		{
			breaksSuspended = false;
		}
	}

	private void processLatex(DBGPCommand c)
		throws DBGPException, IOException, URISyntaxException
    {
    	if (status == DBGPStatus.BREAK)
    	{
    		throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
    	}

		int i = c.data.indexOf(' ');
		File dir = new File(new URI(c.data.substring(0, i)));
		File file = new File(new URI(c.data.substring(i + 1)));

    	SourceFile source = interpreter.getSourceFile(file);
    	boolean headers = (c.getOption(DBGPOptionType.C).value.equals("latexdoc"));

    	if (source == null)
    	{
    		cdataResponse(file + ": file not found");
    	}
    	else
    	{
			File tex = new File(dir.getPath() + File.separator + file.getName() + ".tex");
			PrintWriter pw = new PrintWriter(tex);
			source.printLatexCoverage(pw, headers);
			pw.close();
			cdataResponse("Latex coverage written to " + tex);
    	}
    }

	private void processWord(DBGPCommand c)
		throws DBGPException, IOException, URISyntaxException
	{
		if (status == DBGPStatus.BREAK)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		int i = c.data.indexOf(' ');
		File dir = new File(new URI(c.data.substring(0, i)));
		File file = new File(new URI(c.data.substring(i + 1)));

		SourceFile source = interpreter.getSourceFile(file);

		if (source == null)
		{
			cdataResponse(file + ": file not found");
		}
		else
		{
			File html = new File(dir.getPath() + File.separator + file.getName() + ".doc");
			PrintWriter pw = new PrintWriter(html);
			source.printWordCoverage(pw);
			pw.close();
			cdataResponse("Word HTML coverage written to " + html);
		}
	}

	private void processCurrentLine(DBGPCommand c) throws DBGPException, IOException
	{
		if ((status != DBGPStatus.BREAK && status != DBGPStatus.STOPPING)
			|| breakpoint == null)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		if (breakpoint.location.startLine == 0)
		{
			cdataResponse("Stopped [" + Thread.currentThread().getName() + "] Thread has not started");
		}
		else
		{
    		OutputStream out = new ByteArrayOutputStream();
    		PrintWriter pw = new PrintWriter(out);
    		pw.println("Stopped [" + Thread.currentThread().getName() + "] " + breakpoint.location);
    		pw.println(interpreter.getSourceLine(
    			breakpoint.location.file, breakpoint.location.startLine, ":  "));
    		pw.close();
    		cdataResponse(out.toString());
		}
	}

	private void processCurrentSource(DBGPCommand c) throws DBGPException, IOException
	{
		if ((status != DBGPStatus.BREAK && status != DBGPStatus.STOPPING)
			|| breakpoint == null)
		{
			throw new DBGPException(DBGPErrorCode.NOT_AVAILABLE, c.toString());
		}

		File file = breakpoint.location.file;
		int current = breakpoint.location.startLine;

		if (current == 0)
		{
			cdataResponse("Thread has not started");
		}
		else
		{
    		int start = current - SOURCE_LINES;
    		if (start < 1) start = 1;
    		int end = start + SOURCE_LINES*2 + 1;
    
    		StringBuilder sb = new StringBuilder();
    
    		for (int src = start; src < end; src++)
    		{
    			sb.append(interpreter.getSourceLine(
    				file, src, (src == current) ? ":>>" : ":  "));
    			sb.append("\n");
    		}
    
    		cdataResponse(sb.toString());
		}
	}

	private void processPOG(DBGPCommand c) throws Exception
	{
		ProofObligationList all = interpreter.getProofObligations();
		ProofObligationList list = null;

		if (c.data.equals("*"))
		{
			list = all;
		}
		else
		{
			list = new ProofObligationList();
			String name = c.data + "(";

			for (ProofObligation po: all)
			{
				if (po.name.indexOf(name) >= 0)
				{
					list.add(po);
				}
			}
 		}

		if (list.isEmpty())
		{
			cdataResponse("No proof obligations generated");
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Generated ");
			sb.append(plural(list.size(), "proof obligation", "s"));
			sb.append(":\n");
			sb.append(list);
			cdataResponse(sb.toString());
		}
	}

	private String plural(int n, String s, String pl)
	{
		return n + " " + (n != 1 ? s + pl : s);
	}

	private static void writeCoverage(Interpreter interpreter, File coverage)
		throws IOException
	{
		Properties.init();

		for (File f: interpreter.getSourceFiles())
		{
			SourceFile source = interpreter.getSourceFile(f);

			File data = new File(coverage.getPath() + File.separator + f.getName() + ".covtbl");
			PrintWriter pw = new PrintWriter(data);
			source.writeCoverage(pw);
			pw.close();
		}
		
		Properties.parser_tabstop = 1;
	}

	@Override
	public DebugExecutor getExecutor(LexLocation location, Context ctxt)
	{
		return null;	// Not used
	}
}
