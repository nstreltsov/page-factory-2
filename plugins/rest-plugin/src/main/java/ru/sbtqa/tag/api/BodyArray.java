package ru.sbtqa.tag.api;

import groovy.json.JsonBuilder;
import org.apache.commons.beanutils.ConvertUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Type for arrays used in JSON body
 *
 * @param <E> Array members type
 */
public class BodyArray<E> {
    private final ArrayList<E> array;

    public BodyArray(String values, Class<E> genericType) {
        this.array = new ArrayList<>();
        if (values.isEmpty()){
            return;
        }
        List<Object> collect = Arrays.stream(values.split("(?<!\\\\),\\s*"))
                .map(value -> ConvertUtils
                        .convert(value.replace("\"", "\\\"")
                                .replace("\\,", ","), genericType))
                .collect(Collectors.toList());
        this.array.addAll((Collection<? extends E>) collect);
    }

    /**
     * @return Generated typed array
     */
    public List<E> getArray() {
        return this.array;
    }

    /**
     * @return JSON array fragment
     */
    @Override
    public String toString() {
        if (array.isEmpty()){
            return "[]";
        }
        JsonBuilder builder = new JsonBuilder();
        builder.call(array);
        return builder.toString();
    }
}
