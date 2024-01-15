package education.sinsw.po.create.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import education.sinsw.json.PoCreateRequestJson;
import education.sinsw.po.create.factory.POCreateAvroRecordFactory;
import education.sinsw.po.create.factory.PoCreateAvroMapper;
import education.sinsw.po.create.holder.PoCreateHolder;
import education.sinsw.po.create.util.PropertyHolder;
import education.sinsw.sap.inbound.avro_schemas.CreatePurchaserOrder;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import nsw.sinsw.streams.avro.ce.AvroCloudEventData;
import nsw.sinsw.streams.config.MappingConfig;
import nsw.sinsw.streams.exception.SinswProcessorException;
import nsw.sinsw.streams.util.JsonUtil;
import nsw.sinsw.streams.util.KafkaUtil;
import nsw.sinsw.streams.validation.CloudEventValidator;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class PoCreateTransformer
        implements ValueTransformerWithKey<byte[], PoCreateRequestJson, PoCreateHolder> {

    private static final Logger LOG = LoggerFactory.getLogger(PoCreateTransformer.class);
    private static final ObjectMapper jsonMapper = JsonUtil.newObjectMapper();
    private final SchemaRegistryClient schemaRegistryClient;
    private final CloudEventValidator validator = new CloudEventValidator();
    private final Serializer<GenericRecord> avroWriter;
    private ProcessorContext context = null;
    private PropertyHolder propertyHolder;

    public PoCreateTransformer(SchemaRegistryClient schemaRegistryClient, Serializer<GenericRecord> avroWriter, PropertyHolder propertyHolder) {
        this.schemaRegistryClient = schemaRegistryClient;
        this.avroWriter = avroWriter;
        this.propertyHolder = propertyHolder;
    }

    @Override
    public void init(ProcessorContext processorContext) {
        this.context = processorContext;
    }

    @Override
    public PoCreateHolder transform(byte[] bytes, PoCreateRequestJson poCreateRequestJson) {
        try {
            //TODO implement validate
            //validator.validateSubject(value, "po-create", "po-change");
            validator.validateSubject(context.headers(), "po-create");
            //validator.validateExtensionPresent(value, "edunswproducer");
            MappingConfig mappingConfig = createMappingConfig();
            CloudEvent event = doTransform(poCreateRequestJson);
            return PoCreateHolder.from(context, event, mappingConfig);
        } catch (SinswProcessorException fpe) {
            LOG.error(fpe.getMessage(), fpe);
            return PoCreateHolder.from(context, poCreateRequestJson, null, fpe);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            SinswProcessorException sinswpe = SinswProcessorException.newOther(ex);
            return PoCreateHolder.from(context, poCreateRequestJson, null, sinswpe);
        }

    }

    private MappingConfig createMappingConfig() {
        MappingConfig mappingConfig = new MappingConfig();
        mappingConfig.setSchemaSubject(propertyHolder.getPoCreateJsonInboundTopicSchemaSubject());
        mappingConfig.setSchemaVersion("latest");
        mappingConfig.setDestTopic(propertyHolder.getPoCreateSAPAvroInTopic());
        mappingConfig.setDestSchemaSubject(propertyHolder.getPoCreateSAPAvroInboundTopicSchemaSubject());
        mappingConfig.setDestSchemaVersion("latest");
        return mappingConfig;
    }

    protected CloudEvent doTransform(PoCreateRequestJson poCreateRequestJson) {
        validateSourceJsonSchema(poCreateRequestJson);
        CreatePurchaserOrder createPurchaserOrder = PoCreateAvroMapper.convertToCreatePurchaserOrder(poCreateRequestJson);
        GenericRecord parsedGenericRecord = createDestinationGenericRecord(createPurchaserOrder);
        CloudEvent destinationCloudEvent = createDestinationCloudEvent(parsedGenericRecord);

        return destinationCloudEvent;
    }

    private void validateSourceJsonSchema(PoCreateRequestJson poCreateRequestJson) {
        try{
            validateJsonSchema(poCreateRequestJson);
        } catch (Exception e) {
            e.printStackTrace();
            throw SinswProcessorException.newSchemaValidationException(e);
        }
    }

    private CloudEvent createDestinationCloudEvent(GenericRecord parsedGenericRecord) {
        System.out.println("******* PO Create HEADERS *******");
        context.headers().forEach(header -> System.out.println(header.key() + ":" +  new String(header.value(), StandardCharsets.UTF_8)));
        System.out.println("**************");
        Headers headers = context.headers();
        String specVersion = KafkaUtil.readHeaders(headers, "specversion");
        String id = KafkaUtil.readHeaders(headers, "id");
        String subject = KafkaUtil.readHeaders(headers, "subject");
        String type = KafkaUtil.readHeaders(headers, "type");
        String source = KafkaUtil.readHeaders(headers, "source");


        final String edunswiam = "SRM_SCI_RFC";
        final String edunswreplyto = "PORTT";
        final String edunswproducer = "SAP";

        CloudEvent event =
                CloudEventBuilder.v1()
                        .withId(id)
                        .withType(type)
                        .withSource(URI.create(source))
                        .withSubject(subject)
                        .withTime(OffsetDateTime.now(ZoneId.of("Australia/Sydney")))
                        .withExtension("edunswinreplyto", UUID.randomUUID().toString())
                        .withExtension("edunswiam", edunswiam)
                        .withExtension("edunswreplyto", edunswreplyto)
                        .withExtension("edunswproducer", edunswproducer)
                        .build();


        CloudEventBuilder builder = CloudEventBuilder.v1(event).withData("application/avro",
                AvroCloudEventData.wrap(propertyHolder.getPoCreateSAPAvroInTopic(), parsedGenericRecord, avroWriter::serialize));
        return builder.build();

    }

    private GenericRecord createDestinationGenericRecord(CreatePurchaserOrder createPurchaserOrder) {
        MappingConfig mappingConfig = createMappingConfig();
        AvroSchema avroSchema = KafkaUtil.getAvroSchema(schemaRegistryClient, mappingConfig.getDestSchemaSubject(), mappingConfig.getDestSchemaVersion());
        GenericRecord parsedGenericRecord = POCreateAvroRecordFactory.createGenericRecord(createPurchaserOrder, avroSchema.rawSchema());
        return parsedGenericRecord;
    }

    private void validateJsonSchema(PoCreateRequestJson poCreateRequestJson) {
        Object jsonObj = null;
        JsonSchema jsonSchema = null;
        String jsonFileName = "poCreateRequestJson.json";
        String schemaFileContent = null;
        try {
            schemaFileContent = JsonUtil.readSchemaFile(jsonFileName);
            String json = jsonMapper.writeValueAsString(poCreateRequestJson);
            LOG.info(jsonFileName + " content:\n"+json);
            JsonNode targetJsonNode = jsonMapper.readTree(json);
            jsonObj = jsonMapper.readValue(jsonMapper.writeValueAsBytes(targetJsonNode), Object.class);
            jsonSchema = new JsonSchema(schemaFileContent);
            jsonSchema.validate(jsonObj);
        } catch (JsonProcessingException e) {
            LOG.error("jsons schema validation processing exception..." + e.getMessage());
            throw  SinswProcessorException.newSchemaValidationException(e);
        } catch(ValidationException ve) {
            StringBuilder sb = new StringBuilder();
            sb.append(jsonFileName + " schema validation failure:  ");
            for(String str: ve.getAllMessages()) {
                sb.append(str).append("\n");
            }
            throw new RuntimeException(sb.toString());
        } catch (IOException e) {
            LOG.error("Failed to read the json schema file..." + jsonFileName);
            throw  SinswProcessorException.newSchemaValidationException(e);
        }

    }

    @Override
    public void close() {

    }
}
