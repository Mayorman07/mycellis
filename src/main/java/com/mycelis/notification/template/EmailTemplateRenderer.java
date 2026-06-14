package com.mycelis.notification.template;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

/**
 * Renders Thymeleaf email templates from src/main/resources/templates/email/.
 *
 * Templates use {@code th:text}, {@code th:href}, {@code th:if}, etc.
 * Variables passed in `model` are available as ${variable} inside the template.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    /**
     * @param templateName  filename without .html, prefixed with email/
     *                      e.g. "email/verification-email"
     * @param model         variables exposed inside the template
     * @return              the fully rendered HTML
     */
    public String render(String templateName, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        return templateEngine.process(templateName, context);
    }
}