package silentium.commons.configuration.transformers;

import com.google.common.base.Splitter;
import silentium.commons.configuration.PropertyTransformer;
import silentium.commons.configuration.TransformationException;

import java.lang.reflect.Field;

/**
 * This class implements basic array transfromer.
 * <p/>
 * Array can be represented by String array. In other cases
 * {@link org.bnsworld.commons.configuration.TransformationException} is thrown
 *
 * @author Tatanka
 */
public class ArrayTransformer implements PropertyTransformer<Iterable<String>> {
	/**
	 * Shared instance of this transformer. It's thread-safe so no need of multiple instances
	 */
	public static final ArrayTransformer SHARED_INSTANCE = new ArrayTransformer();

	private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

	/**
	 * Transforms string to Array
	 *
	 * @param value value that will be transformed
	 * @param field value will be assigned to this field
	 * @return Array that represetns value
	 * @throws org.bnsworld.commons.configuration.TransformationException
	 *          if somehting went wrong
	 */
	@Override
	public Iterable<String> transform(final String value, final Field field) throws TransformationException {
		return COMMA_SPLITTER.split(value);
	}
}