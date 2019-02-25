package ru.sbtqa.tag.pagefactory.aspects;

import cucumber.api.Result;
import cucumber.api.event.Event;
import cucumber.api.event.TestCaseFinished;
import cucumber.api.event.TestStepFinished;
import gherkin.pickles.PickleStep;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import ru.sbtqa.tag.pagefactory.allure.CategoriesInjector;
import ru.sbtqa.tag.pagefactory.allure.Category;
import ru.sbtqa.tag.pagefactory.allure.ErrorHandler;
import ru.sbtqa.tag.pagefactory.optional.PickleStepCustom;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@Aspect
public class CriticalStepCheckAspect {
    private static final String STEP_FIELD_NAME = "step";
    private static final String NON_CRITICAL_CATEGORY_NAME = "Non-critical failures";
    private static final String NON_CRITICAL_CATEGORY_MESSAGE = "Some non-critical steps are failed";
    private final Category nonCriticalCategory = new Category(NON_CRITICAL_CATEGORY_NAME,
            NON_CRITICAL_CATEGORY_MESSAGE, null,
            Arrays.asList(Status.PASSED.value()));

    private ThreadLocal<List<String>> brokenCases = ThreadLocal.withInitial(ArrayList::new);
    private ThreadLocal<Map<String, Throwable>> brokenTests = ThreadLocal.withInitial(HashMap::new);
    private ThreadLocal<PickleStep> currentBroken = new ThreadLocal<>();

    @Pointcut("execution(* cucumber.runtime.StepDefinitionMatch.runStep(..))")
    public void runStep() {
    }

    @Pointcut("execution(* cucumber.runner.EventBus.send(..)) && args(event,..) && if()")
    public static boolean sendStepFinished(Event event) {
        return event instanceof TestStepFinished;
    }

    @Pointcut("execution(* cucumber.runner.EventBus.send(..)) && args(event,..) && if()")
    public static boolean sendCaseFinished(Event event) {
        return event instanceof TestCaseFinished;
    }

    @Around("runStep()")
    public void runStep(ProceedingJoinPoint joinPoint) throws Throwable {
        Object instance = joinPoint.getThis();
        if (instance != null && FieldUtils.getDeclaredField(instance.getClass(), STEP_FIELD_NAME, true) != null) {
            Field stepField = FieldUtils.getDeclaredField(instance.getClass(), STEP_FIELD_NAME, true);
            stepField.setAccessible(true);
            Object step = stepField.get(instance);

            if (!(step instanceof PickleStepCustom)
                    || ((PickleStepCustom) step).isCritical()) {
                joinPoint.proceed();
            } else {
                PickleStepCustom pickleStep = (PickleStepCustom) step;
                try {
                    joinPoint.proceed();
                } catch (Throwable e) {
                    AllureLifecycle lifecycle = Allure.getLifecycle();
                    Optional<String> currentTestCase = lifecycle.getCurrentTestCase();
                    if (!currentTestCase.isPresent()) {
                        throw e;
                    }
                    stepField.set(instance, pickleStep);
                    this.currentBroken.set(pickleStep.step);
                    this.brokenCases.get().add(currentTestCase.get());
                    this.brokenTests.get().put(pickleStep.getText(), e);

                    ErrorHandler.attachError(e.getMessage(), e);
                    ErrorHandler.attachScreenshot();

                    CategoriesInjector.inject(nonCriticalCategory);
                }
            }
        } else {
            joinPoint.proceed();
        }
    }

    @Around("sendCaseFinished(event)")
    public void sendCaseFinished(ProceedingJoinPoint joinPoint, Event event) throws Throwable {
        TestCaseFinished testCaseFinished = (TestCaseFinished) event;

        if (currentBroken.get() != null && testCaseFinished.result.isOk(true)) {
            final Result result = new Result(Result.Type.PASSED, ((TestCaseFinished) event).result.getDuration(),
                    new AutotestError(NON_CRITICAL_CATEGORY_MESSAGE));

            event = new TestCaseFinished(event.getTimeStamp(),
                    testCaseFinished.testCase, result);

            joinPoint.proceed(new Object[]{event});
        } else {
            joinPoint.proceed();
        }
    }

    @Around("sendStepFinished(event)")
    public void sendStepFinished(ProceedingJoinPoint joinPoint, Event event) throws Throwable {
        TestStepFinished testStepFinished = (TestStepFinished) event;
        if (testStepFinished.testStep.isHook() || !testStepFinished.testStep.getPickleStep().equals(currentBroken.get())) {
            joinPoint.proceed();
        } else {
            final Result result = new Result(Result.Type.AMBIGUOUS, ((TestStepFinished) event).result.getDuration(),
                    new AutotestError(format("Non-critical step '%s' failed", testStepFinished.testStep.getStepText()))
            );

            event = new TestStepFinished(event.getTimeStamp(),
                    ((TestStepFinished) event).testStep, result);

            joinPoint.proceed(new Object[]{event});
        }
    }
}