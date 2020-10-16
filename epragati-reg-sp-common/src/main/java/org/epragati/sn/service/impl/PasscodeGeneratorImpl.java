package org.epragati.sn.service.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.epragati.sn.service.PasscodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasscodeGeneratorImpl implements PasscodeGenerator {

	@Value("${generate.passcode.length:8}")
	private Integer passcodeLength;

	@Override
	public String generatePasscode() {

		//TODO: For testing
		return RandomStringUtils.randomAlphanumeric(passcodeLength);
		//return "123456";
	}
}
