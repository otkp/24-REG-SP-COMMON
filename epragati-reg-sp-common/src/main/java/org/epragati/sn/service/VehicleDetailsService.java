package org.epragati.sn.service;

import org.epragati.sn.dto.CustomerDetails;
import org.epragati.sn.dto.SpecialNumberDetailsDTO;
import org.epragati.sn.dto.VehicleDetails;
import org.epragati.sn.vo.BidConfigMasterVO;
import org.springframework.data.util.Pair;

public interface VehicleDetailsService {

	Pair<VehicleDetails, CustomerDetails> getVehicleDetailsByTrNumber(String trNumber, String mobileNo,BidConfigMasterVO bidConfigMaster,boolean isPrNo);


	void generatePRNumber(SpecialNumberDetailsDTO participant, String prSeries);

}
