package org.epragati.sn.service.impl;

import java.util.List;
import java.util.Optional;

import org.epragati.sn.dao.BidFeeMasterDAO;
import org.epragati.sn.dao.CovCategoryMasterDAO;
import org.epragati.sn.dto.BidFeeMaster;
import org.epragati.sn.dto.CovCategoryMaster;
import org.epragati.sn.service.BidFeeMasterService;
import org.epragati.sn.vo.SpecialFeeDetailsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BidFeeMasterServiceImpl implements BidFeeMasterService {

	@Autowired
	private BidFeeMasterDAO bidFeeMasterDAO;

	@Autowired
	private CovCategoryMasterDAO covCategoryMasterDAO;
	
	@Override
	public List<CovCategoryMaster> getCovCategoryMaster(List<String> covCodes){
		
		return covCategoryMasterDAO.findByCovCodeIn(covCodes);
	}

	@Override
	public Optional<BidFeeMaster> getBidFeeMasterByCov(String covCode) {

		Optional<CovCategoryMaster> covCategoryMasterOptional = covCategoryMasterDAO.findByCovCode(covCode);

		if (covCategoryMasterOptional.isPresent()) {

			return bidFeeMasterDAO.findByCovCategoryGroupId(covCategoryMasterOptional.get().getCovCategoryGroupId());
		}

		return Optional.empty();
	}

	@Override
	public Optional<SpecialFeeDetailsVO> getFeeDetailsByCov(String covCode) {
		Optional<BidFeeMaster> bidFeeMasterOptional = getBidFeeMasterByCov(covCode);
		if (!bidFeeMasterOptional.isPresent()) {
			return Optional.empty();
		}

		SpecialFeeDetailsVO feeDetails = new SpecialFeeDetailsVO();

		BidFeeMaster bidFeeMaster = bidFeeMasterOptional.get();

		feeDetails.setRtaFee(bidFeeMaster.getRtaBidFee());
		feeDetails.setSpecialFee(bidFeeMaster.getSpecialNumFee());

		return Optional.of(feeDetails);
	}

}
