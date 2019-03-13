package edu.mit.cci.pogs.runner;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Date;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import edu.mit.cci.pogs.model.dao.taskplugin.ScoringConfiguration;
import edu.mit.cci.pogs.model.dao.taskplugin.ScoringType;
import edu.mit.cci.pogs.model.dao.taskplugin.TaskPlugin;
import edu.mit.cci.pogs.model.jooq.tables.pojos.CompletedTask;
import edu.mit.cci.pogs.runner.wrappers.SessionWrapper;
import edu.mit.cci.pogs.runner.wrappers.TaskWrapper;
import edu.mit.cci.pogs.service.CompletedTaskAttributeService;
import edu.mit.cci.pogs.service.CompletedTaskService;
import edu.mit.cci.pogs.service.SubjectService;
import edu.mit.cci.pogs.service.TaskExecutionAttributeService;
import edu.mit.cci.pogs.service.TeamService;
import edu.mit.cci.pogs.utils.DateUtils;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ScoringRunner extends AbstractJavascriptRunner implements Runnable {

    @Autowired
    private CompletedTaskService completedTaskService;

    private TaskWrapper taskWrapper;

    private SessionWrapper session;

    private TaskPlugin taskPlugin;

    @Autowired
    private CompletedTaskAttributeService completedTaskAttributeService;

    @Autowired
    private TaskExecutionAttributeService taskExecutionAttributeService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private SubjectService subjectService;

    private static final Logger _log = LoggerFactory.getLogger(ScoringRunner.class);


    @Override
    public void run() {
        Long timeBeforeStarts = taskWrapper.getTaskEndTimestamp() - DateUtils.now();
        try {
            if (timeBeforeStarts > 0) {
                System.out.println(" Time until score for Task id: " + taskWrapper.getId() + " is done: " + timeBeforeStarts);
                Thread.sleep(timeBeforeStarts);
            }
            System.out.println(" Score is starting for Task id: " + taskWrapper.getId());
            for (CompletedTask ct : taskWrapper.getCompletedTasks()) {
                if (this.taskPlugin.isScriptType()) {
                    this.runScript(this.taskPlugin.getTaskScoreJsContent(), ct.getId());
                } else {
                  completedTaskService.scoreCompletedTask(ct,taskWrapper);
                }
            }

        } catch (InterruptedException ie) {
            _log.info("Stopping scoring script for task : " + taskWrapper.getId());
        }

    }

    @Override
    public void setupVariableBindings() {

        JSONArray teamMates = teamService.getTeamatesJSONObject(
                teamService.getTeamSubjects(null,session.getId(),
                        session.getCurrentRound().getId(), taskWrapper.getId()));

        this.getEngine().put("teammates", teamMates.toString());


        JSONArray taskAttr = taskExecutionAttributeService.
                listExecutionAttributesAsJsonArray(taskWrapper.getId());

        this.getEngine().put("taskConfigurationAttributes", taskAttr.toString());


        JSONArray completedTaskAttributes = completedTaskAttributeService
                .listCompletedTaskAttributesForCompletedTask(this.getExternalReferenceId());

        this.getEngine().put("completedTaskAttributes", completedTaskAttributes.toString());

    }

    @Override
    public void retrieveScriptVariables() {

        String subjectAttributesToAddJson = (String) this.getEngine()
                .getBindings(ScriptContext.ENGINE_SCOPE).get("subjectAttributesToAdd");
        subjectService.createOrUpdateSubjectsAttributes(subjectAttributesToAddJson);


        String attributesToAddJson = (String) this.getEngine().getBindings(
                ScriptContext.ENGINE_SCOPE).get("completedTaskAttributesToAdd");
        completedTaskAttributeService.createCompletedTaskAttributesFromJsonString(
                attributesToAddJson,this.getExternalReferenceId());
    }

    @Override
    public void handleScriptFailure(ScriptException se) {
        _log.error("Before work script execution error for : " + taskPlugin.getTaskPluginName() + " - " + se.getMessage());
    }

    public TaskWrapper getTaskWrapper() {
        return taskWrapper;
    }

    public void setTaskWrapper(TaskWrapper taskWrapper) {
        this.taskWrapper = taskWrapper;
    }

    public SessionWrapper getSession() {
        return session;
    }

    public void setSession(SessionWrapper session) {
        this.session = session;
    }

    public TaskPlugin getTaskPlugin() {
        return taskPlugin;
    }

    public void setTaskPlugin(TaskPlugin taskPlugin) {
        this.taskPlugin = taskPlugin;
    }
}
