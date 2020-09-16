package com.macaowater.sorter;

import java.util.Comparator;

import com.macaowater.entity.Bill;
import com.macaowater.util.StringUtil;

public class SorterGIRO implements Comparator<Bill> {

	public int compare(Bill b1, Bill b2) {
		String postCode1 = b1.getmAddress().getPostCode();
		String postCode2 = b2.getmAddress().getPostCode();
		String isNoMail1 = String.valueOf(isNoMail(b1));
		String isNoMail2 = String.valueOf(isNoMail(b2));
		String lang1 = "3"; // for Chinese
		String lang2 = "3"; // for Chinese
		
		if (b1.isEBill() && !b2.isEBill()) {
			return 1;
		}
		else if (!b1.isEBill() && b2.isEBill()) {
			return -1;
		}
		
		if (b1.getLang_cd().equals("P")) {
			lang1 = "1";
		}
		else if (b1.getLang_cd().equals("E")) {
			lang1 = "2";
		}
		
		if (b2.getLang_cd().equals("P")) {
			lang2 = "1";
		}
		else if (b2.getLang_cd().equals("E")) {
			lang2 = "2";
		}
		
		int flag = 0;
		flag = lang1.compareTo(lang2);
		
		if (isNoMail1.equals("1") && isNoMail2.equals("1")) {
			return flag;
		}
		else if (isNoMail1.equals("1")) {
			return -1;
		}
		else if (isNoMail2.equals("1")) {
			return 1;
		}
		
		if (flag != 0) {
			return flag;
		}
		
		if (StringUtil.isEmptyStringEx(postCode1)) {
			if (!StringUtil.isEmptyStringEx(postCode2)) {
				flag = 1;
			}
			else {
				flag = 0;
			}
		}
		else if (StringUtil.isEmptyStringEx(postCode2)) {
			flag = -1;
		}
		else {
			flag = postCode1.compareTo(postCode2);
		}
		
		if (flag == 0) {
			// Compare second level
			return flag;
		}
		else {
			return flag;
		}
	}
	
	private int isNoMail(Bill b) {
		int result = 0;
		
		if (!StringUtil.isEmptyString(b.getCheck_bill_reason_cd())) {
			String checkDigit = b.getCheck_bill_reason_cd().trim();
			String mail_flag = b.getMail_flag();
			if (
					checkDigit.equals("B013") ||
					checkDigit.equals("B017") ||
					checkDigit.equals("B030") ||
					checkDigit.equals("B033") ||
					checkDigit.equals("B034") ||
					checkDigit.equals("B035") ||
					checkDigit.equals("B036") ||
					checkDigit.equals("B037") ||
					checkDigit.equals("B038") ||
					checkDigit.equals("B040") ||
					checkDigit.equals("B045")) {
				if (checkDigit.equals("B033")) {
					if (mail_flag.equals("2") || mail_flag.equals("3")) {
						result = 1;
					}
				}
				else {
					result = 1;
				}
			}
		}
		
		return result;
	}
}
