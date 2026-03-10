package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.service.analyzer.SnapshotProcessor;
import ru.yandex.practicum.service.hub.HubEventProcessor;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {
    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args);

            final HubEventProcessor hubEventProcessor = context.getBean(HubEventProcessor.class);
            final SnapshotProcessor snapshotProcessor = context.getBean(SnapshotProcessor.class);

            Thread hubEventsThread = new Thread(hubEventProcessor);
            hubEventsThread.setName("HubEventHandlerThread");
            hubEventsThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Получен сигнал завершения JVM");
                hubEventProcessor.getConsumer().wakeup();
                snapshotProcessor.getConsumer().wakeup();
                try {
                    hubEventsThread.join(5000);
                } catch (InterruptedException e) {
                    log.error("Ошибка при ожидании завершения потока", e);
                }
            }));

            snapshotProcessor.start();

        } catch (Exception e) {
            log.error("КРИТИЧЕСКАЯ ОШИБКА ЗАПУСКА ANALYZER!", e);
            System.exit(1);
        }
    }
}