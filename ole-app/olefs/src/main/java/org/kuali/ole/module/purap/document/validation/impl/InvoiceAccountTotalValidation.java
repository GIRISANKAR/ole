/*
 * Copyright 2009 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.ole.module.purap.document.validation.impl;

import org.kuali.ole.module.purap.PurapConstants;
import org.kuali.ole.module.purap.PurapKeyConstants;
import org.kuali.ole.module.purap.businessobject.InvoiceItem;
import org.kuali.ole.module.purap.businessobject.PurApAccountingLine;
import org.kuali.ole.select.businessobject.OleInvoiceItem;
import org.kuali.ole.sys.document.validation.GenericValidation;
import org.kuali.ole.sys.document.validation.event.AttributedDocumentEvent;
import org.kuali.rice.core.api.util.type.KualiDecimal;
import org.kuali.rice.krad.util.GlobalVariables;

public class InvoiceAccountTotalValidation extends GenericValidation {

    private InvoiceItem itemForValidation;

    /**
     * Verifies account percent. If the total percent does not equal 100, the validation fails.
     */
    @Override
    public boolean validate(AttributedDocumentEvent event) {
        boolean valid = true;

        // validate that the amount total
        KualiDecimal totalAmount = KualiDecimal.ZERO;
        KualiDecimal desiredAmount =
                (itemForValidation.getTotalAmount() == null) ? new KualiDecimal(0) : itemForValidation.getTotalAmount();

        KualiDecimal prorateSurcharge = KualiDecimal.ZERO;
        OleInvoiceItem invoiceItem = (OleInvoiceItem) itemForValidation;
        if (invoiceItem.getItemType().isQuantityBasedGeneralLedgerIndicator() && invoiceItem.getExtendedPrice() != null && invoiceItem.getExtendedPrice().compareTo(KualiDecimal.ZERO) != 0) {
            if (invoiceItem.getItemSurcharge() != null && invoiceItem.getItemTypeCode().equals("ITEM")) {
                prorateSurcharge = new KualiDecimal(invoiceItem.getItemSurcharge()).multiply(invoiceItem.getItemQuantity());
            }
            desiredAmount = desiredAmount.subtract(prorateSurcharge);
        }

        for (PurApAccountingLine account : itemForValidation.getSourceAccountingLines()) {
            if (account.getAmount() != null) {
                totalAmount = totalAmount.add(account.getAmount());
            } else {
                totalAmount = totalAmount.add(KualiDecimal.ZERO);
            }
        }

        if (desiredAmount.compareTo(totalAmount) != 0) {
            GlobalVariables.getMessageMap().putError(PurapConstants.ITEM_TAB_ERROR_PROPERTY, PurapKeyConstants.ERROR_ITEM_ACCOUNTING_TOTAL_AMOUNT, itemForValidation.getItemIdentifierString(), desiredAmount.toString());
            valid = false;
        }


        return valid;
    }

    public InvoiceItem getItemForValidation() {
        return itemForValidation;
    }

    public void setItemForValidation(InvoiceItem itemForValidation) {
        this.itemForValidation = itemForValidation;
    }

}
