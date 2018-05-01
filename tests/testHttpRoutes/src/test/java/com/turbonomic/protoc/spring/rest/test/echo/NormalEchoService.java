package com.turbonomic.protoc.spring.rest.test.echo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.turbonomic.protoc.spring.rest.testHttpRoutes.EchoServiceGrpc.EchoServiceImplBase;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.DeleteEchoRequest;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.DeleteEchoResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.Echo;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.GetEchoRequest;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.GetEchoResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.MultiGetEchoRequest;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.MultiGetEchoResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.NewEchoRequest;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.NewEchoResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.UpdateEchoRequest;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.UpdateEchoResponse;

import io.grpc.stub.StreamObserver;


@NotThreadSafe
public class NormalEchoService extends EchoServiceImplBase {

    private Map<Long, Echo> echoMap = new HashMap<>();

    public void getEcho(GetEchoRequest request,
                        StreamObserver<GetEchoResponse> responseObserver) {
        final GetEchoResponse.Builder responseBuilder = GetEchoResponse.newBuilder();
        final Echo echo = echoMap.get(request.getId());
        if (echo != null) {
            responseBuilder.setEcho(echo);
        }
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    public void multiGetEcho(MultiGetEchoRequest request,
                             StreamObserver<MultiGetEchoResponse> responseObserver) {
        final MultiGetEchoResponse response = MultiGetEchoResponse.newBuilder()
                .addAllEcho(request.getIdList().stream()
                    .map(id -> echoMap.get(id))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void deleteEcho(DeleteEchoRequest request,
                           StreamObserver<DeleteEchoResponse> responseObserver) {
        final DeleteEchoResponse.Builder responseBuilder = DeleteEchoResponse.newBuilder();
        final Echo echo = echoMap.remove(request.getId());
        if (echo != null) {
            responseBuilder.setEcho(echo);
        }
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    public void newEcho(NewEchoRequest request,
                        StreamObserver<NewEchoResponse> responseObserver) {
        // Since it's for testing purposes, just overwrite.
        echoMap.put(request.getEcho().getId(), request.getEcho());
        responseObserver.onNext(NewEchoResponse.newBuilder()
                .setEcho(request.getEcho())
                .build());
        responseObserver.onCompleted();
    }

    public void updateEcho(UpdateEchoRequest request,
                           StreamObserver<UpdateEchoResponse> responseObserver) {
        echoMap.put(request.getId(), request.getNewEcho());
        responseObserver.onNext(UpdateEchoResponse.newBuilder()
                .setEcho(echoMap.get(request.getId()))
                .build());
        responseObserver.onCompleted();
    }

    @Nonnull
    public Map<Long, Echo> getEchoMap() {
        return echoMap;
    }
}
