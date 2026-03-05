package ru.yandex.practicum.controller.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@Slf4j
@GrpcService
public class HubRouterController extends HubRouterControllerGrpc.HubRouterControllerImplBase {

    @Override
    public void handleDeviceAction(DeviceActionRequest request, StreamObserver<Empty> responseObserver) {
        log.info("===== ПОЛУЧЕНА КОМАНДА ДЛЯ ХАБА =====");
        log.info("Hub ID: {}", request.getHubId());
        log.info("Scenario: {}", request.getScenarioName());
        log.info("Action: sensorId={}, type={}, value={}",
                request.getAction().getSensorId(),
                request.getAction().getType(),
                request.getAction().hasValue() ? request.getAction().getValue() : "N/A");

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
