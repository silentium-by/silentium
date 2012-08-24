package silentium.commons.io.filters;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author Tatanka
 */
public class CustomFileNameFilter implements FilenameFilter {
	private FilterStrategy strategy = FilterStrategy.ENDS_WITH;
	private String pattern;

	public static CustomFileNameFilter create() {
		return new CustomFileNameFilter();
	}

	public CustomFileNameFilter setStrategy(final FilterStrategy strategy) {
		this.strategy = strategy;
		return this;
	}

	public CustomFileNameFilter setPattern(final String pattern) {
		this.pattern = pattern;
		return this;
	}

	@Override
	public boolean accept(final File dir, final String name) {
		return strategy.apply(dir, name, pattern);
	}
}