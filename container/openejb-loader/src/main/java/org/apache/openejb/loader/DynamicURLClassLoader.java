package org.apache.openejb.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

public class DynamicURLClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public DynamicURLClassLoader(URLClassLoader classLoader) {
        super(classLoader.getURLs());
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}