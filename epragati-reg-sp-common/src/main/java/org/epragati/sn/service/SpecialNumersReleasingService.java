package org.epragati.sn.service;

import java.util.List;

/**
 * 
 * @author krishnarjun.pampana
 *
 */
public interface SpecialNumersReleasingService {
	/**
	 * Service to release/reserve Special Numbers from the Block (Based on the Biding status pending)
	 * @return
	 */
	List<String> clrearSpPaymentPendingRecords();
	List<String>  clrearFinalpaymentPendingRecords();

}

