/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.maven;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.xtext.util.Strings.concat;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.xtend.core.compiler.batch.XtendBatchCompiler;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * @author Michael Clay - Initial contribution and API
 */
public abstract class AbstractXtendCompilerMojo extends AbstractXtendMojo {
	protected static final Predicate<String> FILE_EXISTS = new Predicate<String>() {

		public boolean apply(String filePath) {
			return new File(filePath).exists();
		}
	};

	@Inject
	protected Provider<XtendBatchCompiler> xtendBatchCompilerProvider;

	/**
	 * Xtend-File encoding argument for the compiler.
	 * 
	 * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
	 */
	protected String encoding;

	/**
	 * Set this to false to suppress the creation of *._trace files.
	 * 
	 * @parameter default-value="true" expression="${writeTraceFiles}"
	 */
	protected boolean writeTraceFiles;

	protected XtendBatchCompiler createXtendBatchCompiler() {
		Injector injector = new XtendMavenStandaloneSetup().createInjectorAndDoEMFRegistration();
		return injector.getInstance(XtendBatchCompiler.class);
	}

	protected void compile(XtendBatchCompiler xtend2BatchCompiler, String classPath, List<String> sourceDirectories,
			String outputPath) throws MojoExecutionException {
		xtend2BatchCompiler.setResourceSetProvider(new MavenProjectResourceSetProvider(project));
		Iterable<String> filtered = filter(sourceDirectories, FILE_EXISTS);
		if (Iterables.isEmpty(filtered)) {
			getLog().info(
					"skip compiling sources because the configured directory '" + Iterables.toString(sourceDirectories)
							+ "' does not exists.");
			return;
		}
		getLog().debug("Set temp directory: " + getTempDirectory());
		xtend2BatchCompiler.setTempDirectory(getTempDirectory());
		getLog().debug("Set DeleteTempDirectory: " + false);
		xtend2BatchCompiler.setDeleteTempDirectory(false);
		getLog().debug("Set classpath: " + classPath);
		xtend2BatchCompiler.setClassPath(classPath);
		getLog().debug("Set source path: " + concat(File.pathSeparator, newArrayList(filtered)));
		xtend2BatchCompiler.setSourcePath(concat(File.pathSeparator, newArrayList(filtered)));
		getLog().debug("Set output path: " + outputPath);
		xtend2BatchCompiler.setOutputPath(outputPath);
		getLog().debug("Set encoding: " + encoding);
		xtend2BatchCompiler.setFileEncoding(encoding);
		getLog().debug("Set writeTraceFiles: " + writeTraceFiles);
		xtend2BatchCompiler.setWriteTraceFiles(writeTraceFiles);
		if (!xtend2BatchCompiler.compile()) {
			throw new MojoExecutionException("Error compiling xtend sources in '"
					+ concat(File.pathSeparator, newArrayList(filtered)) + "'.");
		}
	}

	protected abstract String getTempDirectory();

	protected void addDependencies(Set<String> classPath, List<Artifact> dependencies) {
		for (Artifact artifact : dependencies) {
			classPath.add(artifact.getFile().getAbsolutePath());
		}
	}
}