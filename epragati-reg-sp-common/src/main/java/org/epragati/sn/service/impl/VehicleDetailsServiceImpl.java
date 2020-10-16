package org.epragati.sn.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.epragati.common.vo.PrGenerationInput;
import org.epragati.constants.CommonConstants;
import org.epragati.constants.CovCategory;
import org.epragati.constants.ExceptionDescEnum;
import org.epragati.constants.OwnerTypeEnum;
import org.epragati.exception.BadRequestException;
import org.epragati.master.dao.DuplicatePrNumberDAO;
import org.epragati.master.dao.GeneratedPrDetailsDAO;
import org.epragati.master.dao.MasterCovDAO;
import org.epragati.master.dao.OfficeDAO;
import org.epragati.master.dao.RegServiceDAO;
import org.epragati.master.dao.StagingRegistrationDetailsDAO;
import org.epragati.master.dto.DuplicatePrNumbers;
import org.epragati.master.dto.GeneratedPrDetailsDTO;
import org.epragati.master.dto.MasterCovDTO;
import org.epragati.master.dto.OfficeDTO;
import org.epragati.master.dto.StagingRegistrationDetailsDTO;
import org.epragati.master.service.LogMovingService;
import org.epragati.master.service.PrSeriesService;
import org.epragati.master.service.impl.PrSeriesServiceImpl;
import org.epragati.regservice.dto.RegServiceDTO;
import org.epragati.sn.dto.CustomerDetails;
import org.epragati.sn.dto.SpecialNumberDetailsDTO;
import org.epragati.sn.dto.VehicleDetails;
import org.epragati.sn.numberseries.dao.PRPoolDAO;
import org.epragati.sn.numberseries.dto.PRPoolDTO;
import org.epragati.sn.service.VehicleDetailsService;
import org.epragati.sn.vo.BidConfigMasterVO;
import org.epragati.util.BidNumberType;
import org.epragati.util.BidStatus;
import org.epragati.util.StatusRegistration;
import org.epragati.util.payment.ModuleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class VehicleDetailsServiceImpl implements VehicleDetailsService {

	private static final Logger logger = LoggerFactory.getLogger(PrSeriesServiceImpl.class);

	@Autowired
	private MasterCovDAO covDAO;

	@Autowired
	private StagingRegistrationDetailsDAO stagingRegistrationDetailsDAO;


	@Autowired
	private PRPoolDAO numbersPoolDAO;

	@Autowired
	private ActionsDetailsHelper actionsDetailsHelper;

	@Autowired
	private OfficeDAO officeDAO;

	@Autowired
	private GeneratedPrDetailsDAO generatedPrDetailsDAO;

	@Autowired
	private RegServiceDAO regServiceDAO;

	@Autowired
	private DuplicatePrNumberDAO duplicatePrNumberDAO;

	@Autowired
	private LogMovingService logMovingService;

	@Autowired
	private PrSeriesService prSeriesService;

	@Override
	public Pair<VehicleDetails, CustomerDetails> getVehicleDetailsByTrNumber(String trOrPrNumber, String mobileNo,
			BidConfigMasterVO bidConfigMaster,boolean isPrNo) {
		Optional<StagingRegistrationDetailsDTO> stagingDetails ;
		logger.info("Staging query excution start: {}, mobile: {}", trOrPrNumber, mobileNo);
		if(isPrNo) {
			stagingDetails = stagingRegistrationDetailsDAO
					.findByOldPrNoAndApplicantDetailsContactMobile(trOrPrNumber, mobileNo);
		}else {
			stagingDetails = stagingRegistrationDetailsDAO
					.findByTrNoAndApplicantDetailsContactMobile(trOrPrNumber, mobileNo);
		}
		logger.info("End : {}, mobile: {}", trOrPrNumber, mobileNo);

		if (!stagingDetails.isPresent()) {
			throw new BadRequestException("Application not found for either TR/old PR Number:" + trOrPrNumber
					+ " or mobile number:" + mobileNo + " miss matched.");
		}

		doValidateFromStagingDetails(stagingDetails.get(), bidConfigMaster);
		VehicleDetails vehicleDetails = new VehicleDetails();
		CustomerDetails customerDetails = new CustomerDetails();

		MasterCovDTO cov = covDAO.findByCovcode(stagingDetails.get().getClassOfVehicle().toString());
		if(isPrNo) {
			vehicleDetails.setTrNumber(stagingDetails.get().getOldPrNo());
		}else {
			vehicleDetails.setTrNumber(stagingDetails.get().getTrNo());
		} 
		vehicleDetails.setApplicationNumber(stagingDetails.get().getApplicationNo());
		vehicleDetails.setClassOfVehicle(cov);
		
		CovCategory vehicleType;
		if (OwnerTypeEnum.POLICE.equals(stagingDetails.get().getOwnerType())) {
			vehicleType = CovCategory.P;
		} else if (OwnerTypeEnum.Stu.equals(stagingDetails.get().getOwnerType())) {
			vehicleType = CovCategory.Z;
		} else {
			vehicleType = CovCategory.valueOf(cov.getCategory());
		}
		
		vehicleDetails.setVehicleType(vehicleType);
		vehicleDetails.setRtaOffice(stagingDetails.get().getOfficeDetails());
		vehicleDetails.setIsDoByOldPrNo(isPrNo);
		vehicleDetails.setIsFromReassigment(stagingDetails.get().isFromReassigment());

		customerDetails.setEmailId(stagingDetails.get().getApplicantDetails().getContact().getEmail());
		customerDetails.setFirstName(stagingDetails.get().getApplicantDetails().getFirstName());
		customerDetails.setLastName(stagingDetails.get().getApplicantDetails().getLastName());
		customerDetails.setMobileNo(stagingDetails.get().getApplicantDetails().getContact().getMobile());

		return Pair.of(vehicleDetails, customerDetails);
	}

	private void doValidateFromStagingDetails(StagingRegistrationDetailsDTO stagingRegistrationDetailsDTO,
			BidConfigMasterVO bidConfigMaster) {

		if (!stagingRegistrationDetailsDTO.getSpecialNumberRequired()) {
			throw new BadRequestException(
					"Applicant not selected special number required Option while TR generation Time. TR number: "
							+ stagingRegistrationDetailsDTO.getTrNo());
		}
		if(CommonConstants.OTHER.equalsIgnoreCase(stagingRegistrationDetailsDTO.getOfficeDetails().getOfficeCode())) {
			throw new BadRequestException(
					"You are not eligible for special number selection. TR No: "
							+ stagingRegistrationDetailsDTO.getTrNo());
		}

		if (StringUtils.isNoneBlank(stagingRegistrationDetailsDTO.getPrNo())) {
			throw new BadRequestException(
					"Applicant have already PR number Existed. PR Number: " + stagingRegistrationDetailsDTO.getPrNo());
		}
		Long durationDays;
		if(stagingRegistrationDetailsDTO.isFromReassigment()) {
			durationDays = ChronoUnit.DAYS.between(stagingRegistrationDetailsDTO.getReassignmentDoneDate().toLocalDate(),
					LocalDate.now());
		}else {
			durationDays = ChronoUnit.DAYS.between(stagingRegistrationDetailsDTO.getTrGeneratedDate().toLocalDate(),
					LocalDate.now());
		}
		Long bidMaxAllowDays = bidConfigMaster.getBidMaxAllowDays();

		/*if (stagingRegistrationDetailsDTO.getMigrationSource() != null) {
			OldVersionDataConfig oldVersionDataConfig = bidConfigMaster.getOldVersionDataConfigDetails().stream()
					.filter(v -> stagingRegistrationDetailsDTO.getMigrationSource().equals(v.getMigrationSource()))
					.findFirst().orElse(null);
			if (oldVersionDataConfig == null) {
				throw new BadRequestException("BidConfigMaster data not found for migration data ,MigrationSource:{} "
						+ stagingRegistrationDetailsDTO.getMigrationSource());
			}
			bidMaxAllowDays = oldVersionDataConfig.getBidMaxAllowDays();
		}*/

		if (durationDays > bidMaxAllowDays) {
			throw new BadRequestException(
					"Your TR validity expired. TR number:" + stagingRegistrationDetailsDTO.getTrNo());
		}

	}

	@Override
	public void generatePRNumber(SpecialNumberDetailsDTO participant, String prSeries) {

		Optional<OfficeDTO> officeDetails = Optional.empty();
		Optional<RegServiceDTO> regServiceDetails = Optional.empty();
		Optional<StagingRegistrationDetailsDTO> stagingDetails = Optional.empty();

		if(participant.getVehicleDetails().getIsDoByOldPrNo()) {
			stagingDetails = stagingRegistrationDetailsDAO.findByOldPrNoAndApplicantDetailsContactMobile(
					participant.getVehicleDetails().getTrNumber(), participant.getCustomerDetails().getMobileNo());
		}else {
			stagingDetails = stagingRegistrationDetailsDAO.findByTrNoAndApplicantDetailsContactMobile(
					participant.getVehicleDetails().getTrNumber(), participant.getCustomerDetails().getMobileNo());
		}
		if (!stagingDetails.isPresent()) {
			throw new BadRequestException(
					"Application not found for TR/old PR Number: " + participant.getVehicleDetails().getTrNumber());
		}
		officeDetails = officeDAO.findByOfficeCode(stagingDetails.get().getOfficeDetails().getOfficeCode());
		if (!officeDetails.isPresent()) {
			throw new BadRequestException(
					"office details not found for: " + participant.getVehicleDetails().getApplicationNumber());
		}
		PrGenerationInput prGenerationInput = new PrGenerationInput();
		prGenerationInput.setApplicationNo(stagingDetails.get().getApplicationNo());
		prGenerationInput.setTrNumber(participant.getVehicleDetails().getTrNumber());
		prGenerationInput.setSelectedNo(participant.getSelectedNo());
		prGenerationInput.setGeneratePr(Boolean.TRUE);
		prGenerationInput.setBlockNo(Boolean.FALSE);
		prGenerationInput.setModule(participant.getModule());
		prGenerationInput.setPrSeries(prSeries);
		prGenerationInput.setIsDoByOldPrNo(participant.getVehicleDetails().getIsDoByOldPrNo());
		prGenerationInput.setIsFromReassigment(participant.getVehicleDetails().getIsFromReassigment());
		prGenerationInput.setMobileNo(participant.getCustomerDetails().getMobileNo());
		prGenerationInput.setPrNo(participant.getSelectedPrSeries());

		if (participant.getBidStatus().getCode().equals(BidStatus.BIDWIN.getCode())) {
			// Assign selected number
			prGenerationInput.setNumberType(participant.getBidVehicleDetails().getAllocatedBidNumberType().getCode());
			if (stagingDetails.get().getApplicationStatus()
					.equalsIgnoreCase(StatusRegistration.SPECIALNOPENDING.getDescription())) {

				generateSpPrNO(participant.getModule(), participant.getSelectedNo(), stagingDetails,
						regServiceDetails, officeDetails, prSeries,
						participant.getBidVehicleDetails().getAllocatedBidNumberType(),prGenerationInput.getPrNo());
				//invokePrRegService(prGenerationInput);
				prSeriesService.processPrForSP(prGenerationInput);
			} else {
				generateSpPrNO(participant.getModule(), participant.getSelectedNo(), stagingDetails,
						regServiceDetails, officeDetails, prSeries,
						participant.getBidVehicleDetails().getAllocatedBidNumberType(),prGenerationInput.getPrNo());
			}


		} else {
			// Assign random number
			prGenerationInput.setNumberType(BidNumberType.N.getCode());// assign
			prGenerationInput.setModule(participant.getModule());
			stagingDetails.get().setPrType(BidNumberType.N.getCode());
			logMovingService.moveStagingToLog(stagingDetails.get().getApplicationNo());
			if (stagingDetails.get().getApplicationStatus()
					.equalsIgnoreCase(StatusRegistration.SPECIALNOPENDING.getDescription())) { 
				if(prSeriesService.isAssignNumberNow() || StringUtils.isNotBlank(stagingDetails.get().getPrNo())) {
					invokePrRegService(prGenerationInput);					
				}else {
					stagingDetails.get().setApplicationStatus(StatusRegistration.PRNUMBERPENDING.getDescription());
					stagingRegistrationDetailsDAO.save(stagingDetails.get());
				}
				
			}else {
				stagingRegistrationDetailsDAO.save(stagingDetails.get());
			}

		}
	}

	public void invokePrRegService(PrGenerationInput prGenerationInput) {

		try {
			prSeriesService.processPrForSP(prGenerationInput);
		} catch (Exception e) {
			if(StringUtils.isNoneBlank(prGenerationInput.getApplicationNo())) {
				StagingRegistrationDetailsDTO st = stagingRegistrationDetailsDAO.findOne(prGenerationInput.getApplicationNo());
				if(st!=null) {
					if(st.getSchedulerIssues()==null) {
						st.setSchedulerIssues(new ArrayList<>());
					}
					st.getSchedulerIssues().add(LocalDateTime.now()+": while bidAlteration process"
							+", Exception: "+e.getMessage() );
					stagingRegistrationDetailsDAO.save(st);
				}
			}
			logger.error("Exception {}", e);
		}
		
	}



		//		HttpHeaders headers = new HttpHeaders();
		//		headers.setContentType(MediaType.APPLICATION_JSON);
		//		HttpEntity<PrGenerationInput> httpEntity = new HttpEntity<>(prGenerationInput, headers);
		//		try {
		//			restTemplate= new RestTemplate();
		//			restTemplate.exchange(prGenerationUrl, HttpMethod.POST, httpEntity, String.class);
		//		} catch (Exception ex) {
		//			logger.error("Exception while call {} URL{}", prGenerationUrl, ex);
		//		}
	

	//	// move to sp
	//	private String assignSpNumber(Integer selectedNo, StagingRegistrationDetailsDTO stagingRegDetails) {
	//
	//		Optional<PRPoolDTO> numberPoolOptional = numbersPoolDAO.findByOfficeCodeAndRegTypeAndPoolStatusInAndPrNumber(
	//				stagingRegDetails.getOfficeDetails().getOfficeCode(),
	//				CovCategory.getCovCategory(stagingRegDetails.getVehicleType()), numberPoolStatusForSpNo(), selectedNo);
	//		
	//			if (numberPoolOptional.isPresent() && !NumberPoolStatus.LEFTOVER.equals(numberPoolOptional.get().getPoolStatus())) {
	//				numberPoolOptional.get().setPoolStatus(NumberPoolStatus.RESERVED);
	//				numbersPoolDAO.save(numberPoolOptional.get());
	//				return NumberPoolStatus.RESERVED.getDescription();
	//			}
	//		 else {
	//			throw new BadRequestException("Please select new Number..");
	//		}
	//		
	//	}
	//
	//	private List<NumberPoolStatus> numberPoolStatusForSpNo() {
	//		List<NumberPoolStatus> list = new ArrayList<>();
	//		list.add(NumberPoolStatus.OPEN);
	//		list.add(NumberPoolStatus.RESERVED);
	//		list.add(NumberPoolStatus.BLOCKED);
	//		return list;
	//	}

	// move to sp
	private String generateSpPrNO(ModuleEnum module, Integer selectedNo,
			Optional<StagingRegistrationDetailsDTO> stagingRegDetailsOpt, Optional<RegServiceDTO> regServiceDTOOpt,
			Optional<OfficeDTO> officeDetails, String prSeries, BidNumberType numberType,String prNo) {

		StagingRegistrationDetailsDTO stagingRegDetails = null;
		RegServiceDTO regServiceDTO = null;
		if (stagingRegDetailsOpt.isPresent()) {
			stagingRegDetails = stagingRegDetailsOpt.get();
		}
		if (regServiceDTOOpt.isPresent()) {
			regServiceDTO = regServiceDTOOpt.get();
		}

		Optional<PRPoolDTO> numberPoolOptional = Optional.empty();
		if(StringUtils.isNoneBlank(prNo)) {
			numberPoolOptional = numbersPoolDAO.findByPrNo(prNo);
		}else {
		numberPoolOptional = numbersPoolDAO.findByOfficeCodeAndRegTypeAndPrNumberAndPrSeries(
				stagingRegDetails.getOfficeDetails().getOfficeCode(),
				CovCategory.getCovCategory(stagingRegDetails.getVehicleType()), selectedNo, prSeries);
		}
		if (numberPoolOptional.isPresent()) {
			numberPoolOptional.get().setNumberType(numberType);
			return generateNextNumber(officeDetails, numberPoolOptional.get(), stagingRegDetails, regServiceDTO,
					module);
		} else {
			/*
			 * logger.info("Unable to assign special pr.Tr no is: " +
			 * stagingRegDetails.getTrNo() + "and pr no is: " + selectedNo);
			 */
			throw new BadRequestException("Please select new Number..");
		}
	}

	private String generateNextNumber(Optional<OfficeDTO> officeDetails, PRPoolDTO numbersPool,
			StagingRegistrationDetailsDTO stagingRegDetails, RegServiceDTO regServiceDTO, ModuleEnum module) {

		
		actionsDetailsHelper.updateActionsDetails(numbersPool, ExceptionDescEnum.ACTIONBY.getDesciption());
		String prNo = numbersPool.getPrNo();
		List<GeneratedPrDetailsDTO> oldPrRecords = generatedPrDetailsDAO.findByPrNo(prNo);
			if (oldPrRecords.size() > 1) {
				DuplicatePrNumbers duplicateNumbers = new DuplicatePrNumbers();
				duplicateNumbers.setPr(oldPrRecords.stream().findFirst().get().getPrNo());
				duplicateNumbers.setPrCount(oldPrRecords.size());
				duplicateNumbers.setSource(BidNumberType.S.getCode());
				duplicatePrNumberDAO.save(duplicateNumbers);
				logger.info("More then on same pr found. PR number is: " + prNo);
				throw new BadRequestException("More then on same pr found. PR number is: " + prNo);
			} else if(oldPrRecords.size()==1 &&(null==stagingRegDetails || 
					!stagingRegDetails.getApplicationNo().equals(oldPrRecords.get(0).getApplicationNo()))) {
				DuplicatePrNumbers duplicateNumbers = new DuplicatePrNumbers();
				duplicateNumbers.setPr(oldPrRecords.stream().findFirst().get().getPrNo());
				duplicateNumbers.setPrCount(oldPrRecords.size());
				duplicateNumbers.setSource(BidNumberType.S.getCode());
				duplicatePrNumberDAO.save(duplicateNumbers);
				logger.info("Same pr found. PR number is: " + prNo);
				throw new BadRequestException("Same pr found. PR number is: " + prNo);
			}
			
		if (module != null && module.equals(ModuleEnum.CITIZEN)) {
			logger.info(
					"PR generate for application no: " + regServiceDTO.getApplicationNo() + "and pr no is: " + prNo);
			regServiceDTO.getAlterationDetails().setPrNo(prNo);
			regServiceDAO.save(regServiceDTO);
		} else {
			logger.info("PR generate for tr no: " + stagingRegDetails.getTrNo() + "and pr no is: " + prNo);
			stagingRegDetails.setPrNo(prNo);
			logMovingService.moveStagingToLog(stagingRegDetails.getApplicationNo());
			stagingRegistrationDetailsDAO.save(stagingRegDetails);
		}

		//numbersPoolDAO.save(numbersPool);
		saveGeneratedPrDetails(stagingRegDetails, prNo, "SP", regServiceDTO, module);
		return prNo;

	}

	private void saveGeneratedPrDetails(StagingRegistrationDetailsDTO stagingRegDetails, String prNo, String source,
			RegServiceDTO regServiceDTO, ModuleEnum module) {

		GeneratedPrDetailsDTO generatedPrDetailsDTO = new GeneratedPrDetailsDTO();
		if (module != null && module.equals(ModuleEnum.CITIZEN)) {
			generatedPrDetailsDTO.setApplicationNo(regServiceDTO.getApplicationNo());
			generatedPrDetailsDTO
			.setName(regServiceDTO.getRegistrationDetails().getApplicantDetails().getDisplayName());
			generatedPrDetailsDTO.setOfficeCode(regServiceDTO.getOfficeDetails().getOfficeCode());
			generatedPrDetailsDTO.setPrNo(prNo);
			generatedPrDetailsDTO.setSource(source);
			// generatedPrDetailsDTO.setTrNo(stagingRegDetails.getTrNo());
		} else {
			generatedPrDetailsDTO.setApplicationNo(stagingRegDetails.getApplicationNo());
			generatedPrDetailsDTO.setName(stagingRegDetails.getApplicantDetails().getDisplayName());
			generatedPrDetailsDTO.setOfficeCode(stagingRegDetails.getOfficeDetails().getOfficeCode());
			generatedPrDetailsDTO.setPrNo(prNo);
			generatedPrDetailsDTO.setSource(source);
			generatedPrDetailsDTO.setTrNo(stagingRegDetails.getTrNo());
		}

		generatedPrDetailsDAO.save(generatedPrDetailsDTO);

	}

	public static String appendZero(Integer number, int length) {
		return String.format("%0" + (length) + "d", number);
	}
}
