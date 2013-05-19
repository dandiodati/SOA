/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.svcmeta;

// jdk imports

// third party imports

// nightfire imports
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.message.util.xml.ParsedXPath;

/**
 * Contains XPaths for use in loading persisted services
 */
public class BuildPaths
{
    // basic paths
    public final ParsedXPath idPath;
    public final ParsedXPath displayNamePath;
    public final ParsedXPath historyTitlePath;
    public final ParsedXPath fullNamePath;
    public final ParsedXPath helpPath;
    public final ParsedXPath defaultPath;
    public final ParsedXPath valuePath;
    public final ParsedXPath refPath;


    public final ParsedXPath actionsPath;
    public final ParsedXPath includesPath;
    public final ParsedXPath guiDefsPath;
    public final ParsedXPath messageTypesPath;



    // component paths
    public final ParsedXPath minOccursPath;
    public final ParsedXPath maxOccursPath;
    public final ParsedXPath defaultSupplierPath;
    public final ParsedXPath allowableActionsPath;
    public final ParsedXPath allowableActionsTypePath;
    public final ParsedXPath userCreatablePath;
    public final ParsedXPath infoFieldsPath;
    public final ParsedXPath summaryFieldsPath;
    public final ParsedXPath confirmationFieldsPath;
    public final ParsedXPath detailFieldsPath;
    public final ParsedXPath ackDetailFieldsPath;
    public final ParsedXPath txSummaryFieldsPath;
    public final ParsedXPath ackTxSummaryFieldsPath;
    public final ParsedXPath titleFieldsPath;
    public final ParsedXPath appendQueryDataPath;
    public final ParsedXPath allowableMsgTypesPath;
    public final ParsedXPath convertToUTC_TZ_TSFsPath;
    public final ParsedXPath convertToUTC_TZ_AckTSFsPath;
    public final ParsedXPath convert_TZ_TSFsPath;
    public final ParsedXPath timezone_To_Convert_TZ_TSFsPath;
    public final ParsedXPath historyDetailFieldsPath;


    public final ParsedXPath historySummaryFieldsPath;
    public final ParsedXPath historyQueryFieldsPath;

    // action paths
    public final ParsedXPath checkStatePath;
    public final ParsedXPath editRequiredPath;
    public final ParsedXPath submissionInfoPath;
    public final ParsedXPath disableWithoutLockPath;
    public final ParsedXPath allowInEmptyStatePath;
    public final ParsedXPath allowImportPath;
    public final ParsedXPath controllingPermissionPath;
    public final ParsedXPath serviceTypeToConvertPath;
    public final ParsedXPath controllingQueryPath;
    public final ParsedXPath conversionRequiredPath;
    public final ParsedXPath displaySuffixPath;
    public final ParsedXPath tpAliasMethodPath;
    public final ParsedXPath removeNodesPath;
    public final ParsedXPath uploadExcelPath;


    // field paths
    public final ParsedXPath abbreviationPath;
    public final ParsedXPath dataTypePath;
    public final ParsedXPath customPath;
    public final ParsedXPath customNamePath;
    public final ParsedXPath customValuePath;

    // data type paths
    public final ParsedXPath minLengthPath;
    public final ParsedXPath maxLengthPath;
    public final ParsedXPath usagePath;
    public final ParsedXPath formatPath;
    public final ParsedXPath examplesPath;
    public final ParsedXPath enumPath;
    public final ParsedXPath baseTypePath;
    public final ParsedXPath optionValuesPath;
    public final ParsedXPath displayValuesPath;
    public final ParsedXPath descriptionsPath;

    public final ParsedXPath testConditionPath;

    public final ParsedXPath messageTypeResultPath;
    public final ParsedXPath redirectPagePath;
    public final ParsedXPath newWinWidthPath;
    public final ParsedXPath newWinHeightPath;

    public final ParsedXPath keyPath;



   // bundle / build paths
    public final ParsedXPath bundleRootPath;
    public final ParsedXPath metaDataNamePath;
    public final ParsedXPath componentsPath;

    public final ParsedXPath modifiersInfoPath;
    public final ParsedXPath svcDefsPath;
    public final ParsedXPath historyInfoPath;


    // service def and service group def  paths
    public final ParsedXPath serviceRootPath;
    public final ParsedXPath stateDefPath;
    public final ParsedXPath subTypePath;
    public final ParsedXPath subTypeOrderPath;
    public final ParsedXPath inflightSupplierPath;
    public final ParsedXPath inflightVersionPath;
    public final ParsedXPath uomSupportPath;
    public final ParsedXPath hideSaveForMsgTypePath;
    public final ParsedXPath hideValidateForMsgTypePath;
    public final ParsedXPath importOverwriteSupportedPath;

    // service group / build paths
    public final ParsedXPath serviceGroupsPath;
    public final ParsedXPath servicesPath;
    public final ParsedXPath anti997SuppliersPath;
    public final ParsedXPath show997SuppliersPath;
    public final ParsedXPath totalRowCountSupporterSuppliersPath;
    public final ParsedXPath modifyPermissionPath;
    public final ParsedXPath updateRequestFieldSupportPath;
    public final ParsedXPath validateRequiredPath;
    public final ParsedXPath saveRequiredPath;
    public final ParsedXPath templateSupportPath;
    public final ParsedXPath display997SupportPath;
    public final ParsedXPath abandonActionSupportPath;
    public final ParsedXPath suspendResumeSupportPath;
    public final ParsedXPath lockingPath;
    public final ParsedXPath historyPagingPath;
    public final ParsedXPath printingPath;
    public final ParsedXPath autoIncrementVerPath;
    public final ParsedXPath autoIncrementVerOnNewOrderPath;
    public final ParsedXPath customerUseSupportPath;
    public final ParsedXPath customerUseReadOnlyPath;
    public final ParsedXPath falloutPermissionPath;

    // queryField paths
    public final ParsedXPath queryTypeFieldPath;

    /**
     * Constructor
     * @throws FrameworkException on error
     */
    public BuildPaths() throws FrameworkException
    {
        // basic paths
        idPath = new ParsedXPath("@name");
        displayNamePath = new ParsedXPath("DisplayName/@value");
        historyTitlePath = new ParsedXPath("HistoryTitle/@value");
        fullNamePath = new ParsedXPath("FullName/@value");
        helpPath = new ParsedXPath("Help");
        defaultPath = new ParsedXPath("@default");
        valuePath = new ParsedXPath("@value");
        refPath = new ParsedXPath("@ref");


        actionsPath = new ParsedXPath("/*/Action");
        messageTypesPath = new ParsedXPath("/*/MessageType");
        includesPath = new ParsedXPath("/*/include/@location");
        guiDefsPath = new ParsedXPath("/*/GUIDefinitions/*");



        // component paths
        minOccursPath = new ParsedXPath("MinOccurs/@value");
        maxOccursPath = new ParsedXPath("MaxOccurs/@value");
        defaultSupplierPath = new ParsedXPath("Supplier/@value");
        allowableActionsPath = new ParsedXPath("AllowableActions/Name/@value");
        allowableActionsTypePath = new ParsedXPath("AllowableActions/@type");
        userCreatablePath = new ParsedXPath("UserCreatable/@value");
        allowableMsgTypesPath = new ParsedXPath("AllowableMessageTypes/Name");
        infoFieldsPath = new ParsedXPath("InfoFields/Name/@value");
        summaryFieldsPath = new ParsedXPath("SummaryFields/Name/@value");
        confirmationFieldsPath = new ParsedXPath("ConfirmationFields/Name/@value");
        detailFieldsPath = new ParsedXPath("DetailFields/Name/@value");
        ackDetailFieldsPath = new ParsedXPath("ACKDetailFields/Name/@value");
        txSummaryFieldsPath = new ParsedXPath("TransactionSummaryFields/Name/@value");
        historyDetailFieldsPath = new ParsedXPath("HistoryDetailFields/Name/@value");
        ackTxSummaryFieldsPath = new ParsedXPath("AckTransactionSummaryFields/Name/@value");
        titleFieldsPath = new ParsedXPath("TitleFields/Name/@value");
        appendQueryDataPath = new ParsedXPath("AppendQueryData/@value");
        convertToUTC_TZ_TSFsPath = new ParsedXPath("ConvertToUTC_TZ_TSFs");
        convertToUTC_TZ_AckTSFsPath = new ParsedXPath("ConvertToUTC_TZ_AckTSFs");
        convert_TZ_TSFsPath = new ParsedXPath("Convert_TZ_TSFs");

        timezone_To_Convert_TZ_TSFsPath = new ParsedXPath("Convert_TZ_TSFs/@value");

        historySummaryFieldsPath = new ParsedXPath("SummaryFields/Name");
        historyQueryFieldsPath = new ParsedXPath("QueryFields/Name");

        // action paths
        checkStatePath = new ParsedXPath("CheckState/@value");
        editRequiredPath = new ParsedXPath("EditRequired/@value");
        submissionInfoPath = new ParsedXPath("SubmissionInfo");
        disableWithoutLockPath = new ParsedXPath("DisableWithoutLock/@value");
        allowInEmptyStatePath = new ParsedXPath("AllowInEmptyState/@value");
        allowImportPath    = new ParsedXPath("ImportRequired/@value");
        controllingPermissionPath = new ParsedXPath("ControllingPermission/@value");
        serviceTypeToConvertPath = new ParsedXPath("ServiceTypeToConvert/@value");
        controllingQueryPath = new ParsedXPath("ControllingQuery/@value");
        conversionRequiredPath = new ParsedXPath("ConversionRequired/@value");
        displaySuffixPath = new ParsedXPath("DisplaySuffix/@value");
        tpAliasMethodPath = new ParsedXPath("TPAliasMethod/@value");
        removeNodesPath = new ParsedXPath("RemoveNodes");

        // field paths
        abbreviationPath = new ParsedXPath("Abbreviation/@value");
        dataTypePath = new ParsedXPath("DataType");
        customPath = new ParsedXPath("CustomValue");
        customNamePath = idPath;
        customValuePath = valuePath;


        // data type paths
        minLengthPath = new ParsedXPath("MinLen/@value");
        maxLengthPath = new ParsedXPath("MaxLen/@value");
        usagePath = new ParsedXPath("Usage/@value");
        formatPath = new ParsedXPath("Format/@value");
        examplesPath = new ParsedXPath("Example/@value");
        enumPath = new ParsedXPath("Options");
        baseTypePath = new ParsedXPath("BaseType/@value");
        optionValuesPath = new ParsedXPath("Option/OptionValue/@value");
        displayValuesPath = new ParsedXPath("Option/DisplayName/@value");
        descriptionsPath = new ParsedXPath("Option/OptionHelp/@value");

        //action specific
        messageTypeResultPath = new ParsedXPath("MessageTypeResult/@value");
        keyPath = new ParsedXPath("Key/@value");

        // custom page to redirect to for an action
        redirectPagePath = new ParsedXPath("RedirectPage/@value");
        // indicates that this action should be displayed in a new window
        newWinWidthPath = new ParsedXPath("NewWindow/@width");
        newWinHeightPath = new ParsedXPath("NewWindow/@height");
        // message types
        testConditionPath = new ParsedXPath("TestCondition/@value");


        // bundle related paths
        bundleRootPath = new ParsedXPath("/BundleDef/Bundle");
        metaDataNamePath = new ParsedXPath("MetaDataName/@value");
        componentsPath = new ParsedXPath("SvcComponent");
        // global service comp defintiions
        svcDefsPath = new ParsedXPath("/*/SvcComponent");
        modifiersInfoPath = new ParsedXPath("Modifiers");
        historyInfoPath = new ParsedXPath("History");



        // service / build paths
        serviceRootPath = new ParsedXPath("/ServiceDef/Service");
        // service paths
        stateDefPath = new ParsedXPath("StateDef/@value");
        subTypePath = new ParsedXPath("SubType/@value");
        subTypeOrderPath = new ParsedXPath("SubType/@order");
        inflightSupplierPath = new ParsedXPath("InflightSupplier/@value");
        inflightVersionPath = new ParsedXPath("InflightVersion/@value");
        hideSaveForMsgTypePath = new ParsedXPath("HideSaveForMsgType");
        hideValidateForMsgTypePath = new ParsedXPath("HideValidateForMsgType");
        importOverwriteSupportedPath = new ParsedXPath("ImportOverwriteSupported/@value");

        // service group defintiions
        serviceGroupsPath = new ParsedXPath("/ServiceGroupDef/ServiceGroup");
        servicesPath = new ParsedXPath("Services/Name/@value");
        anti997SuppliersPath = new ParsedXPath("Hide997Support/Name/@value") ;
        show997SuppliersPath = new ParsedXPath("Show997Support/Name/@value") ;
        totalRowCountSupporterSuppliersPath = new ParsedXPath("TotalRowCountSupport/Name/@value") ;
        modifyPermissionPath = new ParsedXPath("ModifyPermission/@value");
        updateRequestFieldSupportPath = new ParsedXPath("UpdateRequestFieldSupport/@value");
        validateRequiredPath = new ParsedXPath("ValidateRequired/@value");
        saveRequiredPath = new ParsedXPath("SaveRequired/@value");
        templateSupportPath = new ParsedXPath("TemplateSupport/@value");
        display997SupportPath = new ParsedXPath("Display997Support/@value");
        uomSupportPath = new ParsedXPath("UOMSupport/@value");
        abandonActionSupportPath = new ParsedXPath("AbandonActionSupport/@value");
        suspendResumeSupportPath = new ParsedXPath("SuspendResumeSupport/@value");
        customerUseSupportPath = new ParsedXPath("CustomerUseSupport/@value");
        customerUseReadOnlyPath = new ParsedXPath("CustomerUseSupport/@readOnly");
        autoIncrementVerPath = new ParsedXPath("AutoIncrementVer/@value");
        autoIncrementVerOnNewOrderPath = new ParsedXPath("AutoIncrementVerOnNewOrder/@value");
        lockingPath = new ParsedXPath("LockingSupport/@value");
        historyPagingPath = new ParsedXPath("HistoryPagingSupport/@value");
        printingPath = new ParsedXPath("PrintingSupport/@value");
        falloutPermissionPath = new ParsedXPath("FalloutPermission/@value");
        uploadExcelPath = new ParsedXPath("UploadExcelSupport/@value");

        // query field paths
        queryTypeFieldPath = new ParsedXPath("@typeField");

    }
}