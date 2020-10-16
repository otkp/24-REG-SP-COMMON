package org.epragati.sn.payment.service;

import org.epragati.util.payment.PayStatusEnum;

public class PaymentUpdateDetails {

	private PayStatusEnum payStatus;
	private String appNumberAppId;
	private String transactionNo;
	private String paymentId;

	public PayStatusEnum getPayStatus() {
		return payStatus;
	}

	public void setPayStatus(PayStatusEnum payStatus) {
		this.payStatus = payStatus;
	}

	public String getAppNumberAppId() {
		return appNumberAppId;
	}

	public void setAppNumberAppId(String appNumberAppId) {
		this.appNumberAppId = appNumberAppId;
	}

	public String getTransactionNo() {
		return transactionNo;
	}

	public void setTransactionNo(String transactionNo) {
		this.transactionNo = transactionNo;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

}
