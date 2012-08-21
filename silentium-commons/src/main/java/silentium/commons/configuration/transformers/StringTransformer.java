package silentium.commons.configuration.transformers;

import silentium.commons.configuration.PropertyTransformer;
import silentium.commons.configuration.TransformationException;

import java.lang.reflect.Field;

/**
 * This class is here just for writing less "ifs" in the code. Does nothing
 *
 * @author SoulKeeper
 */
public class StringTransformer implements PropertyTransformer<String> {
	/**
	 * Shared instance of this transformer. It's thread-safe so no need of multiple instances
	 */
	public static final StringTransformer SHARED_INSTANCE = new StringTransformer();

	/**
	 * Just returns value object
	 *
	 * @param value value that will be transformed
	 * @param field value will be assigned to this field
	 * @return return value object
	 * @throws TransformationException never thrown
	 */
	@Override
	public String transform(final String value, final Field field) throws TransformationException {
		return value;
	}
}
