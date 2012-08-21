package silentium.commons.configuration.transformers;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import silentium.commons.configuration.PropertyTransformer;
import silentium.commons.configuration.TransformationException;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Thransforms string to InetSocketAddress. InetSocketAddress can be represented in following ways:
 * <ul>
 * <li>address:port</li>
 * <li>*:port - will use all avaiable network interfaces</li>
 * </ul>
 *
 * @author SoulKeeper
 */
public class InetSocketAddressTransformer implements PropertyTransformer<InetSocketAddress> {
	/**
	 * Shared instance of this transformer. It's thread-safe so no need of multiple instances
	 */
	public static final InetSocketAddressTransformer SHARED_INSTANCE = new InetSocketAddressTransformer();

	private static final Splitter COLON_SPLITTER = Splitter.on(":").trimResults().omitEmptyStrings();

	/**
	 * Transforms string to InetSocketAddress
	 *
	 * @param value value that will be transformed
	 * @param field value will be assigned to this field
	 * @return InetSocketAddress that represetns value
	 * @throws org.bnsworld.commons.configuration.TransformationException
	 *          if somehting went wrong
	 */
	@Override
	public InetSocketAddress transform(final String value, final Field field) throws TransformationException {
		final Iterable<String> parts = COLON_SPLITTER.split(value);
		final List<String> addressParts = Lists.newArrayList(parts);

		if (addressParts.size() != 2)
			throw new TransformationException("Can't transform property, must be in format \"address:port\"");

		try {
			if ("*".equals(addressParts.get(0)))
				return new InetSocketAddress(Integer.parseInt(addressParts.get(1)));
			else {
				final InetAddress address = InetAddress.getByName(addressParts.get(0));
				final int port = Integer.parseInt(addressParts.get(1));

				return new InetSocketAddress(address, port);
			}
		} catch (Exception e) {
			throw new TransformationException(e);
		} finally {
			addressParts.clear();
		}
	}
}
