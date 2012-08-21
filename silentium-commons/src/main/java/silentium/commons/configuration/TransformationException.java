package silentium.commons.configuration;

/**
 * This exception is internal for configuration process. Thrown by
 * {@link org.bnsworld.commons.configuration.PropertyTransformer} when transformaton error occurs and is catched by
 * {@link org.bnsworld.commons.configuration.ConfigurableProcessor}
 *
 * @author SoulKeeper
 */
public class TransformationException extends RuntimeException {
	/**
	 * SerialID
	 */
	private static final long serialVersionUID = -6641235751743285902L;

	/**
	 * Creates new instance of exception
	 */
	public TransformationException() {
	}

	/**
	 * Creates new instance of exception
	 *
	 * @param message exception message
	 */
	public TransformationException(final String message) {
		super(message);
	}

	/**
	 * Creates new instance of exception
	 *
	 * @param message exception message
	 * @param cause   exception that is the reason of this exception
	 */
	public TransformationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates new instance of exception
	 *
	 * @param cause exception that is the reason of this exception
	 */
	public TransformationException(final Throwable cause) {
		super(cause);
	}
}
