package education.sinsw.po.create.factory;

import education.sinsw.json.Error;
import education.sinsw.json.PoCreateRequestJson;
import education.sinsw.po.create.holder.PoCreateHolder;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.message.Encoding;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.kafka.CloudEventDeserializer;
import io.cloudevents.kafka.CloudEventSerializer;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.json.KafkaJsonSchemaSerde;
import nsw.sinsw.streams.avro.ce.AvroCloudEventDataMapper;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.HashMap;
import java.util.Map;

import static io.cloudevents.kafka.CloudEventDeserializer.MAPPER_CONFIG;
import static io.cloudevents.kafka.CloudEventSerializer.ENCODING_CONFIG;
import static io.cloudevents.kafka.CloudEventSerializer.EVENT_FORMAT_CONFIG;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY;

/** */
public class SerdesFactory {

  private final Map<String, Object> defaultConfigs;

  public SerdesFactory(Map<String, Object> defaultConfigs) {
    this.defaultConfigs = new HashMap<>(defaultConfigs);
  }

  public Serde<CloudEvent> cloudEvent(Encoding encoding) {
    CloudEventSerializer cloudEventSerializer = new CloudEventSerializer();
    cloudEventSerializer.configure(
            Map.of(ENCODING_CONFIG, encoding, EVENT_FORMAT_CONFIG, JsonFormat.CONTENT_TYPE), false);
    CloudEventDeserializer cloudEventDeserializer = new CloudEventDeserializer();
    return Serdes.serdeFrom(cloudEventSerializer, cloudEventDeserializer);
  }

  public Serde<CloudEvent> cloudEvent(Encoding encoding, DataMapperType mapperType) {

    Map<String, Object> configs = new HashMap<>(defaultConfigs);

    CloudEventSerializer cloudEventSerializer = new CloudEventSerializer();
    cloudEventSerializer.configure(
            Map.of(ENCODING_CONFIG, encoding, EVENT_FORMAT_CONFIG, JsonFormat.CONTENT_TYPE), false);

    CloudEventDeserializer cloudEventDeserializer = new CloudEventDeserializer();

    if (mapperType == DataMapperType.CONFLUENT_AVRO) {
      GenericAvroDeserializer avroDeserializer = new GenericAvroDeserializer();
      avroDeserializer.configure(configs, false);
      cloudEventDeserializer.configure(
              Map.of(MAPPER_CONFIG, AvroCloudEventDataMapper.from(avroDeserializer)), false);
    }

    return Serdes.serdeFrom(cloudEventSerializer, cloudEventDeserializer);
  }

  public static KafkaJsonSchemaSerde<PoCreateRequestJson> poCreateInboundSerde(SchemaRegistryClient schemaRegistryClient, String schemaRegistryUrl) {
    KafkaJsonSchemaSerde<PoCreateRequestJson> serde = new KafkaJsonSchemaSerde<>(schemaRegistryClient);
    Map<String, Object> serdeConfig = new HashMap<>();
    serdeConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    serdeConfig.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, PoCreateRequestJson.class.getName());
    serdeConfig.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
    serde.configure(serdeConfig, false);
    return serde;
  }

  public static KafkaJsonSchemaSerde<Error> errorSerde(SchemaRegistryClient schemaRegistryClient, String schemaRegistryUrl) {
    Map<String, Object> configs = new HashMap<>();
/*
    configs.put(
            VALUE_SUBJECT_NAME_STRATEGY, "io.confluent.kafka.serializers.subject.RecordNameStrategy");
*/
    KafkaJsonSchemaSerde<Error> errorSerde = new KafkaJsonSchemaSerde<>(schemaRegistryClient);
    Map<String, Object> serdeConfig = new HashMap<>();
    serdeConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    serdeConfig.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, Error.class.getName());
    serdeConfig.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
    errorSerde.configure(serdeConfig, false);

    return errorSerde;
  }

  public Serde<PoCreateHolder> parsedOutput(Serde<CloudEvent> fromSerde) {
    return Serdes.serdeFrom(
            new Serializer<>() {
              @Override
              public byte[] serialize(String s, PoCreateHolder p) {
                return fromSerde.serializer().serialize(s, p.getParsed());
              }

              @Override
              public byte[] serialize(String t, Headers h, PoCreateHolder p) {
                return fromSerde.serializer().serialize(t, h, p.getParsed());
              }
            },
            (t, r) -> null);
  }

  public Serde<GenericRecord> genericAvro(SubjectStrategy strategy) {
    Map<String, Object> configs = new HashMap<>(defaultConfigs);
    if (strategy == SubjectStrategy.RECORD_NAME) {
      configs.put(VALUE_SUBJECT_NAME_STRATEGY, "io.confluent.kafka.serializers.subject.RecordNameStrategy");
    }

    Serde<GenericRecord> avroType = new GenericAvroSerde();
    avroType.configure(configs, false);
    return avroType;
  }

  public enum DataMapperType {
    CONFLUENT_AVRO
  }

  public enum SubjectStrategy {
    TOPIC_NAME,
    RECORD_NAME
  }
}
