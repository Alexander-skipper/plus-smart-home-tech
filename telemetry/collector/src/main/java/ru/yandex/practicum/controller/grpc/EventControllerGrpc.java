package ru.yandex.practicum.controller.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.handler.HubEventHandler;
import ru.yandex.practicum.handler.SensorEventHandler;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class EventControllerGrpc extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    private final Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;

    public EventControllerGrpc(Set<SensorEventHandler> sensorHandlers,
                               Set<HubEventHandler> hubHandlers) {
        this.sensorEventHandlers = sensorHandlers.stream()
                .collect(Collectors.toMap(
                        SensorEventHandler::getMessageType,
                        Function.identity()
                ));
        this.hubEventHandlers = hubHandlers.stream()
                .collect(Collectors.toMap(
                        HubEventHandler::getMessageType,
                        Function.identity()
                ));
    }

    @Override
    public StreamObserver<SensorEventProto> collectSensorEvent(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<SensorEventProto>() {
            @Override
            public void onNext(SensorEventProto request) {
                try {
                    log.info("Получено событие сенсора: hubId={}, sensorId={}, type={}",
                            request.getHubId(), request.getId(), request.getPayloadCase());

                    SensorEventHandler handler = sensorEventHandlers.get(request.getPayloadCase());
                    if (handler != null) {
                        handler.handle(request);
                    } else {
                        log.warn("Не найден обработчик для события сенсора: {}", request.getPayloadCase());
                    }
                } catch (Exception e) {
                    log.error("Ошибка обработки события сенсора", e);
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Ошибка в потоке событий сенсора", t);
                responseObserver.onError(new StatusRuntimeException(
                        Status.INTERNAL.withDescription(t.getMessage()).withCause(t)));
            }

            @Override
            public void onCompleted() {
                log.info("Завершен поток событий сенсора");
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<HubEventProto> collectHubEvent(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<HubEventProto>() {
            @Override
            public void onNext(HubEventProto request) {
                try {
                    log.info("Получено событие хаба: hubId={}, type={}",
                            request.getHubId(), request.getPayloadCase());

                    HubEventHandler handler = hubEventHandlers.get(request.getPayloadCase());
                    if (handler != null) {
                        handler.handle(request);
                    } else {
                        log.warn("Не найден обработчик для события хаба: {}", request.getPayloadCase());
                    }
                } catch (Exception e) {
                    log.error("Ошибка обработки события хаба", e);
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Ошибка в потоке событий хаба", t);
                responseObserver.onError(new StatusRuntimeException(
                        Status.INTERNAL.withDescription(t.getMessage()).withCause(t)));
            }

            @Override
            public void onCompleted() {
                log.info("Завершен поток событий хаба");
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }
}