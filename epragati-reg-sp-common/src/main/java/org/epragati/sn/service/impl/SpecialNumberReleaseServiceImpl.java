package org.epragati.sn.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.epragati.exception.BadRequestException;
import org.epragati.sn.dao.SpecialNumberDetailsDAO;
import org.epragati.sn.dto.SpecialNumberDetailsDTO;
import org.epragati.sn.service.SpecialNumersReleasingService;
import org.epragati.sn.service.SpecialPremiumNumberService;
import org.epragati.util.BidStatus;
import org.epragati.util.SourceUtil;
import org.epragati.util.payment.ModuleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author naga.pulaparthi
 *
 */
@Service
public class SpecialNumberReleaseServiceImpl implements SpecialNumersReleasingService{

	private static final Logger logger = LoggerFactory.getLogger(SpecialNumberReleaseServiceImpl.class);

	@Autowired
	private SpecialNumberDetailsDAO specialNumberDetailsDAO;

	
	@Autowired
	private SpecialPremiumNumberService specialPremiumNumberService;
	
	
	
	private List<String> errors;
	@Override
	
	public List<String> clrearSpPaymentPendingRecords() {
		logger.info("Start clrearSpPaymentPendingRecords()");
		errors =new ArrayList<>();
		List<SpecialNumberDetailsDTO> specialNumberPendingList = specialNumberDetailsDAO.findByBidStatus(BidStatus.SPPAYMENTPENDING);
		for(SpecialNumberDetailsDTO specialNumberDetailsDTO : specialNumberPendingList){
			try{
				specialPremiumNumberService.processToverifyPayments(specialNumberDetailsDTO.getSpecialNumberAppId(),ModuleEnum.SPNR,SourceUtil.SCHEDULER.getName());
			}catch (BadRequestException e) {
				errors.add(e.getMessage());
				logger.error("Exception while clrearFinalpaymentPendingRecords : {}",e.getMessage());
			}
			catch (Exception e) {
				errors.add(e.getMessage());
				logger.error("Exception while clrearFinalpaymentPendingRecords : {}",e);
			}
		}
		logger.info("End clrearSpPaymentPendingRecords()");
		return errors;
	}

	@Override
	public List<String>  clrearFinalpaymentPendingRecords()
	{   
		logger.info("Start clrearFinalpaymentPendingRecords()");
		errors =new ArrayList<>();
		List<SpecialNumberDetailsDTO> finalPaymentPendingList = specialNumberDetailsDAO.findByBidStatus(BidStatus.FINALPAYMENTPENDING);
		for(SpecialNumberDetailsDTO specialNumberDetailsDTO : finalPaymentPendingList){
			try{
				specialPremiumNumberService.processToverifyPayments(specialNumberDetailsDTO.getSpecialNumberAppId(),ModuleEnum.SPNB,SourceUtil.SCHEDULER.getName());
			}
			catch (BadRequestException e) {
				errors.add(e.getMessage());
				logger.error("Exception while clrearFinalpaymentPendingRecords : {}",e.getMessage());
			}
			catch (Exception e) {
				errors.add(e.getMessage());
				logger.error("Exception while clrearFinalpaymentPendingRecords : {}",e);
			}
		}
		logger.info("End clrearFinalpaymentPendingRecords()");
		return errors;
	}


}
