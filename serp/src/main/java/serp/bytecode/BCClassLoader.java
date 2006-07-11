/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serp.bytecode;

/**
 * Class loader that will attempt to find requested classes in a given
 * {@link Project}.
 *
 * @author Abe White
 */
public class BCClassLoader extends ClassLoader {

    private Project _project = null;

    /**
     * Constructor. Supply the project to use when looking for classes.
     */
    public BCClassLoader(Project project) {
        _project = project;
    }

    /**
     * Constructor. Supply the project to use when looking for classes.
     *
     * @param parent the parent classoader
     */
    public BCClassLoader(Project project, ClassLoader loader) {
        super(loader);
        _project = project;
    }

    /**
     * Return this class loader's project.
     */
    public Project getProject() {
        return _project;
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        byte[] bytes;
        try {
            BCClass type;
            if (!_project.containsClass(name))
                type = createClass(name);
            else
                type = _project.loadClass(name);
            if (type == null)
                throw new ClassNotFoundException(name);

            bytes = type.toByteArray();
        } catch (RuntimeException re) {
            throw new ClassNotFoundException(re.toString());
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    /**
     * Override this method if unfound classes should be created on-the-fly.
     * Returns null by default.
     */
    protected BCClass createClass(String name) {
        return null;
    }
}
