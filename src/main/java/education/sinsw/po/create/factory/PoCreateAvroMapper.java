package education.sinsw.po.create.factory;

import education.sinsw.json.Accounting;
import education.sinsw.json.Item;
import education.sinsw.json.PoCreateRequestJson;
import education.sinsw.sap.inbound.avro_schemas.*;
import org.apache.commons.collections.CollectionUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class PoCreateAvroMapper {

    public static education.sinsw.sap.inbound.avro_schemas.CreatePurchaserOrder convertToCreatePurchaserOrder(PoCreateRequestJson poCreateRequestJson) {
        education.sinsw.sap.inbound.avro_schemas.CreatePurchaserOrder createPurchaserOrder = new education.sinsw.sap.inbound.avro_schemas.CreatePurchaserOrder();

        //1 setting sourceHeader
        education.sinsw.sap.inbound.avro_schemas.SourceHeaderType sourceHeaderType = new SourceHeaderType();
        sourceHeaderType.setUser(poCreateRequestJson.getSourceHeader().getUser());
        sourceHeaderType.setTrackingID(poCreateRequestJson.getSourceHeader().getTrackingId());
        sourceHeaderType.setService(poCreateRequestJson.getSourceHeader().getService());
        sourceHeaderType.setCreationDateTime(convertDateToString(poCreateRequestJson.getSourceHeader().getCreationDateTime()));

        createPurchaserOrder.setSourceHeader(sourceHeaderType);

        //2. Setting poHeader
        education.sinsw.sap.inbound.avro_schemas.POHeaderType poHeaderType = new POHeaderType();

        poHeaderType.setPurchaseOrderText(poCreateRequestJson.getPoHeader().getPoHeaderName());
        poHeaderType.setSupplier(poCreateRequestJson.getPoHeader().getVendor());
        education.sinsw.sap.inbound.avro_schemas.CompanyCodeType companyCodeType = new education.sinsw.sap.inbound.avro_schemas.CompanyCodeType(getBytes(poCreateRequestJson.getPoHeader().getCompanyCode()));
        poHeaderType.setCompanyCode(companyCodeType);

        education.sinsw.sap.inbound.avro_schemas.PaymentTermsType paymentTermsType = new education.sinsw.sap.inbound.avro_schemas.PaymentTermsType(getBytes(poCreateRequestJson.getPoHeader().getPaymentTerms()));
        poHeaderType.setPaymentTerms(paymentTermsType);
        poHeaderType.setContractID(poCreateRequestJson.getPoHeader().getPorttContractNumber());
        education.sinsw.sap.inbound.avro_schemas.ContractSegmentType contractSegmentType = new education.sinsw.sap.inbound.avro_schemas.ContractSegmentType(getBytes(poCreateRequestJson.getPoHeader().getPorttContractSegment()));
        poHeaderType.setContractSegment(contractSegmentType);
        SendToSupplierType sendToSupplierType = null;
        if(poCreateRequestJson.getPoHeader().getPoNotSentToSupplier().equals("Y")) {
            sendToSupplierType = SendToSupplierType.Y;
        } else {
            sendToSupplierType = SendToSupplierType.N;
        }
        poHeaderType.setSendToSupplier(sendToSupplierType);
        poHeaderType.setCreatedByUser(poCreateRequestJson.getPoHeader().getPoCreator());

        education.sinsw.sap.inbound.avro_schemas.RequesterType requesterType = new education.sinsw.sap.inbound.avro_schemas.RequesterType();
        requesterType.setRequesterID(poCreateRequestJson.getPoHeader().getRequestor().getRequestorId());
        education.sinsw.sap.inbound.avro_schemas.RequesterPositionType requesterPositionType = new education.sinsw.sap.inbound.avro_schemas.RequesterPositionType(getBytes(poCreateRequestJson.getPoHeader().getRequestor().getRequestorPosition()));
        requesterType.setRequesterPosition(requesterPositionType);
        poHeaderType.setRequester(requesterType);

        education.sinsw.sap.inbound.avro_schemas.GoodsRecipientType goodsRecipientType = new education.sinsw.sap.inbound.avro_schemas.GoodsRecipientType();
        goodsRecipientType.setGoodsRecipientID(poCreateRequestJson.getPoHeader().getRecipient().getRecipientId());
        education.sinsw.sap.inbound.avro_schemas.GRPositionType grPositionType = new education.sinsw.sap.inbound.avro_schemas.GRPositionType(getBytes(poCreateRequestJson.getPoHeader().getRecipient().getRecipientPosition()));
        goodsRecipientType.setGRPosition(grPositionType);
        poHeaderType.setGoodsRecipient(goodsRecipientType);

        education.sinsw.sap.inbound.avro_schemas.ApproverType approverType = new ApproverType();
        approverType.setApproverID(poCreateRequestJson.getPoHeader().getApprover().getApproverId());
        education.sinsw.sap.inbound.avro_schemas.ApproverPositionType approverPositionType = new ApproverPositionType(getBytes(poCreateRequestJson.getPoHeader().getApprover().getApproverPosition()));
        approverType.setApproverPosition(approverPositionType);
        poHeaderType.setApprover(approverType);
        createPurchaserOrder.setPOHeader(poHeaderType);

        //3. Setting line items
        createPurchaserOrder.setPurchaseOrderitems(getPurchaseOrderItemsList(poCreateRequestJson));

        return createPurchaserOrder;
    }

    //TODO move this method to util
    private static String convertDateToString(Date creationDateTime) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        DateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(creationDateTime);
    }

    private static byte[] getBytes(String input) {
        if(null != input) {
            return input.getBytes();
        }
        return null;
    }

    private static List<PurchaseOrderitemsType> getPurchaseOrderItemsList(PoCreateRequestJson poCreateRequestJson) {
        List<PurchaseOrderitemsType> purchaseOrderitemsTypes = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(poCreateRequestJson.getItems())) {
            purchaseOrderitemsTypes = poCreateRequestJson.getItems().stream().map(item -> createPurchaseOrderItems(item)).collect(Collectors.toList());
        }
        return purchaseOrderitemsTypes;
    }

    private static education.sinsw.sap.inbound.avro_schemas.PurchaseOrderitemsType createPurchaseOrderItems(Item item) {
        education.sinsw.sap.inbound.avro_schemas.PurchaseOrderitemsType purchaseOrderitemsType = new education.sinsw.sap.inbound.avro_schemas.PurchaseOrderitemsType();
        education.sinsw.sap.inbound.avro_schemas.PurchaseOrderItemNoType purchaseOrderItemNoType = new education.sinsw.sap.inbound.avro_schemas.PurchaseOrderItemNoType(getBytes(item.getPoLineItemNumber()));
        purchaseOrderitemsType.setPurchaseOrderItemNo(purchaseOrderItemNoType);
        purchaseOrderitemsType.setItemType(item.getItemType());
        purchaseOrderitemsType.setPurchaseOrderItemText(item.getDescription());
        purchaseOrderitemsType.setOrderQuantity(item.getQuantity());
        purchaseOrderitemsType.setNetPriceAmount(item.getPriceAmount());
        purchaseOrderitemsType.setPurchaseOrderQuantityUnit(item.getUnit());
        purchaseOrderitemsType.setNetPriceQuantity(item.getPricePerUnit());
        purchaseOrderitemsType.setDeliverydate(item.getDeliveryDate());
        purchaseOrderitemsType.setValidityStartDate(item.getValidityFrom());
        purchaseOrderitemsType.setValidityEndDate(item.getValidityTo());

        education.sinsw.sap.inbound.avro_schemas.TaxCodeType taxCodeType = new education.sinsw.sap.inbound.avro_schemas.TaxCodeType(getBytes(item.getTaxCode()));
        purchaseOrderitemsType.setTaxCode(taxCodeType);
        education.sinsw.sap.inbound.avro_schemas.PlantType plantType = new education.sinsw.sap.inbound.avro_schemas.PlantType(getBytes(item.getPlant()));
        purchaseOrderitemsType.setPlant(plantType);
        education.sinsw.sap.inbound.avro_schemas.StorageLocationType storageLocationType = new education.sinsw.sap.inbound.avro_schemas.StorageLocationType(getBytes(item.getStorageLocation()));
        purchaseOrderitemsType.setStorageLocation(storageLocationType);

        purchaseOrderitemsType.setProductCategory(item.getProductCategory());

        purchaseOrderitemsType.setAccountAssignment(getAccountAssignmentList(item));
        return purchaseOrderitemsType;

    }

    private static List<AccountAssignmentType> getAccountAssignmentList(Item item) {
        List<AccountAssignmentType> accountAssignmentTypeList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(item.getAccounting())) {
            accountAssignmentTypeList = item.getAccounting().stream().map(accounting -> createAccountAssignmentType(accounting)).collect(Collectors.toList());
        }
        return accountAssignmentTypeList;
    }

    private static education.sinsw.sap.inbound.avro_schemas.AccountAssignmentType createAccountAssignmentType(Accounting accounting) {
        education.sinsw.sap.inbound.avro_schemas.AccountAssignmentType accountAssignmentType = new education.sinsw.sap.inbound.avro_schemas.AccountAssignmentType();
        accountAssignmentType.setAccntCategory(accounting.getAccountCategory());
        education.sinsw.sap.inbound.avro_schemas.AccountAssignmentNumberType accountAssignmentNumberType = new education.sinsw.sap.inbound.avro_schemas.AccountAssignmentNumberType(getBytes(accounting.getAccountSequenceNumber()));
        accountAssignmentType.setAccountAssignmentNumber(accountAssignmentNumberType);
        accountAssignmentType.setWBSElement(accounting.getWbs());
        accountAssignmentType.setCostCenter(accounting.getCostCentre());
        accountAssignmentType.setInternalOrder(accounting.getInternalOrder());
        accountAssignmentType.setGLAccount(accounting.getGlAccount());
        accountAssignmentType.setFund(accounting.getFund());
        accountAssignmentType.setMultipleAcctAssgmtDistrPercent(accounting.getDistribution());
        return accountAssignmentType;
    }

}
