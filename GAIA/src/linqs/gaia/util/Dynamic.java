package linqs.gaia.util;

import java.lang.reflect.Constructor;

import linqs.gaia.configurable.Configurable;
import linqs.gaia.exception.InvalidAssignmentException;
import linqs.gaia.exception.InvalidOperationException;

/**
 * Utilities to simplify the dynamic generation of java objects
 * 
 * @author namatag
 *
 */
public class Dynamic {
	/**
	 * Creates a new instance of an object given it's class name
	 * Note: Based in part on code from weka.core.Util.
	 * 
	 * @param classType Class that the instantiated object should
	 * be assignable to.  An exception is thrown if this is not the case
	 * @param className the fully qualified class name of the object
	 * @return Dynamically created object
	 */
	public static Object forName(Class<?> classType,
			String className) {

		Object o = null;
		try {
			Class<?> c = Class.forName(className);
			
			if (!classType.isAssignableFrom(c)) {
				throw new InvalidOperationException(classType.getName() + " is not assignable from "
						+ className);
			}
			
			o = c.newInstance();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Exception instantiating: "
					+className+" of class type "+classType+"\n"+e);
		}
		
		return o;
	}
	
	/**
	 * Create dynamic objects with arguments
	 * 
	 * @param classType Class that the instantiated object should
	 * be assignable to.  An exception is thrown if this is not the case
	 * @param className the fully qualified class name of the object
	 * @param argsClass Classes of constructor arguments
	 * @param argValues Values of constructor arguments
	 * @return Dynamically created object 
	 */
	public static Object forName(Class<?> classType, String className,
			Class<?>[] argsClass, Object[] argValues) {

		Object o = null;
		try {
			Class<?> c = Class.forName(className);
			
			if (!classType.isAssignableFrom(c)) {
				throw new InvalidOperationException(classType.getName() + " is not assignable from "
						+ className);
			}
			
			Constructor<?> constructor = c.getConstructor(argsClass);
			o = constructor.newInstance(argValues);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		return o;
	}
	
	/**
	 * Create a dynamic object for the given class, you also have the option
	 * of assigning the dynamic object a configurable object id (cid).
	 * If not assigning a configurable ID, confClassName has the same
	 * form as the argument className in the forName function.
	 * If assigning a configuration id for the configurable object,
	 * use the form &lt;cid&gt;:&lt;class&gt;.
	 * 
	 * @param classType Class that the instantiated object should
	 * be assignable to.  An exception is thrown if this is not the case
	 * @param confClassName Configurable class name
	 * @return Dynamically created object
	 */
	public static Object forConfigurableName(Class<?> classType, String confClassName,
			Class<?>[] argsClass, Object[] argValues) {
		String cid = null;
		String cclass = confClassName.trim();
		if(confClassName.contains(":")) {
			String parts[] = confClassName.split(":");
			if(parts.length != 2) {
				throw new InvalidAssignmentException("Expected format: <cid>:<class> Received: "+confClassName);
			}
			
			cid = parts[0].trim();
			cclass = parts[1].trim();
		}
		Object obj = forName(classType, cclass, argsClass, argValues);
		
		if(!(obj instanceof Configurable)) {
			throw new InvalidOperationException("Class not Configurable: "+cclass);
		}
		
		if(cid != null) {
			((Configurable) obj).setCID(cid);
		}
		
		return obj;
	}
	
	/**
	 * Create a dynamic object for the given class. You also have the option
	 * of assigning the dynamic object a configurable object id (cid).
	 * If not assigning a configurable ID, confClassName has the same
	 * form as the argument className in the forName function.
	 * If assigning a configuration id for the configurable object,
	 * use the form &lt;cid&gt;:&lt;class&gt;.
	 * 
	 * @param classType Class that the instantiated object should
	 * be assignable to.  An exception is thrown if this is not the case
	 * @param confClassName Configurable class name
	 * @return Dynamically created object
	 */
	public static Object forConfigurableName(Class<?> classType,
			String confClassName) {
		String cid = null;
		String cclass = confClassName.trim();
		if(confClassName.contains(":")) {
			String parts[] = confClassName.split(":");
			if(parts.length != 2) {
				throw new InvalidAssignmentException("Expected format: <cid>:<class> Received: "+confClassName);
			}
			
			cid = parts[0].trim();
			cclass = parts[1].trim();
		}
		Object obj = forName(classType, cclass);
		
		if(!(obj instanceof Configurable)) {
			throw new InvalidOperationException("Class not Configurable: "+cclass);
		}
		
		if(cid != null) {
			((Configurable) obj).setCID(cid);
		}
		
		return obj;
	}
	
	/**
	 * Performs the same task as forConfigurableName
	 * plus it copies the parameters from the source
	 * Configurable object into the new Configurable object.
	 * 
	 * @param classType Class that the instantiated object should
	 * be assignable to.  An exception is thrown if this is not the case
	 * @param confClassName Configurable class name
	 * @param source Configurable object to copy parameters from
	 * @return Dynamically created object
	 */
	public static Object forConfigurableName(Class<?> classType,
			String confClassName, Configurable source) {
		Object obj = Dynamic.forConfigurableName(classType, confClassName);
		((Configurable) obj).copyParameters(source);
		
		return obj;
	}
	
	/**
	 * Check to see if a class with the given name exists.
	 * If it does, return true and false otherwise.
	 * 
	 * @param className Class whose existence we need to check
	 * @return True if the class exists and false otherwise.
	 */
	public boolean exists(String className)
	{
		try {
			Class.forName(className);
			return true;
		}
		catch (ClassNotFoundException exception) {
			return false;
		}
	}
}
