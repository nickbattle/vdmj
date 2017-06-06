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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.mapper;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

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
		
		return this;	// Convenient for getInstance().init().convert(obj)
	}

	/**
	 * Initialise the progress stack and converted objects map, but only if no conversion is
	 * in progress.
	 */
	public ClassMapper checkInit()
	{
		if (inProgress.isEmpty())
		{
			init();
		}
		
		return this;	// Convenient for getInstance().checkInit().convert(obj)
	}

	/**
	 * Fields used during the processing of the configuration file
	 */
	private final String configFile;
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
	
	/**
	 * A class to define how to construct one destPackage class, passing srcPackage
	 * object fields to the Constructor.
	 */
	private static class MapParams
	{
		public final int lineNo;
		public final Class<?> srcClass;
		public final Class<?> destClass;
		public final List<Field> srcFields;
		public final boolean unmapped;

		public Constructor<?> constructor;

		public MapParams(int lineNo, Class<?> srcClass, Class<?> destClass, List<Field> srcFields, boolean unmapped)
		{
			this.lineNo = lineNo;
			this.srcClass = srcClass;
			this.destClass = destClass;
			this.srcFields = srcFields;
			this.unmapped = unmapped;	
		}
		
		@Override
		public String toString()
		{
			return "map " + srcClass.getSimpleName() + " to " + destClass.getSimpleName();
		}
	}
	
	private void error(String message)
	{
		System.err.println(configFile + " line " + lineNo + ": " + message);
		errorCount++;
	}

	/**
	 * Read a mappings file and populate the mappings table. 
	 */
	private void readMappings() throws Exception
	{
		InputStream is = getClass().getResourceAsStream("/" + configFile);
		MappingReader reader = new MappingReader(configFile, is);
		
		try
		{
			while (true)
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
    					
    				case EOF:
    					return;
    					
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

	private void processUnmapped(Mapping command)
	{
		try
		{
			Class<?> toIgnore = Class.forName(command.source);
			mappings.put(toIgnore, new MapParams(lineNo, toIgnore, toIgnore, null, true));
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
		
		try
		{
			Class<?> srcClass = Class.forName(srcPackage + "." + srcClassname);
			Class<?> destClass = Class.forName(destPackage + "." + destClassname);
			
			Map<String, Field> srcFields = new HashMap<String, Field>();

			for (String fieldname: srcParams)
			{
				srcFields.put(fieldname, findField(srcClass, fieldname));
			}
			
			List<Field> selectedFields = new Vector<Field>();
			
			if (Modifier.isAbstract(srcClass.getModifiers()))
			{
				if (!Modifier.isAbstract(destClass.getModifiers()))
				{
					error("Source " + srcClassname + " is abstract, but mapping is not");
				}
				
				if (!srcParams.isEmpty())
				{
					error("Abstract class cannot have parameter substitutions");
				}
			}
			else if (Modifier.isAbstract(destClass.getModifiers()))
			{
				error("Mapped " + destClassname + " is abstract, but source is not");
			}

			for (String field: destParams)
			{
				if (field.equals("this"))
				{
					selectedFields.add(null);
				}
				else if (srcFields.containsKey(field))
				{
					selectedFields.add(srcFields.get(field));
				}
				else
				{
					error("Field not identified in " + srcClassname + ": " + field);
				}
			}
			
			for (String field: srcParams)
			{
				if (!destParams.contains(field))
				{
					error("Field not used in constructor " + destClassname + ": " + field);
				}
			}

			mappings.put(srcClass, new MapParams(lineNo, srcClass, destClass, selectedFields, false));
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
				Class<?>[] paramTypes = new Class<?>[mp.srcFields.size()];
				int a = 0;
				
				for (Field field: mp.srcFields)
				{
					Class<?> fieldType = null;
					MapParams mapping = null;
					
					if (field == null)	// ie. "this"
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
				catch (NoClassDefFoundError e)
				{
					error("No class definition found: " + mp.destClass.getSimpleName());
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
    			Object[] args = new Object[mp.srcFields.size()];
    			int a = 0;
    
    			for (Field field: mp.srcFields)
    			{
    				if (field == null)	// ie. "this"
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
 
    			for (Field field: mp.srcFields)
    			{
    				if (field != null)
    				{
    					Progress progress = isInProgress(field.get(source));
    
    					if (progress != null)
    					{
            				progress.updates.add(new Pair(result, field.getName()));
            			}
    				}
    			}
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
