package org.epragati.sn.vo;

import java.util.List;

import org.epragati.sn.dto.SpecialNumberDetailsDTO;


public class BidParticipantsResult {

	private List<SpecialNumberDetailsDTO> losserParticipantDetails;
	private List<SpecialNumberDetailsDTO> winnerParticipantDetails;
	private List<SpecialNumberDetailsDTO> othersParticipantDetails;

	public BidParticipantsResult(List<SpecialNumberDetailsDTO> losserParticipantDetails,
			List<SpecialNumberDetailsDTO> winnerParticipantDetails) {
		super();
		this.losserParticipantDetails = losserParticipantDetails;
		this.winnerParticipantDetails = winnerParticipantDetails;
	}

	public BidParticipantsResult(List<SpecialNumberDetailsDTO> losserParticipantDetails,
			List<SpecialNumberDetailsDTO> winnerParticipantDetails,
			List<SpecialNumberDetailsDTO> othersParticipantDetails) {
		super();
		this.losserParticipantDetails = losserParticipantDetails;
		this.winnerParticipantDetails = winnerParticipantDetails;
		this.othersParticipantDetails = othersParticipantDetails;
	}

	public BidParticipantsResult() {
		super();
	}

	public List<SpecialNumberDetailsDTO> getLosserParticipantDetails() {
		return losserParticipantDetails;
	}

	public void setLosserParticipantDetails(List<SpecialNumberDetailsDTO> losserParticipantDetails) {
		this.losserParticipantDetails = losserParticipantDetails;
	}

	public List<SpecialNumberDetailsDTO> getWinnerParticipantDetails() {
		return winnerParticipantDetails;
	}

	public void setWinnerParticipantDetails(List<SpecialNumberDetailsDTO> winnerParticipantDetails) {
		this.winnerParticipantDetails = winnerParticipantDetails;
	}

	public List<SpecialNumberDetailsDTO> getOthersParticipantDetails() {
		return othersParticipantDetails;
	}

	public void setOthersParticipantDetails(List<SpecialNumberDetailsDTO> othersParticipantDetails) {
		this.othersParticipantDetails = othersParticipantDetails;
	}

}
