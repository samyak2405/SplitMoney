package com.splitmoney.application.config;

import com.splitmoney.application.grpc.PaymentGrpcServiceImpl;
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

    @Value("${grpc.server.port:9090}")
    private int grpcServerPort;

    @Value("${grpc.client.settlement-callback.address:localhost:9091}")
    private String settlementCallbackAddress;

    private Server grpcServer;

    /** gRPC server — payment-service listens for calls from splitwise-backend. */
    @Bean(destroyMethod = "")
    public Server grpcServer(PaymentGrpcServiceImpl paymentGrpcService) throws IOException {
        grpcServer = NettyServerBuilder.forPort(grpcServerPort)
                .addService(paymentGrpcService)
                .build()
                .start();
        log.info("gRPC server started on port {}", grpcServerPort);
        return grpcServer;
    }

    /** Channel to splitwise-backend gRPC server (for settlement callbacks). */
    @Bean
    public ManagedChannel settlementCallbackChannel() {
        String[] parts = settlementCallbackAddress.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9091;
        return NettyChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @PreDestroy
    public void shutdown() {
        if (grpcServer != null) {
            try {
                grpcServer.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("gRPC server shut down");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
