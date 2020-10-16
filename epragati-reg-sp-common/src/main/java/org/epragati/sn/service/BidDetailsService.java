package org.epragati.sn.service;

import java.util.List;
import java.util.Optional;

import org.epragati.sn.dto.SpecialNumberDetailsDTO;
import org.epragati.sn.payment.service.PaymentUpdateDetails;
import org.epragati.sn.vo.BidConfigMasterVO;
import org.epragati.sn.vo.BidHistoryVo;
import org.epragati.sn.vo.BidIncrementalAmountInput;
import org.epragati.sn.vo.BindFinalAmountInput;
import org.epragati.sn.vo.SearchPaymentStatusVO;
import org.epragati.sn.vo.SpecialNumberDetailsVo;

public interface BidDetailsService {
	
	Optional<SpecialNumberDetailsVo> getByTrNumberAndPasscode(String trNumber, String mobileNo, String passcode);
	
	Optional<SpecialNumberDetailsVo> getBidderDetails(String specialNumberAppId);
	
	Optional<Long> isBidDurationInBetween();
	
	List<BidHistoryVo> addBidIncrementalAmount(BidIncrementalAmountInput input);
	
	void clearBidIncrementalAmount(String spAppId);
	
	void doBidLeft(BindFinalAmountInput input);
	
	SpecialNumberDetailsDTO doBidFinalPay(BindFinalAmountInput input);	
	
	void updateFinalBidPaymentStatus(PaymentUpdateDetails paymentUpdateDetails);

	BidConfigMasterVO isSpecialNumberRegistrationDurationInBetween();

	SpecialNumberDetailsDTO doBidFinalRepay(String specialNumberAppId);

/**
 * 
 * @param trNo
 * @return
 */
	SearchPaymentStatusVO searchPaymentStatus(String trNo);
	
	Optional<SpecialNumberDetailsVo> getByTrNumberAndMobileNo(String trNumber, String mobileNo);

	/**
	 * 
	 * @param trNo
	 * @param chassisNumber
	 * @param mobile
	 * @return
	 */
	String getByTrNoAndChassisNumberAndMobile(String trNo, String chassisNumber, String mobile,boolean isPrNo);

	/**
	 * 
	 * @param trNo
	 * @param chassisNumber
	 * @param mobile
	 * @param ip String trNumber,
			String mobileNo) {
	 * @return
	 */
	void specialNumberAlterationProcess(String trNo, String chassisNumber, String mobile, String ip,boolean isPrNo);
	
}
