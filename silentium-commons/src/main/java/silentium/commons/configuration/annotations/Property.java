package silentium.commons.configuration.annotations;

import silentium.commons.configuration.PropertyTransformer;

import java.lang.annotation.*;

/**
 * This annotation is used to mark field that should be processed by
 * {@link org.bnsworld.commons.configuration.ConfigurableProcessor}<br>
 * <br>
 * <p/>
 * This annotation is Documented, all definitions with it will appear in javadoc
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {
	/**
	 * This string shows to {@link org.bnsworld.commons.configuration.ConfigurableProcessor} that init value of the
	 * object should not be overriden.
	 */
	String DEFAULT_VALUE = "DO_NOT_OVERWRITE_INITIALIAZION_VALUE";

	/**
	 * Property name in configuration
	 *
	 * @return name of the property that will be used
	 */
	String key();

	/**
	 * PropertyTransformer to use.<br>
	 * List of automaticly transformed types:<br>
	 * <ul>
	 * <li>{@link Boolean} and boolean by {@link org.bnsworld.commons.configuration.transformers.BooleanTransformer}</li>
	 * <li>{@link Byte} and byte by {@link org.bnsworld.commons.configuration.transformers.ByteTransformer}</li>
	 * <li>{@link Character} and char by {@link org.bnsworld.commons.configuration.transformers.CharTransformer}</li>
	 * <li>{@link Short} and short by {@link org.bnsworld.commons.configuration.transformers.ShortTransformer}</li>
	 * <li>{@link Integer} and int by {@link org.bnsworld.commons.configuration.transformers.IntegerTransformer}</li>
	 * <li>{@link Float} and float by {@link org.bnsworld.commons.configuration.transformers.FloatTransformer}</li>
	 * <li>{@link Long} and long by {@link org.bnsworld.commons.configuration.transformers.LongTransformer}</li>
	 * <li>{@link Double} and double by {@link org.bnsworld.commons.configuration.transformers.DoubleTransformer}</li>
	 * <li>{@link String} by {@link org.bnsworld.commons.configuration.transformers.StringTransformer}</li>
	 * <li>{@link Enum} and enum by {@link org.bnsworld.commons.configuration.transformers.EnumTransformer}</li>
	 * <li>{@link java.io.File} by {@link org.bnsworld.commons.configuration.transformers.FileTransformer}</li>
	 * <li>{@link java.net.InetSocketAddress} by
	 * {@link org.bnsworld.commons.configuration.transformers.InetSocketAddressTransformer}</li>
	 * <li>{@link java.util.regex.Pattern} by {@link org.bnsworld.commons.configuration.transformers.PatternTransformer}
	 * <li>{@link String[]} by {@link org.bnsworld.commons.configuration.transformers.ArrayTransformer}
	 * </ul>
	 * <p/>
	 * If your value is one of this types - just leave this field empty
	 *
	 * @return returns class that will be used to transform value
	 */
	Class<? extends PropertyTransformer> propertyTransformer() default PropertyTransformer.class;

	/**
	 * Represents default value that will be parsed if key not found. If this key equals(default) {@link #DEFAULT_VALUE}
	 * init value of the object won't be overriden
	 *
	 * @return default value of the property
	 */
	String defaultValue() default DEFAULT_VALUE;
}
