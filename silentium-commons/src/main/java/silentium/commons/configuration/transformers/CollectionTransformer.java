package silentium.commons.configuration.transformers;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import silentium.commons.configuration.PropertyTransformer;
import silentium.commons.configuration.TransformationException;
import silentium.commons.utils.ClassUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This class implements basic Collection transfromer.
 * <p/>
 * Collection can be represented by List or Set. In other cases
 * {@link org.bnsworld.commons.configuration.TransformationException} is thrown
 * <p/>
 * TODO Collection<?> instead Collection<String>.
 *
 * @author Tatanka
 */
public class CollectionTransformer implements PropertyTransformer<Collection<String>> {
	/**
	 * Shared instance of this transformer. It's thread-safe so no need of multiple instances
	 */
	public static final CollectionTransformer SHARED_INSTANCE = new CollectionTransformer();

	private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

	/**
	 * Transforms string to Collection.
	 *
	 * @param value value that will be transformed
	 * @param field value will be assigned to this field
	 * @return Set or List that represetns value
	 * @throws org.bnsworld.commons.configuration.TransformationException
	 *          if somehting went wrong
	 */
	@Override
	public Collection<String> transform(final String value, final Field field) throws TransformationException {
		final Class<? extends Collection<String>> clazz = (Class<? extends Collection<String>>) field.getType();
		final Iterable<String> strings = COMMA_SPLITTER.split(value);

		if (ClassUtils.isSubclass(clazz, List.class))
			return Lists.newArrayList(strings);
		else if (ClassUtils.isSubclass(clazz, Set.class))
			return Sets.newHashSet(strings);
		else
			throw new TransformationException("Collection is not instance of List or Set.");
	}
}