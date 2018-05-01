package com.turbonomic.protoc.spring.rest.test.echo;

import com.turbonomic.protoc.spring.rest.testServices.Echo;
import com.turbonomic.protoc.spring.rest.testServices.Echo.EchoResponse;
import com.turbonomic.protoc.spring.rest.testServices.EchoServiceGrpc.EchoServiceImplBase;

import io.grpc.stub.StreamObserver;

public class NormalEchoService extends EchoServiceImplBase {
    @Override
    public void echo(Echo.EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
        responseObserver.onNext(Echo.EchoResponse.newBuilder().setEcho(request.getEchoThis()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void serverStreamEcho(Echo.EchoRequest request, StreamObserver<Echo.EchoResponse> responseObserver) {
        responseObserver.onNext(Echo.EchoResponse.newBuilder().setEcho(request.getEchoThis()).build());
        responseObserver.onNext(Echo.EchoResponse.newBuilder().setEcho(request.getEchoThis()).build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Echo.EchoRequest> clientStreamEcho(StreamObserver<Echo.EchoResponse> responseObserver) {
        return new StreamObserver<Echo.EchoRequest>() {
            private Echo.EchoRequest lastRequest = null;

            @Override
            public void onNext(Echo.EchoRequest value) {
                lastRequest = value;
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                if (lastRequest != null) {
                    responseObserver.onNext(Echo.EchoResponse.newBuilder()
                            .setEcho(lastRequest.getEchoThis())
                            .build());
                }
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Echo.EchoRequest> biStreamEcho(StreamObserver<Echo.EchoResponse> responseObserver) {
        return new StreamObserver<Echo.EchoRequest>() {
            @Override
            public void onNext(Echo.EchoRequest value) {
                responseObserver.onNext(Echo.EchoResponse.newBuilder()
                        .setEcho(value.getEchoThis())
                        .build());
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
