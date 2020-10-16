package org.epragati.sn.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.epragati.sn.dto.SpecialNumberDetailsDTO;
import org.epragati.sn.numberseries.dto.PRPoolDTO;
import org.epragati.sn.payment.service.PaymentUpdateDetails;
import org.epragati.sn.vo.BidConfigMasterVO;
import org.epragati.sn.vo.LeftOverVO;
import org.epragati.sn.vo.NumberSeriesDetailsVO;
import org.epragati.sn.vo.NumberSeriesSelectionInput;
import org.epragati.sn.vo.SpecialFeeAndNumberDetailsVO;
import org.epragati.sn.vo.SpecialNumberDetailsVo;
import org.epragati.util.payment.ModuleEnum;
import org.epragati.util.payment.PayStatusEnum;

public interface SpecialPremiumNumberService {

	SpecialFeeAndNumberDetailsVO getNumberSerialsByTrNumber(String trNumber, String mobileNo,
			BidConfigMasterVO bidConfigMaster,boolean isPrNo,String rang, String seriesId);

	SpecialNumberDetailsDTO doSpecialPremiumPay(NumberSeriesSelectionInput input,BidConfigMasterVO bidConfigMaster);

	Optional<String>  updateSpecialNumberPaymentStatus(PaymentUpdateDetails paymentUpdateDetails);

	SpecialNumberDetailsDTO doSpecialPremiumRepay(String specialNumberAppId);
	
	void resendPassCodeAlert(String trNo,String mobileNo);
	
	Optional<String> viewPassCode(String trNo,String mobileNo);
	
	PayStatusEnum processToverifyPayments(String specialNumberAppId,ModuleEnum module,String source);
	
	Integer getParticipantsCount(String bidNumberDtlsId);
	
	Set<String> getListOfLeftOverNumberSeries(String trNumber, String mobileNo, boolean isPrNo,BidConfigMasterVO bidConfigMaster);
	
	List<LeftOverVO> getListOfLeftOverNumbers(String trOrPrNumber, String mobileNo, String prSeries,boolean isPrNo,BidConfigMasterVO bidConfigMaster); 
	
	void handleTrExpiredRecords();
	
	void validateRequest(String authToken, String ip);
	
	List<NumberSeriesDetailsVO> getNumberRangByTROrPR(String trNumber, String mobileNo,BidConfigMasterVO bidConfigMaster,boolean isPrNo);
	
	void doBidWinnerProcess(PRPoolDTO prPoolDTO, SpecialNumberDetailsDTO specialNumberDetailsDTO);
	
	List<SpecialNumberDetailsVo> searchPaymentStatusBySelectedPrSeries(String selectedPrSeries);
}
