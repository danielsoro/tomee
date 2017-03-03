/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomee.cli;

import org.apache.openejb.loader.IO;
import org.apache.openejb.loader.SystemClassPath;
import org.apache.openejb.util.JavaSecurityManagers;
import org.apache.openejb.util.PropertyPlaceHolderHelper;
import org.apache.openejb.util.URLs;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * @version $Rev$ $Date$
 */
public class Bootstrap {

    private static final String OPENEJB_VERSION_PROPERTIES_FILE_NAME = "openejb-version.properties";
    private static final String OPENEJB_HOME_PROPERTY_NAME = "openejb.home";
    private static final String OPENEJB_BASE_PROPERTY_NAME = "openejb.base";
    private static final String OPENEJB_CLI_MAIN_CLASS_NAME = "org.apache.openejb.cli.MainImpl";

    private static void setupHome(final String[] args) {
        for (final String arg : args) {
            if (arg.startsWith("-D" + OPENEJB_HOME_PROPERTY_NAME)) {
                addProperty(arg);
                continue;
            }

            if (arg.startsWith("-D" + OPENEJB_BASE_PROPERTY_NAME)) {
                addProperty(arg);
            }
        }

        final String homeProperty = JavaSecurityManagers.getSystemProperty(OPENEJB_HOME_PROPERTY_NAME);
        if (homeProperty != null && new File(homeProperty).exists()) {
            return;
        }

        try {
            final URL classURL = Thread.currentThread().getContextClassLoader().getResource(OPENEJB_VERSION_PROPERTIES_FILE_NAME);

            if (classURL != null) {
                String propsString = classURL.getFile();

                propsString = propsString.substring(0, propsString.indexOf("!"));

                final URI uri = URLs.uri(propsString);

                final File jarFile = new File(uri.getSchemeSpecificPart());

                if (jarFile.getName().contains("openejb-core")) {
                    final File lib = jarFile.getParentFile();
                    final File home = lib.getParentFile().getCanonicalFile();

                    JavaSecurityManagers.setSystemProperty(OPENEJB_HOME_PROPERTY_NAME, home.getAbsolutePath());
                }
            }
        } catch (final Exception e) {
            System.err.println("Error setting " + OPENEJB_HOME_PROPERTY_NAME + " property: " + e.getClass() + ": " + e.getMessage());
        }
    }

    private static void addProperty(final String arg) {
        final String prop = arg.substring(arg.indexOf("-D") + 2, arg.indexOf("="));
        final String val = arg.substring(arg.indexOf("=") + 1);

        JavaSecurityManagers.setSystemProperty(prop, val);
    }

    private static void setupClasspath() {
        final String base = JavaSecurityManagers.getSystemProperty(OPENEJB_BASE_PROPERTY_NAME, "");
        final String home = JavaSecurityManagers.getSystemProperty("catalina.home", JavaSecurityManagers.getSystemProperty(OPENEJB_HOME_PROPERTY_NAME, base));
        try {
            final File lib = new File(home + File.separator + "lib");
            final SystemClassPath systemCP = new SystemClassPath();
            File config = new File(base, "conf/catalina.properties");
            if (!config.isFile()) {
                config = new File(home, "conf/catalina.properties");
            }
            if (config.isFile()) { // like org.apache.catalina.startup.Bootstrap.createClassLoader()
                String val = IO.readProperties(config).getProperty("common.loader", lib.getAbsolutePath());
                val = PropertyPlaceHolderHelper.simpleValue(val.replace("${catalina.", "${openejb.")); // base/home

                final StringTokenizer tokenizer = new StringTokenizer(val, ",");
                while (tokenizer.hasMoreElements()) {
                    String repository = tokenizer.nextToken().trim();
                    if (repository.isEmpty()) {
                        continue;
                    }

                    if (repository.startsWith("\"") && repository.endsWith("\"")) {
                        repository = repository.substring(1, repository.length() - 1);
                    }

                    if (repository.endsWith("*.jar")) {
                        final File dir = new File(repository.substring(0, repository.length() - "*.jar".length()));
                        if (dir.isDirectory()) {
                            systemCP.addJarsToPath(dir);
                        }
                    } else if (repository.endsWith(".jar")) {
                        final File file = new File(repository);
                        if (file.isFile()) {
                            systemCP.addJarToPath(file.toURI().toURL());
                        }
                    } else {
                        final File dir = new File(repository);
                        if (dir.isDirectory()) {
                            systemCP.addJarToPath(dir.toURI().toURL());
                        }
                    }
                }
            } else {
                systemCP.addJarsToPath(lib);
                systemCP.addJarToPath(lib.toURI().toURL());
            }
        } catch (final Exception e) {
            System.err.println("Error setting up the classpath: " + e.getClass() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Read commands from BASE_PATH (using XBean's ResourceFinder) and execute the one specified on the command line
     */
    public static void main(final String[] args) throws Exception {
        setupHome(args);
        setupClasspath();

        final Class<?> clazz = Bootstrap.class.getClassLoader().loadClass(OPENEJB_CLI_MAIN_CLASS_NAME);
        final Main main = (Main) clazz.newInstance();
        try {
            main.main(args);
        } catch (final SystemExitException e) {
            System.exit(e.getExitCode());
        }
    }

}
