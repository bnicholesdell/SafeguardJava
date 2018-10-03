package io.jenkins.plugins.sample;

import com.oneidentity.safeguard.safeguardclient.ISafeguardA2AContext;
import com.oneidentity.safeguard.safeguardclient.Safeguard;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String safeguardAppliance;
    private final String certificatePath;
    private final char[] passPhrase;
    private final char[] apiKey;

    @DataBoundConstructor
    public HelloWorldBuilder(String safeguardAppliance, String certificatePath, String passPhrase, String apiKey) {
        this.safeguardAppliance = safeguardAppliance;
        this.certificatePath = certificatePath;
        this.passPhrase = passPhrase.toCharArray();
        this.apiKey = apiKey.toCharArray();
    }

    public String getSafeguardAppliance() {
        return safeguardAppliance;
    }

    public String getCertificatePath() {
        return certificatePath;
    }

    public String getPassPhrase() {
        return new String(passPhrase);
    }

    public String getApiKey() {
        return new String(apiKey);
    }


//    @DataBoundSetter
//    public void setUseFrench(boolean useFrench) {
//        this.useFrench = useFrench;
//    }

    public class AddToEnvironment implements EnvironmentContributingAction {

        private String key;

        private String value;

        public AddToEnvironment(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void buildEnvVars(AbstractBuild<?, ?> ab, EnvVars ev) {
            if (ev != null && key != null && value != null) {
                ev.put(key, value);
            }
        }
        
        @Override public String getIconFileName() {
            return null;
        }

        @Override public String getDisplayName() {
            return "SafeguardAddToEnvironment";
        }

        @Override public String getUrlName() {
            return null;
        }

    }    
    
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Using certificate, " + certificatePath);
        listener.getLogger().println("With API key, " + getApiKey());
        listener.getLogger().println("To retrieve password, *********");
        try {
            ISafeguardA2AContext a2aConnection = Safeguard.A2A.GetContext(getSafeguardAppliance(), getCertificatePath(), 
                    this.passPhrase, null, null);
            char[] password = a2aConnection.RetrievePassword(this.apiKey);
            a2aConnection.Dispose();
            
            run.addAction(new AddToEnvironment("SGPASSKEY", new String(password)));

            listener.getLogger().println(Messages.HelloWorldBuilder_DescriptorImpl_success_credentialRetrieval());
        } catch (Exception ex) {
            throw new InterruptedException(Messages.HelloWorldBuilder_DescriptorImpl_errors_credentialRetrieval());
        }
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckSafeguardAppliance(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingSafeguardAppliance());
            return FormValidation.ok();
        }
        
        public FormValidation doCheckCertificatePath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingCertificatePath());
            return FormValidation.ok();
        }

        public FormValidation doCheckPassPhrase(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingPassPhrase());
            return FormValidation.ok();
        }

        public FormValidation doCheckApiKey(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingApiKey());
            return FormValidation.ok();
        }

        public FormValidation doTestCredential(@QueryParameter("safeguardAppliance") final String sgAppliance, 
                @QueryParameter("certificatePath") final String certPath, 
                @QueryParameter("passPhrase") String pPhrase, @QueryParameter("apiKey") String aKey)
                throws IOException, ServletException {
            try {
                ISafeguardA2AContext a2aConnection = Safeguard.A2A.GetContext(sgAppliance, certPath, pPhrase.toCharArray(), null, null);
                a2aConnection.RetrievePassword(aKey.toCharArray());
                a2aConnection.Dispose();
                
                return FormValidation.ok(Messages.HelloWorldBuilder_DescriptorImpl_success_credentialRetrieval());
            } catch (Exception ex) {
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_credentialRetrieval() + ex.getMessage());
            }
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
        }

    }

}
