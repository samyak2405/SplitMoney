package com.javaproject.splitewise.config;

import com.javaproject.splitewise.grpc.SettlementCallbackGrpcServiceImpl;
import com.splitmoney.grpc.payment.PaymentGrpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class GrpcConfig {

    @Value("${payment.grpc.address:localhost:9090}")
    private String paymentGrpcAddress;

    @Value("${grpc.server.port:9091}")
    private int grpcServerPort;

    private Server grpcServer;

    /** Channel → payment-service gRPC server (port 9090). */
    @Bean
    public ManagedChannel paymentServiceChannel() {
        String[] parts = paymentGrpcAddress.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;
        return NettyChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    /** Blocking stub for initiating payments via gRPC. */
    @Bean
    public PaymentGrpcServiceGrpc.PaymentGrpcServiceBlockingStub paymentGrpcStub(ManagedChannel paymentServiceChannel) {
        return PaymentGrpcServiceGrpc.newBlockingStub(paymentServiceChannel);
    }

    /** gRPC server — splitewise-backend listens for settlement callbacks from payment-service (port 9091). */
    @Bean(destroyMethod = "")
    public Server grpcServer(SettlementCallbackGrpcServiceImpl callbackService) throws IOException {
        grpcServer = NettyServerBuilder.forPort(grpcServerPort)
                .addService(callbackService)
                .build()
                .start();
        log.info("gRPC callback server started on port {}", grpcServerPort);
        return grpcServer;
    }

    @PreDestroy
    public void shutdown() {
        if (grpcServer != null) {
            try {
                grpcServer.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("gRPC callback server shut down");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
