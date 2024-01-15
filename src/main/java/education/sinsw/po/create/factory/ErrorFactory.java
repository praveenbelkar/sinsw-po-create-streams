package education.sinsw.po.create.factory;

import education.sinsw.json.Error;
import education.sinsw.json.ErrorDetail;
import education.sinsw.json.RecordContext;
import education.sinsw.po.create.holder.PoCreateHolder;
import io.quarkus.runtime.util.ExceptionUtil;

public class ErrorFactory {

  public static final ErrorFactory INSTANCE = new ErrorFactory();

  public Error poCreateError(PoCreateHolder poCreateHolder) {
    return buildPoCreateError(poCreateHolder);
  }

  private Error buildPoCreateError(PoCreateHolder poCreateHolder) {
    Error error = new Error();
    RecordContext recordContext = new RecordContext();
    recordContext.setTopic(poCreateHolder.getContext().topic());
    recordContext.setPartition(poCreateHolder.getContext().partition());
    recordContext.setOffset((int)poCreateHolder.getContext().offset());
    recordContext.setTimestamp(poCreateHolder.getContext().timestamp());
    recordContext.setCorrelationId(poCreateHolder.getCorelationId());

    ErrorDetail errorDetail = new ErrorDetail();
    errorDetail.setMessage(poCreateHolder.getException().getMessage());
    errorDetail.setErrorClass(poCreateHolder.getException().getClass().getName());
    errorDetail.setStackTrace(ExceptionUtil.generateStackTrace(poCreateHolder.getException()));
    Throwable rootCause = ExceptionUtil.getRootCause(poCreateHolder.getException());
    if (rootCause != null) errorDetail.setCausedBy(rootCause.getMessage());

    error.setRecordContext(recordContext);
    error.setErrorDetail(errorDetail);
    error.setKind(poCreateHolder.getException().getKind().name());

    return error;
  }

}
