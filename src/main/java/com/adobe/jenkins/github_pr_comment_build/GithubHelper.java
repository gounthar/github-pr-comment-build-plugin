package com.adobe.jenkins.github_pr_comment_build;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.model.Job;
import java.io.IOException;
import javax.annotation.Nonnull;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.github_branch_source.Connector;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * client utility methods
 */
public class GithubHelper {

    private final static Logger LOG = LoggerFactory.getLogger(GithubHelper.class);

    private GithubHelper() {
        // private
    }

    public static boolean isAuthorized(final Job<?, ?> job, final String author, String minimumPermissions) {
        try {
            GHRepository ghRepository = getGHRepository(job);
            GHPermissionType authorPermissions = ghRepository.getPermission(author);
            boolean authorized = false;
            switch (GHPermissionType.valueOf(minimumPermissions)) {
                case NONE:
                    authorized = true;
                    break;
                default: // break intentionally omitted
                case WRITE:
                    if(authorPermissions == GHPermissionType.WRITE || authorPermissions == GHPermissionType.ADMIN) {
                        authorized = true;
                    }
                    break;
                case ADMIN:
                    if(authorPermissions == GHPermissionType.ADMIN) {
                        authorized = true;
                    }
                    break;
            }

            LOG.debug("User {} authorized: {}", author, authorized);
            return authorized;
        } catch (final IOException | IllegalArgumentException e) {
            LOG.debug("Received an exception while trying to check if user {} is a collaborator for repo of job {}",
                    author, job.getFullName());
            LOG.debug("isAuthorized() - Exception", e);
            return false;
        }
    }

    private static GHRepository getGHRepository(@Nonnull final Job<?, ?> job) throws IOException {
        SCMSource scmSource = SCMSource.SourceByItem.findSource(job);
        if (scmSource instanceof GitHubSCMSource gitHubSource) {
            StandardCredentials credentials = Connector.lookupScanCredentials(
                    job, gitHubSource.getApiUri(), gitHubSource.getCredentialsId(), gitHubSource.getRepoOwner());
            GitHub github = Connector.connect(gitHubSource.getApiUri(), credentials);
            return github.getRepository(gitHubSource.getRepoOwner() + "/" + gitHubSource.getRepository());
        }
        throw new IllegalArgumentException("Job's SCM is not GitHub.");
    }
}
