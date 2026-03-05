package ru.yandex.practicum.service.analyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.model.enums.ConditionOperation;
import ru.yandex.practicum.model.enums.ConditionType;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioAnalyzer {

    private final ScenarioRepository scenarioRepository;
    private final ActionExecutor actionExecutor;

    public void analyze(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        log.debug("Получен снапшот для хаба {} с {} датчиками", hubId, sensorsState.size());

        List<Scenario> scenarios = scenarioRepository.findByHubIdWithDetails(hubId);

        if (scenarios.isEmpty()) {
            log.debug("Нет сценариев для хаба {}", hubId);
            return;
        }

        log.info("Найдено {} сценариев для хаба {}", scenarios.size(), hubId);

        scenarios.forEach(scenario -> {
            log.debug("Проверка сценария '{}' для хаба {}", scenario.getName(), hubId);

            boolean allConditionsMet = checkAllConditions(scenario, sensorsState);

            if (allConditionsMet) {
                log.info("ВСЕ УСЛОВИЯ ВЫПОЛНЕНЫ для сценария '{}' хаба {}", scenario.getName(), hubId);
                actionExecutor.executeActions(hubId, scenario.getName(), scenario.getActions());
            } else {
                log.debug("Условия не выполнены для сценария '{}' хаба {}", scenario.getName(), hubId);
            }
        });
    }

    private boolean checkAllConditions(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        return scenario.getConditions().stream()
                .allMatch(sc -> checkCondition(sc, sensorsState));
    }

    private boolean checkCondition(ScenarioCondition scenarioCondition, Map<String, SensorStateAvro> sensorsState) {
        SensorStateAvro sensorState = sensorsState.get(scenarioCondition.getSensor().getId());

        if (sensorState == null) {
            log.debug("Датчик {} не найден в снапшоте для сценария '{}'",
                    scenarioCondition.getSensor().getId(), scenarioCondition.getScenario().getName());
            return false;
        }

        Object sensorValue = extractValue(sensorState.getData(), scenarioCondition.getCondition().getType());

        if (sensorValue == null) {
            log.debug("Не удалось извлечь значение типа {} из датчика {} для сценария '{}'",
                    scenarioCondition.getCondition().getType(),
                    scenarioCondition.getSensor().getId(),
                    scenarioCondition.getScenario().getName());
            return false;
        }

        boolean result = compare(sensorValue,
                scenarioCondition.getCondition().getOperation(),
                scenarioCondition.getCondition().getValue());

        log.info("=== ПРОВЕРКА УСЛОВИЯ СЦЕНАРИЯ '{}' ===", scenarioCondition.getScenario().getName());
        log.info("Датчик ID: {}", scenarioCondition.getSensor().getId());
        log.info("Тип датчика: {}", scenarioCondition.getCondition().getType());
        log.info("Значение датчика: {} ({})", sensorValue, sensorValue.getClass().getSimpleName());
        log.info("Операция: {}", scenarioCondition.getCondition().getOperation());
        log.info("Пороговое значение: {}", scenarioCondition.getCondition().getValue());
        log.info("Результат: {}", result);
        log.info("======================================");

        return result;
    }

    private Object extractValue(Object data, ConditionType type) {
        if (data instanceof ClimateSensorAvro climate) {
            return switch (type) {
                case TEMPERATURE -> climate.getTemperatureC();
                case HUMIDITY -> climate.getHumidity();
                case CO2LEVEL -> climate.getCo2Level();
                default -> {
                    log.warn("Неподдерживаемый тип условия {} для ClimateSensor", type);
                    yield null;
                }
            };
        } else if (data instanceof LightSensorAvro light) {
            return switch (type) {
                case LUMINOSITY -> light.getLuminosity();
                default -> {
                    log.warn("Неподдерживаемый тип условия {} для LightSensor", type);
                    yield null;
                }
            };
        } else if (data instanceof MotionSensorAvro motion) {
            return switch (type) {
                case MOTION -> motion.getMotion();  // Возвращает Boolean
                default -> {
                    log.warn("Неподдерживаемый тип условия {} для MotionSensor", type);
                    yield null;
                }
            };
        } else if (data instanceof SwitchSensorAvro switchSensor) {
            return switch (type) {
                case SWITCH -> switchSensor.getState();  // Возвращает Boolean
                default -> {
                    log.warn("Неподдерживаемый тип условия {} для SwitchSensor", type);
                    yield null;
                }
            };
        } else if (data instanceof TemperatureSensorAvro temperature) {
            return switch (type) {
                case TEMPERATURE -> temperature.getTemperatureC();
                default -> {
                    log.warn("Неподдерживаемый тип условия {} для TemperatureSensor", type);
                    yield null;
                }
            };
        }
        log.warn("Неизвестный тип данных: {}", data.getClass());
        return null;
    }

    private boolean compare(Object sensorValue, ConditionOperation operation, Integer threshold) {
        if (sensorValue instanceof Boolean boolVal) {
            if (operation != ConditionOperation.EQUALS) {
                log.warn("Для boolean значения допустима только операция EQUALS, получена: {}", operation);
                return false;
            }

            boolean expectedValue = threshold != null && threshold == 1;
            log.debug("Сравнение boolean: актуальное={}, ожидаемое={} (threshold={})",
                    boolVal, expectedValue, threshold);
            return boolVal == expectedValue;

        } else if (sensorValue instanceof Integer intVal) {
            if (threshold == null) {
                log.warn("Для int значения threshold не может быть null");
                return false;
            }

            return switch (operation) {
                case EQUALS -> {
                    boolean eq = intVal.equals(threshold);
                    log.debug("Сравнение int: {} == {} -> {}", intVal, threshold, eq);
                    yield eq;
                }
                case GREATER_THAN -> {
                    boolean gt = intVal > threshold;
                    log.debug("Сравнение int: {} > {} -> {}", intVal, threshold, gt);
                    yield gt;
                }
                case LOWER_THAN -> {
                    boolean lt = intVal < threshold;
                    log.debug("Сравнение int: {} < {} -> {}", intVal, threshold, lt);
                    yield lt;
                }
                default -> {
                    log.warn("Неподдерживаемая операция {} для int", operation);
                    yield false;
                }
            };
        }

        log.warn("Неподдерживаемый тип значения для сравнения: {}", sensorValue.getClass());
        return false;
    }
}