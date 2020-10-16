package org.epragati.sn.service;

import java.util.List;
import java.util.Optional;

import org.epragati.sn.dto.BidFeeMaster;
import org.epragati.sn.dto.CovCategoryMaster;
import org.epragati.sn.vo.SpecialFeeDetailsVO;

public interface BidFeeMasterService {

	Optional<BidFeeMaster> getBidFeeMasterByCov(String covCode);

	Optional<SpecialFeeDetailsVO> getFeeDetailsByCov(String covCode);

	List<CovCategoryMaster> getCovCategoryMaster(List<String> covCodes);
}
