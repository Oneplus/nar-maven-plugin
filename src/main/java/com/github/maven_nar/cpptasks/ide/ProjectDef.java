/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.ide;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.TargetInfo;

/**
 * Requests the creation of an IDE project file. Experimental.
 *
 * Implementation status: msdev5, msdev6 and cbuilderx
 * generate reasonable project files for simple projects,
 * xcode and msdev7 and msdev71 capture source file lists and
 * a few settings.
 *
 * @author Curt Arnold
 */
public final class ProjectDef extends DataType {
  /**
   * Name of property that must be present or definition will be ignored. May
   * be null.
   */
  private String ifProp;

  /**
   * Name of property that must be absent or definition will be ignored. May
   * be null.
   */
  private String unlessProp;

  /**
   * Project file name.
   */
  private File outFile;

  /**
   * Project name.
   */
  private String name;

  /**
   * Fail on error.
   */
  private boolean failOnError = true;

  /**
   * Overwrite existing project file.
   */
  private boolean overwrite = true;

  /**
   * Project writer.
   */
  private ProjectWriter projectWriter;

  /**
   * Object directory.
   *
   */
  private File objDir;

  /**
   * List of dependency definitions.
   */
  private final List<DependencyDef> dependencies = new ArrayList<DependencyDef>();

  /**
   * List of comments.
   */
  private final List<CommentDef> comments = new ArrayList<CommentDef>();

  /**
   * Constructor.
   *
   */
  public ProjectDef() {
  }

  /**
   * Add comment for the generated project file.
   * 
   * @param comment
   *          comment, may not be null.
   */
  public void addComment(final CommentDef comment) {
    this.comments.add(comment);

  }

  /**
   * Add a dependency definition to the project.
   * 
   * @param dependency
   *          dependency.
   */
  public void addDependency(final DependencyDef dependency) {
    this.dependencies.add(dependency);

  }

  /**
   * Required by documentation generator.
   */
  public void execute() {
    throw new org.apache.tools.ant.BuildException("Not an actual task, but looks like one for documentation purposes");
  }

  /**
   * Executes the task. Compiles the given files.
   *
   * @param task
   *          cc task
   * @param sources
   *          source files (includes headers)
   * @param targets
   *          compilation targets
   * @param linkTarget
   *          link target
   */
  public void execute(final CCTask task, final List<File> sources, final Map<String, TargetInfo> targets,
      final TargetInfo linkTarget) {
    try {
      this.projectWriter.writeProject(this.outFile, task, this, sources, targets, linkTarget);
    } catch (final BuildException ex) {
      if (this.failOnError) {
        throw ex;
      } else {
        task.log(ex.toString());
      }
    } catch (final Exception ex) {
      if (this.failOnError) {
        throw new BuildException(ex);
      } else {
        task.log(ex.toString());
      }
    }
  }

  public List<CommentDef> getComments() {
    return new ArrayList<CommentDef>(this.comments);
  }

  public List<DependencyDef> getDependencies() {
    return new ArrayList<DependencyDef>(this.dependencies);
  }

  /**
   * Get name.
   * 
   * @return String name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the object files directory.
   * 
   * @return directory, may be null.
   */
  public File getObjdir() {
    return this.objDir;
  }

  /**
   * Sets the directory used for object files. If not specified,
   * the object files directory from cc task will be used.
   * 
   * @param oDir
   *          object file directory.
   */
  public void getObjdir(final File oDir) {
    this.objDir = oDir;
  }

  /**
   * Gets whether an existing project file should be overwritten,
   * default is true. If false and the project file exists,
   * the value of failonerror will determine if the task fails.
   *
   * @return value
   */
  public boolean getOverwrite() {
    return this.overwrite;
  }

  /**
   * Determine if this def should be used.
   *
   * Definition will be active if the "if" variable (if specified) is set and
   * the "unless" variable (if specified) is not set and that all reference
   * or extended definitions are active
   *
   * @return true if processor is active
   */
  public boolean isActive() {
    final Project project = getProject();
    if (!CUtil.isActive(project, this.ifProp, this.unlessProp)) {
      return false;
    }
    return true;
  }

  /**
   * Class name for a user-supplied project writer. Use the "type"
   * attribute to specify built-in project writer implementations.
   *
   * @param className
   *          full class name
   *
   */
  public void setClassname(final String className) {
    Object proc = null;
    try {
      final Class<?> implClass = ProjectDef.class.getClassLoader().loadClass(className);
      try {
        final Method getInstance = implClass.getMethod("getInstance", new Class[0]);
        proc = getInstance.invoke(null, new Object[0]);
      } catch (final Exception ex) {
        proc = implClass.newInstance();
      }
    } catch (final Exception ex) {
      throw new BuildException(ex);
    }
    this.projectWriter = (ProjectWriter) proc;
  }

  /**
   * Sets whether a failure to write the project file should cause the
   * task to fail. Default is true.
   *
   * @param value
   *          new value
   */
  public void setFailonerror(final boolean value) {
    this.failOnError = value;
  }

  /**
   * Sets the property name for the 'if' condition.
   *
   * The configuration will be ignored unless the property is defined.
   *
   * The value of the property is insignificant, but values that would imply
   * misinterpretation ("false", "no") will throw an exception when
   * evaluated.
   *
   * @param propName
   *          name of property
   */
  public void setIf(final String propName) {
    this.ifProp = propName;
  }

  /**
   * Set name.
   * 
   * @param value
   *          String name
   */
  public void setName(final String value) {
    this.name = value;
  }

  /**
   * Sets the name for the generated project file.
   *
   * @param outfile
   *          output file name
   */
  public void setOutfile(final File outfile) {
    //
    // if file name was empty, skip link step
    //
    if (outfile == null || outfile.toString().length() > 0) {
      this.outFile = outfile;
    }
  }

  /**
   * Sets whether an existing project file should be overwritten,
   * default is true. If false and the project file exists,
   * the value of failonerror will determine if the task fails.
   *
   * @param value
   *          new value
   */
  public void setOverwrite(final boolean value) {
    this.overwrite = value;
  }

  /**
   * Set project type.
   *
   *
   * <table width="100%" border="1">
   * <thead>Supported project formats </thead>
   * <tr>
   * <td>cbuilderx</td>
   * <td>Borland C++BuilderX</td>
   * </tr>
   * <tr>
   * <td>msvc5</td>
   * <td>Microsoft Visual C++ 97</td>
   * </tr>
   * <tr>
   * <td>msvc6</td>
   * <td>Microsoft Visual C++ 6</td>
   * </tr>
   * <tr>
   * <td>msvc7</td>
   * <td>Microsoft Visual C++.NET</td>
   * </tr>
   * <tr>
   * <td>msvc71</td>
   * <td>Microsoft Visual C++.NET 2003</td>
   * </tr>
   * <tr>
   * <td>msvc8</td>
   * <td>Microsoft Visual C++ 2005</td>
   * </tr>
   * <tr>
   * <td>msvc9</td>
   * <td>Microsoft Visual C++ 2008</td>
   * </tr>
   * <tr>
   * <td>xcode</td>
   * <td>Apple Xcode</td>
   * </tr>
   * </table>
   *
   * @param value
   *          new value
   */
  public void setType(final ProjectWriterEnum value) {
    this.projectWriter = value.getProjectWriter();
  }

  /**
   * Set the property name for the 'unless' condition.
   *
   * If named property is set, the configuration will be ignored.
   *
   * The value of the property is insignificant, but values that would imply
   * misinterpretation ("false", "no") of the behavior will throw an
   * exception when evaluated.
   *
   * @param propName
   *          name of property
   */
  public void setUnless(final String propName) {
    this.unlessProp = propName;
  }

}
