package silentium.commons.configuration.transformers;

import silentium.commons.configuration.PropertyTransformer;
import silentium.commons.configuration.TransformationException;

import java.lang.reflect.Field;

/**
 * Transforms enum string representation to enum. String must match case definition of enum, for instance:
 * <p/>
 * <pre>
 * enum{
 *  FILE,
 *  URL
 * }
 * </pre>
 * <p/>
 * will be parsed with string "FILE" but not "file".
 *
 * @author SoulKeeper
 */
public class EnumTransformer implements PropertyTransformer<Enum<?>> {
	/**
	 * Shared instance of this transformer. It's thread-safe so no need of multiple instances
	 */
	public static final EnumTransformer SHARED_INSTANCE = new EnumTransformer();

	/**
	 * Trnasforms string to enum
	 *
	 * @param value value that will be transformed
	 * @param field value will be assigned to this field
	 * @return Enum object representing the value
	 * @throws TransformationException if somehting went wrong
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Enum<?> transform(final String value, final Field field) throws TransformationException {
		final Class<? extends Enum> clazz = (Class<? extends Enum>) field.getType();

		try {
			return Enum.valueOf(clazz, value);
		} catch (Exception e) {
			throw new TransformationException(e);
		}
	}
}
