/*
 * Copyright (C) 2017 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.webjars;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.dependency.fromDependencies.UnpackDependenciesMojo;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.dependency.utils.markers.DefaultFileMarkerHandler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Unpack all webjar dependencies and strip the version.
 */
@Mojo(name = "unpack", requiresDependencyResolution = ResolutionScope.TEST,
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class UseWebjarsMojo extends UnpackDependenciesMojo {

    /**
     * If META-INF/resources/webjars should be omitted.
     */
    @Parameter(property = "webjar.flatten")
    private boolean flatten;

    @Override
    protected void doExecute() throws MojoExecutionException {
        checkConfig();
        adjustConfig();
        unpack();
    }

    private void checkConfig() throws MojoExecutionException {
        if (useRepositoryLayout) {
            throw new MojoExecutionException("useRepositoryLayout is not supported.");
        }
        if (useSubDirectoryPerArtifact && !stripVersion) {
            throw new MojoExecutionException("useSubDirectoryPerArtifact must be used together with stripVersion.");
        }
    }

    private void adjustConfig() {
        if (getIncludes().length() == 0) {
            setIncludes("META-INF/resources/webjars/**");
            getLog().info("Set includes to '" + getIncludes() + "'");
        }
        if (getOutputDirectory().getName().equals("dependency")) {
            setOutputDirectory(new File(getOutputDirectory().getParentFile(), "webjars"));
            getLog().info("Set outputDirectory to '" + getOutputDirectory() + "'");
        }
        final String groups = "org.webjars";
        if (includeGroupIds == null || !includeGroupIds.contains(groups)) {
            includeGroupIds = groups + "," + includeGroupIds;
            getLog().info("Set includeGroupIds to '" + includeGroupIds + "'");
        }
    }

    private void unpack() throws MojoExecutionException {
        DependencyStatusSets dss = getDependencySets(this.failOnMissingClassifierArtifact);

        for (Artifact artifact : dss.getResolvedDependencies()) {
            File destDir = DependencyUtil.getFormattedOutputDirectory(useSubDirectoryPerScope, useSubDirectoryPerType,
                    useSubDirectoryPerArtifact, useRepositoryLayout,
                    stripVersion, outputDirectory, artifact);
            unpack(artifact, destDir, getIncludes(), getExcludes(), getEncoding());
            removeVersion(artifact, destDir);
            DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler(artifact, this.markersDirectory);
            handler.setMarker();
        }

        for (Artifact artifact : dss.getSkippedDependencies()) {
            getLog().info(artifact.getId() + " already exists in destination.");
        }
    }

    private void removeVersion(Artifact artifact, File dir) throws MojoExecutionException {
        try {
            final File base = new File(dir, "META-INF/resources/webjars/" + artifact.getArtifactId());
            final File[] versions = base.listFiles();
            if (versions == null || versions.length == 0) {
                getLog().warn("directory " + base + " is empty.");
            } else if (versions.length > 1) {
                getLog().warn("directory " + base + " contains more than one version: " + Arrays.asList(versions));
            } else {
                move(versions[0], flatten ? new File(dir, artifact.getArtifactId()) : base);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Problem removing version in " + dir, e);
        }
    }

    private void move(File from, File to) throws IOException {
        getLog().debug("Moving " + from + " to " + to);
        final File[] roots = from.listFiles();
        if (roots != null) {
            for (final File root : roots) {
                if (root.isDirectory()) {
                    final File target = new File(to, root.getName());
                    getLog().debug("Moving directory" + root + " to " + target);
                    FileUtils.moveDirectory(root, target);
                } else {
                    getLog().debug("Moving file" + root + " to " + to);
                    FileUtils.moveFileToDirectory(root, to, true);
                }
            }
        }
    }
}
