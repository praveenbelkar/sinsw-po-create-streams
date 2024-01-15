package education.sinsw.po.create.util;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PropertyHolder {

    @ConfigProperty(name = "po.create.inbound.sap.avro.topic", defaultValue = "sinsw-systems-sap-in-purchase-order-create-avro")
    private String poCreateSAPAvroInTopic;

    @ConfigProperty(name = "po.create.inbound.json.topic", defaultValue = "sinsw-systems-in-purchase-order-create-json")
    private String poCreateJsonInTopic;

    @ConfigProperty(name = "po.error.topic", defaultValue = "sinsw-systems-internal-error")
    private String poErrorTopic;

    @ConfigProperty(name = "subject.po.create.inbound.json", defaultValue = "sinsw-systems-in-purchase-order-create-json-value")
    private String poCreateJsonInboundTopicSchemaSubject;

    @ConfigProperty(name = "subject.po.create.inbound.sap.avro", defaultValue = "education.sinsw.sap.inbound.avro_schemas.CreatePurchaserOrder")
    private String poCreateSAPAvroInboundTopicSchemaSubject;

    @ConfigProperty(name = "quarkus.kafka-streams.schema-registry-url", defaultValue = "http://172.16.46.58:8081")
    private String schemaRegistryUrl;

    private String systemName = "PoCreateProcessor";

    public String getPoCreateSAPAvroInTopic() {
        return poCreateSAPAvroInTopic;
    }

    public String getPoCreateJsonInTopic() {
        return poCreateJsonInTopic;
    }

    public String getPoErrorTopic() {
        return poErrorTopic;
    }

    public String getPoCreateJsonInboundTopicSchemaSubject() {
        return poCreateJsonInboundTopicSchemaSubject;
    }

    public String getPoCreateSAPAvroInboundTopicSchemaSubject() {
        return poCreateSAPAvroInboundTopicSchemaSubject;
    }

    public String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    public String getSystemName() {
        return systemName;
    }
}
