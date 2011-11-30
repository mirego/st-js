/**
 *  Copyright 2011 Alexandru Craciun, Eyal Kaspi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.stjs.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.stjs.generator.utils.PreConditions;

/**
 * This class represents a class and the corresponding generated javascript file. The information about dependencies and
 * sources are stored at generation time in a properties file that has as name [class-name].stjs (and it's packed along
 * with the source file in the same folder). Thus, if a STJS library is built, it will be delivered with all this
 * information, as the original Java code will no longer be available with the library.
 * 
 * @author acraciun
 * 
 */
public class STJSClass implements ClassWithJavascript {
	private static final String DEPENDENCIES_PROP = "dependencies";

	private static final String GENERATED_JS_FILE_PROP = "js";

	private final Properties properties;

	private final DependencyResolver dependencyResolver;
	private List<String> dependencies = Collections.emptyList();
	private List<ClassWithJavascript> directDependencies = null;

	private URI generatedJavascriptFile;

	private final String className;
	private final File targetFolder;

	/**
	 * constructor for storage
	 */
	public STJSClass(DependencyResolver dependencyResolver, File targetFolder, String className) {
		PreConditions.checkNotNull(dependencyResolver);
		PreConditions.checkNotNull(targetFolder);
		PreConditions.checkNotNull(className);
		this.targetFolder = targetFolder;
		this.className = className;
		this.properties = new Properties();
		this.dependencyResolver = dependencyResolver;
	}

	/**
	 * constructor for loading
	 * 
	 * @param builtProjectClassLoader
	 * @param className
	 */
	public STJSClass(DependencyResolver dependencyResolver, ClassLoader classLoader, String className) {
		PreConditions.checkNotNull(dependencyResolver);
		PreConditions.checkNotNull(classLoader);
		PreConditions.checkNotNull(className);

		this.className = className;
		this.targetFolder = null;
		this.dependencyResolver = dependencyResolver;
		properties = new Properties();
		try {
			InputStream inputStream = classLoader.getResourceAsStream(getPropertiesFileName());
			if (inputStream != null) {
				properties.load(inputStream);
			}
		} catch (IOException e) {
			// maybe it does not exist
		}
		// deps
		String depProp = properties.getProperty(DEPENDENCIES_PROP);
		if (depProp != null) {
			// remove []
			depProp = depProp.trim();
			if (depProp.length() > 2) {
				String deps[] = depProp.substring(1, depProp.length() - 1).split(",");
				dependencies = Arrays.asList(deps);
			}
		}
		// js file
		String jsFile = properties.getProperty(GENERATED_JS_FILE_PROP);
		if (jsFile != null) {
			try {
				generatedJavascriptFile = new URI(jsFile);
			} catch (URISyntaxException e) {
				System.err.println("Could not load URI from " + jsFile);
			}
		}
	}

	private String getPropertiesFileName() {
		return className.replace('.', File.separatorChar) + ".stjs";
	}

	private File getStjsPropertiesFile() {
		File propFile = new File(targetFolder, getPropertiesFileName());
		propFile.getParentFile().mkdirs();
		return propFile;
	}

	public void store() {
		if (targetFolder == null) {
			throw new IllegalStateException("This properties file was open for read only");
		}
		FileOutputStream propertiesWriter = null;
		try {
			propertiesWriter = new FileOutputStream(getStjsPropertiesFile());
			properties.store(propertiesWriter, "Generated by STJS ");
		} catch (IOException e1) {
			throw new RuntimeException("Could not open properties file " + getStjsPropertiesFile() + ":" + e1, e1);
		} finally {
			try {
				if (propertiesWriter != null) {
					System.out.println("Wrote: " + getStjsPropertiesFile());
					propertiesWriter.close();
				}
			} catch (IOException e) {
				// silent
			}
		}
	}

	public void setDependencies(List<String> dependencies) {
		this.dependencies = dependencies;
		if (dependencies != null) {
			properties.put(DEPENDENCIES_PROP, dependencies.toString());
		} else {
			properties.remove(DEPENDENCIES_PROP);
		}
	}

	public void setGeneratedJavascriptFile(URI generatedJavascriptFile) {
		this.generatedJavascriptFile = generatedJavascriptFile;
		if (generatedJavascriptFile != null) {
			properties.put(GENERATED_JS_FILE_PROP, generatedJavascriptFile.toString());
		} else {
			properties.remove(GENERATED_JS_FILE_PROP);
		}

	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public List<URI> getJavascriptFiles() {
		if (generatedJavascriptFile == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(generatedJavascriptFile);
	}

	@Override
	public List<ClassWithJavascript> getDirectDependencies() {
		if (directDependencies == null) {
			directDependencies = new ArrayList<ClassWithJavascript>(dependencies.size());
			for (String className : dependencies) {
				directDependencies.add(dependencyResolver.resolve(className.trim()));
			}
		}
		return directDependencies;
	}

	@Override
	public String toString() {
		return "STJSClass [className=" + className + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + className.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		STJSClass other = (STJSClass) obj;

		return className.equals(other.className);
	}

}
