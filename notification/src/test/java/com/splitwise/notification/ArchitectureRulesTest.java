package com.splitwise.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.splitwise.notification.web.InternalEmailTemplateController;
import com.splitwise.notification.web.InternalNotificationController;
import com.splitwise.notification.web.UserNotificationController;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    @Test
    void webLayerShouldNotInjectPersistenceRepositories() {
        List<Class<?>> controllers = List.of(
                InternalNotificationController.class,
                UserNotificationController.class,
                InternalEmailTemplateController.class
        );
        for (Class<?> controllerClass : controllers) {
            for (Field field : controllerClass.getDeclaredFields()) {
                assertThat(field.getType().getPackageName())
                        .as("Controller %s should not depend on persistence package", controllerClass.getSimpleName())
                        .doesNotContain(".persistence.");
            }
        }
    }
}
