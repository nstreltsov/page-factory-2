package ru.sbtqa.tag.api.annotation.applicators;

import java.lang.reflect.Field;
import ru.sbtqa.tag.api.EndpointEntry;
import ru.sbtqa.tag.api.annotation.Query;
import ru.sbtqa.tag.api.utils.PlaceholderUtils;

/**
 * Applicator for {@link Query} annotation
 */
public class QueryApplicator extends DefaultApplicator implements Applicator {

    public QueryApplicator(EndpointEntry entry, Field field) {
        super(entry, field);
    }

    @Override
    public void apply() {
        String path = endpoint.getPath();
        String name = field.getAnnotation(Query.class).name();
        String placeholder = PlaceholderUtils.createPlaceholder(name);
        Object fieldValue = get(field);

        if (path.contains(placeholder) && fieldValue != null) {
            String replacedPath = PlaceholderUtils.replacePlaceholder(path, field, name, fieldValue);
            endpoint.setPath(replacedPath);
            set(field, null);
        }
    }
}
