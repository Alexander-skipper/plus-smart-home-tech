package ru.yandex.practicum.service.analyzer;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.model.ScenarioAction;
import ru.yandex.practicum.model.enums.ActionType;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
public class ActionExecutor {

    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public ActionExecutor(@GrpcClient("hub-router")
                          HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {
        this.hubRouterClient = hubRouterClient;
    }

    @PostConstruct
    public void checkConnection() {
        try {
            log.info("Проверка gRPC соединения с hub-router...");
            // Просто проверяем, что канал существует
            if (hubRouterClient != null && hubRouterClient.getChannel() != null) {
                log.info("gRPC клиент настроен, канал: {}", hubRouterClient.getChannel().authority());
            } else {
                log.error("gRPC клиент не инициализирован!");
            }
        } catch (Exception e) {
            log.error("ОШИБКА gRPC соединения: {}", e.getMessage(), e);
        }
    }

    public void executeActions(String hubId, String scenarioName, Set<ScenarioAction> actions) {
        log.info("Выполнение действий для сценария '{}', hubId={}, действий: {}",
                scenarioName, hubId, actions.size());

        actions.forEach(scenarioAction -> {
            try {
                DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                        .setSensorId(scenarioAction.getSensor().getId())
                        .setType(mapActionType(scenarioAction.getAction().getType()));

                if (scenarioAction.getAction().getValue() != null) {
                    actionBuilder.setValue(scenarioAction.getAction().getValue());
                }

                Instant now = Instant.now();
                DeviceActionRequest request = DeviceActionRequest.newBuilder()
                        .setHubId(hubId)
                        .setScenarioName(scenarioName)
                        .setAction(actionBuilder.build())
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(now.getEpochSecond())
                                .setNanos(now.getNano()))
                        .build();

                log.info("Отправка gRPC запроса: hubId={}, scenario={}, sensorId={}, action={}, value={}",
                        hubId, scenarioName, scenarioAction.getSensor().getId(),
                        scenarioAction.getAction().getType(),
                        scenarioAction.getAction().getValue());

                hubRouterClient.handleDeviceAction(request);

                log.info("Команда успешно отправлена: hubId={}, scenario={}, sensorId={}",
                        hubId, scenarioName, scenarioAction.getSensor().getId());

            } catch (StatusRuntimeException e) {
                log.error("Ошибка gRPC при отправке действия: статус={}, описание={}",
                        e.getStatus(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Ошибка выполнения действия", e);
            }
        });
    }

    private ActionTypeProto mapActionType(ActionType type) {
        return switch (type) {
            case ACTIVATE -> ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ActionTypeProto.DEACTIVATE;
            case INVERSE -> ActionTypeProto.INVERSE;
            case SET_VALUE -> ActionTypeProto.SET_VALUE;
        };
    }
}