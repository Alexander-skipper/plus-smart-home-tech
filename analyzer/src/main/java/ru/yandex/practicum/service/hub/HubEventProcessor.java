package ru.yandex.practicum.service.hub;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.model.enums.*;
import ru.yandex.practicum.repository.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    @Getter
    private final KafkaConsumer<String, HubEventAvro> consumer;

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Value("${kafka.topics.hubs-in}")
    private String hubsInTopic;

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of(hubsInTopic));
            log.info("HubEventProcessor подписан на топик {}", hubsInTopic);

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    handleHubEvent(record.value());
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }

        } catch (WakeupException e) {
            log.info("HubEventProcessor получил сигнал завершения");
        } catch (Exception e) {
            log.error("Ошибка в HubEventProcessor", e);
        } finally {
            consumer.close();
            log.info("HubEventProcessor закрыт");
        }
    }

    @Transactional
    private void handleHubEvent(HubEventAvro event) {
        log.info("Обработка события хаба: hubId={}, payloadType={}",
                event.getHubId(), event.getPayload().getClass().getSimpleName());

        try {
            if (event.getPayload() instanceof DeviceAddedEventAvro) {
                handleDeviceAdded(event.getHubId(), (DeviceAddedEventAvro) event.getPayload());
            } else if (event.getPayload() instanceof DeviceRemovedEventAvro) {
                handleDeviceRemoved(event.getHubId(), (DeviceRemovedEventAvro) event.getPayload());
            } else if (event.getPayload() instanceof ScenarioAddedEventAvro) {
                handleScenarioAdded(event.getHubId(), (ScenarioAddedEventAvro) event.getPayload());
            } else if (event.getPayload() instanceof ScenarioRemovedEventAvro) {
                handleScenarioRemoved(event.getHubId(), (ScenarioRemovedEventAvro) event.getPayload());
            } else {
                log.warn("Неизвестный тип payload: {}", event.getPayload().getClass());
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба: hubId={}", event.getHubId(), e);
            throw e;
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro payload) {
        Sensor sensor = Sensor.builder()
                .id(payload.getId())
                .hubId(hubId)
                .build();
        sensorRepository.save(sensor);
        log.info("Датчик добавлен: hubId={}, sensorId={}, type={}",
                hubId, payload.getId(), payload.getType());
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro payload) {
        Optional<Sensor> sensorOpt = sensorRepository.findByIdAndHubId(payload.getId(), hubId);
        if (sensorOpt.isPresent()) {
            sensorRepository.delete(sensorOpt.get());
            log.info("Датчик удален: hubId={}, sensorId={}", hubId, payload.getId());
        } else {
            log.warn("Попытка удалить несуществующий датчик: hubId={}, sensorId={}",
                    hubId, payload.getId());
        }
    }

    @Transactional
    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro payload) {
        log.info("Добавление сценария: hubId={}, scenarioName={}", hubId, payload.getName());

        try {
            Optional<Scenario> existingScenarioOpt = scenarioRepository.findByHubIdAndNameWithDetails(hubId, payload.getName());

            Scenario scenario;
            if (existingScenarioOpt.isPresent()) {
                scenario = existingScenarioOpt.get();
                log.debug("Найден существующий сценарий '{}' для хаба {}. Обновление...", payload.getName(), hubId);
                scenario.getConditions().clear();
                scenario.getActions().clear();
            } else {
                log.debug("Сценарий '{}' для хаба {} не найден. Создание нового...", payload.getName(), hubId);
                scenario = Scenario.builder()
                        .hubId(hubId)
                        .name(payload.getName())
                        .build();
                scenario = scenarioRepository.save(scenario);
            }

            for (ScenarioConditionAvro conditionAvro : payload.getConditions()) {
                Sensor sensor = sensorRepository.findByIdAndHubId(conditionAvro.getSensorId(), hubId)
                        .orElseThrow(() -> new RuntimeException("Датчик не найден: " + conditionAvro.getSensorId()));

                Condition condition = Condition.builder()
                        .type(ConditionType.valueOf(conditionAvro.getType().name()))
                        .operation(ConditionOperation.valueOf(conditionAvro.getOperation().name()))
                        .value(conditionAvro.getValue() instanceof Integer ? (Integer) conditionAvro.getValue() : null)
                        .build();
                Condition savedCondition = conditionRepository.save(condition);

                ScenarioCondition scenarioCondition = ScenarioCondition.builder()
                        .scenario(scenario)
                        .sensor(sensor)
                        .condition(savedCondition)
                        .build();
                scenarioConditionRepository.save(scenarioCondition);
                scenario.getConditions().add(scenarioCondition);
            }

            for (DeviceActionAvro actionAvro : payload.getActions()) {
                Sensor sensor = sensorRepository.findByIdAndHubId(actionAvro.getSensorId(), hubId)
                        .orElseThrow(() -> new RuntimeException("Датчик не найден: " + actionAvro.getSensorId()));

                Action action = Action.builder()
                        .type(ActionType.valueOf(actionAvro.getType().name()))
                        .value(actionAvro.getValue())
                        .build();
                Action savedAction = actionRepository.save(action);

                ScenarioAction scenarioAction = ScenarioAction.builder()
                        .scenario(scenario)
                        .sensor(sensor)
                        .action(savedAction)
                        .build();
                scenarioActionRepository.save(scenarioAction);
                scenario.getActions().add(scenarioAction);
            }

            if (existingScenarioOpt.isEmpty()) {
                scenarioRepository.save(scenario);
            }

            log.info("Сценарий успешно сохранен: hubId={}, scenarioName={}", hubId, payload.getName());

        } catch (Exception e) {
            log.error("Ошибка при сохранении сценария: hubId={}, scenarioName={}",
                    hubId, payload.getName(), e);
            throw e;
        }
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro payload) {
        Optional<Scenario> scenarioOpt = scenarioRepository.findByHubIdAndName(hubId, payload.getName());
        if (scenarioOpt.isPresent()) {
            scenarioRepository.delete(scenarioOpt.get());
            log.info("Сценарий удален: hubId={}, scenarioName={}", hubId, payload.getName());
        } else {
            log.warn("Попытка удалить несуществующий сценарий: hubId={}, scenarioName={}",
                    hubId, payload.getName());
        }
    }
}