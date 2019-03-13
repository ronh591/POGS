package edu.mit.cci.pogs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.cci.pogs.model.dao.completedtaskscore.CompletedTaskScoreDao;
import edu.mit.cci.pogs.model.jooq.tables.pojos.CompletedTask;
import edu.mit.cci.pogs.model.jooq.tables.pojos.CompletedTaskScore;
import edu.mit.cci.pogs.runner.wrappers.TaskScoreWrapper;
import edu.mit.cci.pogs.runner.wrappers.TaskWrapper;
import edu.mit.cci.pogs.runner.wrappers.TeamWrapper;

@Service
public class TaskScoreService {

    @Autowired
    private CompletedTaskScoreDao completedTaskScoreDao;


    public List<TaskScoreWrapper> getTaskScoreWrappers(List<TaskWrapper> taskWrapperList, List<TeamWrapper> teamWrappers, Long subjectId) {
        List<TaskScoreWrapper> taskScoreWrappers = new ArrayList<>();

        for (
                TaskWrapper tw : taskWrapperList) {

            if (tw.getShouldScore()) {
                //get all scores in the team's order
                TaskScoreWrapper tsw = new TaskScoreWrapper();
                tsw.setTaskWrapper(tw);
                tsw.setTeamScore(new ArrayList<>());

                Map<Long, Double> teamScore = new HashMap<>();
                for (TeamWrapper tew : teamWrappers) {
                    teamScore.put(tew.getTeam().getId(), 0d);
                }

                for (CompletedTask ct : tw.getCompletedTasks()) {
                    CompletedTaskScore cts = completedTaskScoreDao
                            .getByCompletedTaskId(ct.getId());
                    if (cts != null) {
                        if (ct.getSubjectId() == null) {
                            teamScore.put(ct.getTeamId(), cts.getTotalScore());
                        } else {
                            if (ct.getSubjectId().equals(subjectId)) {
                                teamScore.put(ct.getTeamId(), cts.getTotalScore());
                            }
                        }
                    }
                }
                for (TeamWrapper tew : teamWrappers) {
                    tsw.getTeamScore().add(teamScore.get(tew.getTeam().getId()));
                }

                taskScoreWrappers.add(tsw);

            }
        }
        return taskScoreWrappers;
    }
}
