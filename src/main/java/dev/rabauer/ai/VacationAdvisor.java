package dev.rabauer.ai;

import io.quarkiverse.langchain4j.RegisterAiService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@RegisterAiService
public interface VacationAdvisor {

    @SystemMessage("""
            You are an HR assistant helping a manager quickly evaluate vacation requests.
            Reply in at most 3 short sentences: summarize the request, then give a clear
            recommendation (approve or deny) with a brief reason. If scheduling conflicts are
            listed, factor them into your recommendation instead of inventing other reasons.
            Do not use markdown.
            """)
    @UserMessage("""
            Employee: {employeeName}
            Vacation dates: {startDate} to {endDate}
            Reason given: {reason}
            Known scheduling conflicts: {conflicts}
            """)
    String review(String employeeName, String startDate, String endDate, String reason, String conflicts);
}
