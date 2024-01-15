package education.sinsw.po.create.factory;

import education.sinsw.sap.inbound.avro_schemas.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.List;

public class POCreateAvroRecordFactory {
    public static GenericRecord createGenericRecord(CreatePurchaserOrder createPurchaserOrder, Schema avroSchema) {
        GenericRecord record = new GenericData.Record(avroSchema);
        record.put("SourceHeader", createSourceHeader(record, createPurchaserOrder));
        record.put("POHeader", createPoHeader(record, createPurchaserOrder));
        record.put("PurchaseOrderitems", createPurchaseOrderItems(record, createPurchaserOrder));

        return record;
    }

    // use when having a union type with non primitives
    private static Schema schemaFrom(GenericRecord record, String fieldname, String typename) {

        Schema schema = record.getSchema().getField(fieldname).schema();
        if (schema.isUnion()) {
            Integer idx = schema.getIndexNamed(typename);
            if (idx == null) {
                System.out.println("Union type not found " + typename);
                return schema;
            }
            return schema.getTypes().get(idx);
        }
        return schema;
    }

    private static GenericRecord createSourceHeader(GenericRecord record, CreatePurchaserOrder createPurchaserOrder) {
        GenericRecord sourceHeader = new GenericData.Record(record.getSchema().getField("SourceHeader").schema());
        SourceHeaderType sourceHeaderIn = createPurchaserOrder.getSourceHeader();
        sourceHeader.put("TrackingID", sourceHeaderIn.getTrackingID());
        sourceHeader.put("CreationDateTime", sourceHeaderIn.getCreationDateTime());
        sourceHeader.put("Service", sourceHeaderIn.getService());
        sourceHeader.put("User", sourceHeaderIn.getUser());
        return sourceHeader;
    }

    private static GenericRecord createPoHeader(GenericRecord record, CreatePurchaserOrder createPurchaserOrder) {
        GenericRecord poHeader = new GenericData.Record(record.getSchema().getField("POHeader").schema());
        POHeaderType poHeaderType = createPurchaserOrder.getPOHeader();
        poHeader.put("PurchaseOrderText", poHeaderType.getPurchaseOrderText());
        poHeader.put("Supplier", poHeaderType.getSupplier());
        poHeader.put("CompanyCode", poHeaderType.getCompanyCode());

        if(createPurchaserOrder.getPOHeader().getPaymentTerms().bytes() != null) {
            //poHeader.put("PaymentTerms", new GenericData.Fixed(schemaFrom(poHeader, "PaymentTerms", PaymentTermsType.getClassSchema().getFullName()), getBytes(createPurchaserOrder.getPOHeader().getPaymentTerms())));
            //poHeader.put("PaymentTerms", new GenericData.Fixed(schemaFrom(poHeader, "PaymentTerms", PaymentTermsType.getClassSchema().getFullName()), "sdfd".getBytes(StandardCharsets.UTF_8)));
            poHeader.put("PaymentTerms", new GenericData.Fixed(schemaFrom(poHeader, "PaymentTerms", PaymentTermsType.getClassSchema().getFullName()), createPurchaserOrder.getPOHeader().getPaymentTerms().bytes()));
        }

        poHeader.put("ContractID", poHeaderType.getContractID());
        poHeader.put("ContractSegment", poHeaderType.getContractSegment());
        poHeader.put("SendToSupplier", poHeaderType.getSendToSupplier());
        poHeader.put("CreatedByUser", poHeaderType.getCreatedByUser());

        poHeader.put("Requester", createRequesterForPoHeader(poHeader, createPurchaserOrder));
        poHeader.put("GoodsRecipient", createGoodsRecipientForPoHeader(poHeader, createPurchaserOrder));
        poHeader.put("Approver", createApproverForPoHeader(poHeader, createPurchaserOrder));
        return poHeader;
    }

    private static GenericRecord createRequesterForPoHeader(GenericRecord poHeader, CreatePurchaserOrder createPurchaserOrder) {
        GenericRecord requester = new GenericData.Record(poHeader.getSchema().getField("Requester").schema());
        RequesterType requesterType = createPurchaserOrder.getPOHeader().getRequester();
        requester.put("RequesterID", requesterType.getRequesterID());
        if(createPurchaserOrder.getPOHeader().getRequester().getRequesterPosition().bytes() != null) {
            requester.put("RequesterPosition", new GenericData.Fixed(schemaFrom(requester, "RequesterPosition", RequesterPositionType.getClassSchema().getFullName()), createPurchaserOrder.getPOHeader().getRequester().getRequesterPosition().bytes()));
        }
        return requester;
    }

    private static GenericRecord createGoodsRecipientForPoHeader(GenericRecord poHeader, CreatePurchaserOrder createPurchaserOrder) {
        GenericRecord goodsRecipient = new GenericData.Record(poHeader.getSchema().getField("GoodsRecipient").schema());
        GoodsRecipientType goodsRecipientType = createPurchaserOrder.getPOHeader().getGoodsRecipient();
        goodsRecipient.put("GoodsRecipientID", goodsRecipientType.getGoodsRecipientID());
        if(createPurchaserOrder.getPOHeader().getGoodsRecipient().getGRPosition().bytes() != null) {
            goodsRecipient.put("GRPosition", new GenericData.Fixed(schemaFrom(goodsRecipient, "GRPosition", GRPositionType.getClassSchema().getFullName()), createPurchaserOrder.getPOHeader().getGoodsRecipient().getGRPosition().bytes()));
        }
        return goodsRecipient;
    }

    private static GenericRecord createApproverForPoHeader(GenericRecord poHeader, CreatePurchaserOrder createPurchaserOrder) {
        GenericRecord approver = new GenericData.Record(poHeader.getSchema().getField("Approver").schema());
        ApproverType approverType = createPurchaserOrder.getPOHeader().getApprover();
        approver.put("ApproverID", approverType.getApproverID());
        if(createPurchaserOrder.getPOHeader().getApprover().getApproverPosition().bytes() != null) {
            approver.put("ApproverPosition", new GenericData.Fixed(schemaFrom(approver, "ApproverPosition", ApproverPositionType.getClassSchema().getFullName()), createPurchaserOrder.getPOHeader().getApprover().getApproverPosition().bytes()));
        }
        return approver;
    }

    private static GenericArray<GenericRecord> createPurchaseOrderItems(GenericRecord record, CreatePurchaserOrder createPurchaserOrder) {
        Schema arraySchema = schemaFrom(record, "PurchaseOrderitems", "array");
        List<GenericRecord> purchaseOrderItems = new ArrayList<>();
        for(PurchaseOrderitemsType purchaseOrderitem: createPurchaserOrder.getPurchaseOrderitems()) {
            GenericRecord poItemRecord = new GenericData.Record(arraySchema.getElementType());
            if(purchaseOrderitem.getPurchaseOrderItemNo() != null) {
                poItemRecord.put("PurchaseOrderItemNo", new GenericData.Fixed(schemaFrom(poItemRecord, "PurchaseOrderItemNo", PurchaseOrderItemNoType.getClassSchema().getFullName()), purchaseOrderitem.getPurchaseOrderItemNo().bytes()));
            }
            poItemRecord.put("ItemType", purchaseOrderitem.getItemType());
            poItemRecord.put("PurchaseOrderItemText", purchaseOrderitem.getPurchaseOrderItemText());
            poItemRecord.put("OrderQuantity", purchaseOrderitem.getOrderQuantity());
            poItemRecord.put("NetPriceAmount", purchaseOrderitem.getNetPriceAmount());
            poItemRecord.put("PurchaseOrderQuantityUnit", purchaseOrderitem.getPurchaseOrderQuantityUnit());
            poItemRecord.put("NetPriceQuantity", purchaseOrderitem.getNetPriceQuantity());
            poItemRecord.put("Deliverydate", purchaseOrderitem.getDeliverydate());
            poItemRecord.put("ValidityStartDate", purchaseOrderitem.getValidityStartDate());
            poItemRecord.put("ValidityEndDate", purchaseOrderitem.getValidityEndDate());
            if(purchaseOrderitem.getTaxCode().bytes() != null) {
                poItemRecord.put("TaxCode", new GenericData.Fixed(schemaFrom(poItemRecord, "TaxCode", TaxCodeType.getClassSchema().getFullName()), purchaseOrderitem.getTaxCode().bytes()));
            }
            if(purchaseOrderitem.getPlant().bytes() != null) {
                poItemRecord.put("Plant", new GenericData.Fixed(schemaFrom(poItemRecord, "Plant", PlantType.getClassSchema().getFullName()), purchaseOrderitem.getPlant().bytes()));
            }
            if(purchaseOrderitem.getStorageLocation().bytes() != null) {
                poItemRecord.put("StorageLocation", new GenericData.Fixed(schemaFrom(poItemRecord, "StorageLocation", StorageLocationType.getClassSchema().getFullName()), purchaseOrderitem.getStorageLocation().bytes()));
            }
            poItemRecord.put("ProductCategory", purchaseOrderitem.getProductCategory());
            poItemRecord.put("AccountAssignment", createAccountAssignments(poItemRecord, purchaseOrderitem));

            purchaseOrderItems.add(poItemRecord);
        }
        return new GenericData.Array<>(arraySchema, purchaseOrderItems);
    }

    private static GenericArray<GenericRecord> createAccountAssignments(GenericRecord poItemRecord, PurchaseOrderitemsType purchaseOrderitem) {
        Schema arraySchema = schemaFrom(poItemRecord, "AccountAssignment", "array");
        List<GenericRecord> accountAssignments = new ArrayList<>();
        for(AccountAssignmentType accountAssignment: purchaseOrderitem.getAccountAssignment()) {
            GenericRecord accountAssignmentRecord = new GenericData.Record(arraySchema.getElementType());
            accountAssignmentRecord.put("AccntCategory", accountAssignment.getAccntCategory());
            if(accountAssignment.getAccountAssignmentNumber() != null) {
                accountAssignmentRecord.put("AccountAssignmentNumber", new GenericData.Fixed(schemaFrom(accountAssignmentRecord, "AccountAssignmentNumber", AccountAssignmentNumberType.getClassSchema().getFullName()), accountAssignment.getAccountAssignmentNumber().bytes()));
            }
            accountAssignmentRecord.put("WBSElement", accountAssignment.getWBSElement());
            accountAssignmentRecord.put("CostCenter", accountAssignment.getCostCenter());
            accountAssignmentRecord.put("InternalOrder", accountAssignment.getInternalOrder());
            accountAssignmentRecord.put("GLAccount", accountAssignment.getGLAccount());
            accountAssignmentRecord.put("Fund", accountAssignment.getFund());
            accountAssignmentRecord.put("MultipleAcctAssgmtDistrPercent", accountAssignment.getMultipleAcctAssgmtDistrPercent());

            accountAssignments.add(accountAssignmentRecord);
        }
        return new GenericData.Array<>(arraySchema, accountAssignments);
    }

}
