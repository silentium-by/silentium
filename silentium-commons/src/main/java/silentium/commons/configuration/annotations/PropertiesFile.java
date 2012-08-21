package silentium.commons.configuration.annotations;

import java.lang.annotation.*;

/**
 * This annotation is used for parsing properties files by
 * {@link org.bnsworld.commons.configuration.PropertiesParser}<br>
 * <br>
 * <p/>
 * This annotation is Documented, all definitions with it will appear in javadoc.
 *
 * @author Tatanka
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertiesFile {
	/**
	 * Properties file patch.
	 *
	 * @return properties file patch.
	 */
	String propertiesPatch();
}
