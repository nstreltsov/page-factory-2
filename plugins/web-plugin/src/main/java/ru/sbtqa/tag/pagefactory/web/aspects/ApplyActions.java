package ru.sbtqa.tag.pagefactory.web.aspects;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import ru.sbtqa.tag.pagefactory.actions.PageActions;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.pagefactory.web.actions.WebPageActions;

@Aspect
public class ApplyActions {

    private static PageActions pageActions = new WebPageActions();

    @Before("preinitialization(ru.sbtqa.tag.pagefactory.WebPage.new(..))")
    public void apply() {
        Environment.setPageActions(pageActions);
    }
}
