package uk.co.javahelp.maven.plugin.fitnesse.mojo;

import static org.codehaus.plexus.util.IOUtil.close;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugin.surefire.SurefireReportParameters;
import org.apache.maven.surefire.failsafe.model.FailsafeSummary;
import org.apache.maven.surefire.failsafe.model.io.xpp3.FailsafeSummaryXpp3Writer;
import org.apache.maven.surefire.suite.RunResult;

import uk.co.javahelp.maven.plugin.fitnesse.responders.run.DelegatingResultsListener;
import fitnesse.junit.JUnitXMLTestListener;
import fitnesse.junit.PrintTestListener;
import fitnesse.junit.TestHelper;
import fitnesse.responders.run.ResultsListener;
import fitnesse.responders.run.TestSummary;

/**
 * Goal that runs FitNesse tests using fitnesse.junit.TestHelper.
 * Intended to be bound to the 'integration-test' phase.
 *
 * @goal run-tests
 * @requiresDependencyResolution
 */
public class RunTestsMojo extends AbstractMojo implements SurefireReportParameters {

	@Override
    public void executeInternal() throws MojoExecutionException, MojoFailureException {
        final ResultsListener resultsListener = new DelegatingResultsListener(
                new PrintTestListener(), new JUnitXMLTestListener( this.resultsDir.getAbsolutePath()));
        final TestHelper helper = new TestHelper(this.workingDir, this.reportsDir.getAbsolutePath(), resultsListener);
        // Strange side-effect behaviour:
        // If debug=false, FitNesse falls into wiki mode
        helper.setDebugMode(true);

        try {
            // Creating a SymLink is easiest when FitNesse is running in 'wiki server' mode
    		if(this.createSymLink) {
    			final String portString= this.port.toString();
	            this.fitNesseHelper.launchFitNesseServer(portString, this.workingDir, this.root, this.logDir);
	            this.fitNesseHelper.createSymLink(this.suite, this.test, this.project.getBasedir(), this.testResourceDirectory, this.port);
        	    this.fitNesseHelper.shutdownFitNesseServer(portString);
                Thread.sleep(50L); // Give our SymLink instance a chance to shutdown again
			}

            final String[] pageNameAndType = this.fitNesseHelper.calcPageNameAndType(this.suite, this.test);
            final TestSummary summary = helper.run(pageNameAndType[0], pageNameAndType[1], this.suiteFilter, this.excludeSuiteFilter, this.port);
            getLog().info(summary.toString());
            final RunResult result = new RunResult(summary.right, summary.exceptions, summary.wrong, summary.ignores);
            SurefireHelper.reportExecution(this, result, getLog());
            final FailsafeSummary failsafeSummary = new FailsafeSummary();
            failsafeSummary.setResult(result.getForkedProcessCode());
            writeSummary(failsafeSummary);
        } catch (Exception e) {
            throw new MojoExecutionException("Exception running FitNesse", e);
        }
    }

    private void writeSummary(FailsafeSummary summary)
            throws MojoExecutionException {
        if (!summaryFile.getParentFile().isDirectory()) {
            summaryFile.getParentFile().mkdirs();
        }

        Writer writer = null;
        try {
            writer = new FileWriter(this.summaryFile);
            FailsafeSummaryXpp3Writer xpp3Writer = new FailsafeSummaryXpp3Writer();
            xpp3Writer.write(writer, summary);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            close(writer);
        }
    }

    // ------------------------------------------------------------------------
    // See http://maven.apache.org/plugins/maven-surefire-plugin/test-mojo.html
    // ------------------------------------------------------------------------

	@Override
    public boolean isSkipTests() {
        return false;
    }

	@Override
    public void setSkipTests(boolean skipTests) {}

	@Override
    public boolean isSkipExec() {
        return false;
    }

	@Override
    public void setSkipExec(boolean skipExec) {}

	@Override
    public boolean isSkip() {
        return false;
    }

	@Override
    public void setSkip(boolean skip) {}

	@Override
    public boolean isTestFailureIgnore() {
        return true;
    }

	@Override
    public void setTestFailureIgnore(boolean testFailureIgnore) {}

	@Override
    public File getBasedir() {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
    public void setBasedir(File basedir) {
        // TODO Auto-generated method stub

    }

	@Override
    public File getTestClassesDirectory() {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
    public void setTestClassesDirectory(File testClassesDirectory) {
        // TODO Auto-generated method stub

    }

	@Override
    public File getReportsDirectory() {
        return this.reportsDir;
    }

	@Override
    public void setReportsDirectory(File reportsDirectory) {
        this.reportsDir = reportsDirectory;
    }

	@Override
    public Boolean getFailIfNoTests() {
        return false;
    }

	@Override
    public void setFailIfNoTests(Boolean failIfNoTests) {}
}