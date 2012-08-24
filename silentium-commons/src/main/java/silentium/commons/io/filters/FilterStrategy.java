package silentium.commons.io.filters;

import java.io.File;

/**
 * @author Tatanka
 */
public enum FilterStrategy {
	STARTS_WITH {
		@Override
		boolean apply(final File dir, final String name, final String pattern) {
			return name.startsWith(pattern);
		}
	},
	ENDS_WITH {
		@Override
		boolean apply(final File dir, final String name, final String pattern) {
			return name.endsWith(pattern);
		}
	};

	abstract boolean apply(final File dir, final String name, final String pattern);
}
