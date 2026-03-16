package ru.yandex.practicum.service.analyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.model.enums.ActionType;
import ru.yandex.practicum.model.enums.ConditionOperation;
import ru.yandex.practicum.model.enums.ConditionType;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioAnalyzer {

    private final ScenarioRepository scenarioRepository;
    private final ActionExecutor actionExecutor;

    @Transactional(readOnly = true)
    public void analyze(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        log.debug("Анализ снапшота для хаба {}, датчиков: {}", hubId, sensorsState.size());

        List<Scenario> scenariosLite = scenarioRepository.findByHubIdWithDetails(hubId);

        if (scenariosLite.isEmpty()) {
            log.debug("Нет сценариев для хаба {}", hubId);
            return;
        }

        log.debug("Найдено сценариев для хаба {}: {}", hubId, scenariosLite.size());

        List<Long> scenarioIds = scenariosLite.stream()
                .map(Scenario::getId)
                .collect(Collectors.toList());

        List<Scenario> scenarios = scenarioRepository.findByIdsWithDetails(scenarioIds);

        for (Scenario scenario : scenarios) {
            log.debug("Проверка сценария '{}' для хаба {}", scenario.getName(), hubId);

            if (!hasAllSensors(scenario, sensorsState)) {
                log.debug("Не все датчики для сценария '{}' доступны", scenario.getName());
                continue;
            }

            boolean allConditionsMet = checkAllConditionsFast(scenario, sensorsState);

            if (allConditionsMet) {
                log.info("Условия сценария '{}' выполнены для хаба {}", scenario.getName(), hubId);

                if (shouldExecuteActions(scenario, sensorsState)) {
                    log.info("Выполнение сценария '{}' для хаба {}", scenario.getName(), hubId);
                    actionExecutor.executeActions(hubId, scenario.getName(), scenario.getActions());
                } else {
                    log.debug("Сценарий '{}' уже выполнен, действия не требуются", scenario.getName());
                }
            } else {
                log.debug("Условия сценария '{}' НЕ выполнены", scenario.getName());
            }
        }
    }

    private boolean shouldExecuteActions(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        for (ScenarioAction action : scenario.getActions()) {
            String targetSensorId = action.getSensor().getId();
            SensorStateAvro targetState = sensorsState.get(targetSensorId);
            ActionType actionType = action.getAction().getType();

            if (targetState == null) {
                log.debug("Состояние целевого датчика {} неизвестно, выполняем действие", targetSensorId);
                return true;
            }

            Object currentValue = extractValueForAction(targetState.getData(), actionType);
            boolean shouldExecute = shouldExecuteForActionType(currentValue, actionType, action.getAction().getValue());

            if (shouldExecute) {
                log.debug("Действие {} для датчика {} необходимо выполнить (текущее значение: {})",
                        actionType, targetSensorId, currentValue);
                return true;
            } else {
                log.debug("Действие {} для датчика {} НЕ требуется (текущее значение: {})",
                        actionType, targetSensorId, currentValue);
            }
        }
        return false;
    }

    private boolean shouldExecuteForActionType(Object currentValue, ActionType actionType, Integer targetValue) {
        if (currentValue == null) return true;

        switch (actionType) {
            case ACTIVATE:
                // Активировать, если устройство выключено
                return Boolean.FALSE.equals(currentValue);
            case DEACTIVATE:
                // Деактивировать, если устройство включено
                return Boolean.TRUE.equals(currentValue);
            case INVERSE:
                // Инвертировать всегда имеет смысл
                return true;
            case SET_VALUE:
                // Установить значение, если текущее отличается от целевого
                if (currentValue instanceof Integer) {
                    return !currentValue.equals(targetValue);
                }
                return true;
            default:
                return true;
        }
    }

    private Object extractValueForAction(Object data, ActionType actionType) {
        if (data instanceof SwitchSensorAvro switchSensor) {
            return switchSensor.getState();
        } else if (data instanceof ClimateSensorAvro climate) {
            return climate.getTemperatureC();
        } else if (data instanceof LightSensorAvro light) {
            return light.getLuminosity();
        } else if (data instanceof MotionSensorAvro motion) {
            return motion.getMotion();
        } else if (data instanceof TemperatureSensorAvro temperature) {
            return temperature.getTemperatureC();
        }
        return null;
    }

    private boolean hasAllSensors(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        for (ScenarioCondition condition : scenario.getConditions()) {
            if (!sensorsState.containsKey(condition.getSensor().getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean checkAllConditionsFast(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        for (ScenarioCondition sc : scenario.getConditions()) {
            if (!checkConditionFast(sc, sensorsState)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkConditionFast(ScenarioCondition scenarioCondition, Map<String, SensorStateAvro> sensorsState) {
        SensorStateAvro sensorState = sensorsState.get(scenarioCondition.getSensor().getId());

        if (sensorState == null) {
            return false;
        }

        Object sensorValue = extractValueFast(sensorState.getData(), scenarioCondition.getCondition().getType());

        if (sensorValue == null) {
            return false;
        }

        return compareFast(sensorValue,
                scenarioCondition.getCondition().getOperation(),
                scenarioCondition.getCondition().getValue());
    }

    private Object extractValueFast(Object data, ConditionType type) {
        if (data instanceof ClimateSensorAvro climate) {
            return switch (type) {
                case TEMPERATURE -> climate.getTemperatureC();
                case HUMIDITY -> climate.getHumidity();
                case CO2LEVEL -> climate.getCo2Level();
                default -> null;
            };
        } else if (data instanceof LightSensorAvro light) {
            return switch (type) {
                case LUMINOSITY -> light.getLuminosity();
                default -> null;
            };
        } else if (data instanceof MotionSensorAvro motion) {
            return switch (type) {
                case MOTION -> motion.getMotion();
                default -> null;
            };
        } else if (data instanceof SwitchSensorAvro switchSensor) {
            return switch (type) {
                case SWITCH -> switchSensor.getState();
                default -> null;
            };
        } else if (data instanceof TemperatureSensorAvro temperature) {
            return switch (type) {
                case TEMPERATURE -> temperature.getTemperatureC();
                default -> null;
            };
        }
        return null;
    }

    private boolean compareFast(Object sensorValue, ConditionOperation operation, Integer threshold) {
        if (sensorValue instanceof Boolean boolVal) {
            if (operation != ConditionOperation.EQUALS) {
                return false;
            }
            boolean expectedValue = threshold != null && threshold == 1;
            return boolVal == expectedValue;
        } else if (sensorValue instanceof Integer intVal) {
            if (threshold == null) {
                return false;
            }
            return switch (operation) {
                case EQUALS -> intVal.equals(threshold);
                case GREATER_THAN -> intVal > threshold;
                case LOWER_THAN -> intVal < threshold;
                default -> false;
            };
        }
        return false;
    }
}