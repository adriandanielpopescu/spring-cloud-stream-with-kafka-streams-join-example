# Spring Cloud Stream with Kafka Streams Join Example

This project is showing how to join two kafka topics using Kafka Streams with Spring Cloud Stream on Cloud Foundry. Two input topics
are joined into a new output topic which contains the joined records. All of the topics are using the Avro format for keys and values, 
and their schema is published to the Schema Registry using Confluent platform. For a detailed description on running Kafka Streams with
Spring Cloud Streams please check the following post: [Running Kafka-Streams with Spring Cloud Stream on Cloud Foundry](https://scaleoutdata.com/running-kafka-streams-with-spring-cloud-stream-on-cloud-foundry).

## Resource Configuration
There are multiple possibilities in joining two inputs in Kafka Streams. In this project, we focus only on `(KStream, KStream)` windowed joins.
The main resource configuration parameters that are particularly relevant for Kafka Streams applications are the memory size and disk quota. 
Kafka Streams uses the so called **state stores** to keep the internal state of the application. Depending on the type of state store 
that is used: **persistent** or **in-memory**, the application may require memory and/or disk tuning. JVM memory pools (such as direct memory 
size, reserved code cache size)  can be also configured by specifying the configurations in the JAVA_OPTS environmental variable. For an example,
please check the [manifest.yml](manifest/manifest.yml) file. 

## Application Configuration
For configuring Spring Cloud Stream with Kafka Streams we use the [applcation.yml](src/main/resources/config/application.yml) configuration file.
The first section declares the application properties: the names of the two input topics, the kafka broker address, and the schema registry url. 
Please adjust them as needed to fit the configuration of your kafka setup.

The next property, `spring.application.name` is very important. This property defines the name of the kafka streams application,
and is used as a prefix in all the internal kafka topics created by the kafka streams library.

Two main categories of properties are following in the configuration file: **binder** properties, and **binding** properties. 
Binder properties are describing the messaging queue implementation (such as Kafka). Binding properties are describing the properties 
of the input/output queues (such as the name of the Kafka topic, the content-type, etc). 

### Binder
`spring.cloud.stream.kafka.streams.binder.*` properties define the properties corresponding to the binder, in our case Kafka. The kafka brokers, and
kafka default configuration can be provided in this section of the configuration file.

### Bindings
`spring.cloud.stream.kafka.streams.bindings.*` properties define the binding properties of the inputs and outputs. For each input the application is
a consumer, and for each output the application is a producer. Custom kafka properties can be provided for each binding.

`spring.cloud.stream.bindings.*` properties define the mapping of each binding name to a **destination** 
(a Kafka topic, when using Kafka as a binder).

Finally, to instruct Spring Boot to enable the bindings, a binding interface has to be provided
and the following annotation has to be specified `@EnableBinding([BindingInterface.class])`. In this project,
[KafkaStreamsBindings](src/main/java/com/scaleoutdata/spring/cloud/stream/kafka/streams/join_example/config/KafkaStreamsBindings.java) is the 
interface that declares the binding methods that will be implemented by the Spring binder. This interface is then enabled in the  
[JoinExampleApplication](src/main/java/com/scaleoutdata/spring/cloud/stream/kafka/streams/join_example/JoinExampleApplication.java) using 
`@EnableBinding(KafkaStreamsBindings.class)`.

### Custom SerDes
By default, Spring Cloud Stream chooses a default SerDe implementation for serializing / deserializing key/values from the Kafka topics 
based on the content-type. If native serilization/deserialization is desired, we need to set `useNativeEncoding: true` and additionally 
define default key/value SerDes for the binder, or custom SerDes for each binding. 
In this project we use native Kafka serialization/deserialization by configuring the application to use kafka libraries
(i.e., default.value.serde: io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde). 

### Native Kafka SerDes for primitive types
When using native Kafka SerDes, serialization/deserialization of primitive type should be done through a custom class that uses under 
the hood KafkaAvroSerializer() and KafkaAvroDeserializer() rather than using the Kafka SerDes for primitive types (e.g., Serdes.String(), 
Serdes.Long(), etc). The reason is an [incompatibility](https://stackoverflow.com/questions/51955921/serde-class-for-avro-primitive-type) 
between Confluent and Kafka SerDes for primitive types. Thus, we defined 
[StringAvroSerde](src/main/java/com/scaleoutdata/spring/cloud/stream/kafka/streams/join_example/serdes/StringAvroSerde.java) class which uses
KafkaAvroSerializer and KafkaAvroDeserializer.


## Testing
This project uses `TopologyTestDriver` to test the streaming pipeline. Since the streaming pipeline uses Avro objects which are going to be
registered into Kafka's Schema Registry we need a mock server that will mock the functionality of the schema registry. For this purpose we 
use `schema-registry-mock` artifact which mocks under the hood the Schema registry object as `SchemaRegistryMock`. For an example, please
check [TestKafkaStreamsController.java](src/test/java/com/scaleoutdata/spring/cloud/stream/kafka/streams/join_example/controller/TestKafkaStreamsController.java). For a complete description of the testing API you can check [Unit Testing Kafka Streams with Avro](https://scaleoutdata.com/unit-testing-kafka-streams-with-avro-schemas/).

For running the tests simply run: `mvn test`

## Deployment
This project can be compiled with Maven and then deployed to Cloud Foundry platform using cf tools. 

`mvn package`\
`cf push -f manifest/manifest.yml -p target/spring-cloud-stream-kafka-streams-0.0.1.jar`

Or to run it locally:
```
mvn spring-boot:run

```