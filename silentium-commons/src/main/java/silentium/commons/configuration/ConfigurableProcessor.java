package silentium.commons.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.configuration.annotations.Property;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Properties;

/**
 * This class is designed to process classes and interfaces that have fields marked with {@link org.bnsworld.commons.configuration.annotations.Property} annotation
 *
 * @author SoulKeeper
 */
public class ConfigurableProcessor {
	private static final Logger log = LoggerFactory.getLogger(ConfigurableProcessor.class);

	/**
	 * This method is an entry point to the parser logic.<br>
	 * Any object or class that have {@link org.bnsworld.commons.configuration.annotations.Property} annotation in it or it's parent class/interface can be submitted
	 * here.<br>
	 * If object(new Something()) is submitted, object fields are parsed. (non-static)<br>
	 * If class is submitted(Sotmething.class), static fields are parsed.<br>
	 * <p/>
	 *
	 * @param object     Class or Object that has {@link org.bnsworld.commons.configuration.annotations.Property} annotations.
	 * @param properties Properties that should be used while seraching for a {@link org.bnsworld.commons.configuration.annotations.Property#key()}
	 */
	public static void process(Object object, final Properties... properties) {
		final Class clazz;

		if (object instanceof Class) {
			clazz = (Class) object;
			object = null;
		} else
			clazz = object.getClass();

		process(clazz, object, properties);
	}

	/**
	 * This method uses recurcieve calls to launch search for {@link org.bnsworld.commons.configuration.annotations.Property} annotation on itself and
	 * parents\interfaces.
	 *
	 * @param clazz Class of object
	 * @param obj   Object if any, null if parsing class (static fields only)
	 * @param props Properties with keys\values
	 */
	private static void process(final Class<?> clazz, final Object obj, final Properties... props) {
		processFields(clazz, obj, props);

		// Interfaces can't have any object fields, only static
		// So there is no need to parse interfaces for instances of objects
		// Only classes (static fields) can be located in interfaces
		if (obj == null)
			for (final Class<?> itf : clazz.getInterfaces())
				process(itf, obj, props);

		final Class<?> superClass = clazz.getSuperclass();

		if (!Objects.equals(superClass, Object.class))
			process(superClass, obj, props);
	}

	/**
	 * This method runs throught the declared fields watching for the {@link org.bnsworld.commons.configuration.annotations.Property} annotation. It also watches for
	 * the field modifiers like {@link java.lang.reflect.Modifier#STATIC} and {@link java.lang.reflect.Modifier#FINAL}
	 *
	 * @param clazz Class of object
	 * @param obj   Object if any, null if parsing class (static fields only)
	 * @param props Properties with keys\values
	 */
	private static void processFields(final Class<?> clazz, final Object obj, final Properties... props) {
		for (final Field field : clazz.getDeclaredFields()) {
			// Static fields should not be modified when processing object
			if (Modifier.isStatic(field.getModifiers()) && obj != null)
				continue;

			// Not static field should not be processed when parsing class
			if (!Modifier.isStatic(field.getModifiers()) && obj == null)
				continue;

			if (field.isAnnotationPresent(Property.class))
				// Final fields should not be processed
				if (Modifier.isFinal(field.getModifiers())) {
					final RuntimeException re = new RuntimeException("Attempt to proceed final field " + field.getName()
							+ " of class " + clazz.getName());
					log.error(re.getLocalizedMessage(), re);
					throw re;
				} else
					processField(field, obj, props);
		}
	}

	/**
	 * This method takes {@link Property} annotation and does sets value according to annotation property. For this
	 * reason {@link #getFieldValue(java.lang.reflect.Field, java.util.Properties[])} can be called, however if method
	 * sees that there is no need - field can remain with it's initial value.
	 * <p/>
	 * Also this method is capturing and logging all {@link Exception} that are thrown by underlying methods.
	 *
	 * @param field field that is going to be processed
	 * @param obj   Object if any, null if parsing class (static fields only)
	 * @param props Properties with kyes\values
	 */
	private static void processField(final Field field, final Object obj, final Properties... props) {
		final boolean oldAccessible = field.isAccessible();
		field.setAccessible(true);

		try {
			final Property property = field.getAnnotation(Property.class);

			if (!Property.DEFAULT_VALUE.equals(property.defaultValue()) || isKeyPresent(property.key(), props))
				field.set(obj, getFieldValue(field, props));
			else
				log.debug("Field " + field.getName() + " of class " + field.getDeclaringClass().getName() + " wasn't modified");
		} catch (Exception e) {
			final RuntimeException re = new RuntimeException("Can't transform field " + field.getName() + " of class "
					+ field.getDeclaringClass(), e);
			log.error(re.getLocalizedMessage(), re);
			throw re;
		}

		field.setAccessible(oldAccessible);
	}

	/**
	 * This method is responsible for receiving field value.<br>
	 * It tries to load property by key, if not found - it uses default value.<br>
	 * Transformation is done using {@link org.bnsworld.commons.configuration.PropertyTransformerFactory}
	 *
	 * @param field field that has to be transformed
	 * @param props properties with key\values
	 * @return transformed object that will be used as field value
	 * @throws TransformationException if something goes wrong during transformation
	 */
	private static Object getFieldValue(final Field field, final Properties... props) throws TransformationException {
		final Property property = field.getAnnotation(Property.class);
		final String defaultValue = property.defaultValue();
		final String key = property.key();
		String value = null;

		if (key.isEmpty())
			log.warn("Property " + field.getName() + " of class " + field.getDeclaringClass().getName()
					+ " has empty key");
		else
			value = findPropertyByKey(key, props);

		if (value == null) {
			value = defaultValue;

			log.debug("Using default value for field " + field.getName() + " of class "
					+ field.getDeclaringClass().getName());
		}

		final PropertyTransformer<?> pt = PropertyTransformerFactory.newTransformer(field.getType(), property
				.propertyTransformer());
		return pt.transform(value, field);
	}

	/**
	 * Finds value by key in properties
	 *
	 * @param key   value key
	 * @param props properties to loook for the key
	 * @return value if found, null otherwise
	 */
	private static String findPropertyByKey(final String key, final Properties... props) {
		for (final Properties p : props)
			if (p.containsKey(key))
				return p.getProperty(key);

		return null;
	}

	/**
	 * Checks if key is present in the given properties
	 *
	 * @param key   key to check
	 * @param props prperties to look for key
	 * @return true if key present, false in other case
	 */
	private static boolean isKeyPresent(final String key, final Properties... props) {
		return findPropertyByKey(key, props) != null;
	}
}
