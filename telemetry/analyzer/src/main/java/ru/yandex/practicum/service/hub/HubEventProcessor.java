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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        debugPrintHubSensors(hubId);
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
            boolean exists = scenarioRepository.existsByHubIdAndName(hubId, payload.getName());

            Scenario scenario;
            if (exists) {
                log.debug("Сценарий '{}' для хаба {} уже существует. Обновление...", payload.getName(), hubId);

                scenario = scenarioRepository.findByHubIdAndNameWithDetails(hubId, payload.getName())
                        .orElseThrow(() -> new RuntimeException("Сценарий не найден: " + payload.getName()));

                scenarioConditionRepository.deleteByScenarioId(scenario.getId());
                scenarioActionRepository.deleteByScenarioId(scenario.getId());

                scenario.getConditions().clear();
                scenario.getActions().clear();

                scenarioRepository.flush();
            } else {
                log.debug("Сценарий '{}' для хаба {} не найден. Создание нового...", payload.getName(), hubId);
                scenario = Scenario.builder()
                        .hubId(hubId)
                        .name(payload.getName())
                        .build();
                scenario = scenarioRepository.save(scenario);
            }

            Map<String, Sensor> sensorsMap = sensorRepository.findByHubId(hubId).stream()
                    .collect(Collectors.toMap(Sensor::getId, Function.identity()));

            log.debug("Найдено датчиков для хаба {}: {}", hubId, sensorsMap.size());

            Set<ScenarioCondition> conditions = new HashSet<>();
            for (ScenarioConditionAvro conditionAvro : payload.getConditions()) {
                Sensor sensor = sensorsMap.get(conditionAvro.getSensorId());
                if (sensor == null) {
                    throw new RuntimeException("Датчик не найден: " + conditionAvro.getSensorId() + " для хаба " + hubId);
                }

                Integer value = null;
                Object avroValue = conditionAvro.getValue();

                if (avroValue != null) {
                    switch (conditionAvro.getType()) {
                        case MOTION:
                        case SWITCH:
                            value = Boolean.TRUE.equals(avroValue) ? 1 : 0;
                            break;
                        case TEMPERATURE:
                        case HUMIDITY:
                        case CO2LEVEL:
                        case LUMINOSITY:
                            value = (Integer) avroValue;
                            break;
                        default:
                            log.warn("Неизвестный тип условия: {}, значение не сохраняется", conditionAvro.getType());
                    }
                }

                Condition condition = Condition.builder()
                        .type(ConditionType.valueOf(conditionAvro.getType().name()))
                        .operation(ConditionOperation.valueOf(conditionAvro.getOperation().name()))
                        .value(value)
                        .build();
                Condition savedCondition = conditionRepository.save(condition);

                ScenarioCondition scenarioCondition = ScenarioCondition.builder()
                        .scenario(scenario)
                        .sensor(sensor)
                        .condition(savedCondition)
                        .build();
                conditions.add(scenarioCondition);
            }
            scenarioConditionRepository.saveAll(conditions);
            scenario.getConditions().addAll(conditions);

            log.debug("Сохранено {} условий для сценария '{}'", conditions.size(), payload.getName());

            Set<ScenarioAction> actions = new HashSet<>();
            for (DeviceActionAvro actionAvro : payload.getActions()) {
                Sensor sensor = sensorsMap.get(actionAvro.getSensorId());
                if (sensor == null) {
                    throw new RuntimeException("Датчик не найден: " + actionAvro.getSensorId() + " для хаба " + hubId);
                }

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
                actions.add(scenarioAction);
            }
            scenarioActionRepository.saveAll(actions);
            scenario.getActions().addAll(actions);

            log.debug("Сохранено {} действий для сценария '{}'", actions.size(), payload.getName());

            scenario = scenarioRepository.save(scenario);

            log.info("Сценарий успешно сохранен: hubId={}, scenarioName={}, условий={}, действий={}",
                    hubId, payload.getName(), conditions.size(), actions.size());

            debugPrintScenarioData(hubId, payload.getName());

        } catch (Exception e) {
            log.error("Ошибка при сохранении сценария: hubId={}, scenarioName={}",
                    hubId, payload.getName(), e);
            throw e;
        }
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro payload) {
        Optional<Scenario> scenarioOpt = scenarioRepository.findByHubIdAndName(hubId, payload.getName());
        if (scenarioOpt.isPresent()) {
            Scenario scenario = scenarioOpt.get();

            for (ScenarioCondition sc : scenario.getConditions()) {
                conditionRepository.delete(sc.getCondition());
            }
            for (ScenarioAction sa : scenario.getActions()) {
                actionRepository.delete(sa.getAction());
            }

            scenarioRepository.delete(scenario);
            log.info("Сценарий удален: hubId={}, scenarioName={}", hubId, payload.getName());
        } else {
            log.warn("Попытка удалить несуществующий сценарий: hubId={}, scenarioName={}",
                    hubId, payload.getName());
        }
    }

    private void debugPrintHubSensors(String hubId) {
        List<Sensor> sensors = sensorRepository.findByHubId(hubId);
        log.info("=== ДАТЧИКИ ХАБА {} ({} шт.) ===", hubId, sensors.size());
        sensors.forEach(s -> log.info("  - ID: {}", s.getId()));
    }

    private void debugPrintScenarioData(String hubId, String scenarioName) {
        log.info("=== ОТЛАДКА: Данные сценария {} для хаба {} ===", scenarioName, hubId);

        scenarioRepository.findByHubIdAndNameWithDetails(hubId, scenarioName).ifPresentOrElse(
                scenario -> {
                    log.info("Сценарий ID: {}, имя: {}", scenario.getId(), scenario.getName());

                    log.info("Условия ({}):", scenario.getConditions().size());
                    scenario.getConditions().forEach(sc -> {
                        log.info("  - Датчик: {}, Condition ID: {}, тип: {}, операция: {}, значение: {}",
                                sc.getSensor().getId(),
                                sc.getCondition().getId(),
                                sc.getCondition().getType(),
                                sc.getCondition().getOperation(),
                                sc.getCondition().getValue());
                    });

                    log.info("Действия ({}):", scenario.getActions().size());
                    scenario.getActions().forEach(sa -> {
                        log.info("  - Датчик: {}, Action ID: {}, тип: {}, значение: {}",
                                sa.getSensor().getId(),
                                sa.getAction().getId(),
                                sa.getAction().getType(),
                                sa.getAction().getValue());
                    });
                },
                () -> log.warn("Сценарий {} для хаба {} НЕ НАЙДЕН в БД!", scenarioName, hubId)
        );
    }
}