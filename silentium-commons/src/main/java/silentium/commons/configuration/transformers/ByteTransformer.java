package silentium.commons.configuration.transformers;

import silentium.commons.configuration.PropertyTransformer;
import silentium.commons.configuration.TransformationException;

import java.lang.reflect.Field;

/**
 * Transforms String to Byte. String can be in decimal or hex format.
 * {@link org.bnsworld.commons.configuration.TransformationException} will be thrown if something goes wrong
 *
 * @author SoulKeeper
 */
public class ByteTransformer implements PropertyTransformer<Byte> {
	/**
	 * Shared instance of this transformer. It's thread-safe so no need of multiple instances
	 */
	public static final ByteTransformer SHARED_INSTANCE = new ByteTransformer();

	/**
	 * Tansforms string to byte
	 *
	 * @param value value that will be transformed
	 * @param field value will be assigned to this field
	 * @return Byte object that represents value
	 * @throws TransformationException if something went wrong
	 */
	@Override
	public Byte transform(final String value, final Field field) throws TransformationException {
		try {
			return Byte.decode(value);
		} catch (Exception e) {
			throw new TransformationException(e);
		}
	}
}
