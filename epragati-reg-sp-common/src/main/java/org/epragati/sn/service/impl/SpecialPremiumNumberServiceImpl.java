package org.epragati.sn.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.epragati.cache.CacheData;
import org.epragati.constants.CovCategory;
import org.epragati.constants.MessageKeys;
import org.epragati.constants.Schedulers;
import org.epragati.exception.BadRequestException;
import org.epragati.master.dao.GeneratedPrDetailsDAO;
import org.epragati.master.dao.OfficeDAO;
import org.epragati.master.dao.RegistrationDetailDAO;
import org.epragati.master.dao.StagingRegistrationDetailsDAO;
import org.epragati.master.dto.OfficeDTO;
import org.epragati.master.dto.RegistrationDetailsDTO;
import org.epragati.master.dto.StagingRegistrationDetailsDTO;
import org.epragati.master.service.PrSeriesService;
import org.epragati.payments.vo.PaymentGateWayResponse;
import org.epragati.registration.service.ServiceProviderFactory;
import org.epragati.rta.service.impl.service.RTAService;
import org.epragati.rta.vo.PrGenerationVO;
import org.epragati.service.notification.MessageTemplate;
import org.epragati.service.notification.NotificationTemplates;
import org.epragati.service.notification.NotificationUtil;
import org.epragati.sn.dao.SpecialNumberDetailsDAO;
import org.epragati.sn.dto.BidFeeMaster;
import org.epragati.sn.dto.CustomerDetails;
import org.epragati.sn.dto.SpecialNumberDetailsDTO;
import org.epragati.sn.dto.SpecialNumberFeeDetails;
import org.epragati.sn.dto.VehicleDetails;
import org.epragati.sn.mappers.SpecialNumberDetailsMapper;
import org.epragati.sn.numberseries.dao.PRPoolDAO;
import org.epragati.sn.numberseries.dao.PrimesNumbersDAO;
import org.epragati.sn.numberseries.dto.BidParticipantsDto;
import org.epragati.sn.numberseries.dto.PRPoolDTO;
import org.epragati.sn.numberseries.mapper.BidNumbersDetailsMapper;
import org.epragati.sn.payment.service.PaymentUpdateDetails;
import org.epragati.sn.payment.service.SnPaymentGatewayService;
import org.epragati.sn.service.BidDetailsService;
import org.epragati.sn.service.BidFeeMasterService;
import org.epragati.sn.service.NumberSeriesService;
import org.epragati.sn.service.PasscodeGenerator;
import org.epragati.sn.service.SpecialPremiumNumberService;
import org.epragati.sn.service.VehicleDetailsService;
import org.epragati.sn.vo.BidConfigMasterVO;
import org.epragati.sn.vo.LeftOverVO;
import org.epragati.sn.vo.NumberSeries;
import org.epragati.sn.vo.NumberSeriesDetailsVO;
import org.epragati.sn.vo.NumberSeriesSelectionInput;
import org.epragati.sn.vo.SpecialFeeAndNumberDetailsVO;
import org.epragati.sn.vo.SpecialFeeDetailsVO;
import org.epragati.sn.vo.SpecialNumberDetailsVo;
import org.epragati.util.AppMessages;
import org.epragati.util.BidNumberType;
import org.epragati.util.BidStatus;
import org.epragati.util.GateWayResponse;
import org.epragati.util.NumberPoolStatus;
import org.epragati.util.SourceUtil;
import org.epragati.util.StatusRegistration;
import org.epragati.util.SumOfDigits;
import org.epragati.util.payment.ModuleEnum;
import org.epragati.util.payment.PayStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class SpecialPremiumNumberServiceImpl implements SpecialPremiumNumberService {

	private static final Logger logger = LoggerFactory.getLogger(SpecialPremiumNumberServiceImpl.class);

	@Value("${reg.dealer.prGeneration.url:}")
	private String prGenerationUrl;
	
	@Autowired
	private VehicleDetailsService vehicleDetailsService;

	@Autowired
	private SpecialNumberDetailsDAO specialNumberDetailsDAO;

	@Autowired
	private SpecialNumberDetailsMapper specialNumberDetailsMapper;

	@Autowired
	private BidFeeMasterService bidFeeMasterService;

	@Autowired
	private PasscodeGenerator passcodeGenerator;

	@Autowired
	private ActionsDetailsHelper actionsDetailsHelper;

	@Autowired
	private BidNumbersDetailsMapper bidNumbersDetailsMapper;

	@Autowired
	private NotificationUtil notifications;

	@Autowired
	private NotificationTemplates notificationTemplate;
	
	@Autowired
	private SnPaymentGatewayService paymentGatewayService;
	
	@Autowired
	private BidDetailsService bidDetailsService;
	
	@Autowired
	private PRPoolDAO numbersPoolDAO;
	
	@Autowired
	private OfficeDAO officeDAO;
	
	@Autowired
	private PrSeriesService prSeriesService;
	

	@Autowired
	protected RestTemplate restTemplate;
	
	@Autowired
	protected StagingRegistrationDetailsDAO stagingRegistrationDetailsDAO;
	
	@Autowired
	protected SumOfDigits sumOfDigits;
	
	@Autowired
	protected GeneratedPrDetailsDAO generatedPrDetailsDAO;
	
	@Autowired
	protected PrimesNumbersDAO primesNumberMasterDAO;
	
	@Autowired
	protected RegistrationDetailDAO registrationDetailDAO;
	
	@Autowired
	protected RTAService rtaService;

	@Autowired
	private AppMessages appMessages;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private ServiceProviderFactory serviceProviderFactory;
	
	private String schedulerUser = "Scheduler";

	@Override
	public SpecialFeeAndNumberDetailsVO getNumberSerialsByTrNumber(String trNumber, String mobileNo,
			BidConfigMasterVO bidConfigMaster,boolean isPrNo,String rang, String seriesId) {
		logger.info("Start for getNumberSerialsByTrNumber() trNumber [{}], mobileNo [{}]", trNumber, mobileNo);
		Pair<VehicleDetails, CustomerDetails> vehicleAndCustomerPair = vehicleDetailsService
				.getVehicleDetailsByTrNumber(trNumber, mobileNo, bidConfigMaster,isPrNo);
		boolean isFromReassigment=false;
		if(null!=vehicleAndCustomerPair.getFirst().getIsFromReassigment()) {
			isFromReassigment=vehicleAndCustomerPair.getFirst().getIsFromReassigment();
		}

		doValidateSpecialNumberDetails(vehicleAndCustomerPair.getFirst().getApplicationNumber(),isFromReassigment);

		Optional<SpecialFeeDetailsVO> specialFeeDetailsOptional = bidFeeMasterService
				.getFeeDetailsByCov(vehicleAndCustomerPair.getFirst().getClassOfVehicle().getCovcode());

		if (!specialFeeDetailsOptional.isPresent()) {
			throw new BadRequestException("No matching fee details found");
		}

		NumberSeriesService numberSeriesService=serviceProviderFactory.getNumberSeriesServiceInstent();
		SpecialFeeAndNumberDetailsVO result = numberSeriesService.getNumberSeriesByOfficeCode(vehicleAndCustomerPair.getFirst().getRtaOffice().getOfficeCode(),
				vehicleAndCustomerPair.getFirst().getVehicleType(),rang,seriesId);

		result.setSpecialFeeDetails(specialFeeDetailsOptional.get());
		return result;

	}

	@Override
	public SpecialNumberDetailsDTO doSpecialPremiumPay(NumberSeriesSelectionInput input,
			BidConfigMasterVO bidConfigMaster) {
	
			Pair<VehicleDetails, CustomerDetails> vehicleAndCustomerPair = vehicleDetailsService
					.getVehicleDetailsByTrNumber(input.getTrNumber(), input.getMobileNo(), bidConfigMaster,input.isPrNo());
			
			boolean isFromReassigment=false;
			if(null!=vehicleAndCustomerPair.getFirst().getIsFromReassigment()) {
				isFromReassigment=vehicleAndCustomerPair.getFirst().getIsFromReassigment();
			}

			doValidateSpecialNumberDetails(vehicleAndCustomerPair.getFirst().getApplicationNumber(),isFromReassigment);

			PRPoolDTO prPoolDTO = numbersPoolDAO.findOne(input.getBidNumberId());
			if(prPoolDTO==null) {
				throw new BadRequestException("Selected number (" + input.getNumberSeries() + ") is not found in number pool.");
			}
			input.setApplicationNo(vehicleAndCustomerPair.getFirst().getApplicationNumber());
			Boolean isNumberOpen = this.isNumberOpen(prPoolDTO,input);
			if (!isNumberOpen) {
				throw new BadRequestException("Selected number (" + input.getNumberSeries() + ") is not in OPEN state.");
			}

			NumberSeries numberSeries = bidNumbersDetailsMapper.convertEntity(prPoolDTO);

			Optional<SpecialNumberFeeDetails> specialNumberFeeDetailsOptional = getSpecialNumberDetails(vehicleAndCustomerPair.getFirst(), numberSeries);

			if (!specialNumberFeeDetailsOptional.isPresent()) {
				throw new BadRequestException("No matching fee details found");
			}

			SpecialNumberDetailsDTO entity = specialNumberDetailsMapper.convertEntity(vehicleAndCustomerPair.getFirst(),
					vehicleAndCustomerPair.getSecond(), numberSeries);

			String officeNumberSeries=entity.getVehicleDetails().getRtaOffice().getOfficeNumberSeries();
			if(null==officeNumberSeries) {
				Optional<OfficeDTO> office= officeDAO.findByOfficeCode(entity.getVehicleDetails().getRtaOffice().getOfficeCode());
				if(office.isPresent()) {
					officeNumberSeries=office.get().getOfficeNumberSeries();
				}
			}
			entity.setSelectedPrSeries(prPoolDTO.getPrNo());

			entity.setSelectedNo(numberSeries.getNumber());

			entity.setSpecialNumberFeeDetails(specialNumberFeeDetailsOptional.get());

			Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
					.findTopByVehicleDetailsApplicationNumberOrderByCreatedDateDesc(
							entity.getVehicleDetails().getApplicationNumber());
			if (entityOptional.isPresent()) {
				entity.setBidIteration(entityOptional.get().getBidIteration() + 1);

			} else {
				entity.setBidIteration(1);
			}
			entity.setBidStatus(BidStatus.SPPAYMENTPENDING);

			actionsDetailsHelper.updateActionsDetails(entity, input.getTrNumber());

			entity.setCreatedDate(LocalDateTime.now());
			entity.setCreatedBy(input.getTrNumber());

			specialNumberDetailsDAO.save(entity);

			return entity;
	}
	
	@Override
	public Optional<String> updateSpecialNumberPaymentStatus(PaymentUpdateDetails paymentUpdateDetails) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(paymentUpdateDetails.getAppNumberAppId());

		if (!entityOptional.isPresent()) {
			logger.error("No data from SpecialNumberDetailsDTO AppNumberAppId:{}",
					paymentUpdateDetails.getAppNumberAppId());
			return Optional.empty();
		}

		SpecialNumberDetailsDTO entity = entityOptional.get();

		if (isSpecialNumberPaymentDone(entity)) {
			return Optional.empty();
		}
		entity.getSpecialNumberFeeDetails().setPaymentId(paymentUpdateDetails.getPaymentId());
		entity.setBidStatus(getSpecialNumberStatusFromPay(paymentUpdateDetails.getPayStatus()));

		actionsDetailsHelper.updateActionsDetails(entity, entity.getVehicleDetails().getApplicationNumber());

		if (BidStatus.SPPAYMENTDONE.equals(entity.getBidStatus())) {
			synchronized(entity.getBidVehicleDetails().getBidNumberDtlsId().intern()) {
				PRPoolDTO numbersPool = numbersPoolDAO.findOne(entity.getBidVehicleDetails().getBidNumberDtlsId());

				if (numbersPool == null) {
					return Optional.empty();
				}
				
				if(Arrays.asList(CovCategory.Z,CovCategory.P).contains(entity.getVehicleDetails().getVehicleType()) ){
					doBidWinnerProcess(numbersPool,entity);
				}else {
					entity.setPasscode(passcodeGenerator.generatePasscode());
					BidParticipantsDto bidParticipantDetails = specialNumberDetailsMapper.convertBidParticipants(entity);
					doReservedNumber(numbersPool,bidParticipantDetails);
					sendNotifications(MessageTemplate.SP_NUM_PASSCODE.getId(), entity);
				}
			}

		} else if (BidStatus.SPPAYMENTFAILURE.equals(entity.getBidStatus())) {
			//TODO: No Need to handle as of now. 
			//entity.getSpecialNumberFeeDetails().setTransactionNo(n);

		}

		specialNumberDetailsDAO.save(entity);
		return Optional.ofNullable(entity.getPasscode());
	}
	
	public void doBidWinnerProcess(PRPoolDTO prPoolDTO, SpecialNumberDetailsDTO specialNumberDetailsDTO) {

		try {

			BidParticipantsDto bidParticipantDetails = specialNumberDetailsMapper.convertBidParticipants(specialNumberDetailsDTO);

			prPoolDTO.setAssignedBidder(bidParticipantDetails);

			doBidNumberAssigned(prPoolDTO);

			specialNumberDetailsDTO.setBidStatus(BidStatus.BIDWIN);
			specialNumberDetailsDTO.getBidVehicleDetails().setAllocatedBidNumberType(prPoolDTO.getNumberType());
			actionsDetailsHelper.updateActionsDetails(specialNumberDetailsDTO, schedulerUser);
			specialNumberDetailsDAO.save(specialNumberDetailsDTO);
			// Invoking to generate PR Number with selected
			vehicleDetailsService.generatePRNumber(specialNumberDetailsDTO,prPoolDTO.getPrSeries());
			notifications.sendNotifications(MessageTemplate.SP_BID_WIN.getId(), specialNumberDetailsDTO);
		} catch (Exception e) {
			logger.error("Exception while winning process for specialNumberAppId:{}, and Exception: {}",specialNumberDetailsDTO.getSpecialNumberAppId(),e);
		}
	}
	private void doBidNumberAssigned(PRPoolDTO prPoolDTO) {
		
		prPoolDTO.setPoolStatus(NumberPoolStatus.ASSIGNED);
		prPoolDTO.setBidProcessStatus(NumberPoolStatus.BidProcessStatus.DONE);
		actionsDetailsHelper.updateActionsDetails(prPoolDTO, schedulerUser);
		numbersPoolDAO.save(prPoolDTO);

	}
	private boolean isSpecialNumberPaymentDone(SpecialNumberDetailsDTO entity) {
		if (entity.getActionsDetailsLog() == null || entity.getActionsDetailsLog().isEmpty()) {
			return false;
		}
		return entity.getActionsDetailsLog().stream()
				.anyMatch(status -> status.getAction().equalsIgnoreCase(BidStatus.SPPAYMENTDONE.getDescription()));
	}

	public SpecialNumberDetailsDTO doSpecialPremiumRepay(String specialNumberAppId) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findBySpecialNumberAppId(specialNumberAppId);

		if (!entityOptional.isPresent()) {
			throw new BadRequestException("No record found");
		}
		

		SpecialNumberDetailsDTO specialNumberDetails = entityOptional.get();
		
		if(!LocalDate.now().equals(specialNumberDetails.getCreatedDate().toLocalDate())) {
			throw new BadRequestException("The bid competition already finalized " );
		}
		if (specialNumberDetails.getActionsDetailsLog().stream()
				.anyMatch(p -> p.getAction().equals(BidStatus.SPPAYMENTDONE.getDescription()))) {
			specialNumberDetails.setBidStatus(BidStatus.SPPAYMENTDONE);
			updateSpecialNumbers(specialNumberDetails,
					"Based on Action logs,Resticted the repay and because He has already paid the amount.");
			throw new BadRequestException("You ware already paid the special number selection amount for number: "
					+ specialNumberDetails.getSelectedNo());
		}

		if (BidStatus.SPPAYMENTFAILURE.getCode().equals(specialNumberDetails.getBidStatus().getCode())) {

			specialNumberDetails.setBidStatus(BidStatus.SPPAYMENTPENDING);
			specialNumberDetails.getSpecialNumberFeeDetails().setTransactionNo(CacheData.getPaymentTxidNo());
			return updateSpecialNumbers(specialNumberDetails, null);
		}

		throw new BadRequestException("Invalid bid status : " + specialNumberDetails.getBidStatus());
	}

	private SpecialNumberDetailsDTO updateSpecialNumbers(SpecialNumberDetailsDTO specialNumberDetails, String reason) {

		actionsDetailsHelper.updateActionsDetails(specialNumberDetails,
				specialNumberDetails.getVehicleDetails().getApplicationNumber(), reason,
				specialNumberDetails.getBidStatus().getDescription());
		return specialNumberDetailsDAO.save(specialNumberDetails);
	}

	private BidStatus getSpecialNumberStatusFromPay(PayStatusEnum payStatus) {
		BidStatus status = BidStatus.SPPAYMENTPENDING;

		if (PayStatusEnum.SUCCESS.equals(payStatus)) {

			status = BidStatus.SPPAYMENTDONE;
		} else if (PayStatusEnum.FAILURE.equals(payStatus)) {
			status = BidStatus.SPPAYMENTFAILURE;
		} else if (PayStatusEnum.PENDING.equals(payStatus)) {
			status = BidStatus.SPPAYMENTPENDING;
		}
		return status;
	}

	private void doValidateSpecialNumberDetails(final String applicationNo,boolean isFromReassigment) {
		
			Optional<SpecialNumberDetailsDTO> specialNumberDetailsOptional = specialNumberDetailsDAO
					.findTopByVehicleDetailsApplicationNumberOrderByCreatedDateDesc(applicationNo);

			if (specialNumberDetailsOptional.isPresent()) {

				SpecialNumberDetailsDTO entity = specialNumberDetailsOptional.get();

				if (BidStatus.BIDWIN.equals(entity.getBidStatus()) && !isFromReassigment) {

					throw new BadRequestException("You have already won the bid.");
				}
				if (BidStatus.SPPAYMENTPENDING.equals(entity.getBidStatus())) {

					throw new BadRequestException("Please verify payment, your payment is pending for the Number series:"
							+ entity.getBidVehicleDetails().getBiddingVehicleNumber());
				}
				if (BidStatus.SPPAYMENTFAILURE.equals(entity.getBidStatus())) {

					throw new BadRequestException("Please re-pay the amount, your privious payment is failed for the Number series:"
							+ entity.getBidVehicleDetails().getBiddingVehicleNumber());
				}
				if (BidStatus.BIDLIMITEXCEED.equals(entity.getBidStatus())) {

					throw new BadRequestException("Your bid limits are exceeded.");
				}

				if (BidStatus.SPPAYMENTDONE.equals(entity.getBidStatus())) {

					throw new BadRequestException("You have already paid for the Number series:"
							+ entity.getBidVehicleDetails().getBiddingVehicleNumber());
				}
				if (Arrays.asList(BidStatus.FINALPAYMENTDONE.getCode(), BidStatus.FINALPAYMENTFAILURE.getCode(),
						BidStatus.FINALPAYMENTFAILURE.getCode()).contains(entity.getBidStatus().getCode())) {

					throw new BadRequestException("Invalid status to procced Status:" + entity.getBidStatus());
				}
			}
	}

	private void sendNotifications(Integer templateId, SpecialNumberDetailsDTO entity) {

		try {
			if (entity != null) {

				notifications.sendEmailNotification(notificationTemplate::fillTemplate, templateId, entity,
						entity.getCustomerDetails().getEmailId());

				notifications.sendMessageNotification(notificationTemplate::fillTemplate, templateId, entity,
						entity.getCustomerDetails().getMobileNo());
				entity.setLastPasscodeSentDateTime(LocalDateTime.now());
				if (entity.getPassCodeResentCound() == null) {
					entity.setPassCodeResentCound(0);
				}
				entity.setPassCodeResentCound(entity.getPassCodeResentCound() + 1);

			}

		} catch (Exception e) {
			logger.error("Failed to send notifications for template id: {}; {}", templateId, e);
		}

	}

	@Override
	public void resendPassCodeAlert(String trNumber, String mobileNo) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findByVehicleDetailsTrNumberAndCustomerDetailsMobileNoAndBidStatus(trNumber, mobileNo,
						BidStatus.SPPAYMENTDONE);

		if (!entityOptional.isPresent()) {
			logger.error("SpecialNumberDetails not found for TR No: {}, Mobile No: {}", trNumber, mobileNo);
			throw new BadRequestException(
					"Special NumberDetails not found for TR No: " + trNumber + ", Mobile No: " + mobileNo);
		}
		Optional<BidConfigMasterVO> bidConfigMasterOptional = prSeriesService.getBidConfigMasterData(Boolean.TRUE);
		if (!bidConfigMasterOptional.isPresent()) {
			logger.error("Bid Config Master data not found");
			throw new BadRequestException("Bid Config Master Data not found.Please contact to admin");
		}
		if (bidConfigMasterOptional.get().getPassCodeResentMaxCount() <= entityOptional.get()
				.getPassCodeResentCound()) {
			logger.warn("Max pass code resend alerts outcount limits exceeded for TR No: {}, Mobile No: {}", trNumber,
					mobileNo);
			throw new BadRequestException("Max pass code resend alerts outcount limits exceeded");
		}
		if (entityOptional.get().getLastPasscodeSentDateTime() != null) {
			long seconds = ChronoUnit.SECONDS.between(entityOptional.get().getLastPasscodeSentDateTime(),
					LocalDateTime.now());
			if (bidConfigMasterOptional.get().getPassCodeResentMinTimeInterVal() > seconds) {
				logger.warn("Pass code resend request sent already for TR No: {}, Mobile No: {}", trNumber, mobileNo);
				throw new BadRequestException("Passcode resend request successful, You will receive passcode shortly");
			}
		}
		sendNotifications(MessageTemplate.SP_NUM_PASSCODE.getId(), entityOptional.get());
		specialNumberDetailsDAO.save(entityOptional.get());

	}

	@Override
	public Optional<String> viewPassCode(String trNo, String mobileNo) {

		Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
				.findByVehicleDetailsTrNumberAndCustomerDetailsMobileNoAndBidStatus(trNo, mobileNo,
						BidStatus.SPPAYMENTDONE);
		if (!entityOptional.isPresent()) {
			logger.error("SpecialNumberDetails not found for TR No: {}, Mobile No: {}", trNo, mobileNo);
			throw new BadRequestException(
					"Special NumberDetails not found for TR No: " + trNo + ", Mobile No: " + mobileNo);
		}
		return Optional.ofNullable(entityOptional.get().getPasscode());
	}

	@Override
	public PayStatusEnum processToverifyPayments(String specialNumberAppId, ModuleEnum module,String source) {

		synchronized(specialNumberAppId.intern()) {
			Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO.findBySpecialNumberAppId(specialNumberAppId);
			if (!entityOptional.isPresent()) {
				throw new BadRequestException("No record found");
			}
			checkingIsRelatedTOToday(entityOptional.get());
			String transactionNo=null;
			if (ModuleEnum.SPNR.equals(module)) {
				if(!SourceUtil.SCHEDULER.getName().equals(source)) {
					bidDetailsService.isSpecialNumberRegistrationDurationInBetween();
				}
				if(!BidStatus.SPPAYMENTPENDING.getCode().equals(entityOptional.get().getBidStatus().getCode())) {
					throw new BadRequestException("Invalid bid status : " + entityOptional.get().getBidStatus());
				}
				if(null !=entityOptional.get().getSpecialNumberFeeDetails() 
						&& null!=entityOptional.get().getSpecialNumberFeeDetails().getTransactionNo()) {
					transactionNo=entityOptional.get().getSpecialNumberFeeDetails().getTransactionNo();
				}
			}
			if (ModuleEnum.SPNB.equals(module)) {
				if(!SourceUtil.SCHEDULER.getName().equals(source)) {
					Optional<Long> durationOptional = bidDetailsService.isBidDurationInBetween();
					if (!durationOptional.isPresent()) {
						throw new BadRequestException("e-Bidding closed today.");
					}
				}
				if(!BidStatus.FINALPAYMENTPENDING.getCode().equals(entityOptional.get().getBidStatus().getCode())) {
					throw new BadRequestException("Invalid bid status : " + entityOptional.get().getBidStatus());
				}
				if(null !=entityOptional.get().getBidFinalDetails() 
						&& null!=entityOptional.get().getBidFinalDetails().getTransactionNo()) {
					transactionNo=entityOptional.get().getBidFinalDetails().getTransactionNo();
				}
			}
			PaymentGateWayResponse paymentGateWayResponse=paymentGatewayService.processVerify(specialNumberAppId,transactionNo);
			return paymentGateWayResponse.getPaymentStatus();
		}
	}
	
	@Override
	public Integer getParticipantsCount(String bidNumberDtlsId) {

		PRPoolDTO prPoolDTO = numbersPoolDAO.findOne(bidNumberDtlsId);

		if (prPoolDTO != null) {
			return prPoolDTO.getBidParticipants() == null ? 0 : prPoolDTO.getBidParticipants().size();
		}
		return 0;

	}
	
	@Override
	public Set<String> getListOfLeftOverNumberSeries(String trNumber, String mobileNo, boolean isPrNo,BidConfigMasterVO bidConfigMaster) {
		
		Pair<VehicleDetails, CustomerDetails> vehicleAndCustomerPair = vehicleDetailsService
				.getVehicleDetailsByTrNumber(trNumber, mobileNo, bidConfigMaster,isPrNo);

		boolean isFromReassigment=false;
		if(null!=vehicleAndCustomerPair.getFirst().getIsFromReassigment()) {
			isFromReassigment=vehicleAndCustomerPair.getFirst().getIsFromReassigment();
		}

		doValidateSpecialNumberDetails(vehicleAndCustomerPair.getFirst().getApplicationNumber(),isFromReassigment);
		
		String officeCode = vehicleAndCustomerPair.getFirst().getRtaOffice().getOfficeCode();
		CovCategory regType = vehicleAndCustomerPair.getFirst().getVehicleType();
		NumberSeriesService numberSeriesService=serviceProviderFactory.getNumberSeriesServiceInstent();
		return  numberSeriesService.getListOfLeftOverAvalibleSeries(officeCode, regType);

//		Set<String> prSeriesData = new HashSet<String>();
//		if (StringUtils.isNoneBlank(officeCode) && null!=regType) {
//			List<PRPoolDTO> leftOverList = numbersPoolDAO.findByOfficeCodeAndRegTypeAndPoolStatusAndNumberType(officeCode,
//					regType, NumberPoolStatus.LEFTOVER, BidNumberType.P);
//			leftOverList.forEach(pooldata -> {
//				if (pooldata.getPrSeries() != null)
//					prSeriesData.add(pooldata.getPrSeries());
//			});
//
//		}
//		return prSeriesData;
	}
	
	@Override
	public List<LeftOverVO> getListOfLeftOverNumbers(String trOrPrNumber, String mobileNo, String prSeries,
			boolean isPrNo,BidConfigMasterVO bidConfigMaster) {
		
		Pair<VehicleDetails, CustomerDetails> vehicleAndCustomerPair = vehicleDetailsService
				.getVehicleDetailsByTrNumber(trOrPrNumber, mobileNo, bidConfigMaster,isPrNo);
		
		boolean isFromReassigment=false;
		if(null!=vehicleAndCustomerPair.getFirst().getIsFromReassigment()) {
			isFromReassigment=vehicleAndCustomerPair.getFirst().getIsFromReassigment();
		}

		doValidateSpecialNumberDetails(vehicleAndCustomerPair.getFirst().getApplicationNumber(),isFromReassigment);
		
		String officeCode = vehicleAndCustomerPair.getFirst().getRtaOffice().getOfficeCode();
		CovCategory regType = vehicleAndCustomerPair.getFirst().getVehicleType();
		NumberSeriesService numberSeriesService=serviceProviderFactory.getNumberSeriesServiceInstent();
		return  numberSeriesService.getrAvalibleLeftOverNumbers(officeCode, regType,prSeries);
		
//		List<LeftOverVO> leftOverListResult = null;
//		if (StringUtils.isNoneBlank(officeCode) && null!=regType) {
//			leftOverListResult = this.getLeftOverNumberSeriesByOfficeCode(officeCode,
//					regType, prSeries);
//		}
//		return leftOverListResult;
	}
	
	
	
	@Override
	public void validateRequest(String authToken, String ip) {
		logger.warn(" [{}] These IP  is trying to access our schedulers", ip);
		Optional<BidConfigMasterVO> resultOptional= prSeriesService.getBidConfigMasterData(Boolean.TRUE);
		if (resultOptional.isPresent()
				&& resultOptional.get().getSchedulerAuthToken().equals(authToken)
				&& resultOptional.get().getIpNoToAccesSchedulers().contains(ip)) {
			return;
		}
		throw new BadRequestException("Autherization failed.");
	}
	
	


	

	@Override
	public void handleTrExpiredRecords() {
		
		logger.info("Started handleTrExpiredRecords");
		Optional<BidConfigMasterVO> bidConfigMasterOptional = prSeriesService.getBidConfigMasterData(Boolean.TRUE);
		if(bidConfigMasterOptional.isPresent()) {
			doTRExpiredProcess(bidConfigMasterOptional.get(),false);//process for new register vehiclea
			doTRExpiredProcess(bidConfigMasterOptional.get(),true);//process for re assignment vehiclea
		}else {
			logger.error("BidConfigMaster not defined in DB");
		}
		logger.info("End handleTrExpiredRecords");

	}


	private void checkingIsRelatedTOToday(SpecialNumberDetailsDTO specialNumberDetailsDTO) {
		if(!LocalDate.now().equals(specialNumberDetailsDTO.getCreatedDate().toLocalDate())) {
			throw new BadRequestException("To day not allowed for this transaction");
		}
		
	}
	
	
	
	private boolean doReservedNumber(PRPoolDTO numbersPool, BidParticipantsDto bidParticipantDetails) {

		logger.debug("doReservedNumber start......");
		
		if (numbersPool.getBidParticipants() != null && numbersPool.getBidParticipants().stream()
				.anyMatch(bidPart -> bidPart.getTrNumber().equals(bidParticipantDetails.getTrNumber()))) {
			return false;
		}

		if (!NumberPoolStatus.LEFTOVER.equals(numbersPool.getPoolStatus())) {
			numbersPool.setPoolStatus(NumberPoolStatus.RESERVED);
			if (BidNumberType.N.equals(numbersPool.getNumberType())) {
				numbersPool.setNumberType(BidNumberType.S);
			}
		}

		if (numbersPool.getBidParticipants() == null) {
			numbersPool.setBidParticipants(Arrays.asList(bidParticipantDetails));
			numbersPool.setBidProcessStatus(NumberPoolStatus.BidProcessStatus.OPEN);
		} else {
			sendIntimationNotification(numbersPool.getBidParticipants());
			numbersPool.getBidParticipants().add(bidParticipantDetails);
		}

		numbersPool.setReservedDate(LocalDate.now());
		actionsDetailsHelper.updateActionsDetails(numbersPool, bidParticipantDetails.getSpecialNumberAppId());
		numbersPoolDAO.save(numbersPool);
		logger.debug("doReservedNumber end......");
		return true;
	}
	
	private void sendIntimationNotification(List<BidParticipantsDto> bidParticipants) {
		try {
			bidParticipants.stream().forEach(b -> {

				Optional<SpecialNumberDetailsDTO> entityOptional = specialNumberDetailsDAO
						.findBySpecialNumberAppId(b.getSpecialNumberAppId());
				if (entityOptional.isPresent()) {
					notifications.sendNotifications(MessageTemplate.SP_BID_INTIMATION.getId(), entityOptional.get());
				}
			});
		} catch (Exception e) {
			logger.error("Exception while send Bidding initimayion alerts. {}", e);
		}
	}
	
	
	private Boolean isNumberOpen(PRPoolDTO prPoolDTO, NumberSeriesSelectionInput input) {

		if (bidNumbersDetailsMapper.isReservedBeforeToday(prPoolDTO)) {
			return false;
		}
		if (Arrays.asList(NumberPoolStatus.OPEN, NumberPoolStatus.REOPEN).contains(prPoolDTO.getPoolStatus())) {
			
			PrGenerationVO prGenVO= new PrGenerationVO();
			prGenVO.setApplicationNo(input.getApplicationNo());
			prGenVO.setNumberlocked(Boolean.TRUE);
			prGenVO.setPrSeries(prPoolDTO.getPrSeries());
			prGenVO.setSelectedNo(prPoolDTO.getPrNumber());
			try {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<PrGenerationVO> httpEntity = new HttpEntity<>(prGenVO, headers);
				ResponseEntity<String> response = null;
				response = restTemplate.exchange(prGenerationUrl+"generatePrNo", HttpMethod.POST, httpEntity, String.class);
				logger.info("Responce status from:{} is :{}" + prGenerationUrl, response.getStatusCode());
				
				if (response.hasBody()) {
					GateWayResponse<String> inputOptional = parseJson(response.getBody(),
							new TypeReference<GateWayResponse<String>>() {
							});
					if(null==inputOptional) {
						throw new BadRequestException(HttpStatus.FAILED_DEPENDENCY.getReasonPhrase());
					}
					if (!inputOptional.getStatus()) {
						throw new BadRequestException(inputOptional.getMessage());
					}
					return Boolean.TRUE;
				}
			} catch (Exception ex) {
				logger.error("Exception while number locking:{}" , ex);
				throw ex;
			}
			
		}
		return Arrays
				.asList(NumberPoolStatus.RESERVED, NumberPoolStatus.OPEN, NumberPoolStatus.BLOCKED,
						NumberPoolStatus.LOCKED, NumberPoolStatus.LEFTOVER, NumberPoolStatus.REOPEN)
				.contains(prPoolDTO.getPoolStatus());

	}
	
	
	
	
	private void doTRExpiredProcess(BidConfigMasterVO bidConfigMaster,boolean isReassignment) {
		int count = 1;
		while (count < 400) {

			LocalDateTime requiredDate = LocalDateTime.now()
					.minusDays(bidConfigMaster.getBidMaxAllowDays() - 1);

			List<StagingRegistrationDetailsDTO> stagingDetails;
			Pageable pageable;
			if(!isReassignment) {
				//OrderByTrGeneratedDateDes
				pageable = new PageRequest(0, 50,new Sort(new Order(Direction.DESC, "trGeneratedDate")));
				stagingDetails=stagingRegistrationDetailsDAO
						.findByApplicationStatusAndTrGeneratedDateLessThanAndIsFromReassigmentAndPrNoIsNullAndIsTrExpiredIsNull(
								StatusRegistration.SPECIALNOPENDING.getDescription(),requiredDate,isReassignment, pageable);
			}else {
				pageable = new PageRequest(0, 50,new Sort(new Order(Direction.DESC, "reassignmentDoneDate")));
				stagingDetails=stagingRegistrationDetailsDAO
						.findByApplicationStatusAndIsFromReassigmentAndReassignmentDoneDateLessThanAndPrNoIsNull(
								StatusRegistration.SPECIALNOPENDING.getDescription(),true,requiredDate, pageable);
			}


			logger.info("Iteratior Count:[{}], size of staging record:,[{}]",count,stagingDetails.size());
			if (stagingDetails.isEmpty()) {
				break;
			}
			stagingDetails.forEach(st -> {
				try {
					doTrexpiredProcess(st);
				} catch (BadRequestException e) {
					logger.error("Exception while handling handleTrExpiredRecords" + e.getMessage());
				} catch (Exception e) {
					logger.error("Exception while handling handleTrExpiredRecords" + e);
				}

			});
			stagingDetails.clear();
			count++;
		}

	}
	
	private void doTrexpiredProcess(StagingRegistrationDetailsDTO st) {
		
		Optional<SpecialNumberDetailsDTO> bidWinner;
		if(st.isFromReassigment()) {
			bidWinner = specialNumberDetailsDAO
				.findByVehicleDetailsTrNumberAndBidStatusAndCreatedDateGreaterThan(st.getTrNo(), BidStatus.BIDWIN,st.getReassignmentDoneDate());
		}else {
			bidWinner = specialNumberDetailsDAO
					.findByVehicleDetailsTrNumberAndBidStatus(st.getTrNo(), BidStatus.BIDWIN);
		}
		
		if (!bidWinner.isPresent() && isAlreadyExistInRegDetails(st)) {
			try {
				st.setSpecialNumberRequired(Boolean.FALSE);
				st.setPrType(BidNumberType.N.getCode());
				st.setIsTrExpired(Boolean.TRUE);
				rtaService.assignPR(st);
			}catch(Exception e) {
				logger.error("Exception while handling handleTrExpiredRecords: {}", e.getMessage());
				logger.debug("Exception while handling handleTrExpiredRecords {}", e);
				if(st.getSchedulerIssues()==null) {
					st.setSchedulerIssues(new ArrayList<>());
				}
				st.getSchedulerIssues().add(LocalDateTime.now()+": where "+Schedulers.TREXPIRED
						+", Issue: "+e.getMessage());
				st.setSpecialNumberRequired(Boolean.TRUE);
				st.setIsTrExpired(null);
				st.setApplicationStatus(StatusRegistration.SPECIALNOPENDING.getDescription());
				stagingRegistrationDetailsDAO.save(st);
			}
		}else {
			logger.warn("Skipping records _id:{}",st.getApplicationNo());

		}
	}
	
	private boolean isAlreadyExistInRegDetails(StagingRegistrationDetailsDTO st) {
//		if (CollectionUtils.isNotEmpty(st.getBidAlterDetails()) && st.getBidAlterDetails().stream().anyMatch(
//				bidAlt -> Arrays.asList(BidStatus.SPNOTREQUIRED, BidStatus.BIDLEFT).contains(bidAlt.getChangeTo()))) {
//			return false;
//		}
		Optional<RegistrationDetailsDTO> regDetailsOptin = registrationDetailDAO
				.findTopByVahanDetailsChassisNumberAndVahanDetailsEngineNumberOrderByLUpdateDesc(st.getVahanDetails().getChassisNumber(),
						st.getVahanDetails().getEngineNumber());
		if (regDetailsOptin.isPresent()) {
			RegistrationDetailsDTO regDTO= regDetailsOptin.get();
			if(!(regDTO.getApplicationNo().equals(st.getApplicationNo()) && StringUtils.isBlank(regDTO.getPrNo()))) {
				return false;
			}
		}
		return true;
	}
	private <T> T parseJson(String value, TypeReference<T> valueTypeRef) {
		try {
			return objectMapper.readValue(value, valueTypeRef);
		} catch (IOException ioe) {
			logger.error(appMessages.getLogMessage(MessageKeys.PARSEJSON_JSONTOOBJECT), ioe);
		}
		return null;
	}

	@Override
	public List<NumberSeriesDetailsVO> getNumberRangByTROrPR(String trOrPrNumber, String mobileNo,
			BidConfigMasterVO bidConfigMaster, boolean isPrNo) {
		
		Pair<VehicleDetails, CustomerDetails> vehicleAndCustomerPair = vehicleDetailsService
				.getVehicleDetailsByTrNumber(trOrPrNumber, mobileNo, bidConfigMaster,isPrNo);
		
		boolean isFromReassigment=false;
		if(null!=vehicleAndCustomerPair.getFirst().getIsFromReassigment()) {
			isFromReassigment=vehicleAndCustomerPair.getFirst().getIsFromReassigment();
		}

		doValidateSpecialNumberDetails(vehicleAndCustomerPair.getFirst().getApplicationNumber(),isFromReassigment);
		
		//String officeCode = vehicleAndCustomerPair.getFirst().getRtaOffice().getOfficeCode();
		CovCategory regType = vehicleAndCustomerPair.getFirst().getVehicleType();
		NumberSeriesService numberSeriesService=serviceProviderFactory.getNumberSeriesServiceInstent();
		return numberSeriesService.getNumberRange(regType,Boolean.FALSE);
	}
	
	private Optional<SpecialNumberFeeDetails> getSpecialNumberDetails(VehicleDetails vehicleDetails,
			NumberSeries numberSeries) {

		Optional<BidFeeMaster> bidFeeMasterOptional = bidFeeMasterService.getBidFeeMasterByCov(
				vehicleDetails.getClassOfVehicle().getCovcode());
		if (!bidFeeMasterOptional.isPresent()) {
			return Optional.empty();
		}

		BidFeeMaster bidFeeMaster = bidFeeMasterOptional.get();

		SpecialNumberFeeDetails specialNumberFeeDetails = new SpecialNumberFeeDetails();
		specialNumberFeeDetails.setBidFeeMaster(bidFeeMaster);

		if (BidNumberType.P.equals(numberSeries.getNumberType())) {

			specialNumberFeeDetails.setApplicationAmount(numberSeries.getAmount());
			specialNumberFeeDetails.setTotalAmount(numberSeries.getAmount() + bidFeeMaster.getRtaBidFee());
		} else {
			specialNumberFeeDetails.setApplicationAmount(bidFeeMaster.getSpecialNumFee());
			specialNumberFeeDetails.setTotalAmount(bidFeeMaster.getSpecialNumFee() + bidFeeMaster.getRtaBidFee());
		}
		specialNumberFeeDetails.setServicesAmount(bidFeeMaster.getRtaBidFee());
		specialNumberFeeDetails.setTransactionNo(CacheData.getPaymentTxidNo());
		return Optional.of(specialNumberFeeDetails);
	}

	@Override
	public List<SpecialNumberDetailsVo> searchPaymentStatusBySelectedPrSeries(String selectedPrSeries) {
		List<SpecialNumberDetailsDTO> spBidDetails = null;
		if (StringUtils.isNotBlank(selectedPrSeries)) {
			spBidDetails = specialNumberDetailsDAO.findBySelectedPrSeriesAndBidStatusIn(selectedPrSeries,Arrays.asList(BidStatus.BIDWIN, BidStatus.BIDLOOSE, BidStatus.BIDTIE));
		}
		 List<SpecialNumberDetailsVo> snVoList=specialNumberDetailsMapper.convertEntity(spBidDetails);
			 snVoList = Stream.concat(snVoList.stream().filter(val -> BidStatus.BIDWIN.getDescription().equals(val.getBidStatus().getDescription())),
					 snVoList.stream().filter(val -> !BidStatus.BIDWIN.getDescription().equals(val.getBidStatus().getDescription()))    )
					 .collect(Collectors.toCollection(ArrayList::new));			 
		return snVoList;
	}
	
}
