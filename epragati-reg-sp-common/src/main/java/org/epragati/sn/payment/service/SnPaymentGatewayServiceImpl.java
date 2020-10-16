package org.epragati.sn.payment.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.epragati.constants.ExceptionDescEnum;
import org.epragati.exception.BadRequestException;
import org.epragati.master.dao.GateWayDAO;
import org.epragati.master.dto.GateWayDTO;
import org.epragati.master.service.LogMovingService;
import org.epragati.payment.dto.PayURefundResponse;
import org.epragati.payment.dto.PaymentTransactionDTO;
import org.epragati.payment.dto.PaymentTransactionResponseDTO;
import org.epragati.payment.dto.PaymentsSucessRecordsDTO;
import org.epragati.payment.mapper.PaymentVerifyRequestMapper;
import org.epragati.payments.dao.PaymentSuccessDAO;
import org.epragati.payments.dao.PaymentTransactionDAO;
import org.epragati.payments.service.PaymentGateWay;
import org.epragati.payments.service.PaymentGateWayService;
import org.epragati.payments.service.PaymentGatewayFactoryProvider;
import org.epragati.payments.vo.PaymentGateWayResponse;
import org.epragati.payments.vo.PaymentReqParams;
import org.epragati.payments.vo.TransactionDetailVO;
import org.epragati.sn.service.BidDetailsService;
import org.epragati.sn.service.SpecialPremiumNumberService;
import org.epragati.util.payment.GatewayTypeEnum;
import org.epragati.util.payment.ModuleEnum;
import org.epragati.util.payment.PayStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class SnPaymentGatewayServiceImpl implements SnPaymentGatewayService {

	private static final Logger logger = LoggerFactory.getLogger(SnPaymentGatewayServiceImpl.class);

	/*	@Value("${payment.service.payu.payUKey}")
	private String payUKey;// "; gtKFFx k4waet98

	@Value("${payment.service.payu.refundurl}")
	private String payURefundUrl;

	@Value("${payment.service.payu.verify.authorization}")
	private String payUAuthroization;*/

	@Autowired
	private SpecialPremiumNumberService specialPremiumNumberService;

	@Autowired
	private BidDetailsService bidDetailsService;

	@Autowired
	private PaymentGatewayFactoryProvider paymentGatewayFactoryProvider;

	@Autowired
	private PaymentTransactionDAO paymentTransactionDAO;

	@Autowired
	private PaymentGateWayService paymentGatewayService;


	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private GateWayDAO gatewayDao;

	@Autowired
	private LogMovingService logMovingService;

	@Autowired
	private PaymentVerifyRequestMapper paymentVerifyRequestMapper;

	//TODO need remove below commented in futur
	/*	@Value("${keystore.file.location:}")
	private String keyStoreFile;



	// Determines the timeout in milliseconds until a connection is established.
    private static final int CONNECT_TIMEOUT = 30000;

    // The timeout when requesting a connection from the connection manager.
    private static final int REQUEST_TIMEOUT = 30000;

    // The timeout for waiting for data
    private static final int SOCKET_TIMEOUT = 60000;

    private static final int MAX_TOTAL_CONNECTIONS = 50;
    private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 20 * 1000;
	//TODO need to move App config file.
	@PostConstruct
	public void init_old() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, KeyManagementException, UnrecoverableKeyException {
		if(StringUtils.isNoneBlank(keyStoreFile)) {
			SSLContextBuilder builder = new SSLContextBuilder();
			try {
	            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
	        } catch (NoSuchAlgorithmException | KeyStoreException e) {
	        	logger.error("Pooling Connection Manager Initialisation failure because of {}",  e);
	        }
			SSLConnectionSocketFactory socketFactory = null;
	        try {
	        	socketFactory = new SSLConnectionSocketFactory(builder.build());
	        } catch (KeyManagementException | NoSuchAlgorithmException e) {
	        	logger.error("Pooling Connection Manager Initialisation failure because of {}" , e);
	        }
	        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
	                .<ConnectionSocketFactory>create().register("https", socketFactory)
	                .register("http", new PlainConnectionSocketFactory())
	                .build();

	        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
	        poolingConnectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);


	        RequestConfig requestConfig = RequestConfig.custom()
	                .setConnectionRequestTimeout(REQUEST_TIMEOUT)
	                .setConnectTimeout(CONNECT_TIMEOUT)
	                .setSocketTimeout(SOCKET_TIMEOUT).build();

	        CloseableHttpClient httpcilent= HttpClients.custom()
	                .setDefaultRequestConfig(requestConfig)
	                .setConnectionManager(poolingConnectionManager)
	                .setKeepAliveStrategy(connectionKeepAliveStrategy())
	                .build();

	        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
	        requestFactory.setHttpClient(httpcilent);
			restTemplate = new RestTemplate(requestFactory);
			logger.info("Rest template creation Done: {} ",restTemplate);
		}else {
			restTemplate=new RestTemplate();
		}
	}

	public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                HeaderElementIterator it = new BasicHeaderElementIterator
                        (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();

                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        return Long.parseLong(value) * 1000;
                    }
                }
                return DEFAULT_KEEP_ALIVE_TIME_MILLIS;
            }
        };
    }*/

	@Override
	public void prepareRequestObject(TransactionDetailVO transactionDetailVO) {

		paymentGatewayService.prepareRequestObject(transactionDetailVO, null, null);
	}

	@Override
	public PaymentGateWayResponse processResponse(PaymentGateWayResponse paymentGateWayResponse) {
		PaymentGateWay paymentGateWay = paymentGatewayFactoryProvider
				.getPaymentGateWayInstance(paymentGateWayResponse.getGatewayTypeEnum());
		GateWayDTO gatewayValue = gatewayDao.findByGateWayType(GatewayTypeEnum.PAYU);
		Map<String, String> gatewayDetails = gatewayValue.getGatewayDetails();
		paymentGateWayResponse = paymentGateWay.processResponse(paymentGateWayResponse, gatewayDetails);
		updateTransactionDetails(paymentGateWayResponse);
		logger.debug("Payment processResponse Sucess");
		return paymentGateWayResponse;

	}

	@Override
	public PaymentGateWayResponse processVerify(String appFormNo,String transactionNo) {
		synchronized(appFormNo.intern()) {
			//Optional<PaymentTransactionDTO> optionalDTO = getLatestTransactionDateByTransactionRefNumber(appFormNo);
			Optional<PaymentTransactionDTO> optionalDTO = paymentTransactionDAO
					.findTopByApplicationFormRefNumAndTransactioNo(appFormNo,transactionNo);
			if (optionalDTO.isPresent()) {
				PaymentTransactionDTO payTransctionDTO = optionalDTO.get();
				logger.debug("Tranasction detetails {} ", payTransctionDTO);
				// Getting Last transaction
				PaymentGateWay paymentGateWay = paymentGatewayFactoryProvider.getPaymentGateWayInstance(
						GatewayTypeEnum.getGatewayTypeEnumById(payTransctionDTO.getPaymentGatewayType()));
				PaymentGateWayResponse paymentGateWayResponse = paymentGateWay.processVerify(payTransctionDTO);
				paymentGateWayResponse.setAppTransNo(appFormNo);
				updateTransactionDetails(paymentGateWayResponse);

				updatePaymentStatus(paymentGateWayResponse);

				return paymentGateWayResponse;

			} else {
				logger.error("Applicantion payment transaction details not found : Application form number:{}",
						appFormNo);
				throw new BadRequestException(ExceptionDescEnum.NOTFOUNF_TRAN_NUMBER.getDesciption());
			}
		}
	}

	public Optional<PaymentTransactionDTO> getLatestTransactionDateByTransactionRefNumber(String applicationFormNo) {

		List<PaymentTransactionDTO> paymentList = paymentTransactionDAO.findByApplicationFormRefNum(applicationFormNo);
		if (paymentList != null && paymentList.size() > 0) {
			paymentList.sort((o1, o2) -> o2.getRequest().getRequestTime().compareTo(o1.getRequest().getRequestTime()));
			return Optional.of(paymentList.get(0));
		}
		return Optional.empty();
	}

	/**
	 * 
	 * updateTransactionDetails used to update payment collection based on
	 * payment response
	 * 
	 * as form number
	 * 
	 *
	 * @param paymentGateWayResponse
	 */
	private void updateTransactionDetails(PaymentGateWayResponse paymentGateWayResponse) {
		if (!Objects.isNull(paymentGateWayResponse) && !Objects.isNull(paymentGateWayResponse.getAppTransNo()) && !Objects.isNull(paymentGateWayResponse.getTransactionNo())) {
			//Optional<PaymentTransactionDTO> optionalDTO = getLatestTransactionDateByTransactionRefNumber(paymentGateWayResponse.getAppTransNo());
			
			Optional<PaymentTransactionDTO> optionalDTO = paymentTransactionDAO
					.findTopByApplicationFormRefNumAndTransactioNo(paymentGateWayResponse.getAppTransNo(),paymentGateWayResponse.getTransactionNo());
			
			if (optionalDTO.isPresent()) {
				PaymentTransactionDTO payTransctionDTO = optionalDTO.get();
				PaymentTransactionResponseDTO response = new PaymentTransactionResponseDTO();
				response.setBankTransactionRefNum(paymentGateWayResponse.getBankTranRefNumber());
				response.setResponseTime(getCurrentTime());
				response.setIsHashValidationSucess(paymentGateWayResponse.getIsHashValidationSucess());
				response.setPayUResponse(paymentGateWayResponse.getPayUResponse());
				payTransctionDTO.setPayStatus(paymentGateWayResponse.getPaymentStatus().getDescription());

				response.setResponseDeatils(paymentGateWayResponse.getPayUResponse().toString());
				paymentGateWayResponse.setModuleCode(payTransctionDTO.getModuleCode());

				if (payTransctionDTO.getResponse() != null) {
					List<PaymentTransactionResponseDTO> responseLog = payTransctionDTO.getResponseLog();
					if (responseLog == null)
						responseLog = new ArrayList<>();

					responseLog.add(payTransctionDTO.getResponse());
					payTransctionDTO.setResponseLog(responseLog);
				}
				payTransctionDTO.setResponse(response);
				if(null!=paymentGateWayResponse.getPaymentVerifyRequest()) {
					payTransctionDTO.setPaymentVerifyRequest(paymentVerifyRequestMapper.convertVO(paymentGateWayResponse.getPaymentVerifyRequest()));
				}
				logger.debug("Tranasction detetails {} ", payTransctionDTO);
				logMovingService.movePaymnetsToLog(payTransctionDTO.getApplicationFormRefNum());
				paymentTransactionDAO.save(payTransctionDTO);

			} else {
				logger.error(
						"Applicantion payment transaction details not found : based on  Application form number and Transaction No");
				throw new BadRequestException(ExceptionDescEnum.NOTFOUND_APP_REF_NUMBER.getDesciption());
			}
		} else {
			logger.error("Applicant form  number not found  for payment responce save");
			throw new BadRequestException(ExceptionDescEnum.NULL_APP_REF_NUMBER.getDesciption());
		}

	}
	@Value("${isInTestPayment:}")
	private Boolean isInTestPayment;
	@Override
	public Optional<String> processRefundByPaymentId(String transactionNo, String paymentId, Double refundAmount) {

		Optional<PaymentTransactionDTO> optionalDTO = paymentTransactionDAO.findByTransactioNo(transactionNo);
		if (optionalDTO.isPresent()) {
			if(isInTestPayment) {
				refundAmount=1.0d;
			}
			PaymentTransactionDTO payTransctionDTO = optionalDTO.get();
			logger.debug("Tranasction detetails {} ", payTransctionDTO);

			PayURefundResponse payUVerifyResponse = processPayURefundByPaymentId(paymentId, refundAmount);

			// Save the refund response
			if (payTransctionDTO.getPayURefundResponse() != null) {
				if (payTransctionDTO.getPayURefundResponseLog() == null) {
					payTransctionDTO.setPayURefundResponseLog(new ArrayList<>());
				}
				payTransctionDTO.getPayURefundResponseLog().add(payTransctionDTO.getPayURefundResponse());
			}
			payTransctionDTO.setPayURefundResponse(payUVerifyResponse);
			if(0==payUVerifyResponse.getStatus() && null!=payTransctionDTO.getFeeDetailsDTO()) {
				payTransctionDTO.getFeeDetailsDTO().setRefundAmound(refundAmount);
			}
			logMovingService.movePaymnetsToLog(payTransctionDTO.getApplicationFormRefNum());
			paymentTransactionDAO.save(payTransctionDTO);

			if (payUVerifyResponse.getStatus() == 0 && StringUtils.isNotBlank(payUVerifyResponse.getResult())) {
				return Optional.of(payUVerifyResponse.getResult());
			}

			return Optional.empty();

		} else {
			logger.error("Applicantion payment transaction details not found : Payment transaction number:{}",
					paymentId);
			throw new BadRequestException(ExceptionDescEnum.NOTFOUNF_TRAN_NUMBER.getDesciption());
		}
	}

	private PayURefundResponse processPayURefundByPaymentId(String paymentId, Double amount) {

		logger.info("Doing PayUVerifyProcess for payu paymentId id: {}", paymentId);
		GateWayDTO gatewayValue = gatewayDao.findByGateWayType(GatewayTypeEnum.PAYU);
		Map<String, String> gatewayDetails = gatewayValue.getGatewayDetails();

		MultiValueMap<String, String> multiValueMap = getPayURefoundReqParams(paymentId, amount, gatewayDetails.get(GatewayTypeEnum.PayUParams.SPNO_PAYU_KEY.getParamKey()));

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		requestHeaders.add("Authorization",  gatewayDetails.get(GatewayTypeEnum.PayUParams.SPNO_PAYU_AUTHORIZATION.getParamKey()));

		HttpEntity<MultiValueMap<String, String>> payuRequest = new HttpEntity<>(multiValueMap, requestHeaders);
		try {

			return refoundCall(gatewayDetails,payuRequest);

		}
		catch (RestClientException rce) {
			logger.error("RestTemplate Exception while payu refund process.{}", rce);
			throw new BadRequestException("RestTemplate Exception while payu refund process: "+rce.getMessage()) ;

		} catch (Exception e) {
			logger.error("Exception while payU verification.{}", e);
			throw new BadRequestException("Opps.. There is an Exception in payU server..Please try later.");
		}

	}

	private PayURefundResponse refoundCall(Map<String, String> gatewayDetails, HttpEntity<MultiValueMap<String, String>> payuRequest) {
		ResponseEntity<PayURefundResponse> response = restTemplate.postForEntity(gatewayDetails.get(GatewayTypeEnum.PayUParams.PAYU_REFUNDURL.getParamKey()), payuRequest,PayURefundResponse.class);

		logger.info("response status from payu verify: {}", response.getStatusCode());

		if (response.hasBody()) {
			logger.info("payU responce body: {}", response.getBody());
			return response.getBody();
		}
		logger.info("No respopnce body from PayU Server, PayU responce Body:{}", response.getBody());
		throw new BadRequestException("No respopnce body from PayU Server");
	}

	private MultiValueMap<String, String> getPayURefoundReqParams(String paymentId, Double refundAmount,String merchantKey) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("paymentId", paymentId);
		map.add("refundAmount", refundAmount.toString());
		map.add("merchantKey",merchantKey); // Always set to 1
		return map;
	}

	@Override
	public PaymentReqParams convertPayments(TransactionDetailVO transactionDetails, String appFormNo) {
		return paymentGatewayService.convertPayments(transactionDetails, appFormNo);
	}

	@Override
	public Optional<String>  updatePaymentStatus(PaymentGateWayResponse response) {

		PaymentUpdateDetails paymentUpdateDetails = new PaymentUpdateDetails();
		paymentUpdateDetails.setAppNumberAppId(response.getAppTransNo());
		paymentUpdateDetails.setPaymentId(response.getPayUResponse().getPayuMoneyId());
		paymentUpdateDetails.setPayStatus(response.getPaymentStatus());
		paymentUpdateDetails.setTransactionNo(response.getTransactionNo());

		if (ModuleEnum.SPNR.getCode().equalsIgnoreCase(response.getModuleCode())) {

			logger.warn("Application number matched as Speical Number Registration payment. [{}]", response.getModuleCode());

			return specialPremiumNumberService.updateSpecialNumberPaymentStatus(paymentUpdateDetails);

		} else if (ModuleEnum.SPNB.getCode().equalsIgnoreCase(response.getModuleCode())) {

			logger.warn("Application number matched as Speical Bid payment. [{}]", response.getModuleCode());

			bidDetailsService.updateFinalBidPaymentStatus(paymentUpdateDetails);

		}
		return Optional.empty();

	}


	private LocalDateTime getCurrentTime() {
		return LocalDateTime.now();
	}

	@Autowired
	private PaymentSuccessDAO paymentSuccessDAO;

	@Override
	public void verifyAllPaymentFailureRecords() {

		List<PaymentTransactionDTO> spnrFaliedRecords=paymentTransactionDAO.findByModuleCodeAndPayStatus(ModuleEnum.SPNR.getCode(),PayStatusEnum.FAILURE.getDescription());

		List<PaymentsSucessRecordsDTO> list = new ArrayList<>();
		spnrFaliedRecords.stream().forEach(s -> verifyProcess(s,list));
		paymentSuccessDAO.save(list);
		list.clear();
		spnrFaliedRecords.clear();

		//for checking  SPNB data
		spnrFaliedRecords=paymentTransactionDAO.findByModuleCodeAndPayStatus(ModuleEnum.SPNB.getCode(),PayStatusEnum.FAILURE.getDescription());
		spnrFaliedRecords.stream().forEach(s -> verifyProcess(s,list));
		paymentSuccessDAO.save(list);
		list.clear();
		spnrFaliedRecords.clear();
	}
	private void verifyProcess(PaymentTransactionDTO s, List<PaymentsSucessRecordsDTO> list) {
		try {
		PaymentGateWay paymentGateWay = paymentGatewayFactoryProvider.getPaymentGateWayInstance(GatewayTypeEnum.PAYU);
		PaymentGateWayResponse paymentGateWayResponse = paymentGateWay.processVerify(s);
		s.setPayStatus(paymentGateWayResponse.getPaymentStatus().getDescription());
		PaymentTransactionResponseDTO response = new PaymentTransactionResponseDTO();
		response.setResponseDeatils(paymentGateWayResponse.getPayUResponse().toString());
		s.setResponse(response);
		PaymentsSucessRecordsDTO paymentsSucessRecordsDTO =new PaymentsSucessRecordsDTO();
		BeanUtils.copyProperties(s, paymentsSucessRecordsDTO);
		list.add(paymentsSucessRecordsDTO);
		}catch (Exception e) {
			logger.error("Exceprion while transactionNo:{},  veryfy: {}",s.getTransactioNo(),e);
		}
	}

	@Override
	public PaymentGateWayResponse VerifySpecific(String merchantTransactionIds) {
		PaymentGateWay paymentGateWay = paymentGatewayFactoryProvider.getPaymentGateWayInstance(GatewayTypeEnum.PAYU);
		Optional<PaymentTransactionDTO> opt = paymentTransactionDAO.findByTransactioNo(merchantTransactionIds);
		if(!opt.isPresent()) {
			new BadRequestException("Payments details not found for the transaction: "+merchantTransactionIds);
		}
		return paymentGateWay.processVerify(opt.get());
	}



}
