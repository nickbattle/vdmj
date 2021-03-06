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

package com.fujitsu.vdmj.mapper;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.config.Properties;

/**
 * A class to map classes and extend trees of objects. 
 */
public class ClassMapper
{
	/** The mappers that have already been loaded, indexed by resource name */
	private final static Map<String, ClassMapper> mappers = new HashMap<String, ClassMapper>();
	
	/**
	 * These caches hold the object references converted so far, and keep a stack of
	 * the objects that are currently being processed.
	 */
	private final Stack<Progress> inProgress = new Stack<Progress>();
	
	private final Map<Long, Object> converted = new HashMap<Long, Object>();
	
	private long loadTimeMs;

	/**
	 * Get an instance of a mapper, defined by the mapspec file name (resource).
	 */
	public static ClassMapper getInstance(String config)
	{
		ClassMapper mapper = mappers.get(config);
		
		if (mapper == null)
		{
			mapper = new ClassMapper(config);
			mappers.put(config, mapper);
		}

		return mapper;
	}
	
	/**
	 * Initialise the progress stack and converted objects map.
	 */
	public ClassMapper init()
	{
		inProgress.clear();
		converted.clear();
		
		for (Method m: initializers)
		{
			try
			{
				m.invoke(null, (Object[])null);
			}
			catch (Exception e)
			{
				throw new RuntimeException("Error in mapping initialzer", e);
			}
		}
		
		return this;	// Convenient for getInstance().init().convert(obj)
	}

	/**
	 * Fields used during the processing of the configuration file
	 */
	private final String configFile;
	private Field SELF;
	private String srcPackage = "";
	private String destPackage = "";
	private int lineNo = 0;
	private int errorCount = 0;
	
	/**
	 * The private constructor, passed the resource name of the mapping file.
	 */
	private ClassMapper(String config)
	{
		configFile = config;
		long before = System.currentTimeMillis();

		try
		{
			SELF = ClassMapper.class.getDeclaredField("SELF");
			readMappings();
			verifyConstructors();
		}
		catch (Exception e)
		{
			error(e.getMessage());
		}
		
		loadTimeMs = System.currentTimeMillis() - before;
		
		if (errorCount > 0)
		{
			System.err.println("Aborting with " + errorCount + " errors");
			System.exit(1);
		}
	}

	/** The set of class mappings for one loaded file */
	private final Map<Class<?>, MapParams> mappings = new HashMap<Class<?>, MapParams>();
	
	/** A list of methods to call in the init() processing */
	private final List<Method> initializers = new Vector<Method>();
	
	/**
	 * A class to define how to construct one destPackage class, passing srcPackage
	 * object fields to the Constructor and setter methods.
	 */
	private static class MapParams
	{
		public final int lineNo;
		public final Class<?> srcClass;
		public final Class<?> destClass;
		public final List<Field> ctorFields;
		public final List<Field> setterFields;
		public final boolean unmapped;

		public Constructor<?> constructor;
		public Method[] setters;

		public MapParams(int lineNo, Class<?> srcClass, Class<?> destClass,
				List<Field> ctorFields, List<Field> setterFields, boolean unmapped)
		{
			this.lineNo = lineNo;
			this.srcClass = srcClass;
			this.destClass = destClass;
			this.ctorFields = ctorFields;
			this.setterFields = setterFields;
			this.unmapped = unmapped;	
		}
		
		@Override
		public String toString()
		{
			return "map " + srcClass.getSimpleName() + " to " + destClass.getSimpleName() +
					(setterFields.isEmpty() ? "" : " set " + setterFields);
		}
	}
	
	private void error(String message)
	{
		System.err.println(configFile + " line " + lineNo + ": " + message);
		errorCount++;
	}

	/**
	 * Read mappings file(s) from the classpath and populate the mappings table. 
	 */
	private void readMappings() throws Exception
	{
		Enumeration<URL> urls = this.getClass().getClassLoader().getResources(configFile);

		while (urls.hasMoreElements())
		{
			URL url = urls.nextElement();
			readMapping(configFile, url.openStream());
		}
		
		/**
		 * You can add extra file locations by setting the vdmj.mappingpath property.
		 * This allows more than one mapping file of the same name to be included within
		 * one jar file.
		 */
		String mappingPath = Properties.mapping_search_path;
		
		if (mappingPath != null)
		{
			for (String classpath: mappingPath.split(File.pathSeparator))
			{
				String filename = classpath + "/" + configFile;		// NB. Use slash here!
				InputStream is = getClass().getResourceAsStream(filename);
				
				if (is != null)
				{
					readMapping(filename, is);
				}
			}
		}
	}

	private void readMapping(String filename, InputStream is) throws Exception
	{
		MappingReader reader = new MappingReader(filename, is);
		
		try
		{
			boolean eof = false;
			
			while (!eof)
			{
    			Mapping command = reader.readCommand();
    			lineNo = command.lineNo;
    
    			switch (command.type)
    			{
    				case PACKAGE:
    					processPackage(command);
    					break;
    					
    				case MAP:
    					processMap(command);
    					break;
    					
    				case UNMAPPED:
    					processUnmapped(command);
    					break;
    					
    				case INIT:
    					processInit(command);
    					break;
    					
    				case EOF:
    					eof = true;
    					break;
    					
					case ERROR:
						// try next line
						errorCount++;
						break;
    			}
			}
		}
		finally
		{
    		reader.close();
		}
	}

	private void processInit(Mapping command)
	{
		String classname  = null;
		String methodname = null;

		try
		{
			int end = command.source.lastIndexOf('.');
			
			if (end > 0)
			{
				classname  = command.source.substring(0, end);
				methodname = command.source.substring(end + 1);
				Class<?> initClass = Class.forName(classname);
				Method initMethod = initClass.getMethod(methodname, (Class<?>[])null);
				
				if (!Modifier.isStatic(initMethod.getModifiers()))
				{
					error("Init method is not static: " + classname + "." + methodname + "()");
				}
				else
				{
					initializers.add(initMethod);
				}
			}
			else
			{
				error("Init entry malformed: " + command.source);
			}
		}
		catch (ClassNotFoundException e)
		{
			error("No such class: " + classname);
		}
		catch (NoSuchMethodException e)
		{
			error("Cannot find method: " + classname + "." + methodname + "()");
		}
	}

	private void processUnmapped(Mapping command)
	{
		try
		{
			Class<?> toIgnore = Class.forName(command.source);
			mappings.put(toIgnore, new MapParams(lineNo, toIgnore, toIgnore, null, null, true));
		}
		catch (ClassNotFoundException e)
		{
			error("No such class: " + command.source);
		}
	}

	private void processPackage(Mapping command)
	{
		srcPackage = command.source;
		destPackage = command.destination;
	}

	private void processMap(Mapping command) throws Exception
	{
		String srcClassname = command.source;
		List<String> srcParams = command.varnames;
		String destClassname = command.destination;
		List<String> destParams = command.paramnames;
		List<String> destSetters = command.setnames;
		
		try
		{
			Class<?> srcClass = Class.forName(srcPackage + "." + srcClassname);
			Class<?> destClass = Class.forName(destPackage + "." + destClassname);
			
			Map<String, Field> srcFields = new HashMap<String, Field>();

			for (String fieldname: srcParams)
			{
				srcFields.put(fieldname, findField(srcClass, fieldname));
			}
			
			if (Modifier.isAbstract(srcClass.getModifiers()))
			{
				if (!Modifier.isAbstract(destClass.getModifiers()))
				{
					error("Source " + srcClassname + " is abstract, but mapping is not");
				}
				
				if (!destParams.isEmpty())
				{
					error("Abstract class cannot have ctor parameter substitutions");
				}
			}
			else if (Modifier.isAbstract(destClass.getModifiers()))
			{
				error("Mapped " + destClassname + " is abstract, but source is not");
			}

			List<Field> ctorFields = new Vector<Field>();
			List<Field> setterFields = new Vector<Field>();
			
			for (String field: destParams)
			{
				if (field.equals("this"))
				{
					ctorFields.add(SELF);
				}
				else if (srcFields.containsKey(field))
				{
					ctorFields.add(srcFields.get(field));
				}
				else
				{
					error("Field not identified in " + srcClassname + ": " + field);
				}
			}
			
			for (String field: destSetters)
			{
				if (srcFields.containsKey(field))
				{
					setterFields.add(srcFields.get(field));
				}
				else
				{
					error("Field not identified in " + srcClassname + ": " + field);
				}
			}
			
			for (String field: srcParams)
			{
				if (!destParams.contains(field) && !destSetters.contains(field))
				{
					error("Field not used in constructor or setters, " + destClassname + ": " + field);
				}
			}

			for (String field: destParams)
			{
				if (destSetters.contains(field))
				{
					error("Field used in constructor and setter, " + destClassname + ": " + field);
				}
			}

			mappings.put(srcClass, new MapParams(lineNo, srcClass, destClass, ctorFields, setterFields, false));
		}
		catch (ClassNotFoundException e)
		{
			error("No such class: " + e.getMessage());
		}
		catch (NoSuchFieldException e)
		{
			error("No such field in " + srcClassname + ": " + e.getMessage());
		}
	}
	
	private Field findField(Class<?> src, String field) throws NoSuchFieldException, SecurityException
	{
		try
		{
			return src.getDeclaredField(field);
		}
		catch (NoSuchFieldException e)
		{
			return src.getField(field);
		}
	}

	/**
	 * We cannot verify the constructors exist until all the mappings have been read,
	 * because we map the parameter types from the source as well. This method checks
	 * that all of the mapped constructors exist in the destination classes.
	 */
	private void verifyConstructors()
	{
		for (Class<?> entry: mappings.keySet())
		{
			MapParams mp = mappings.get(entry);
			
			if (mp.unmapped)
			{
				continue;	// fine, it's not mapped
			}
			else if (Modifier.isAbstract(entry.getModifiers()))
			{
				continue;	// fine, it will never be instantiated anyway
			}
			else
			{
				Class<?>[] paramTypes = new Class<?>[mp.ctorFields.size()];
				int a = 0;
				
				for (Field field: mp.ctorFields)
				{
					Class<?> fieldType = null;
					MapParams mapping = null;
					
					if (field == SELF)	// ie. "this"
					{
						fieldType = mp.srcClass;
						mapping = null;
					}
					else
					{
						fieldType = field.getType();
						mapping = mappings.get(fieldType);
					}
					
					if (mapping == null || mapping.unmapped)
					{
						paramTypes[a++] = fieldType;	// eg. java.lang.String
					}
					else
					{
						paramTypes[a++] = mapping.destClass;
					}
				}
				
				try
				{
					lineNo = mp.lineNo;		// For error reporting :)
					mp.constructor = mp.destClass.getConstructor(paramTypes);
				}
				catch (NoSuchMethodException e)
				{
					error("No such constructor: " + mp.destClass.getSimpleName() + "(" + typeString(paramTypes) + ")");
					
					System.err.println("Fields available from " + entry.getSimpleName() + ":");
					
					for (Field f: getAllFields(entry))
					{
						System.err.println("    " + f.getType().getSimpleName() + " " + f.getName());
					}
					
					System.err.println("Constructors available for " + mp.destClass.getSimpleName() + ":");
					
					for (Constructor<?> ctor: mp.destClass.getConstructors())
					{
						System.err.println("    " + mp.destClass.getSimpleName() + "(" + typeString(ctor.getParameterTypes()) + ")");
					}
					
					System.err.println();
				}
				
				mp.setters = new Method[mp.setterFields.size()];
				a = 0;
				
				for (Field field: mp.setterFields)
				{
					Class<?> fieldType = field.getType();
					MapParams mapping = mappings.get(fieldType);
					Class<?> argType = null;
					
					if (mapping == null || mapping.unmapped)
					{
						argType = fieldType;	// eg. java.lang.String unmapped
					}
					else
					{
						argType = mapping.destClass;
					}

					StringBuilder name = new StringBuilder(field.getName());
					name.setCharAt(0, Character.toUpperCase(name.charAt(0)));

					try
					{
						lineNo = mp.lineNo;		// For error reporting :)
						mp.setters[a++] = mp.destClass.getMethod("set" + name, argType);
					}
					catch (NoSuchMethodException e)
					{
						error("No such setter: " + mp.destClass.getSimpleName() +
								".set" + name + "(" + argType.getSimpleName() + ")");
					}
				}
			}
		}
	}
	
	private String typeString(Class<?>[] paramTypes)
	{
		StringBuffer sb = new StringBuffer();
		String prefix = "";
		
		for (Class<?> type: paramTypes)
		{
			sb.append(prefix + type.getSimpleName());
			prefix = ", ";
		}
		
		return sb.toString();
	}
	
	private Set<Field> getAllFields(Class<?> clazz)
	{
		Set<Field> allFields = new HashSet<Field>();
		
		for (Field f: clazz.getFields())
		{
			allFields.add(f);	// Including inherited fields
		}
		
		for (Field f: clazz.getDeclaredFields())
		{
			allFields.add(f);	// Including private local fields
		}
		
		return allFields;
	}
	
	/**
	 * Private class to identify one class in the process of being converted, and a place
	 * to record objects/fields that need to be set when the conversion is complete.
	 */
	private static class Progress
	{
		public final Object source;
		public final List<Pair> updates;
		
		public Progress(Object source)
		{
			this.source = source;
			this.updates = new Vector<Pair>();
		}
		
		@Override
		public String toString()
		{
			return source.getClass().getSimpleName();
		}
	}
	
	/**
	 * Private class to hold an object/field pair to update (see Progress class).
	 */
	private static class Pair
	{
		public final Object object;
		public final String fieldname;
		
		public Pair(Object object, String fieldname)
		{
			this.object = object;
			this.fieldname = fieldname;
		}
	}
	
	/**
	 * Perform a recursive mapping for a source object/tree to a destination object/tree.
	 */
	@SuppressWarnings("unchecked")
	public <T> T convert(Object source) throws Exception
	{
		if (source == null)
		{
			return null;	// Can't do much else, but the type may be wrong
		}
		
		T result = null;
		
		// Check whether we have converted the object already.
		// We use the MappedObject id because the equals methods are
		// not cleanly implemented, and identity collisions can occur.
		// MappedObject IDs are unique for different objects.
		
		if (source instanceof MappedObject)
		{
			MappedObject mo = (MappedObject)source;
    		result = (T)converted.get(mo.getMappedId());
    		
    		if (result != null)
    		{
    			return result;	// Here's the one we've already converted
    		}
		}
		
		try
		{
    		inProgress.push(new Progress(source));
    		
    		Class<?> srcClass = source.getClass();
    		MapParams mp = mappings.get(srcClass);
    
    		if (mp == null)
    		{
    			throw new Exception("No mapping for " + srcClass + " in " + configFile);
    		}
    		else if (mp.unmapped)
    		{
    			result = (T) source;
    		}
    		else
    		{
    			Object[] args = new Object[mp.ctorFields.size()];
    			int a = 0;
    
    			for (Field field: mp.ctorFields)
    			{
    				if (field == SELF)	// ie. "this"
    				{
    					args[a++] = source;
    				}
    				else
    				{
						field.setAccessible(true);
    					Object fieldvalue = field.get(source);
    					
    					if (isInProgress(fieldvalue) == null)
    					{
    						args[a++] = convert(fieldvalue);
    					}
    					else
    					{
    						args[a++] = null;
    					}
    				}
    			}
    			
    			result = (T) mp.constructor.newInstance(args);
    			int s = 0;
    			
    			for (Field setter: mp.setterFields)
    			{
					setter.setAccessible(true);
					Object fieldvalue = setter.get(source);
					Object arg = null;
					
					if (isInProgress(fieldvalue) == null)
					{
						arg = convert(fieldvalue);
					}
					else
					{
						arg = null;
					}
					
    				mp.setters[s++].invoke(result, arg);
    			}
 
    			for (Field field: mp.ctorFields)
    			{
    				if (field != SELF)
    				{
    					Progress progress = isInProgress(field.get(source));
    
    					if (progress != null)
    					{
            				progress.updates.add(new Pair(result, field.getName()));
            			}
    				}
    			}
    			 
    			for (Field field: mp.setterFields)
    			{
					Progress progress = isInProgress(field.get(source));

					if (progress != null)
					{
        				progress.updates.add(new Pair(result, field.getName()));
        			}
    			}
     		}
		}
		catch (InvocationTargetException e)
		{
			if (e.getCause() instanceof Exception)
			{
				throw (Exception)e.getCause();
			}
			else if (e.getTargetException() instanceof Exception)
			{
				throw (Exception)e.getTargetException();
			}
			else
			{
				throw e;
			}
		}
		finally
		{
			Progress progress = inProgress.pop();
			
			if (!progress.updates.isEmpty())
			{
				for (Pair pair: progress.updates)
				{
					Field f = pair.object.getClass().getField(pair.fieldname);
					f.setAccessible(true);
					f.set(pair.object, result);
				}
			}
			
			if (source instanceof MappedObject)
			{
				MappedObject mo = (MappedObject)source;
				converted.put(mo.getMappedId(), result);
			}
		}
		
		return result;
	}

	/**
	 * Check whether an object is already in the process of being converted.
	 */
	private Progress isInProgress(Object source)
	{
		for (Progress update: inProgress)
		{
			if (source == update.source)
			{
				return update;
			}
		}
		
		return null;
	}
	
	public int getNodeCount()
	{
		return converted.size();
	}
	
	/**
	 * Return the load time of the mappings file. This is zeroed after the first request,
	 * because the mapping is not re-loaded after the first usage, and so the cost is
	 * zero.
	 */
	public long getLoadTime()
	{
		long value = loadTimeMs;
		loadTimeMs = 0;
		return value;
	}
}
