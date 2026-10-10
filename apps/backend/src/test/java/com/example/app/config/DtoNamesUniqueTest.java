package com.example.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/** The OpenAPI uses the simple class name as schema name: two DTOs with the same name overwrite each other. */
class DtoNamesUniqueTest {

    @Test
    void dtoSimpleNamesAreUniqueAcrossModules() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));
        Map<String, List<String>> bySimpleName = new TreeMap<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.example.app.modules")) {
            String className = definition.getBeanClassName();
            if (className != null && className.contains(".dto.")) {
                String simple = className.substring(className.lastIndexOf('.') + 1);
                bySimpleName.computeIfAbsent(simple, key -> new ArrayList<>()).add(className);
            }
        }
        assertThat(bySimpleName).isNotEmpty();
        Map<String, List<String>> duplicates = new TreeMap<>();
        bySimpleName.forEach((name, classes) -> {
            if (classes.size() > 1) {
                duplicates.put(name, classes);
            }
        });
        assertThat(duplicates).as("DTO simple names must be unique (prefix with the domain)").isEmpty();
    }
}
