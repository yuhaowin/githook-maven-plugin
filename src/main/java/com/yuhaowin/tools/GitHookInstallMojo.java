package com.yuhaowin.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@Mojo(name = "install", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public final class GitHookInstallMojo extends AbstractMojo {

    private static final String NEW_LINE = System.lineSeparator();
    private static final String SHEBANG = "#!/bin/sh" + NEW_LINE;
    private static final List<String> validHooks = Arrays.asList(
            "applypatch-msg",
            "pre-applypatch",
            "post-applypatch",
            "pre-commit",
            "prepare-commit-msg",
            "commit-msg",
            "post-commit",
            "pre-rebase",
            "post-checkout",
            "post-merge",
            "pre-receive",
            "update",
            "post-receive",
            "post-update",
            "pre-auto-gc",
            "post-rewrite",
            "pre-push");

    /**
     * The hooks that should be installed. For each map entry, the key must be a
     * valid Git hook name (see https://git-scm.com/docs/githooks#_hooks) and the
     * value is the script to install
     */
    @Parameter(name = "hooks")
    private Map<String, String> hooks;

    /**
     * external git hooks shell script
     */
    @Parameter(name = "resource-hooks")
    private Map<String, String> resourceHooks;

    /**
     * Used to validate the .git directory,should appear in the hierarchy of where
     * the project is being built
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private String buildDirectory;

    /**
     * Whether the plugin should be skipped
     */
    @Parameter(property = "githook.plugin.skip")
    private boolean skip = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping GitHook plugin execution");
            return;
        }

        Path hooksDir = getOrCreateHooksDirectory(buildDirectory);
        if (hooksDir == null) {
            getLog().info("No .git directory found, skipping plugin execution");
            throw new MojoExecutionException(String.format("Not a git repository, could not find a .git/hooks directory anywhere in the hierarchy of %s.", buildDirectory));
        }

        buildHooks(hooksDir);
        buildResourceHooks(hooksDir);
    }

    private void buildHooks(Path hooksDir) throws MojoExecutionException {
        if (hooks == null || hooks.isEmpty()) {
            getLog().info("hooks is empty,skip...");
            return;
        }
        for (Map.Entry<String, String> hook : hooks.entrySet()) {
            String hookName = hook.getKey();
            if (!validHooks.contains(hookName)) {
                getLog().error(String.format("`%s` hook is not a valid git-hook name", hookName));
                continue;
            }

            String hookScript = hook.getValue();
            String finalScript = (hookScript.startsWith("#!") ? "" : SHEBANG) + hookScript + NEW_LINE;
            try {
                getLog().info(String.format("Installing %s hook into %s", hookName, hooksDir.toAbsolutePath()));
                writeFile(hooksDir.resolve(hookName), finalScript.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new MojoExecutionException("Could not write hook with name: " + hookName, e);
            }
        }
    }

    private void buildResourceHooks(Path hooksDir) throws MojoExecutionException {
        if (resourceHooks == null || resourceHooks.isEmpty()) {
            getLog().info("resource-hooks is empty,skip...");
            return;
        }
        for (Map.Entry<String, String> hook : resourceHooks.entrySet()) {
            String hookName = hook.getKey();
            if (!validHooks.contains(hookName)) {
                getLog().error(String.format("`%s` hook is not a valid git-hook name", hookName));
                continue;
            }

            Path local = Paths.get("");
            Path hookFilePath = Paths.get(hook.getValue());

            if (!hookFilePath.toAbsolutePath().startsWith(local.toAbsolutePath())) {
                throw new MojoExecutionException("only file inside the project can be used to generate git hooks");
            }
            try {
                getLog().info("Installing " + hookName + " from " + hookFilePath);
                String finalScript = Files.lines(hookFilePath).collect(Collectors.joining("\n"));
                writeFile(hooksDir.resolve(hookName), finalScript.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new MojoExecutionException("could not access hook resource : " + hookFilePath, e);
            }
        }
    }

    private synchronized void writeFile(Path path, byte[] bytes) throws IOException {
        File created = Files.write(path, bytes, CREATE, TRUNCATE_EXISTING).toFile();
        boolean success = created.setExecutable(true, true)
                && created.setReadable(true, true)
                && created.setWritable(true, true);
        if (!success) {
            throw new IllegalStateException(String.format("Could not set permissions on created file %s", created.getAbsolutePath()));
        }
    }

    private Path getOrCreateHooksDirectory(String base) {
        getLog().debug(String.format("Searching for .git directory starting at %s", base));
        File gitMetadataDir = new FileRepositoryBuilder().findGitDir(new File(base)).getGitDir();

        if (gitMetadataDir == null) {
            return null;
        }

        Path hooksDir = gitMetadataDir.toPath().resolve("hooks");
        if (!hooksDir.toFile().exists()) {
            getLog().info(String.format("Creating missing hooks directory at %s", hooksDir.toAbsolutePath()));
            try {
                Files.createDirectories(hooksDir);
            } catch (IOException e) {
                getLog().error(e);
                return null;
            }
        }

        return hooksDir;
    }
}
