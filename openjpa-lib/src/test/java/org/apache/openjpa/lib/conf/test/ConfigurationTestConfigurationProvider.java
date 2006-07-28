package org.apache.openjpa.lib.conf.test;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.Properties;

import org.apache.openjpa.lib.conf.MapConfigurationProvider;

/**
 * Configuration provider used in testing.
 *
 * @author Abe White
 */
public class ConfigurationTestConfigurationProvider
    extends MapConfigurationProvider {

    public ConfigurationTestConfigurationProvider() {
        super(null);
    }

    public boolean loadDefaults(ClassLoader loader)
        throws IOException {
        return load(null, loader);
    }

    public boolean load(String rsrc, ClassLoader loader)
        throws IOException {
        if (rsrc == null)
            rsrc = System.getProperty("openjpatest.properties");
        if (rsrc == null || !rsrc.endsWith(".properties"))
            return false;

        URL url = findResource(rsrc, loader);
        if (url == null)
            throw new MissingResourceException(rsrc, getClass().getName(), 
                rsrc);

        InputStream in = url.openStream();
        Properties props = new Properties();
        if (in != null) {
            try {
                props.load(in);
                addProperties(props);
                return true;
            } finally {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    /**
     * Locate the given resource.
     */
    private URL findResource(String rsrc, ClassLoader loader)
        throws IOException {
        if (loader != null)
            return loader.getResource(rsrc);

        // in jbuilder the classloader can be null
        URL url = null;
        loader = getClass().getClassLoader();
        if (loader != null)
            url = loader.getResource(rsrc);
        if (url == null) {
            loader = Thread.currentThread().getContextClassLoader();
            if (loader != null)
                url = loader.getResource(rsrc);
        }
        if (url == null) {
            loader = ClassLoader.getSystemClassLoader();
            if (loader != null)
                url = loader.getResource(rsrc);
        }
        return url;
    }

    public boolean load(File file)
        throws IOException {
        return false;
    }
}
