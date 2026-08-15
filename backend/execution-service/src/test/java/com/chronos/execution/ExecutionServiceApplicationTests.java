package com.chronos.execution;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ExecutionServiceApplicationTests {

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.chronos.execution.kafka.KafkaExecutionDispatchProducer kafkaExecutionDispatchProducer;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.chronos.execution.kafka.KafkaExecutionRetryProducer kafkaExecutionRetryProducer;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.chronos.execution.kafka.KafkaExecutionDlqProducer kafkaExecutionDlqProducer;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.chronos.execution.client.JobServiceClient jobServiceClient;

    @Test
    void contextLoads() {
    }
}
