package org.epragati.sn.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.epragati.cache.CacheData;
import org.epragati.common.vo.PrGenerationInput;
import org.epragati.constants.CommonConstants;
import org.epragati.exception.BadRequestException;
import org.epragati.master.dao.GeneratedPrDetailsDAO;
import org.epragati.master.dao.HolidayDAO;
import org.epragati.master.dao.StagingRegistrationDetailsDAO;
import org.epragati.master.dao.StagingRegistrationDetailsHistoryLogDAO;
import org.epragati.master.dto.GeneratedPrDetailsDTO;
import org.epragati.master.dto.HolidayDTO;
import org.epragati.master.dto.StagingRegistrationDetailsDTO;
import org.epragati.master.dto.StagingRegistrationDetailsHistoryLogDto;
import org.epragati.master.service.LogMovingService;
import org.epragati.master.service.PrSeriesService;
import org.epragati.sn.dao.SpecialNumberDetailsDAO;
import org.epragati.sn.dto.BidFinalDetails;
import org.epragati.sn.dto.BidHistory;
import org.epragati.sn.dto.SpecialNumberDetailsDTO;
import org.epragati.sn.mappers.BidHistoryMapper;
import org.epragati.sn.mappers.SpecialNumberDetailsMapper;
import org.epragati.sn.numberseries.dao.PRPoolDAO;
import org.epragati.sn.numberseries.dao.PrimesNumbersDAO;
import org.epragati.sn.numberseries.dto.PRPoolDTO;
import org.epragati.sn.payment.service.PaymentUpdateDetails;
import org.epragati.sn.service.BidDetailsService;
import org.epragati.sn.service.VehicleDetailsService;
import org.epragati.sn.vo.BidAlterDetailsVO;
import org.epragati.sn.vo.BidConfigMasterVO;
import org.epragati.sn.vo.BidHistoryVo;
import org.epragati.sn.vo.BidIncrementalAmountInput;
import org.epragati.sn.vo.BindFinalAmountInput;
import org.epragati.sn.vo.SearchPaymentStatusVO;
import org.epragati.sn.vo.SpecialNumberDetailsVo;
import org.epragati.util.BidNumberType;
import org.epragati.util.BidStatus;
import org.epragati.util.DateUtils;
import org.epragati.util.NumberPoolStatus;
import org.epragati.util.StatusRegistration;
import org.epragati.util.payment.ModuleEnum;
import org.epragati.util.payment.PayStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BidDetailsServiceImpl implements BidDetailsService {

	@Autowired
	private SpecialNumberDetailsDAO specialNumberDetailsDAO;

	@Autowired
	private SpecialNumberDetailsMapper specialNumberDetailsMapper;

	@Autowired
	private DateUtils dateUtils;

	@Autowired
	private ActionsDetailsHelper actionsDetailsHelper;

	@Autowired
	private BidHistoryMapper bidHistoryMapper;

	@Autowired
	private VehicleDetailsService vehicleDetailsService;

	@Autowired
	private StagingRegistrationDetailsDAO stagingRegistrationDetailsDAO;

	@Autowired
	private PRPoolDAO pRPoolDAO;

	@Autowired
	private LogMovingService logMovingService;
	@Autowired
	private VehicleDetailsServiceImpl vehicleDetails;
	@Autowired
	private GeneratedPrDetailsDAO generatedPrDetailsDAO;
	
	@Autowired
	PrimesNumbersDAO primesNumbersDAO;
	
	@Autowired
	private HolidayDAO holidayDAO;
	
	@Autowired
	private PrSeriesService prSeriesService;

	@Override
	public Optional<SpecialNumberDetailsVo> getBidderDetails(String specialNumberAppId) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(specialNumberAppId);

		return specialNumberDetailsMapper.convertEntity(entityOptional);
	}

	@Override
	public Optional<SpecialNumberDetailsVo> getByTrNumberAndPasscode(String trNumber, String mobileNo,
			String passcode) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findByVehicleDetailsTrNumberAndCustomerDetailsMobileNoAndPasscodeOrderByCreatedDateDesc(trNumber,
						mobileNo, passcode);

		return specialNumberDetailsMapper.convertEntity(entityOptional);
	}

	@Override
	public Optional<Long> isBidDurationInBetween() {
		Optional<BidConfigMasterVO> bidConfigMasterOptional = prSeriesService.getBidConfigMasterData(Boolean.TRUE);

		if (bidConfigMasterOptional.isPresent()) {

			if (bidConfigMasterOptional.get().getBidStartTime().isBefore(LocalTime.now())
					&& bidConfigMasterOptional.get().getBidEndTime().isAfter(LocalTime.now())) {

				return Optional.of(dateUtils.diff(bidConfigMasterOptional.get().getBidStartTime(),
						bidConfigMasterOptional.get().getBidEndTime()).getSeconds());
			}
		}
		return Optional.empty();
	}

	@Override
	public BidConfigMasterVO isSpecialNumberRegistrationDurationInBetween() {	
		
		Optional<HolidayDTO> holiday =  holidayDAO.findByHolidayDateAndModuleAndHolidayStatusTrue(LocalDate.now(), ModuleEnum.SPNR);
		final String EXCEPTION_MSG="Speical or Premium number registration closed today.";
		
		if(holiday.isPresent()){
			throw new BadRequestException(EXCEPTION_MSG);
		}
		Optional<BidConfigMasterVO> bidConfigMasterOptional = prSeriesService.getBidConfigMasterData(Boolean.TRUE);
		if (bidConfigMasterOptional.isPresent()) {
			DayOfWeek dow = LocalDate.now().getDayOfWeek();
			if(dow.getValue()==bidConfigMasterOptional.get().getDateValueToHoliday()) {
				throw new BadRequestException(EXCEPTION_MSG);
			}
			if (bidConfigMasterOptional.get().getSpecialNumberRegStartTime().isBefore(LocalTime.now())
					&& bidConfigMasterOptional.get().getSpecialNumberRegEndTime().isAfter(LocalTime.now())) {
				return bidConfigMasterOptional.get();
			}
		}
		throw new BadRequestException(EXCEPTION_MSG);
	}


	@Override
	public List<BidHistoryVo> addBidIncrementalAmount(BidIncrementalAmountInput input) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(input.getSpecialNumberAppId());

		if (!entityOptional.isPresent()) {
			throw new BadRequestException("No record found for Special Number Id: " + input.getSpecialNumberAppId());
		}
		SpecialNumberDetailsDTO entity = entityOptional.get();
		
		if(!LocalDate.now().equals(entity.getCreatedDate().toLocalDate())) {
			throw new BadRequestException("The bid competition already finalized ");
		}

		if (!entity.getBidStatus().getCode().equals(BidStatus.SPPAYMENTDONE.getCode()) ) {
			throw new BadRequestException("invalid status :" + entity.getBidStatus());
		}

		BidHistory bidHistory = new BidHistory();
		bidHistory.setBidAmount(input.getIncremntalAmount());
		bidHistory.setCreatedDatetime(LocalDateTime.now());
		bidHistory.setSecBidAmount(input.getIncremntalAmount());
		bidHistory.setIsFinalBid(false);

		if (entity.getBidHistory() == null) {
			entity.setBidHistory(Arrays.asList(bidHistory));
		} else {
			entity.getBidHistory().add(bidHistory);
		}

		specialNumberDetailsDAO.save(entity);

		return bidHistoryMapper.convertEntity(entity.getBidHistory());
	}
	
	@Override
	public void clearBidIncrementalAmount(String spAppId){
		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(spAppId);
		if (!entityOptional.isPresent()) {
			throw new BadRequestException("No record found for Special Number Id: " + spAppId);
		}
		SpecialNumberDetailsDTO entity = entityOptional.get();
		if(!LocalDate.now().equals(entity.getCreatedDate().toLocalDate())) {
			throw new BadRequestException("The bid competition already finalized " );
		}
		if (!entity.getBidStatus().getCode().equals(BidStatus.SPPAYMENTDONE.getCode()) ) {
			throw new BadRequestException("invalid status :" + entity.getBidStatus());
		}
		if(CollectionUtils.isNotEmpty(entity.getBidHistory())) {
			//toDO: neet move 
			entity.getBidHistory().clear();
			specialNumberDetailsDAO.save(entity);
		}
	}

	@Override
	public void doBidLeft(BindFinalAmountInput input) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(input.getSpecialNumberAppId());
		if (!entityOptional.isPresent()) {
			throw new BadRequestException("No record found for Special Number Id: " + input.getSpecialNumberAppId());
		}
		SpecialNumberDetailsDTO entity = entityOptional.get();
		doBidLeft(entity,input.getIsProductionIssues());

	}

	private void doBidLeft(SpecialNumberDetailsDTO entity, boolean b) {
		if(!b) {
			beforeBidLeftValidations(entity);
		}
		entity.setBidStatus(BidStatus.BIDLEFT);
		entity.getBidVehicleDetails().setBidLeftAccepted(Boolean.TRUE);
		actionsDetailsHelper.updateActionsDetails(entity, entity.getVehicleDetails().getApplicationNumber());
		specialNumberDetailsDAO.save(entity);
		vehicleDetailsService.generatePRNumber(entity, StringUtils.EMPTY);
	}

	private void beforeBidLeftValidations(SpecialNumberDetailsDTO entity) {

		if (entity.getBidStatus().getCode().equals(BidStatus.BIDLEFT.getCode())) {
			throw new BadRequestException("You already left and current status :" + entity.getBidStatus());
		}

		if (entity.getBidStatus().getCode().equals(BidStatus.BIDWIN.getCode())) {
			throw new BadRequestException("You already won the bid and current status :" + entity.getBidStatus());
		}

		if (entity.getBidStatus().getCode().equals(BidStatus.FINALPAYMENTDONE.getCode())) {
			throw new BadRequestException("Please wait for bid result and current status :" + entity.getBidStatus());
		}
	}

	@Override
	public SpecialNumberDetailsDTO doBidFinalPay(BindFinalAmountInput input) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(input.getSpecialNumberAppId());

		if (!entityOptional.isPresent()) {
			throw new BadRequestException("No record found for Special Number Id: " + input.getSpecialNumberAppId());
		}

		SpecialNumberDetailsDTO entity = entityOptional.get();

		if (!entity.getBidStatus().getCode().equals(BidStatus.SPPAYMENTDONE.getCode()) ) {
			throw new BadRequestException("Invalid status :" + entity.getBidStatus());
		}

		if (entity.getBidHistory() == null || entity.getBidHistory().isEmpty()) {
			throw new BadRequestException("No Biding is added");
		}
		BidHistory bidHistory = entity.getBidHistory().get(entity.getBidHistory().size() - 1);
		if (bidHistory.getBidAmount() <= entity.getSpecialNumberFeeDetails().getApplicationAmount()) {
			throw new BadRequestException("Bid amount Should be greater than number registration amount "
					+ entity.getSpecialNumberFeeDetails().getApplicationAmount());
		}

		entity.setBidStatus(BidStatus.FINALPAYMENTPENDING);

		bidHistory.setIsFinalBid(true);

		BidFinalDetails bidFinalDetails = new BidFinalDetails();
		bidFinalDetails.setBidAmount(bidHistory.getBidAmount());
		bidFinalDetails.setRtaAmount(entityOptional.get().getSpecialNumberFeeDetails().getApplicationAmount());
		bidFinalDetails.setTotalAmount(bidFinalDetails.getBidAmount() + bidFinalDetails.getRtaAmount());
		bidFinalDetails.setTransactionNo(CacheData.getPaymentTxidNo());

		entity.setBidFinalDetails(bidFinalDetails);

		actionsDetailsHelper.updateActionsDetails(entity, entity.getVehicleDetails().getApplicationNumber());

		specialNumberDetailsDAO.save(entity);

		return entity;
	}

	@Override
	public SpecialNumberDetailsDTO doBidFinalRepay(String specialNumberAppId) {
		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(specialNumberAppId);

		if (!entityOptional.isPresent()) {
			throw new BadRequestException("No record found");
		}

		SpecialNumberDetailsDTO specialNumberDetails = entityOptional.get();

		if (BidStatus.FINALPAYMENTFAILURE.getCode().equals(specialNumberDetails.getBidStatus().getCode())) {

			specialNumberDetails.setBidStatus(BidStatus.FINALPAYMENTPENDING);

			actionsDetailsHelper.updateActionsDetails(specialNumberDetails,
					specialNumberDetails.getVehicleDetails().getApplicationNumber());
			specialNumberDetails.getBidFinalDetails().setTransactionNo(CacheData.getPaymentTxidNo());

			return specialNumberDetailsDAO.save(specialNumberDetails);
		}

		throw new BadRequestException("Invalid bid status : " + specialNumberDetails.getBidStatus());
	}

	@Override
	public void updateFinalBidPaymentStatus(PaymentUpdateDetails paymentUpdateDetails) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(paymentUpdateDetails.getAppNumberAppId());

		if (!entityOptional.isPresent()) {
			return;
		}

		SpecialNumberDetailsDTO entity = entityOptional.get();

		if (isFinalBidPaymentDone(entity)) {
			return;
		}

		entity.getBidFinalDetails().setTransactionNo(paymentUpdateDetails.getTransactionNo());
		entity.getBidFinalDetails().setPaymentId(paymentUpdateDetails.getPaymentId());
		entity.setBidStatus(getFinalBidStatusFromPay(paymentUpdateDetails.getPayStatus()));

		actionsDetailsHelper.updateActionsDetails(entity, entity.getVehicleDetails().getApplicationNumber());

		if (BidStatus.FINALPAYMENTDONE.getCode().equals(entity.getBidStatus().getCode())) {

			// TODO:Send communication to clients

		} else if (BidStatus.FINALPAYMENTFAILURE.getCode().equals(entity.getBidStatus().getCode())) {
			//entity.getBidFinalDetails().setTransactionNo(null);
		}

		specialNumberDetailsDAO.save(entity);
	}

	private BidStatus getFinalBidStatusFromPay(PayStatusEnum payStatus) {
		BidStatus status = BidStatus.FINALPAYMENTPENDING;

		if (PayStatusEnum.SUCCESS.equals(payStatus)) {

			status = BidStatus.FINALPAYMENTDONE;
		} else if (PayStatusEnum.FAILURE.equals(payStatus)) {
			status = BidStatus.FINALPAYMENTFAILURE;
		} else if (PayStatusEnum.PENDING.equals(payStatus) || PayStatusEnum.PENDINGFROMBANK.equals(payStatus)) {
			status = BidStatus.FINALPAYMENTPENDING;
		}
		return status;
	}

	private boolean isFinalBidPaymentDone(SpecialNumberDetailsDTO entity) {
		if (entity.getActionsDetailsLog() == null || entity.getActionsDetailsLog().isEmpty()) {
			return false;
		}
		return entity.getActionsDetailsLog().stream()
				.anyMatch(status -> status.getAction().equalsIgnoreCase(BidStatus.FINALPAYMENTDONE.getDescription()));
	}

	@Override
	public SearchPaymentStatusVO searchPaymentStatus(String trNo) {

		Optional<SpecialNumberDetailsDTO> voOptional = specialNumberDetailsDAO
				.findByVehicleDetailsTrNumberOrderByCreatedDateDesc(trNo);
		if (!voOptional.isPresent()) {
			throw new BadRequestException("No record found :" + trNo);
		}

		SpecialNumberDetailsDTO specialNumberDetailsVo = voOptional.get();
		SearchPaymentStatusVO vo = new SearchPaymentStatusVO();
		vo.setApplicationNo(specialNumberDetailsVo.getSpecialNumberAppId());
		vo.setBidStatus(specialNumberDetailsVo.getBidStatus());
		vo.setName(specialNumberDetailsVo.getCustomerDetails().getFirstName());
		vo.setTrNo(specialNumberDetailsVo.getVehicleDetails().getTrNumber());

		return vo;

	}

	@Override
	public Optional<SpecialNumberDetailsVo> getByTrNumberAndMobileNo(String trNumber, String mobileNo) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findTopByVehicleDetailsTrNumberAndCustomerDetailsMobileNoOrderByCreatedDateDesc(trNumber, mobileNo);

		return specialNumberDetailsMapper.convertEntity(entityOptional);
	}

	@Override
	public String getByTrNoAndChassisNumberAndMobile(String trOrPrNumber, String chassisNumber, String mobile,boolean isPrNo) {
		
		checkBidClosingrunningStage();
		Optional<StagingRegistrationDetailsDTO> stagingOptional;
		if(isPrNo) {
			stagingOptional = stagingRegistrationDetailsDAO
				.findByOldPrNoAndApplicantDetailsContactMobile(trOrPrNumber, mobile);
		}else {
			stagingOptional= stagingRegistrationDetailsDAO
				.findByTrNoAndApplicantDetailsContactMobile(trOrPrNumber, mobile);
			
		}
		if (!stagingOptional.isPresent() || stagingOptional.get().getVahanDetails()==null ||
				!chassisNumber.equals(stagingOptional.get().getVahanDetails().getChassisNumber())) {
			throw new BadRequestException("No Record found");
		}
		
		if(stagingOptional.get().getIsTrExpired()!=null &&
				!stagingOptional.get().getIsTrExpired()){
			throw new BadRequestException("Your TR validity expired. You cann't be prerform further transactions.");
		}
		if (!stagingOptional.get().getSpecialNumberRequired()) {
			return BidStatus.SPREQUIRED.getDescription();

		}
		Optional<SpecialNumberDetailsVo> specialNumberDetailsVo = getByTrNumberAndMobileNo(trOrPrNumber, mobile);
		if (!specialNumberDetailsVo.isPresent()) {
			return BidStatus.SPNOTREQUIRED.getDescription();

		}
		if (Arrays.asList(BidStatus.BIDLOOSE, BidStatus.BIDABSENT, BidStatus.SPPAYMENTFAILURE, BidStatus.BIDTIE,
				BidStatus.SPPAYMENTFAILED).contains(specialNumberDetailsVo.get().getBidStatus())) {
			return BidStatus.BIDLEFT.getDescription();
		}

		throw new BadRequestException("Invalid special number registration status, current status: "
				+ specialNumberDetailsVo.get().getBidStatus());
	}

	@Override
	public void specialNumberAlterationProcess(String trOrPrNumber, String chassisNumber, String mobile, String ip,boolean isPrNo) {
		checkBidClosingrunningStage();
		
		Optional<StagingRegistrationDetailsDTO> stagingOptional;
		if(isPrNo) {
			stagingOptional = stagingRegistrationDetailsDAO
				.findByOldPrNoAndApplicantDetailsContactMobile(trOrPrNumber, mobile);
		}else {
			stagingOptional= stagingRegistrationDetailsDAO
				.findByTrNoAndApplicantDetailsContactMobile(trOrPrNumber, mobile);
			
		}
		if (!stagingOptional.isPresent() || stagingOptional.get().getVahanDetails()==null ||
				!chassisNumber.equals(stagingOptional.get().getVahanDetails().getChassisNumber())) {
			throw new BadRequestException("No Record found");
		}
		if(stagingOptional.get().getIsTrExpired()!=null &&
				!stagingOptional.get().getIsTrExpired()){
			throw new BadRequestException("Your TR validity expired. You cann't be prerform further transactions.");
		}
		if (stagingOptional.get().getBidAlterDetails() != null
				&& stagingOptional.get().getBidAlterDetails().size() > 0) {
			for (BidAlterDetailsVO alterStatus : stagingOptional.get().getBidAlterDetails()) {
				if (alterStatus.getChangeTo().equals(BidStatus.SPNOTREQUIRED)
						|| alterStatus.getChangeTo().equals(BidStatus.BIDLEFT)) {
					throw new BadRequestException("You are already perform the action on Convert to Ordinary Number");
				}
				if (LocalDate.now().equals(alterStatus.getChangeDate().toLocalDate()) ) {
					throw new BadRequestException("You are eligible for E-bidding,Please go for 'Registration for Special/Premium Number' option to select the special number.");
				}
			}
		}
		if (!stagingOptional.get().getSpecialNumberRequired()) {
			handlingSPREQUIRED(stagingOptional.get(), ip);
			return;

		}
		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findTopByVehicleDetailsTrNumberAndCustomerDetailsMobileNoOrderByCreatedDateDesc(trOrPrNumber, mobile);

		if (!entityOptional.isPresent()) {
			handlingSPNOTREQUIRED(stagingOptional.get(), ip);
			return;

		}
		if (Arrays.asList(BidStatus.BIDLOOSE, BidStatus.BIDABSENT, BidStatus.SPPAYMENTFAILURE, BidStatus.BIDTIE,
				BidStatus.SPPAYMENTFAILED).contains(entityOptional.get().getBidStatus())) {
			handlingBIDLEFT(stagingOptional.get(), entityOptional.get(), ip);
			return;
		}

		throw new BadRequestException(
				"Invalid special number registration status, current status: " + entityOptional.get().getBidStatus());
	}

	
	
	private void checkBidClosingrunningStage() {
		Optional<BidConfigMasterVO> bidConfigMasterOptional = prSeriesService.getBidConfigMasterData(Boolean.TRUE);
		if (bidConfigMasterOptional.isPresent() && bidConfigMasterOptional.get().isRunningBidClosingProcess()) {
			throw new BadRequestException("Bid finalize service is running stage.Can you please try this service later.");
		}
		
	}

	private void handlingSPREQUIRED(StagingRegistrationDetailsDTO stagingRegistrationDetailsDTO, String ip) {
		addBidAlterLogs(stagingRegistrationDetailsDTO, BidStatus.SPREQUIRED, ip);
		//TODO: will remove hard code by Anji
		if(CommonConstants.OTHER.equalsIgnoreCase(stagingRegistrationDetailsDTO.getOfficeDetails().getOfficeCode())) {
			throw new BadRequestException(
					"You are not eligible for special number selection. TR No: "
							+ stagingRegistrationDetailsDTO.getTrNo());
		}

		if (StringUtils.isNotBlank(stagingRegistrationDetailsDTO.getPrNo())) {
			Optional<PRPoolDTO> pRPoolopt = pRPoolDAO.findByPrNo(stagingRegistrationDetailsDTO.getPrNo());
			if (!pRPoolopt.isPresent()) {
				throw new BadRequestException(
						"No Record found in PRPool for PrNo " + stagingRegistrationDetailsDTO.getPrNo());
			}
			//			PRPoolDTO pRPoolDTO= pRPoolopt.get();
			//			//pRPoolDTO.setPoolStatus(NumberPoolStatus.REOPEN);
			//			//actionsDetailsHelper.updateActionsDetails(pRPoolDTO, stagingRegistrationDetailsDTO.getApplicationNo());
			List<GeneratedPrDetailsDTO> generatedPrDetails = generatedPrDetailsDAO
					.findByPrNo(stagingRegistrationDetailsDTO.getPrNo());
			//			if (generatedPrDetails.size() > 1) {
			//				throw new BadRequestException("Generated Pr detais found in more than one prNo:"
			//						+ stagingRegistrationDetailsDTO.getPrNo() );
			//			}
			if (generatedPrDetails.size() == 1) {
				GeneratedPrDetailsDTO generatedPrDetailsDTO=generatedPrDetails.get(0);
				generatedPrDetailsDTO.setStatus(NumberPoolStatus.REOPEN.getDescription());
				generatedPrDetailsDTO.setlUpdate(LocalDateTime.now());
				generatedPrDetailsDAO.save(generatedPrDetailsDTO);
			}
			//			pRPoolDAO.save(pRPoolDTO);
		}
		stagingRegistrationDetailsDTO.setPrNo(null);
		stagingRegistrationDetailsDTO.setSpecialNumberRequired(Boolean.TRUE);
		stagingRegistrationDetailsDTO.setPrType(BidNumberType.S.getCode());
		if(StatusRegistration.PRNUMBERPENDING.getDescription().equals(stagingRegistrationDetailsDTO.getApplicationStatus())) {
			stagingRegistrationDetailsDTO.setApplicationStatus(StatusRegistration.SPECIALNOPENDING.getDescription());
		}
		saveStagingData(stagingRegistrationDetailsDTO);

	}

	private void handlingSPNOTREQUIRED(StagingRegistrationDetailsDTO stagingRegistrationDetailsDTO, String ip) {
		stagingRegistrationDetailsDTO.setSpecialNumberRequired(Boolean.FALSE);
		stagingRegistrationDetailsDTO.setPrType(BidNumberType.N.getCode());
		logMovingService.moveStagingToLog(stagingRegistrationDetailsDTO.getApplicationNo());
		addBidAlterLogs(stagingRegistrationDetailsDTO, BidStatus.SPNOTREQUIRED, ip);
		saveStagingData(stagingRegistrationDetailsDTO);
		spFirstTimeleft(stagingRegistrationDetailsDTO);

	}

	private void handlingBIDLEFT(StagingRegistrationDetailsDTO stagingRegistrationDetailsDTO,
			SpecialNumberDetailsDTO specialNumberDetailsDTO, String ip) {

		// TODO: Invoke refund, release number if required
		// Invoking to generate PR Number with random
		addBidAlterLogs(stagingRegistrationDetailsDTO, BidStatus.BIDLEFT, ip);
		stagingRegistrationDetailsDTO.setPrType(BidNumberType.N.getCode());
		stagingRegistrationDetailsDTO.setSpecialNumberRequired(Boolean.FALSE);
		saveStagingData(stagingRegistrationDetailsDTO);
		doBidLeft(specialNumberDetailsDTO,false);
	}

	@Autowired
	private StagingRegistrationDetailsHistoryLogDAO stgHisterDao;
	private void saveStagingData(StagingRegistrationDetailsDTO stagingRegistrationDetailsDTO) {
		StagingRegistrationDetailsDTO stagingBeforeUpdateDTO=stagingRegistrationDetailsDAO.findOne(stagingRegistrationDetailsDTO.getApplicationNo());
		if(stagingBeforeUpdateDTO!=null) {
			StagingRegistrationDetailsHistoryLogDto stagingHisDTO= new StagingRegistrationDetailsHistoryLogDto();
			stagingHisDTO.setStagingDetails(stagingBeforeUpdateDTO);
			stagingHisDTO.setModifiedDate(LocalDateTime.now());
			stgHisterDao.save(stagingHisDTO);
		}
		stagingRegistrationDetailsDAO.save(stagingRegistrationDetailsDTO);
	}

	private void addBidAlterLogs(StagingRegistrationDetailsDTO stagingRegistrationDetailsDTO, BidStatus bidStatus,
			String ip) {

		if (stagingRegistrationDetailsDTO.getBidAlterDetails() == null) {
			stagingRegistrationDetailsDTO.setBidAlterDetails(new ArrayList<>());
		}
		BidAlterDetailsVO bidAlterDetailsVO = new BidAlterDetailsVO();
		bidAlterDetailsVO.setIpAddress(ip);
		bidAlterDetailsVO.setChangeTo(bidStatus);
		bidAlterDetailsVO.setChangeDate(LocalDateTime.now());
		bidAlterDetailsVO.setPrNo(stagingRegistrationDetailsDTO.getPrNo());
		stagingRegistrationDetailsDTO.getBidAlterDetails().add(bidAlterDetailsVO);

	}

	// Bid left First time issue related method
	// Assign random number
	private void spFirstTimeleft(StagingRegistrationDetailsDTO stagingRegistrationDetailsDTO) {
		PrGenerationInput prGenerationInput = new PrGenerationInput();
		prGenerationInput.setApplicationNo(stagingRegistrationDetailsDTO.getApplicationNo());
		prGenerationInput.setIsDoByOldPrNo(stagingRegistrationDetailsDTO.isFromReassigment());
		prGenerationInput.setIsFromReassigment(stagingRegistrationDetailsDTO.isFromReassigment());
		if(prGenerationInput.getIsFromReassigment()) {
			prGenerationInput.setTrNumber(stagingRegistrationDetailsDTO.getOldPrNo());
		}else {
			prGenerationInput.setTrNumber(stagingRegistrationDetailsDTO.getTrNo());
		}
		prGenerationInput.setGeneratePr(Boolean.FALSE);
		prGenerationInput.setNumberType(BidNumberType.N.getCode());// assign
		// general
		// number
		prGenerationInput.setBlockNo(Boolean.FALSE);
		prGenerationInput.setModule(ModuleEnum.CITIZEN);
		prGenerationInput.setMobileNo(stagingRegistrationDetailsDTO.getApplicantDetails().getContact().getMobile());
		stagingRegistrationDetailsDTO.setPrType(BidNumberType.N.getCode());
		logMovingService.moveStagingToLog(stagingRegistrationDetailsDTO.getApplicationNo());
		stagingRegistrationDetailsDAO.save(stagingRegistrationDetailsDTO);
		if (stagingRegistrationDetailsDTO.getApplicationStatus()
				.equalsIgnoreCase(StatusRegistration.SPECIALNOPENDING.getDescription())) {
			vehicleDetails.invokePrRegService(prGenerationInput);

		}
	}

	
}
