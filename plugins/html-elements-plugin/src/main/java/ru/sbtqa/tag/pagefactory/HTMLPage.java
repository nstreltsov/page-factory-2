package ru.sbtqa.tag.pagefactory;

import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementDecorator;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementLocatorFactory;

/**
 * Inherit your html page objects from this class
 */
public abstract class HTMLPage extends WebPage {

    public HTMLPage() {
        super(new HtmlElementDecorator(new HtmlElementLocatorFactory(Environment.getDriverService().getDriver())));
    }
}
