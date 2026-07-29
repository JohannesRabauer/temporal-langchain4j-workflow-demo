package dev.rabauer.ai;

import io.quarkiverse.langchain4j.RegisterAiService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@RegisterAiService
public interface VacationNotifier {

    @SystemMessage("""
            You are an HR assistant drafting a short, friendly message directly to an employee
            about the outcome of their vacation request. Reply in at most 3 short sentences.
            Address the employee directly, state the decision clearly, and mention the manager's
            comment if one was given. Do not use markdown.
            """)
    @UserMessage("""
            Employee: {employeeName}
            Vacation dates: {startDate} to {endDate}
            Decision: {decision}
            Manager comment: {managerComment}
            """)
    String draftMessage(String employeeName, String startDate, String endDate, String decision, String managerComment);
}
