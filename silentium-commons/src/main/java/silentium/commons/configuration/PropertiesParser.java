package silentium.commons.configuration;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.ServerType;
import silentium.commons.configuration.annotations.PropertiesFile;
import silentium.commons.utils.PropertiesUtils;

import java.util.Set;

/**
 * @author Tatanka
 */
public class PropertiesParser {
	private static final Logger log = LoggerFactory.getLogger(PropertiesParser.class);

	public static void parse() {
		final Reflections reflections = new Reflections("silentium." + ServerType.getServerTypeName() + ".configs");
		final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(PropertiesFile.class);

		for (final Class<?> clazz : annotated) {
			final String propertiesPatch = clazz.getAnnotation(PropertiesFile.class).propertiesPatch();

			ConfigurableProcessor.process(clazz, PropertiesUtils.load(propertiesPatch));
			log.info("Loading: {}", propertiesPatch);
		}
	}
}
