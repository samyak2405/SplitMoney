package com.splitmoney.chat.config;

import com.datastax.oss.driver.api.core.CqlSession;
import java.net.InetSocketAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CassandraConfig {

    @Value("${spring.cassandra.contact-points:localhost}")
    private String host;

    @Value("${spring.cassandra.port:9042}")
    private int port;

    @Value("${spring.cassandra.keyspace-name:chatdb}")
    private String keyspaceName;

    @Value("${spring.cassandra.local-datacenter:datacenter1}")
    private String localDc;

    /**
     * Provides the primary CqlSession.
     * Spring Boot's CassandraAutoConfiguration backs off (@ConditionalOnMissingBean)
     * when this bean is present, so we own the full lifecycle here.
     *
     * Steps:
     *  1. Open a system-level session (no keyspace) to create the keyspace and table.
     *  2. Return a session scoped to the keyspace for the application to use.
     */
    @Bean
    @Primary
    public CqlSession cqlSession() {
        InetSocketAddress endpoint = new InetSocketAddress(host, port);

        try (CqlSession init = CqlSession.builder()
                .addContactPoint(endpoint)
                .withLocalDatacenter(localDc)
                .build()) {

            init.execute(
                "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName +
                " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"
            );

            init.execute(
                "CREATE TABLE IF NOT EXISTS " + keyspaceName + ".messages_by_group (" +
                "  group_id     text," +
                "  bucket       text," +
                "  message_id   timeuuid," +
                "  sender_id    text," +
                "  sender_email text," +
                "  content      text," +
                "  msg_type     text," +
                "  created_at   timestamp," +
                "  PRIMARY KEY ((group_id, bucket), message_id)" +
                ") WITH CLUSTERING ORDER BY (message_id DESC)" +
                "   AND default_time_to_live = 7776000"
            );
        }

        return CqlSession.builder()
                .addContactPoint(endpoint)
                .withLocalDatacenter(localDc)
                .withKeyspace(keyspaceName)
                .build();
    }
}
