package education.sinsw.po.create.holder;

import education.sinsw.json.PoCreateRequestJson;
import io.cloudevents.CloudEvent;
import nsw.sinsw.streams.config.MappingConfig;
import nsw.sinsw.streams.exception.SinswProcessorException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.processor.ProcessorContext;

public class PoCreateHolder {
  private CloudEvent parsed;
  private MappingConfig mappingConfig;
  private final CurrentContext context;
  private final SinswProcessorException exception;
  private PoCreateRequestJson poCreateRequestJson;
  private String corelationId;

  private PoCreateHolder(
      CloudEvent parsed,
      MappingConfig mappingConfig,
      CurrentContext context,
      SinswProcessorException exception,
      String corelationId) {
    this.parsed = parsed;
    this.mappingConfig = mappingConfig;
    this.context = context;
    this.exception = exception;
    this.corelationId = corelationId;
  }

  private PoCreateHolder(
          CloudEvent parsed,
          PoCreateRequestJson poCreateRequestJson,
          MappingConfig mappingConfig,
          CurrentContext context,
          SinswProcessorException exception,
          String corelationId) {
    this.parsed = parsed;
    this.poCreateRequestJson = poCreateRequestJson;
    this.mappingConfig = mappingConfig;
    this.context = context;
    this.exception = exception;
    this.corelationId = corelationId;
  }

  public PoCreateHolder(CurrentContext context, SinswProcessorException exception) {
    this.context = context;
    this.exception = exception;
  }
  public static PoCreateHolder from(
      ProcessorContext processorContext, CloudEvent cloudEvent, MappingConfig mappingConfig) {
    return PoCreateHolder.from(processorContext, cloudEvent, mappingConfig, null);
  }

  public static PoCreateHolder from(
      ProcessorContext processorContext,
      CloudEvent cloudEvent,
      MappingConfig mappingConfig,
      SinswProcessorException exception) {
    return new PoCreateHolder(
        cloudEvent, mappingConfig, CurrentContext.wrap(processorContext), exception, cloudEvent.getId());
  }

  public static PoCreateHolder from(
          ProcessorContext processorContext,
          PoCreateRequestJson poCreateRequestJson,
          MappingConfig mappingConfig,
          SinswProcessorException exception) {
    return new PoCreateHolder(
            null, poCreateRequestJson, mappingConfig, CurrentContext.wrap(processorContext), exception, poCreateRequestJson.getSourceHeader().getTrackingId());
  }

  public static PoCreateHolder from(
      ConsumerRecord<byte[], byte[]> consumerRecord, SinswProcessorException exception) {
    return new PoCreateHolder(CurrentContext.wrap(consumerRecord), exception);
  }

  public static PoCreateHolder from(
          ProducerRecord<byte[], byte[]> producerRecord, SinswProcessorException exception) {
    return new PoCreateHolder(CurrentContext.wrap(producerRecord), exception);
  }

  public CloudEvent getParsed() {
    return parsed;
  }

  public MappingConfig getMappingConfig() {
    return mappingConfig;
  }

  public CurrentContext getContext() {
    return context;
  }

  public SinswProcessorException getException() {
    return exception;
  }

  public String getCorelationId() {
    return corelationId;
  }

  public static class CurrentContext {
    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;

    private CurrentContext(String topic, int partition, long offset, long timestamp) {
      this.topic = topic;
      this.partition = partition;
      this.offset = offset;
      this.timestamp = timestamp;
    }

    private static CurrentContext wrap(ProcessorContext processorContext) {
      return new CurrentContext(
          processorContext.topic(),
          processorContext.partition(),
          processorContext.offset(),
          processorContext.timestamp());
    }

    private static CurrentContext wrap(ConsumerRecord<byte[], byte[]> consumerRecord) {
      return new CurrentContext(
          consumerRecord.topic(),
          consumerRecord.partition(),
          consumerRecord.offset(),
          consumerRecord.timestamp());
    }

    private static CurrentContext wrap(ProducerRecord<byte[], byte[]> producerRecord) {
      return new CurrentContext(
              producerRecord.topic(),
              producerRecord.partition(),
              0l,
              producerRecord.timestamp());
    }

    public String topic() {
      return topic;
    }

    public int partition() {
      return partition;
    }

    public long offset() {
      return offset;
    }

    public long timestamp() {
      return timestamp;
    }

  }
}
