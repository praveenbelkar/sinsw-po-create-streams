package education.sinsw.po.create.exception;

import education.sinsw.json.Error;
import education.sinsw.po.create.factory.ErrorFactory;
import education.sinsw.po.create.holder.PoCreateHolder;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import nsw.sinsw.streams.exception.SendToDeadLetterQueueExceptionHandler;
import nsw.sinsw.streams.exception.SinswProcessorException;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

// FIXME - add unit tests
public class PoCreateDeserializationExceptionHandler extends SendToDeadLetterQueueExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PoCreateDeserializationExceptionHandler.class);

  @Override
  protected Error getConsumerError(ConsumerRecord<byte[], byte[]> record, Exception exception) {
    LOG.error("Throwing error from PoCreateDeserializationExceptionHandler");
    PoCreateHolder poCreateHolder = PoCreateHolder.from(record, SinswProcessorException.newUnknownEncodingException(exception));
    return ErrorFactory.INSTANCE.poCreateError(poCreateHolder);
  }
}