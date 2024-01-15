package education.sinsw.po.create.streams;

import education.sinsw.json.Error;
import education.sinsw.json.PoCreateRequestJson;
import education.sinsw.po.create.factory.ErrorFactory;
import education.sinsw.po.create.factory.SerdesFactory;
import education.sinsw.po.create.holder.PoCreateHolder;
import education.sinsw.po.create.transformer.PoCreateTransformer;
import education.sinsw.po.create.util.PropertyHolder;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.message.Encoding;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.streams.serdes.json.KafkaJsonSchemaSerde;
import nsw.sinsw.streams.config.SchemaConfig;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@ApplicationScoped
public class SinswProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SinswProcessor.class);

    @Inject
    SchemaConfig schemaConfig;

    @Inject
    PropertyHolder propertyHolder;

    Serde<CloudEvent> vendorOutSerde;
    Serde<CloudEvent> vendorOutAcksSerde;
    KafkaJsonSchemaSerde<Error> errorSerde;

    KafkaJsonSchemaSerde<PoCreateRequestJson> poCreateReadJsonSerde;
    Serde<PoCreateHolder> poCreateWriteCESerde;

    Serde<GenericRecord> avroWriter;

    @Produces
    public Topology buildTopology() {
        System.out.println("starting...");
        System.out.println("Po create inbound json topic "+propertyHolder.getPoCreateJsonInTopic());
        System.out.println("Po Create Sap inbount avro topic: " +propertyHolder.getPoCreateSAPAvroInTopic());

        StreamsBuilder builder = new StreamsBuilder();

        Map<String, Object> serdeProps = new HashMap<>();

        schemaConfig.initialize();
        initSerdes();

        poCreateInboundImpl(builder);

        final Properties props = new Properties();
        final Topology topology = builder.build(props);

        // FIXME print topology map/diagram
        // https://zz85.github.io/kafka-streams-viz/
        LOG.info(topology.describe().toString());

        return topology;

    }

    private void poCreateInboundImpl(StreamsBuilder builder) {
        // Note: using byte[] for key as currently we're ignoring it...

        KStream<byte[], PoCreateRequestJson> raw = builder.stream(propertyHolder.getPoCreateJsonInTopic(), Consumed.with(Serdes.ByteArray(), poCreateReadJsonSerde));
        raw.peek((k,v) -> System.out.println("Got PO Create json message from Json inbound topic: "+ propertyHolder.getPoCreateJsonInTopic() + "\n*********\n"+v.toString() + "\n********\n"));

        KStream<byte[], PoCreateHolder> parsed =
                raw.transformValues(
                        () ->
                                new PoCreateTransformer(
                                        schemaConfig.getSchemaRegistryClient(),
                                        avroWriter.serializer(), propertyHolder),
                        Named.as("parse_and_validate_poCreateInbound_input"));

        parsed.filter((k, v) -> v.getException() != null, Named.as("poCreate_has_exception"))
                .mapValues(
                        value -> ErrorFactory.INSTANCE.poCreateError(value),
                        Named.as("poCreate_standardize_error_message"))
                        .peek((key,value) -> System.out.println("********** Got PoCreate error \n" +value))
                .to(
                        (k, v, rc) -> {
                            return propertyHolder.getPoErrorTopic();
                        },
                        Produced.with(Serdes.ByteArray(), errorSerde).withName("produce_poCreate_error_event"));

        parsed.filter((k,v) -> v.getException() == null)
                .peek((k,v) -> System.out.println("Producing PoCreate Avro message top topic: " + propertyHolder.getPoCreateSAPAvroInTopic() + "\n**********\n"+ v.getParsed().getData() + "\n**********\n"))
                .to((k, v, rc) -> {
                    for (Header header : rc.headers().toArray()) {
                        rc.headers().remove(header.key());
                    }
                    return propertyHolder.getPoCreateSAPAvroInTopic();
                },  Produced.with(Serdes.ByteArray(), poCreateWriteCESerde));

    }

    private void initSerdes() {
        SerdesFactory serdesFactory = new SerdesFactory(schemaConfig.getSchemaRegistryConfig());
        SchemaRegistryClient schemaRegistryClient = schemaConfig.getSchemaRegistryClient();
        String schemaRegistryUrl = propertyHolder.getSchemaRegistryUrl();

        vendorOutSerde = serdesFactory.cloudEvent(Encoding.BINARY);
        vendorOutAcksSerde = serdesFactory.cloudEvent(Encoding.STRUCTURED, SerdesFactory.DataMapperType.CONFLUENT_AVRO);
        errorSerde = SerdesFactory.errorSerde(schemaRegistryClient, schemaRegistryUrl);

        poCreateReadJsonSerde = SerdesFactory.poCreateInboundSerde(schemaRegistryClient, schemaRegistryUrl);
        poCreateWriteCESerde = serdesFactory.parsedOutput(vendorOutSerde);

        avroWriter = serdesFactory.genericAvro(SerdesFactory.SubjectStrategy.RECORD_NAME);
        //ErrorFactory.INSTANCE.configure(propertyHolder.getSystemName(), serdesFactory.errorSerde(schemaRegistryClient, schemaRegistryUrl).serializer());
    }

}
