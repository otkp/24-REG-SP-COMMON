package org.epragati.sn.payment.service;

import java.util.Optional;

import org.epragati.payments.vo.PaymentGateWayResponse;
import org.epragati.payments.vo.PaymentReqParams;
import org.epragati.payments.vo.TransactionDetailVO;

public interface SnPaymentGatewayService {

	void prepareRequestObject(TransactionDetailVO transactionDetailVO);

	PaymentGateWayResponse processResponse(PaymentGateWayResponse paymentGateWayResponse);

	PaymentGateWayResponse processVerify(String appFormID,String transactionNo);

	PaymentReqParams convertPayments(TransactionDetailVO vo, String appFormNo);

	Optional<String>  updatePaymentStatus(PaymentGateWayResponse response);

	Optional<String> processRefundByPaymentId(String transactionNo, String paymentId, Double refundAmount);
	
	void verifyAllPaymentFailureRecords();
	
	PaymentGateWayResponse VerifySpecific(String merchantTransactionIds);
}
