/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.springboot;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.springboot.kafka.MessageWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.Assert;

@SpringBootTest(classes = KafkaSampleTest.Configuration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EmbeddedKafka(
    partitions = 1,
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
    controlledShutdown = true)
@DirtiesContext
public class KafkaSampleTest {

  @Autowired ConfigurableApplicationContext applicationContext;

  @Autowired TestWorkflowEnvironment testWorkflowEnvironment;

  @Autowired WorkflowClient workflowClient;

  @Autowired KafkaConsumerTestHelper consumer;

  @BeforeEach
  void setUp() {
    applicationContext.start();
  }

  @Test
  public void testKafkaWorflow() throws Exception {
    MessageWorkflow workflow =
        workflowClient.newWorkflowStub(
            MessageWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("KafkaSampleTaskQueue")
                .setWorkflowId("NewMessageWorkflow")
                .build());

    WorkflowClient.start(workflow::start);
    workflow.update("This is a test message");
    WorkflowStub.fromTyped(workflow).getResult(Void.class);
    consumer.getLatch().await();

    Assert.isTrue(consumer.getLatch().getCount() == 0L, "Invalid latch count");
    Assert.isTrue(
        consumer.getPayload().contains("Completing execution: NewMessageWorkflow"),
        "Invalid last event payload");
  }

  @ComponentScan
  public static class Configuration {}
}
