package com.splitmoney.ai.config;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Slf4j
@Configuration
public class CassandraConfig {

    private static final String CREATE_KEYSPACE =
            "CREATE KEYSPACE IF NOT EXISTS splitydb " +
            "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}";

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS splitydb.splity_conversations (" +
            "    group_id text, user_id text, message_id timeuuid, " +
            "    role text, content text, created_at timestamp, " +
            "    PRIMARY KEY ((group_id, user_id), message_id)) " +
            "WITH CLUSTERING ORDER BY (message_id DESC) " +
            "AND default_time_to_live = 7776000";

    @Bean
    public CqlSession cassandraSession(
            @Value("${spring.cassandra.contact-points:localhost}") String host,
            @Value("${spring.cassandra.port:9042}") int port,
            @Value("${spring.cassandra.local-datacenter:datacenter1}") String dc
    ) {
        InetSocketAddress address = new InetSocketAddress(host, port);
        log.info("Bootstrapping Cassandra schema on {}:{}", host, port);

        try (CqlSession bootstrap = CqlSession.builder()
                .addContactPoint(address)
                .withLocalDatacenter(dc)
                .build()) {
            bootstrap.execute(CREATE_KEYSPACE);
            bootstrap.execute(CREATE_TABLE);
        }

        log.info("Cassandra schema ready, connecting to keyspace splitydb");
        return CqlSession.builder()
                .addContactPoint(address)
                .withLocalDatacenter(dc)
                .withKeyspace("splitydb")
                .build();
    }
}
