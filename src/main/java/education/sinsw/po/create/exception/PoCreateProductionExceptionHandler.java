package education.sinsw.po.create.exception;

import education.sinsw.json.Error;
import education.sinsw.po.create.factory.ErrorFactory;
import education.sinsw.po.create.holder.PoCreateHolder;
import nsw.sinsw.streams.exception.ProductionDLQExceptionHandler;
import nsw.sinsw.streams.exception.SinswProcessorException;
import org.apache.kafka.clients.producer.ProducerRecord;

// FIXME - add unit tests
public class PoCreateProductionExceptionHandler extends ProductionDLQExceptionHandler {

  @Override
  protected Error getProducerError(ProducerRecord<byte[], byte[]> record, Exception exception) {
    PoCreateHolder poCreateHolder = PoCreateHolder.from(record, SinswProcessorException.newUnknownEncodingException(exception));
    return ErrorFactory.INSTANCE.poCreateError(poCreateHolder);
  }
}
