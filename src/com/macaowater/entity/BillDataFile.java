package com.macaowater.entity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import oracle.jdbc.OracleTypes;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.macaowater.db.CallResult;
import com.macaowater.db.DBConn;
import com.macaowater.defined.Labels;
import com.macaowater.defined.Line10;
import com.macaowater.defined.Line20;
import com.macaowater.defined.Line30;
import com.macaowater.defined.Line35;
import com.macaowater.defined.Line40;
import com.macaowater.defined.Line50;
import com.macaowater.defined.Line80;
import com.macaowater.pdf.PdfHelper;
import com.macaowater.pdf.PdfLabel;
import com.macaowater.sorter.SorterGIRO;
import com.macaowater.util.CommonEnv;
import com.macaowater.util.DbUtil;
import com.macaowater.util.MathUtil;
import com.macaowater.util.StringUtil;
import com.macaowater.util.XMLAttribute;
import com.macaowater.util.XMLHelper;

/**
 * BillDataFile
 * 
 * @author wugy
 * @date   03/08/2012
 * @comment A4 size -> 595.0/842.0
 * 
 *                  (595,842)
 *   +------------------+ 
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   |                  |
 *   +------------------+
 * (0,0)
 */
public class BillDataFile {

	public void buildPDF() throws Exception {
//		boolean isProdEnv = CommonEnv.isProductEnv();
//		isProdEnv = true;
		
		String logFile = "";
		String csvFile = "";
		String csvFileGiroEBill = "";
		String csvFileGiroMail = "";
		String csvFileGiroNoMail = "";
		String cntFile = "";
		
		String tmpFile_ebill = "";
		String tmpFile_nonebill_mail = "";
		String tmpFile_nonebill_nomail = "";
		
		String pdfFile_ebill = "";
		String pdfFile_nonebill_mail = "";
		String pdfFile_nonebill_nomail = "";
		
		FileWriter csvOutGiroEBill = null;
		FileWriter csvOutGiroMail = null;
		FileWriter csvOutGiroNoMail = null;
		
		// Excluded some bills and re-sort
		if (action.equals("REGEN")) {
			// Exclude some bills
			if (excludedEntities != null && excludedEntities.size() > 0) {
				for (Iterator<BillV2> itBills = bills.iterator(); itBills.hasNext();) {
					Bill bill = itBills.next();
					String entity = bill.getBody_nb() + bill.getAcct_nb() + bill.getTransc_nb();
					if (excludedEntities.contains(entity)) {
						System.out.println("exclude bill -> " + entity);
						itBills.remove();
						String result = cancelEBLHST(conn, bill.getPDFXMLKey());
						if (!StringUtil.isEmptyString(result)) {
							System.out.println("cancelEBLHST result=" + result);
						}
					}
				}
//				for (Bill bill : bills) {
//					String entity = bill.getBody_nb() + bill.getAcct_nb() + bill.getTransc_nb();
//					if (excludedEntities.contains(entity)) {
//						bills.remove(bill);
//					}
//				}
			}
			// Re-sort
			if (tySort.equals("1")) { // Order by GIRO
				SorterGIRO sorter = new SorterGIRO();
				Collections.sort(bills, sorter);
			}
			
			logFile = dataFile.getAbsolutePath().replace("done", "regen") + ".log";
			csvFile = dataFile.getAbsolutePath().replace("done", "regen") + ".csv";
			csvFileGiroEBill = dataFile.getAbsolutePath().replace("done", "regen") + "_giro_ebill.csv";
			csvFileGiroMail = dataFile.getAbsolutePath().replace("done", "regen") + "_giro_mail.csv";
			csvFileGiroNoMail = dataFile.getAbsolutePath().replace("done", "regen") + "_giro_nomail.csv";
			cntFile = dataFile.getAbsolutePath().replace("done", "regen") + ".cnt";
			
			tmpFile_ebill = dataFile.getAbsolutePath().replace(".dat.done", ".tmp.regen.ebill");
			tmpFile_nonebill_mail = dataFile.getAbsolutePath().replace(".dat.done", "_nonebill_mail.tmp.regen.pdf");
			tmpFile_nonebill_nomail = dataFile.getAbsolutePath().replace(".dat.done", "_nonebill_nomail.tmp.regen.pdf");
			
			pdfFile_ebill = dataFile.getAbsolutePath().replace(".dat.done", ".regen.ebill");
			pdfFile_nonebill_mail = dataFile.getAbsolutePath().replace(".dat.done", "_nonebill_mail.regen.pdf");
			pdfFile_nonebill_nomail = dataFile.getAbsolutePath().replace(".dat.done", "_nonebill_nomail.regen.pdf");
		}
		else {
			logFile = dataFile.getAbsolutePath() + ".log";
			csvFile = dataFile.getAbsolutePath() + ".csv";
			cntFile = dataFile.getAbsolutePath() + ".cnt";
			
			tmpFile_ebill = dataFile.getAbsolutePath().replace(".dat", ".tmp.ebill");
			tmpFile_nonebill_mail = dataFile.getAbsolutePath().replace(".dat", "_nonebill_mail.tmp.pdf");
			tmpFile_nonebill_nomail = dataFile.getAbsolutePath().replace(".dat", "_nonebill_nomail.tmp.pdf");
			
			pdfFile_ebill = dataFile.getAbsolutePath().replace(".dat", ".ebill");
			pdfFile_nonebill_mail = dataFile.getAbsolutePath().replace(".dat", "_nonebill_mail.pdf");
			pdfFile_nonebill_nomail = dataFile.getAbsolutePath().replace(".dat", "_nonebill_nomail.pdf");
		}
		
		if (bills.size() > 0) {
			try {		
				int billNumberOfEBill = 0;
				int billNumberOfNonEBillMail = 0;
				int billNumberOfNonEBillNoMail = 0;
				CommonEnv.billNumber = 0;
				
				String book = "";
				String prevGiroEBill = "";
				String prevGiroMail = "";
				String prevGiroNoMail = "";
				int start = 0;
				int startGiroEBill = 0;
				int startGiroMail = 0;
				int startGiroNoMail = 0;
				int end = 0;
				int endGiroEBill = 0;
				int endGiroMail = 0;
				int endGiroNoMail = 0;
				int total = 0;
				int totalGiroEBill = 0;
				int totalGiroMail = 0;
				int totalGiroNoMail = 0;
				
				String fontName = "DFEN_R7.TTF";
				BaseFont chnFont = BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
				
				FileWriter logOut = new FileWriter(logFile);
				FileWriter csvOut = new FileWriter(csvFile);
				
				if (action.equals("REGEN")) {
					csvOutGiroEBill = new FileWriter(csvFileGiroEBill);
					csvOutGiroMail = new FileWriter(csvFileGiroMail);
					csvOutGiroNoMail = new FileWriter(csvFileGiroNoMail);
				}
				FileWriter cntOut = new FileWriter(cntFile);
				
				Map<String, Integer> msgCnt = new HashMap<String, Integer>(); 
				
				StringBuilder bufInfoMultPageBills = new StringBuilder();
				
				// Header of CSV
				csvOut.append("Book,Start,End,Total\n");
				if (action.equals("REGEN")) {
					csvOutGiroEBill.append("Giro,Start,End,Total\n");
					csvOutGiroMail.append("Giro,Start,End,Total\n");
					csvOutGiroNoMail.append("Giro,Start,End,Total\n");
				}
				
				Document pdf_ebill = new Document(PageSize.A4);
				Document pdf_nonebill_mail = new Document(PageSize.A4);
				Document pdf_nonebill_nomail = new Document(PageSize.A4);
				
				PdfWriter writer_ebill = PdfWriter.getInstance(pdf_ebill, new FileOutputStream(new File(tmpFile_ebill)));
				PdfWriter writer_nonebill_mail = PdfWriter.getInstance(pdf_nonebill_mail, new FileOutputStream(new File(tmpFile_nonebill_mail)));
				PdfWriter writer_nonebill_nomail = PdfWriter.getInstance(pdf_nonebill_nomail, new FileOutputStream(new File(tmpFile_nonebill_nomail)));
								
				pdf_ebill.open();
				pdf_nonebill_mail.open();
				pdf_nonebill_nomail.open();
				
				pageNumberOfEBill = 0;
				pageNumberOfNonEBillMail = 0;
				pageNumberOfNonEBillNoMail = 0;
				for (int i = 0, len = bills.size(); i < len; i++) {
					
					BillV2 b = bills.get(i);
//					b.setProdEnv(isProdEnv);
					
					String logLine = "";
					
					String sessionKey = String.valueOf(System.currentTimeMillis());
					
//					if (b.getIn_print().equals("P")) {
					CommonEnv.billNumber++;
					
					int body_nb = Integer.parseInt(b.getBody_nb());
					int acct_nb = Integer.parseInt(b.getAcct_nb());
					int prprty_nb = Integer.parseInt(b.getPrprty_nb());
					int transc_nb = Integer.parseInt(b.getTransc_nb()); 
					// Check if there is a record in MPDFXML
//					boolean hasPDFXML = hasRowInMPDFXML(body_nb, acct_nb, prprty_nb, transc_nb);
					boolean hasPDFXML = false;
					
					if (!hasPDFXML) {
						XMLHelper.setRoot(sessionKey, "pdf");
						b.setSessionKey(sessionKey);
						
						List<XMLAttribute> attrs = new ArrayList<XMLAttribute>();
						XMLHelper.addChunk(sessionKey, "font", attrs, fontName);	
						XMLHelper.addChunk(sessionKey, "bill_dt", attrs, b.getBill_dt());	
					}
					
					System.out.println("building " + b.getBody_nb());
					if (!b.isEBill()) {
						b.setBuildEnvelop(true);

						if (isNoMail(b)) {
							billNumberOfNonEBillNoMail++;
							pageNumberOfNonEBillNoMail = b.buildPDF(pdf_nonebill_nomail, writer_nonebill_nomail, chnFont, pageNumberOfNonEBillNoMail);
							
						}
						else {
							billNumberOfNonEBillMail++;
							pageNumberOfNonEBillMail = b.buildPDF(pdf_nonebill_mail, writer_nonebill_mail, chnFont, pageNumberOfNonEBillMail);
							
						}
						
						logLine +=  String.valueOf(CommonEnv.billNumber) + " ";
					}
					else {
						b.setBuildEnvelop(false);
						billNumberOfEBill++;
						pageNumberOfEBill = b.buildPDF(pdf_ebill, writer_ebill, chnFont, pageNumberOfEBill);
						logLine += "[N] ";
					}
					
					// Insert record into MPDFXML
					if (!hasPDFXML && !StringUtil.isEmptyString(b.getPDFXMLKey())) {
//						String result = DbUtil.writeXML(conn, XMLHelper.getXMLString(sessionKey), b.getPDFXMLKey(), body_nb, acct_nb, prprty_nb, transc_nb, b.getBody_surname(), String.valueOf(b.getBill_am()), b.getBill_dt(), b.getDue_dt(), b.isEBill() ? XMLHelper.object2Xml(b, Bill.class) : "");
						String result = DbUtil.writeXML(conn, XMLHelper.getXMLString(sessionKey), b.getPDFXMLKey(), body_nb, acct_nb, prprty_nb, transc_nb, b.getBody_surname(), String.valueOf(b.getBill_am()), b.getBill_dt(), b.getDue_dt());
						if (!result.equals("OK")) {
							System.out.println("writeXML result=" + result);
						}
						XMLHelper.removeXMLString(sessionKey);
					} 
					
					// Insert record into MEBLHIS if it is a E-BILL
					String in_reminder = "N";
					if (b.getBill_color().equals("2")) {
						in_reminder = "Y";
					}
					if (b.isEBill() && action.equals("NORMAL_GEN")) {
						System.out.println("body_nb=" + body_nb + ",bill_color=" + b.getBill_color());
						String result = insertEBLHST(conn, b.getPDFXMLKey(), body_nb, acct_nb, prprty_nb, transc_nb, in_reminder);
						
						if (!StringUtil.isEmptyString(result)) {
							System.out.println("insertEBLHST result=" + result);
						}
					}
					
					
					String bk = b.getBatch_cd();
					if (!StringUtil.isEmptyString(b.getCheck_bill_reason_cd())) {
						bk = b.getCheck_bill_reason_cd() + "-" + b.getMail_flag();
					}
					
					if (!bk.equals(book)) {
						if (end != 0) {
							csvOut.append(book + "," + start + "," + end + "," + total + "\n");
							total = 0;
						}

						start = CommonEnv.billNumber;	
						end = CommonEnv.billNumber;						
						book = bk;
					}
					else {
						end = CommonEnv.billNumber;
					}
					total++;
					
					if (action.equals("REGEN")) {
						String postCode = b.getmAddress().getPostCode();
						if (isNoMail(b) && !StringUtil.isEmptyString(b.getCheck_bill_reason_cd())) {
							postCode = b.getCheck_bill_reason_cd() + "-" + b.getMail_flag();
						}
						if (StringUtil.isEmptyString(postCode)) {
							postCode = "[No Giro]";
						}
						else {
							postCode = "[" + postCode + "]";
						}
						String giro = b.getLang_cd() + postCode;
						if (b.isEBill()) {
							if (!giro.equals(prevGiroEBill)) {
								if (endGiroEBill != 0) {
									csvOutGiroEBill.append(prevGiroEBill + "," + startGiroEBill + "," + endGiroEBill + "," + totalGiroEBill + "\n");
									totalGiroEBill = 0;
								}
								
								if (endGiroNoMail != 0) {
									csvOutGiroNoMail.append(prevGiroNoMail + "," + startGiroNoMail + "," + endGiroNoMail + "," + totalGiroNoMail + "\n");
									totalGiroNoMail = 0;
									startGiroNoMail = 0;
									endGiroNoMail = 0;
									prevGiroNoMail = "";
								}
								
								if (endGiroMail != 0) {
									csvOutGiroMail.append(prevGiroMail + "," + startGiroMail + "," + endGiroMail + "," + totalGiroMail + "\n");
									totalGiroMail = 0;
									startGiroMail = 0;
									endGiroMail = 0;
									prevGiroMail = "";
								}
								
								startGiroEBill = CommonEnv.billNumber;
								endGiroEBill = CommonEnv.billNumber;
								prevGiroEBill = giro;
							}
							else {
								endGiroEBill = CommonEnv.billNumber;
							}
							totalGiroEBill++;
						}
						else if (isNoMail(b)) {
							if (!giro.equals(prevGiroNoMail)) {
								if (endGiroNoMail != 0) {
									csvOutGiroNoMail.append(prevGiroNoMail + "," + startGiroNoMail + "," + endGiroNoMail + "," + totalGiroNoMail + "\n");
									totalGiroNoMail = 0;
								}
								
								if (endGiroEBill != 0) {
									csvOutGiroEBill.append(prevGiroEBill + "," + startGiroEBill + "," + endGiroEBill + "," + totalGiroEBill + "\n");
									totalGiroEBill = 0;
									startGiroEBill = 0;
									endGiroEBill = 0;
									prevGiroEBill = "";
								}
								
								if (endGiroMail != 0) {
									csvOutGiroMail.append(prevGiroMail + "," + startGiroMail + "," + endGiroMail + "," + totalGiroMail + "\n");
									totalGiroMail = 0;
									startGiroMail = 0;
									endGiroMail = 0;
									prevGiroMail = "";
								}
								
								startGiroNoMail = CommonEnv.billNumber;
								endGiroNoMail = CommonEnv.billNumber;
								prevGiroNoMail = giro;
							}
							else {
								endGiroNoMail = CommonEnv.billNumber;
							}
							totalGiroNoMail++;
						}
						else {
							if (!giro.equals(prevGiroMail)) {
								if (endGiroMail != 0) {
									csvOutGiroMail.append(prevGiroMail + "," + startGiroMail + "," + endGiroMail + "," + totalGiroMail + "\n");
									totalGiroMail = 0;
								}
								
								if (endGiroEBill != 0) {
									csvOutGiroEBill.append(prevGiroEBill + "," + startGiroEBill + "," + endGiroEBill + "," + totalGiroEBill + "\n");
									totalGiroEBill = 0;
									startGiroEBill = 0;
									endGiroEBill = 0;
									prevGiroEBill = "";
								}
								
								if (endGiroNoMail != 0) {
									csvOutGiroNoMail.append(prevGiroNoMail + "," + startGiroNoMail + "," + endGiroNoMail + "," + totalGiroNoMail + "\n");
									totalGiroNoMail = 0;
									startGiroNoMail = 0;
									endGiroNoMail = 0;
									prevGiroNoMail = "";
								}
								
								startGiroMail = CommonEnv.billNumber;
								endGiroMail = CommonEnv.billNumber;
								prevGiroMail = giro;
							}
							else {
								endGiroMail = CommonEnv.billNumber;
							}
							totalGiroMail++;
						}
					}
					
					
					String msgCd = b.getMessage_cd().substring(1);
					if (msgCnt.containsKey(msgCd)) {
						int count = msgCnt.get(msgCd) + 1;
						msgCnt.remove(msgCd);
						msgCnt.put(msgCd, count);
					}
					else {
						msgCnt.put(b.getMessage_cd().substring(1), 1);
					}
					
					logLine += b.getBody_nb() + " ";
					if (!StringUtil.isEmptyString(b.getCheck_bill_reason_cd())) {
						logLine += b.getCheck_bill_reason_cd() + "-" + b.getMail_flag();
					}
					else {
						logLine += b.getBatch_cd();
					}
					logOut.append(logLine + "\n");
					
					// Get information of multi-page bills
					int pgSize = b.getPgInfos().size();
					if ((!b.isEBill() && pgSize > 2) || (b.isEBill() && pgSize > 1)) {
						bufInfoMultPageBills.append("BODY[" + b.getBody_nb() + "] at page " + b.getPgInfos().get(0).getPageNumberOfTotal() + "\n");
					}
					
					if (action.equals("NORMAL_GEN")) {
						if (conn.conn != null) {
							CallResult cr = addPDFLog(conn, dataFile.getName(), b.getBody_nb(), b.getAcct_nb(), b.getTransc_nb(), b.getBatch_nb(), b.getLang_cd(), b.isEBill(), in_reminder, b.getCheck_bill_reason_cd());
							if (cr.getRcd() != 0) {
								logOut.close();
								csvOut.close();
								cntOut.close();
								throw cr.buildException();
							}
						}
					}
					
					b.releaseTxnTables();
				}
				csvOut.append(book + "," + start + "," + end + "," + total + "\n");
				
				if (action.equals("REGEN")) {
					if (endGiroEBill != 0) {
						csvOutGiroEBill.append(prevGiroEBill + "," + startGiroEBill + "," + endGiroEBill + "," + totalGiroEBill + "\n");
					}
					if (endGiroNoMail != 0) {
						csvOutGiroNoMail.append(prevGiroNoMail + "," + startGiroNoMail + "," + endGiroNoMail + "," + totalGiroNoMail + "\n");
					}
					if (endGiroMail != 0) {
						csvOutGiroMail.append(prevGiroMail + "," + startGiroMail + "," + endGiroMail + "," + totalGiroMail + "\n");
					}
					csvOutGiroEBill.close();
					csvOutGiroMail.close();
					csvOutGiroNoMail.close();
				}
				
				for (String msgCd : msgCnt.keySet()) {
					cntOut.append(msgCd + "," + msgCnt.get(msgCd) + "\n");
				}
				
				logOut.close();
				csvOut.close();
				cntOut.close();
				
				pdf_ebill.newPage();
				pdf_ebill.add(new Paragraph("Bill Data file: " + dataFile.getName()));
				pdf_ebill.add(new Paragraph("Bills of EBill: " + billNumberOfEBill));
				pdf_ebill.add(new Paragraph("Bills of NonEBillMail: " + billNumberOfNonEBillMail));
				pdf_ebill.add(new Paragraph("Bills of NonEBillNoMail: " + billNumberOfNonEBillNoMail));
				pdf_ebill.add(new Paragraph("Bills of Total: " + CommonEnv.billNumber));
				if (bufInfoMultPageBills.toString().length() > 0) {
					pdf_ebill.add(new Paragraph("Bills of Multi-pages: \n" + bufInfoMultPageBills.toString()));
				}
				else {
					pdf_ebill.add(new Paragraph("Bills of Multi-pages: None"));
				}
				pdf_ebill.add(new Paragraph("EBill"));
				
				pdf_nonebill_mail.newPage();
				pdf_nonebill_mail.add(new Paragraph("Bill Data file: " + dataFile.getName()));
				pdf_nonebill_mail.add(new Paragraph("Bills of EBill: " + billNumberOfEBill));
				pdf_nonebill_mail.add(new Paragraph("Bills of NonEBillMail: " + billNumberOfNonEBillMail));
				pdf_nonebill_mail.add(new Paragraph("Bills of NonEBillNoMail: " + billNumberOfNonEBillNoMail));
				pdf_nonebill_mail.add(new Paragraph("Bills of Total: " + CommonEnv.billNumber));
				if (bufInfoMultPageBills.toString().length() > 0) {
					pdf_nonebill_mail.add(new Paragraph("Bills of Multi-pages: \n" + bufInfoMultPageBills.toString()));
				}
				else {
					pdf_nonebill_mail.add(new Paragraph("Bills of Multi-pages: None"));
				}
				pdf_nonebill_mail.add(new Paragraph("NonEBill"));
				
				pdf_nonebill_nomail.newPage();
				pdf_nonebill_nomail.add(new Paragraph("Bill Data file: " + dataFile.getName()));
				pdf_nonebill_nomail.add(new Paragraph("Bills of EBill: " + billNumberOfEBill));
				pdf_nonebill_nomail.add(new Paragraph("Bills of NonEBillMail: " + billNumberOfNonEBillMail));
				pdf_nonebill_nomail.add(new Paragraph("Bills of NonEBillNoMail: " + billNumberOfNonEBillNoMail));
				pdf_nonebill_nomail.add(new Paragraph("Bills of Total: " + CommonEnv.billNumber));
				if (bufInfoMultPageBills.toString().length() > 0) {
					pdf_nonebill_nomail.add(new Paragraph("Bills of Multi-pages: \n" + bufInfoMultPageBills.toString()));
				}
				else {
					pdf_nonebill_nomail.add(new Paragraph("Bills of Multi-pages: None"));
				}
				pdf_nonebill_nomail.add(new Paragraph("NonEBill"));
				
				pdf_ebill.close();
				pdf_nonebill_mail.close();
				pdf_nonebill_nomail.close();
				
				PdfReader readerEBill = new PdfReader(tmpFile_ebill);
				PdfReader readerNonEBillMail= new PdfReader(tmpFile_nonebill_mail);
				PdfReader readerNonEBillNoMail= new PdfReader(tmpFile_nonebill_nomail);
				PdfStamper stamper_ebill = new PdfStamper(readerEBill, new FileOutputStream(new File(pdfFile_ebill)));
				PdfStamper stamper_nonebill_mail = new PdfStamper(readerNonEBillMail, new FileOutputStream(new File(pdfFile_nonebill_mail)));
				PdfStamper stamper_nonebill_nomail = new PdfStamper(readerNonEBillNoMail, new FileOutputStream(new File(pdfFile_nonebill_nomail)));
				
//				PdfStamper stamper_mail = new PdfStamper(new PdfReader(tmpFile_mail), new FileOutputStream(new File(pdfFile_mail)), '3');
//				PdfStamper stamper_nomail = new PdfStamper(new PdfReader(tmpFile_nomail), new FileOutputStream(new File(pdfFile_nomail)), '3');
				
				for (int i = 0, len = bills.size(); i < len; i++) {
					BillV2 b = bills.get(i);
					
					PdfLabel lbPage = Labels.getLabel("Page", b.getLang_cd());
					
					int pgSize = b.getPgInfos().size();
					if ((!b.isEBill() && pgSize > 2) || (b.isEBill() && pgSize > 1)) {
					
						XMLHelper.setRoot(b.getPDFXMLKey(), "pdf");
						
						int fontSize = 8;
						int firstPageNumber = 0;
						for (int j = 0; j < pgSize; j++) {
							PageNumberInfo pni = b.getPgInfos().get(j);
							
							if (j == 0) {
								firstPageNumber = pni.getPageNumberOfTotal();
							}
							
							PdfContentByte cb = stamper_ebill.getOverContent(pni.getPageNumberOfTotal());
							if (!b.isEBill()) {
								
								if (isNoMail(b)) {
									cb = stamper_nonebill_nomail.getOverContent(pni.getPageNumberOfTotal());
								}
								else {
									cb = stamper_nonebill_mail.getOverContent(pni.getPageNumberOfTotal());									
								}
							}
							
							float x1 = 500f;
							float y1 = 10f;
							float x2 = 530f;
							float y2 = 10f;
							boolean rotate180 = false;
							
							if (pni.isInEnvelop()) {
								x1 = 120f;
								y1 = 750f;
								x2 = 90f;
								y2 = 750f;
								rotate180 = true;
							}
							
							if (b.getLang_cd().equals("C")) {
								PdfHelper.showText(cb, chnFont, fontSize, x1, y1, "" + pni.getPageNumberOfBill() + " / " + b.getPageNumberOfBill(), rotate180, b.getPDFXMLKey(), pni.getPageNumberOfTotal() - firstPageNumber + 1);					
								PdfHelper.showText(cb, chnFont, fontSize, x2, y2, lbPage.getValue(), rotate180, b.getPDFXMLKey(), pni.getPageNumberOfTotal() - firstPageNumber + 1);	
							}
							else {
								PdfHelper.showText(cb, chnFont, fontSize, x1, y1, lbPage.getValue(), rotate180, b.getPDFXMLKey(), pni.getPageNumberOfTotal() - firstPageNumber + 1);	
								PdfHelper.showText(cb, chnFont, fontSize, x2, y2, "" + pni.getPageNumberOfBill() + " / " + b.getPageNumberOfBill(), rotate180, b.getPDFXMLKey(), pni.getPageNumberOfTotal() - firstPageNumber + 1);	
							}
						}
				
						// Insert MPDFXMLEX
						writeXMLEX(conn, XMLHelper.getXMLString(b.getPDFXMLKey()), b.getPDFXMLKey());
						XMLHelper.removeXMLString(b.getPDFXMLKey());
					}
					
					
				}
				stamper_ebill.close();
				stamper_nonebill_mail.close();
				stamper_nonebill_nomail.close();
				readerEBill.close();
				readerNonEBillMail.close();
				readerNonEBillNoMail.close();

				File tmp = new File(tmpFile_ebill);
				tmp.delete();
				File tmpnon_mail = new File(tmpFile_nonebill_mail);
				tmpnon_mail.delete();
				File tmpnon_nomail = new File(tmpFile_nonebill_nomail);
				tmpnon_nomail.delete();
				
				if (billNumberOfEBill == 0) {
					File mailPDF = new File(pdfFile_ebill);
					mailPDF.delete();
				}
				if (billNumberOfNonEBillMail == 0) {
					File mailPDF = new File(pdfFile_nonebill_mail);
					mailPDF.delete();
				}
				if (billNumberOfNonEBillNoMail == 0) {
					File nomailPDF = new File(pdfFile_nonebill_nomail);
					nomailPDF.delete();
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("CommonEnv.billNumber=" + CommonEnv.billNumber);
				throw new Exception(ex);
			}
		}
	}
	public static String toUnicode(String source) {
		StringBuffer sb = new StringBuffer();
		int len = source.length();
		for (int i = 0; i < len; ++i) {		
			if (source.charAt(i) > 127) 
				sb.append("\\u" + (source.charAt(i) > 255 ? "" : "00") + Integer.toHexString(source.charAt(i)));
			else
				sb.append(source.charAt(i));
		}
		return sb.toString();
	}
	public void buildEntity() throws Exception {
		if (bills != null) {
			bills = null;
		}
		bills = new ArrayList<BillV2>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			String line;
			BillV2 bill = new BillV2();
			BillItem bi = new BillItem();
			while ((line = reader.readLine()) != null) {
				
				if (line.startsWith("10")) {
					String body_nb = StringUtil.extractBytes(line, Line10.BODY_NB_START, Line10.BODY_NB_LEN);
					System.out.println("handling body " + body_nb);
					String acct_nb = StringUtil.extractBytes(line, Line10.ACCT_NB_START, Line10.ACCT_NB_LEN);
					String transc_nb = StringUtil.extractBytes(line, Line10.TRANSC_NB_START, Line10.TRANSC_NB_LEN);
					String in_ebill = StringUtil.extractBytes(line, Line10.IN_EBILL_START, Line10.IN_EBILL_LEN);
					String in_production = StringUtil.extractBytes(line, Line10.IN_PRODUCTION_START, Line10.IN_PRODUCTION_LEN);
					
					bill = new BillV2();
					bill.setBody_nb(body_nb);
					bill.setAcct_nb(acct_nb);
					bill.setTransc_nb(transc_nb);
//					bill.setIn_print(in_ebill);
					bill.setEBill(in_ebill.equals("Y"));
					bill.setProdEnv(in_production.equals("1"));
					
					bills.add(bill);
				}
				
				if (line.startsWith("20")) {
					String prprty_nb = StringUtil.extractBytes(line, Line20.PRPRTY_NB_START, Line20.PRPRTY_NB_LEN);
					String bill_number = StringUtil.extractBytes(line, Line20.BILL_NB_START, Line20.BILL_NB_LEN);
					String lang_cd = StringUtil.extractBytes(line, Line20.LANG_CD_START, Line20.LANG_CD_LEN);
					String mail_flag = StringUtil.extractBytes(line, Line20.MAIL_FLAG_START, Line20.MAIL_FLAG_LEN);
					String bill_color = StringUtil.extractBytes(line, Line20.BILL_COLOR_START, Line20.BILL_COLOR_LEN);
					String check_bill_reason_cd = StringUtil.extractBytes(line, Line20.CHECK_BILL_REASON_CD_START, Line20.CHECK_BILL_REASON_CD_LEN);
					String delivery_order = StringUtil.extractBytes(line, Line20.DELIVERY_ORDER_START, Line20.DELIVERY_ORDER_LEN);
					String building = StringUtil.extractBytes(line, Line20.BUILDING_START, Line20.BUILDING_LEN, "MS950");
					String street_name = StringUtil.extractBytes(line, Line20.STREET_NAME_START, Line20.STREET_NAME_LEN, "MS950");
					String street_nb = StringUtil.extractBytes(line, Line20.STREET_NB_START, Line20.STREET_NB_LEN, "MS950");
					String town_name = StringUtil.extractBytes(line, Line20.TOWN_NAME_START, Line20.TOWN_NAME_LEN, "MS950");
					String body_surname = StringUtil.extractBytes(line, Line20.BODY_SURNAME_START, Line20.BODY_SURNAME_LEN, "MS950");
					String acct_check_digit = StringUtil.extractBytes(line, Line20.ACCT_CHECK_DIGIT_START, Line20.ACCT_CHECK_DIGIT_LEN);
					String acct_ty_code = StringUtil.extractBytes(line, Line20.ACCTYP_CD_START, Line20.ACCTYP_CD_LEN);
					String propty_cd = StringUtil.extractBytes(line, Line20.PROPTY_CD_START, Line20.PROPTY_CD_LEN);
					String address_line1 = StringUtil.extractBytes(line, Line20.ADDRESS_LINE1_START, Line20.ADDRESS_LINE1_LEN, "MS950");
					String address_line2 = StringUtil.extractBytes(line, Line20.ADDRESS_LINE2_START, Line20.ADDRESS_LINE2_LEN, "MS950");
					String address_line3 = StringUtil.extractBytes(line, Line20.ADDRESS_LINE3_START, Line20.ADDRESS_LINE3_LEN, "MS950");
					String address_line4 = StringUtil.extractBytes(line, Line20.ADDRESS_LINE4_START, Line20.ADDRESS_LINE4_LEN, "MS950");
					String address_line5 = StringUtil.extractBytes(line, Line20.ADDRESS_LINE5_START, Line20.ADDRESS_LINE5_LEN, "MS950");
					String address_line6 = StringUtil.extractBytes(line, Line20.ADDRESS_LINE6_START, Line20.ADDRESS_LINE6_LEN, "MS950");
					String cstgrp_nb = StringUtil.extractBytes(line, Line20.CSTGRP_NB_START, Line20.CSTGRP_NB_LEN);
					String blccl_nb = StringUtil.extractBytes(line, Line20.BLCCL_NB_START, Line20.BLCCL_NB_LEN);
					String batch_cd = StringUtil.extractBytes(line, Line20.BATCH_CD_START, Line20.BATCH_CD_LEN);
					String batch_nb = StringUtil.extractBytes(line, Line20.BATCH_NB_START, Line20.BATCH_NB_LEN);
					String reason_cd = StringUtil.extractBytes(line, Line20.REASON_CD_START, Line20.REASON_CD_LEN);
					String transc_ty_cd = StringUtil.extractBytes(line, Line20.TRANSC_TY_START, Line20.TRANSC_TY_LEN);
					String bill_ty = StringUtil.extractBytes(line, Line20.BILL_TY_START, Line20.BILL_TY_LEN);
					String bill_dt = StringUtil.extractBytes(line, Line20.BILL_DT_START, Line20.BILL_DT_LEN);
					String beginning_dt_period = StringUtil.extractBytes(line, Line20.BD_PERIOD_START, Line20.BD_PERIOD_LEN);
					String ending_dt_period = StringUtil.extractBytes(line, Line20.ED_PERIOD_START, Line20.ED_PERIOD_LEN);
					String beginning_dt_item = StringUtil.extractBytes(line, Line20.BD_ITEM_START, Line20.BD_ITEM_LEN);
					String nb_per_aa = StringUtil.extractBytes(line, Line20.NB_PER_AA_START, Line20.NB_PER_AA_LEN);
					String nb_per = StringUtil.extractBytes(line, Line20.NB_PER_START, Line20.NB_PER_LEN);
					String ending_dt_item = StringUtil.extractBytes(line, Line20.ED_ITEM_START, Line20.ED_ITEM_LEN);
					String transc_am = StringUtil.extractBytes(line, Line20.TRANSC_AM_START, Line20.TRANSC_AM_LEN);
					String in_measured = StringUtil.extractBytes(line, Line20.IN_MEASURED_START, Line20.IN_MEASURED_LEN);
					String paymnt_ty = StringUtil.extractBytes(line, Line20.PAYMNT_TY_START, Line20.PAYMNT_TY_LEN);
					String last_balance = StringUtil.extractBytes(line, Line20.LAST_BALANCE_START, Line20.LAST_BALANCE_LEN);
					String due_dt = StringUtil.extractBytes(line, Line20.DUE_DT_START, Line20.DUE_DT_LEN);
					String last_pay_dt = StringUtil.extractBytes(line, Line20.LAST_PAY_DT_START, Line20.LAST_PAY_DT_LEN);
					String autopay_bank_cd = StringUtil.extractBytes(line, Line20.AUTOPAY_BANK_CD_START, Line20.AUTOPAY_BANK_CD_LEN);
					String autopay_dt = StringUtil.extractBytes(line, Line20.AUTOPAY_DT_START, Line20.AUTOPAY_DT_LEN);
					String last_paymnt_receive_dt = StringUtil.extractBytes(line, Line20.LAST_PAYMNT_RECEIVE_DT_START, Line20.LAST_PAYMNT_RECEIVE_DT_LEN);
					String paymnt_cutoff_dt = StringUtil.extractBytes(line, Line20.PAYMENT_CUTOFF_DT_START, Line20.PAYMENT_CUTOFF_DT_LEN);
					String last_received_am = StringUtil.extractBytes(line, Line20.LAST_RECEIVED_AM_START, Line20.LAST_RECEIVED_AM_LEN);
					String late_charge = StringUtil.extractBytes(line, Line20.LATE_CHARGE_START, Line20.LATE_CHARGE_LEN);
					String next_late_charge = StringUtil.extractBytes(line, Line20.NEXT_LATE_CHARGE_START, Line20.NEXT_LATE_CHARGE_LEN);
					String acct_addrid_cd = StringUtil.extractBytes(line, Line20.ACCT_ADDRID_CD_START, Line20.ACCT_ADDRID_CD_LEN);
					String prprty_addrid_cd = StringUtil.extractBytes(line, Line20.PRPRTY_ADDRID_CD_START, Line20.PRPRTY_ADDRID_CD_LEN);
					String GD_nb = StringUtil.extractBytes(line, Line20.GD_NB_START, Line20.GD_NB_LEN);
					String GD_am = StringUtil.extractBytes(line, Line20.GD_AM_START, Line20.GD_AM_LEN);
					String next_reading_dt = StringUtil.extractBytes(line, Line20.NEXT_READING_DT_START, Line20.NEXT_READING_DT_LEN);
					String in_latest_bill = StringUtil.extractBytes(line, Line20.IN_LATEST_BILL_START, Line20.IN_LATEST_BILL_LEN);
					String customer_ty = StringUtil.extractBytes(line, Line20.CUSTOMER_TY_START, Line20.CUSTOMER_TY_LEN);
					String customer_ty_label = StringUtil.extractBytes(line, Line20.CUSTOMER_TY_LABEL_START, Line20.CUSTOMER_TY_LABEL_LEN, "MS950");
					String nb_apartments = StringUtil.extractBytes(line, Line20.NB_APARTMENTS_START, Line20.NB_APARTMENTS_LEN);
					String fase = StringUtil.extractBytes(line, Line20.FASE_START, Line20.FASE_LEN, "MS950");
					String block = StringUtil.extractBytes(line, Line20.BLOCK_START, Line20.BLOCK_LEN, "MS950");
					String floor = StringUtil.extractBytes(line, Line20.FLOOR_START, Line20.FLOOR_LEN, "MS950");
					String flat = StringUtil.extractBytes(line, Line20.FLAT_START, Line20.FLAT_LEN, "MS950");
					String remark = StringUtil.extractBytes(line, Line20.REMARK_START, Line20.REMARK_LEN, "MS950");
					String propty_cd_label = StringUtil.extractBytes(line, Line20.PROPTY_CD_LABEL_START, Line20.PROPTY_CD_LABEL_LEN, "MS950");
					String paymetURL = StringUtil.extractBytes(line, Line20.PAYMENT_URL_START, Line20.PAYMENT_URL_LEN);
					String PDFXMLKey = StringUtil.extractBytes(line, Line20.PDFXMLKEY_START, Line20.PDFXMLKEY_LEN);
					String subsidyInfo = StringUtil.extractBytes(line, Line20.SUBSIDYINFO_START, Line20.SUBSIDYINFO_LEN);
					String maddrssLang = StringUtil.extractBytes(line, Line20.MADDRSS_LANG_START, Line20.MADDRSS_LANG_LEN);
					String maddrssLine1 = StringUtil.extractBytes(line, Line20.MADDRSS_LINE1_START, Line20.MADDRSS_LINE1_LEN, "MS950");
					String maddrssLine2 = StringUtil.extractBytes(line, Line20.MADDRSS_LINE2_START, Line20.MADDRSS_LINE2_LEN, "MS950");
					String maddrssLine3 = StringUtil.extractBytes(line, Line20.MADDRSS_LINE3_START, Line20.MADDRSS_LINE3_LEN, "MS950");
					String maddrssLine4 = StringUtil.extractBytes(line, Line20.MADDRSS_LINE4_START, Line20.MADDRSS_LINE4_LEN, "MS950");
					String maddrssLine5 = StringUtil.extractBytes(line, Line20.MADDRSS_LINE5_START, Line20.MADDRSS_LINE5_LEN, "MS950");
					String maddrssLine6 = StringUtil.extractBytes(line, Line20.MADDRSS_LINE6_START, Line20.MADDRSS_LINE6_LEN, "MS950");
					String pPostCode = StringUtil.extractBytes(line, Line20.P_POST_CODE_START, Line20.P_POST_CODE_LEN);
					String mPostCode = StringUtil.extractBytes(line, Line20.M_POST_CODE_START, Line20.M_POST_CODE_LEN);
					String bankDebitDate = StringUtil.extractBytes(line, Line20.BANK_DEBIT_DATE_START, Line20.BANK_DEBIT_DATE_LEN); // 15264
					
					bill.setPrprty_nb(prprty_nb);
					bill.setBill_number(bill_number);
					
					bill.setLang_cd(lang_cd);
					bill.getPotableWater().setLang(lang_cd);
					bill.getRecycledWater().setLang(lang_cd);
					bill.getpAddress().setLang(lang_cd);
					
					bill.setMail_flag(mail_flag);
					bill.setBill_color(bill_color);
					bill.setCheck_bill_reason_cd(check_bill_reason_cd.trim());
					if (bill.getCheck_bill_reason_cd().equals("B044")) {
						bill.setSpecialCase4HQ(true);
					}
					if (bill.getCheck_bill_reason_cd().equals("B046")) {
						bill.setSpecialCase4HZMB(true);
					}
					bill.setDelivery_order(delivery_order.trim());
					
					bill.getpAddress().setBuilding(building.trim());
					bill.getpAddress().setStreetName(street_name.trim());
					bill.getpAddress().setStreetNb(street_nb.trim());
					bill.getpAddress().setTown(town_name.trim());
					bill.getpAddress().setPostCode(pPostCode);

					bill.setBody_surname(body_surname);
					bill.setAcct_check_digit(acct_check_digit);
					bill.setAcct_ty_code(acct_ty_code);
					bill.setPropty_cd(propty_cd);

					bill.getmAddress().setLang(maddrssLang);
					bill.getmAddress().setPostCode(mPostCode);
					if (!StringUtil.isEmptyString(maddrssLang) && !maddrssLang.equals("N")) {
						bill.getmAddress().setAddrLine1(maddrssLine1);
						bill.getmAddress().setAddrLine2(maddrssLine2);
						bill.getmAddress().setAddrLine3(maddrssLine3);
						bill.getmAddress().setAddrLine4(maddrssLine4);
						bill.getmAddress().setAddrLine5(maddrssLine5);
						bill.getmAddress().setAddrLine6(maddrssLine6);
					}
					else {
						bill.getmAddress().setLang(lang_cd);
						bill.getmAddress().setAddrLine1(address_line1);
						bill.getmAddress().setAddrLine2(address_line2);
						bill.getmAddress().setAddrLine3(address_line3);
						bill.getmAddress().setAddrLine4(address_line4);
						bill.getmAddress().setAddrLine5(address_line5);
						bill.getmAddress().setAddrLine6(address_line6);
					}
					
					bill.setCstgrp_nb(cstgrp_nb);
					bill.setBlccl_nb(blccl_nb);
					bill.setBatch_cd(batch_cd);
					bill.setBatch_nb(batch_nb);
					bill.setReason_cd(reason_cd.trim());
					bill.setTransc_ty_cd(transc_ty_cd);
					bill.setBill_ty(bill_ty);
					bill.setBill_dt(bill_dt);
					bill.setBeginning_dt_period(beginning_dt_period);
					bill.setEnding_dt_period(ending_dt_period);
					bill.setBeginning_dt_item(beginning_dt_item);
					bill.setNb_per_aa(nb_per_aa);
					bill.setNb_per(nb_per);
					bill.setEnding_dt_item(ending_dt_item);
					bill.setTransc_am(transc_am);
					bill.setIn_measured(in_measured);
					bill.setPaymnt_ty(paymnt_ty);
					bill.setLast_balance(last_balance);
					bill.setDue_dt(due_dt);
					bill.setLast_pay_dt(last_pay_dt);
					bill.setAutopay_bank_cd(autopay_bank_cd);
					bill.setAutopay_dt(autopay_dt);
					bill.setLast_paymnt_receive_dt(last_paymnt_receive_dt);
					bill.setPayment_cutoff_dt(paymnt_cutoff_dt);
					bill.setLast_received_am(last_received_am);
					bill.setLate_charge(late_charge);
					bill.setNext_late_charge(next_late_charge);
					bill.setAcct_addrid_cd(acct_addrid_cd);
					bill.setPrprty_addrid_cd(prprty_addrid_cd);
					bill.setGD_nb(GD_nb);
					bill.setGD_am(GD_am);
					bill.getPotableWater().setGdAm(GD_am.trim());
					bill.getPotableWater().setGdNb(GD_nb.trim());
					bill.setNext_reading_dt(next_reading_dt);
					bill.setIn_latest_bill(in_latest_bill);
					bill.setCustomer_ty(customer_ty);
					bill.setCustomer_ty_label(customer_ty_label);
					bill.setNb_apartments(nb_apartments);
					bill.setLb_payment_URL(paymetURL);
					bill.setPDFXMLKey(PDFXMLKey);
					bill.setSubsidyInfo(subsidyInfo);
					bill.setBankDebitDate(bankDebitDate); // 15264
					
					bill.setShowFlag();
					
					bill.getpAddress().setFase(fase.trim());
					bill.getpAddress().setBlock(block.trim());
					bill.getpAddress().setFloor(floor.trim());
					bill.getpAddress().setFlat(flat.trim());
					bill.getpAddress().setRemark(remark.trim());
					bill.getpAddress().setProptyCodeLabel(propty_cd_label.trim());
					
					// get poster slogans after get language
					getPosterSlogans(conn, bill);
				}
				
				if (line.startsWith("30")) {
					String delpnt_nb = StringUtil.extractBytes(line, Line30.DELPNT_NB_START, Line30.DELPNT_NB_LEN);
					String metpnt_nb = StringUtil.extractBytes(line, Line30.METPNT_NB_START, Line30.METPNT_NB_LEN);
					String serial_nb = StringUtil.extractBytes(line, Line30.SERIAL_NB_START, Line30.SERIAL_NB_LEN);
					String present_reading_dt = StringUtil.extractBytes(line, Line30.PRESENT_READING_DT_START, Line30.PRESENT_READING_DT_LEN);
					String present_reading_index = StringUtil.extractBytes(line, Line30.PRESENT_READING_INDEX_START, Line30.PRESENT_READING_INDEX_LEN);
					String present_reading_cd = StringUtil.extractBytes(line, Line30.PRESENT_READING_CD_START, Line30.PRESENT_READING_CD_LEN);
					String previous_reading_dt = StringUtil.extractBytes(line, Line30.PREVIOUS_READING_DT_START, Line30.PREVIOUS_READING_DT_LEN);
					String previous_reading_index = StringUtil.extractBytes(line, Line30.PREVIOUS_READING_INDEX_START, Line30.PREVIOUS_READING_INDEX_LEN);
					String previous_reading_cd = StringUtil.extractBytes(line, Line30.PREVIOUS_READING_CD_START, Line30.PREVIOUS_READING_CD_LEN);
					String chargeable_volume = StringUtil.extractBytes(line, Line30.CHARGABLE_VOLUME_START, Line30.CHARGABLE_VOLUME_LEN);
					String meter_size = StringUtil.extractBytes(line, Line30.METER_SIZE_START, Line30.METER_SIZE_LEN);
					String meter_days = StringUtil.extractBytes(line, Line30.METER_DAYS_START, Line30.METER_DAYS_LEN);
					String supply_mode = StringUtil.extractBytes(line, Line30.SUPPLY_MODE_START, Line30.SUPPLY_MODE_LEN);
					String supply_mode_lb = StringUtil.extractBytes(line, Line30.SUPPLY_MODE_LB_START, Line30.SUPPLY_MODE_LB_LEN, "MS950");
					String supply_quality = StringUtil.extractBytes(line, Line30.SUPPLY_QUALITY_START, Line30.SUPPLY_QUALITY_LEN);
					
					MeterReading mr = new MeterReading();
					mr.setSerial(serial_nb);
					mr.setReadingDate(present_reading_dt);
					mr.setReadingIndex(present_reading_index);
					mr.setReadingCode(present_reading_cd);
					mr.setLastReadingDate(previous_reading_dt);
					mr.setLastReadingIndex(previous_reading_index);
					mr.setLastReadingCode(previous_reading_cd);
					mr.setChargingVolume(chargeable_volume);
					mr.setDaysOfPeriod(meter_days.trim());
					mr.setMeterSize(meter_size);
					
					bill.getpAddress().getConnectTypes().add(new ConnectType(supply_mode.trim(), supply_mode_lb.trim()));
					
					if (supply_quality.equals("1")) { // For portable water
						bill.getPotableWater().setSupplyQuality(supply_quality);
						bill.getPotableWater().setDelpnt_nb(delpnt_nb);
						bill.getPotableWater().setMetpnt_nb(metpnt_nb);
						
						if (StringUtil.isEmptyString(bill.getPotableWater().getSupplyMode())) {
							bill.getPotableWater().setSupplyMode(supply_mode_lb);
						}
						
						bill.getPotableWater().setTotalDays(bill.getPotableWater().getTotalDays() + Integer.parseInt(mr.getDaysOfPeriod()));
						bill.getPotableWater().setTotalVolume(bill.getPotableWater().getTotalVolume() + Integer.parseInt(mr.getChargingVolume()));
												
						bill.getPotableWater().getMeterReadings().add(mr);
					}
					else if (supply_quality.equals("2")) { // For recycled water
						bill.getRecycledWater().setSupplyQuality(supply_quality);
						bill.getRecycledWater().setDelpnt_nb(delpnt_nb);
						bill.getRecycledWater().setMetpnt_nb(metpnt_nb);
						
						if (StringUtil.isEmptyString(bill.getRecycledWater().getSupplyMode())) {
							bill.getRecycledWater().setSupplyMode(supply_mode_lb);
						}
						
						bill.getRecycledWater().setTotalDays(bill.getRecycledWater().getTotalDays() + Integer.parseInt(mr.getDaysOfPeriod()));
						bill.getRecycledWater().setTotalVolume(bill.getRecycledWater().getTotalVolume() + Integer.parseInt(mr.getChargingVolume()));
						
						bill.getRecycledWater().getMeterReadings().add(mr);
					}
				}
				
				if (line.startsWith("35")) {
					
					String billedPeriodsNb = StringUtil.extractBytes(line, Line35.BILLED_PERIOD_NB_START, Line35.BILLED_PERIOD_NB_LEN);
					String maxConsumption = StringUtil.extractBytes(line, Line35.MAX_CONSUMPTION_START, Line35.MAX_CONSUMPTION_LEN);
					String period1 = StringUtil.extractBytes(line, Line35.PERIOD1_START, Line35.PERIOD1_LEN);
					String consumption1 = StringUtil.extractBytes(line, Line35.CONSUMPTION1_START, Line35.CONSUMPTION1_LEN);
					String period2 = StringUtil.extractBytes(line, Line35.PERIOD2_START, Line35.PERIOD2_LEN);
					String consumption2 = StringUtil.extractBytes(line, Line35.CONSUMPTION2_START, Line35.CONSUMPTION2_LEN);
					String period3 = StringUtil.extractBytes(line, Line35.PERIOD3_START, Line35.PERIOD3_LEN);
					String consumption3 = StringUtil.extractBytes(line, Line35.CONSUMPTION3_START, Line35.CONSUMPTION3_LEN);
					String period4 = StringUtil.extractBytes(line, Line35.PERIOD4_START, Line35.PERIOD4_LEN);
					String consumption4 = StringUtil.extractBytes(line, Line35.CONSUMPTION4_START, Line35.CONSUMPTION4_LEN);
					String period5 = StringUtil.extractBytes(line, Line35.PERIOD5_START, Line35.PERIOD5_LEN);
					String consumption5 = StringUtil.extractBytes(line, Line35.CONSUMPTION5_START, Line35.CONSUMPTION5_LEN);
					String period6 = StringUtil.extractBytes(line, Line35.PERIOD6_START, Line35.PERIOD6_LEN);
					String consumption6 = StringUtil.extractBytes(line, Line35.CONSUMPTION6_START, Line35.CONSUMPTION6_LEN);
					String period7 = StringUtil.extractBytes(line, Line35.PERIOD7_START, Line35.PERIOD7_LEN);
					String consumption7 = StringUtil.extractBytes(line, Line35.CONSUMPTION7_START, Line35.CONSUMPTION7_LEN);
					String period8 = StringUtil.extractBytes(line, Line35.PERIOD8_START, Line35.PERIOD8_LEN);
					String consumption8 = StringUtil.extractBytes(line, Line35.CONSUMPTION8_START, Line35.CONSUMPTION8_LEN);
					String period9 = StringUtil.extractBytes(line, Line35.PERIOD9_START, Line35.PERIOD9_LEN);
					String consumption9 = StringUtil.extractBytes(line, Line35.CONSUMPTION9_START, Line35.CONSUMPTION9_LEN);
					String period10 = StringUtil.extractBytes(line, Line35.PERIOD10_START, Line35.PERIOD10_LEN);
					String consumption10 = StringUtil.extractBytes(line, Line35.CONSUMPTION10_START, Line35.CONSUMPTION10_LEN);
					String period11 = StringUtil.extractBytes(line, Line35.PERIOD11_START, Line35.PERIOD11_LEN);
					String consumption11 = StringUtil.extractBytes(line, Line35.CONSUMPTION11_START, Line35.CONSUMPTION11_LEN);
					String period12 = StringUtil.extractBytes(line, Line35.PERIOD12_START, Line35.PERIOD12_LEN);
					String consumption12 = StringUtil.extractBytes(line, Line35.CONSUMPTION12_START, Line35.CONSUMPTION12_LEN);
					String period13 = StringUtil.extractBytes(line, Line35.PERIOD13_START, Line35.PERIOD13_LEN);
					String consumption13 = StringUtil.extractBytes(line, Line35.CONSUMPTION13_START, Line35.CONSUMPTION13_LEN);
					
					bill.getHisto().setPeriodConsumptionNb(billedPeriodsNb);
					bill.getHisto().setMaxConsumption(maxConsumption);
					bill.getHisto().setBlcclNb(bill.getBlccl_nb());
					bill.getHisto().setLang(bill.getLang_cd());
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period1, consumption1));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period2, consumption2));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period3, consumption3));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period4, consumption4));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period5, consumption5));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period6, consumption6));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period7, consumption7));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period8, consumption8));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period9, consumption9));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period10, consumption10));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period11, consumption11));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period12, consumption12));
					bill.getHisto().getPeriodConsumptions().add(new PeriodConsumption(period13, consumption13));
				}
				
				if (line.startsWith("40")) {
					bi = new BillItem();
					
					String item_cd = StringUtil.extractBytes(line, Line40.ITEM_CD_START, Line40.ITEM_CD_LEN);
					String item_desc = StringUtil.extractBytes(line, Line40.ITEM_DESC_START, Line40.ITEM_DESC_LEN, bill.getLang_cd().equals("C") ? "MS950" : "ISO-8859-1");
					String quantity = StringUtil.extractBytes(line, Line40.ITEM_QUANTITY_START, Line40.ITEM_QUANTITY_LEN);
//					String price = StringUtil.extractBytes(line, Line40.ITEM_PRICE_START, Line40.ITEM_PRICE_LEN);
					String start_dt_item = StringUtil.extractBytes(line, Line40.START_DT_ITEM_START, Line40.START_DT_ITEM_LEN);
					String end_dt_item = StringUtil.extractBytes(line, Line40.END_DT_ITEM_START, Line40.END_DT_ITEM_LEN);
//					String net_am = StringUtil.extractBytes(line, Line40.NET_AM_START, Line40.NET_AM_LEN);
					String gross_am = StringUtil.extractBytes(line, Line40.GROSS_AM_START, Line40.GROSS_AM_LEN);
					
					bi.setItemNumber(item_cd);
					bi.setItemDescription(item_desc);
					bi.setItemQuantity(Double.parseDouble(quantity));
					bi.setStartDate(StringUtil.yyyyMMdd2Date(start_dt_item));
					bi.setEndDate(StringUtil.yyyyMMdd2Date(end_dt_item));
					bi.setAmount(CommonEnv.FMCURRENCY.format(Double.parseDouble(gross_am)));
					
					
					
					if (!bi.getItemNumber().equals("1111")) {
						String start_dt_refund = StringUtil.extractBytes(line, Line40.START_DT_REFUND_START, Line40.START_DT_REFUND_LEN);
						String end_dt_refund = StringUtil.extractBytes(line, Line40.END_DT_REFUND_START, Line40.END_DT_REFUND_LEN);
						String start_dt_plain = StringUtil.extractBytes(line, Line40.START_DT_PLAIN_START, Line40.START_DT_PLAIN_LEN);
						String end_dt_plain = StringUtil.extractBytes(line, Line40.END_DT_PLAIN_START, Line40.END_DT_PLAIN_LEN);
						String start_dt_step = StringUtil.extractBytes(line, Line40.START_DT_STEP_START, Line40.START_DT_STEP_LEN);
						String end_dt_step = StringUtil.extractBytes(line, Line40.END_DT_STEP_START, Line40.END_DT_STEP_LEN);
						
						if (!StringUtil.isEmptyString(start_dt_refund.trim())) {
							bi.setStartDateRefund(StringUtil.yyyyMMdd2Date(start_dt_refund));
						}
						if (!StringUtil.isEmptyString(end_dt_refund.trim())) {
							bi.setEndDateRefund(StringUtil.yyyyMMdd2Date(end_dt_refund));
						}
						if (!StringUtil.isEmptyString(start_dt_plain.trim())) {
							bi.setStartDatePlain(StringUtil.yyyyMMdd2Date(start_dt_plain));
						}
						if (!StringUtil.isEmptyString(end_dt_plain.trim())) {
							bi.setEndDatePlain(StringUtil.yyyyMMdd2Date(end_dt_plain));
						}
						if (!StringUtil.isEmptyString(start_dt_step.trim())) {
							bi.setStartDateStep(StringUtil.yyyyMMdd2Date(start_dt_step));
						}
						if (!StringUtil.isEmptyString(end_dt_step.trim())) {
							bi.setEndDateStep(StringUtil.yyyyMMdd2Date(end_dt_step));
						}
						bill.getTxnItems().add(bi);
					}
					else {
						double	dRemainder = Double.parseDouble(gross_am);
						
						if (bill.getBill_ty().equals("3") || bill.getBill_ty().equals("4")) {
							dRemainder %= 0.1;
						}
						
						String sRemainder = CommonEnv.FMCURRENCY.format(dRemainder);
						if (dRemainder > CommonEnv.ZERO_VALUE) {
							sRemainder = "-" + sRemainder;
						}
						
						bill.setRemainder(sRemainder);
					}
				}
				
				if (line.startsWith("50")) {
					ItemLine il = new ItemLine();
					
					String quantity = StringUtil.extractBytes(line, Line50.QUANTITY_START, Line50.QUANTITY_LEN);
					String price = StringUtil.extractBytes(line, Line50.PRICE_START, Line50.PRICE_LEN);
					String net_am = StringUtil.extractBytes(line, Line50.NET_AM_START, Line50.NET_AM_LEN);
//					System.out.println("line=" + toUnicode(line));
					String description = StringUtil.extractBytes(line, Line50.DESCRIPTION_START, Line50.DESCRIPTION_LEN, bill.getLang_cd().equals("C") ? "MS950" : "ISO-8859-1");
//					System.out.println("desc=" + toUnicode(description));
					String band_nb = StringUtil.extractBytes(line, Line50.BAND_NB_START, Line50.BAND_NB_LEN);
					
					il.setConsumption(CommonEnv.FMDECIMAL4DIGITS.format(Double.parseDouble(quantity.trim())));
					il.setPrice(CommonEnv.FMDECIMAL2DIGITS.format(Double.parseDouble(price.trim())));
					il.setAmount(CommonEnv.FMCURRENCY.format(Double.parseDouble(net_am.trim())));
					il.setDescritpion(description.trim());
					il.setBand(band_nb.trim());
					
					if (line.getBytes().length == 235) { // 203 -> 235 for the charge-back dates
						String customer_type = StringUtil.extractBytes(line, Line50.CUSTOMER_TYPE_START, Line50.CUSTOMER_TYPE_LEN);
						String coefficient = StringUtil.extractBytes(line, Line50.COEFFICIENT_START, Line50.COEFFICIENT_LEN);
						String consumption_days = StringUtil.extractBytes(line, Line50.CONSUMPTION_DAYS_START, Line50.CONSUMPTION_DAYS_LEN);
						String ceiling = StringUtil.extractBytes(line, Line50.CEILING_START, Line50.CEILING_LEN);
						String dtStartChgbck = StringUtil.extractBytes(line, Line50.DT_START_CHKBCK_START, Line50.DT_START_CHKBCK_LEN);
						String dtEndChgbck = StringUtil.extractBytes(line, Line50.DT_END_CHKBCK_START, Line50.DT_END_CHKBCK_LEN);
						String dtStartRefund = StringUtil.extractBytes(line, Line50.DT_START_REFUND_START, Line50.DT_START_REFUND_LEN);
						String dtEndRefund = StringUtil.extractBytes(line, Line50.DT_END_REFUND_START, Line50.DT_END_REFUND_LEN);
						
						if (bi.getMaxLenConsumption() == null || il.getConsumption().length() > bi.getMaxLenConsumption().length()) {
							bi.setMaxLenConsumption(il.getConsumption());
						}
						bi.buildCT(customer_type.trim());
						
						double coeff = 1d;
						try {
							coeff = Double.parseDouble(coefficient.trim());
						}
						catch (Exception ex){
							coeff = 1d;
						}
						il.setCoefficient(coeff);
						
						il.setDays(Double.parseDouble(consumption_days.trim()));
						
						il.setMaxQuantityOfABand(Integer.parseInt(ceiling.trim()));
						
						il.setDtStartChgbck(StringUtil.yyyyMMdd2Date(dtStartChgbck));
						il.setDtEndChgbck(StringUtil.yyyyMMdd2Date(dtEndChgbck));
						il.setDtStartRefund(StringUtil.yyyyMMdd2Date(dtStartRefund));
						il.setDtEndRefund(StringUtil.yyyyMMdd2Date(dtEndRefund));
					}
					
					bi.getItemLines().add(il);
				}
				
				if (line.startsWith("80")) {
					String message_cd = StringUtil.extractBytes(line, Line80.MESSAGE_CD_START, Line80.MESSAGE_CD_LEN);
					
					bill.setMessage_cd(message_cd);
					
					// add special transactions -- 2020 WUHAN nCoV -- Gov subsidy
					int bodyNb = Integer.parseInt(bill.getBody_nb());
					int acctNb = Integer.parseInt(bill.getAcct_nb());
					int transcNb = Integer.parseInt(bill.getTransc_nb());
					Transaction txn = DbUtil.getPaymentTransaction(conn, bodyNb, acctNb, transcNb, bill.getLang_cd());
					if (txn != null) {		
						ArrayList<Transaction> listTxns = new ArrayList<Transaction>();
						listTxns.add(txn);
						bill.setSubsTxns(listTxns);
						
						double d_transc_total = 0;
						String sCrAmt;
						double dLateCharge = 0, dCrAmt = 0, dBillTotal = 0, dLastBalance = 0;
												
						dBillTotal = Double.parseDouble(bill.getTransc_am());
						
						d_transc_total = txn.getAm();
						dBillTotal += d_transc_total;
						
						bill.setTransc_am(String.valueOf(dBillTotal));
						
						dLateCharge = Double.parseDouble(bill.getLate_charge());
						dLastBalance = Double.parseDouble(bill.getLast_balance());
						
						dCrAmt = dLastBalance + dLateCharge + dBillTotal;
						sCrAmt = new java.text.DecimalFormat("$#,##0.00").format(dCrAmt);
						
						double roundTo = 10;
						if ("3".equals(bill.getBill_ty()) || "4".equals(bill.getBill_ty())) {
							roundTo = 0.1;
						}
						if (dCrAmt > 0) {
							dCrAmt = MathUtil.mod(dCrAmt, roundTo) * -1;
							if (Math.abs(dCrAmt - 0) < 0.00005) {
								sCrAmt = StringUtil.EMPTY;
							}
							else {
								sCrAmt = new java.text.DecimalFormat("$#,##0.00").format(dCrAmt);
							}
						}
						else if (dCrAmt < 0 || Math.abs(dCrAmt - 0) < 0.00005) {
							sCrAmt = StringUtil.EMPTY;
						}
						
						bill.setRemainder(sCrAmt);
						bill.setGross_am(dLastBalance + dLateCharge + dBillTotal);
					}
				}
				
				
			}
			reader.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}
	
	private boolean isNoMail(BillV2 b) {
		boolean result = false;
		
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
						result = true;
					}
				}
				else {
					result = true;
				}
			}
		}
		
		return result;
	}
	
	private void getPosterSlogans(DBConn dbConn, Bill bill) throws Exception {		
		if (dbConn.conn == null) {
			ArrayList<PosterSlogan> posterSlogans = new ArrayList<PosterSlogan>();
			PosterSlogan ps = new PosterSlogan();
			ps.setMsgNb(1);
			ps.setLb("Test at home");
			ps.setFontSize(12);
			posterSlogans.add(ps);
			
			bill.setPosterSlogans(posterSlogans);
			return;
		}
		
		String sql = 
				"select msg_nb, " + 
				"  case ? when 'C' then lb_chi when 'E' then lb_eng when 'P' then lb_por else lb_chi end lb," + 
				"  case ? when 'C' then font_size_chi when 'E' then font_size_eng when 'P' then font_size_por else font_size_chi end font_size" + 
				"  from lyodba.munimsg" + 
				" where msg_id = 'POSTER_SLOGAN'";
		dbConn.prepareSQL(sql);
		dbConn.setString(1, bill.getLang_cd());
		dbConn.setString(2, bill.getLang_cd());
		ResultSet rs = dbConn.executeQuery();
		ArrayList<PosterSlogan> posterSlogans = new ArrayList<PosterSlogan>();
		while (rs.next()) {
			int msgNb = rs.getInt("msg_nb");
			String lb = rs.getString("lb");
			int fontSize = rs.getInt("font_size");
			if (msgNb >= 1 && msgNb <= 2 && lb != null && fontSize > 0) {
				PosterSlogan ps = new PosterSlogan();
				ps.setMsgNb(msgNb);
				ps.setLb(lb);
				ps.setFontSize(fontSize);
				posterSlogans.add(ps);
			}
		}
		rs.close();
		dbConn.closeStmt();
		
		bill.setPosterSlogans(posterSlogans);
	}
	
	private CallResult addPDFLog(DBConn conn, String billFileName, String bodyNb, String acctNb, String transcNb, String batchNb, String lang, boolean isEBill, String inReminder, String cdBillCheck) throws Exception {
		conn.prepareSQL("{call pkg_pdf_bill.add_log(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
		
		conn.setString(1, billFileName);
		conn.setInt(2, Integer.parseInt(bodyNb));
		conn.setInt(3, Integer.parseInt(acctNb));
		conn.setInt(4, Integer.parseInt(transcNb));
		conn.setString(5, batchNb);
		conn.setString(6, lang);
		conn.setString(7, isEBill ? "Y" : "N");
		conn.setString(8, inReminder);
		if (StringUtil.isEmptyString(cdBillCheck)) {
			conn.setNull(9, OracleTypes.VARCHAR);
		}
		else {
			conn.setString(9, cdBillCheck);
		}
		conn.regOutParm(10, OracleTypes.NUMBER);
		conn.regOutParm(11, OracleTypes.VARCHAR);
		
		conn.execute();
		
		CallResult cr = new CallResult(conn.getInt(10), conn.getString(11));
		conn.closeStmt();
		
		return cr;
	}
	
//	private String writeXML(DBConn dbConn, String xml, String key, int body_nb, int acct_nb, int prprty_nb, int transc_nb, String lb_surname, String bill_am, String bill_dt, String due_dt) throws Exception {
//		dbConn.prepareSQL("{call insert_mpdfxml(?,?,?,?,?,?,?,?,?,?,?,?)}");
//		
//		dbConn.setString(1, key);
//		dbConn.setClob(2, xml);
//		dbConn.setInt(3, body_nb);
//		dbConn.setInt(4, acct_nb);
//		dbConn.setInt(5, prprty_nb);
//		dbConn.setInt(6, transc_nb);
//		dbConn.setString(7, lb_surname);
//		dbConn.setString(8, bill_am);
//		dbConn.setString(9, bill_dt);
//		dbConn.setString(10, due_dt);
//		dbConn.regOutParm(11, OracleTypes.NUMBER);
//		dbConn.regOutParm(12, OracleTypes.VARCHAR);
//		
//		dbConn.execute();		
//		String result = dbConn.getString(12);
//		
//		dbConn.closeStmt();
//		System.out.println("insert_mpdfxml result=" + result);
//		return result;
//	}
	
	private String writeXMLEX(DBConn dbConn, String xml, String key) throws Exception {
		dbConn.prepareSQL("{call insert_mpdfxmlex(?,?,?,?)}");
		
		dbConn.setString(1, key);
		dbConn.setClob(2, xml);
		dbConn.regOutParm(3, OracleTypes.NUMBER);
		dbConn.regOutParm(4, OracleTypes.VARCHAR);
		
		dbConn.execute();		
		String result = dbConn.getString(4);
		
		dbConn.closeStmt();
		
		return result;
	}
	
	private String insertEBLHST(DBConn dbConn, String key, int body_nb, int acct_nb, int prprty_nb, int transc_nb, String in_reminder) throws Exception {
		dbConn.prepareSQL("{call pkg_ebill.insert_ebill_history_v2(?,?,?,?,?,?,?,?,?,?,?,?,?)}");
		
		dbConn.setString(1, key);
		dbConn.setInt(2, body_nb);
		dbConn.setInt(3, acct_nb);
		dbConn.setInt(4, prprty_nb);
		dbConn.setInt(5, transc_nb);
		dbConn.setString(6, in_reminder);
		dbConn.setString(7, "BILFM");
		dbConn.setNull(8, OracleTypes.VARCHAR);
		dbConn.setString(9, "MBLS2647");
		dbConn.setNull(10, OracleTypes.DATE);
		dbConn.setString(11, "30");
		dbConn.regOutParm(12, OracleTypes.NUMBER);
		dbConn.regOutParm(13, OracleTypes.VARCHAR);
		
		dbConn.execute();		
		String result = dbConn.getString(13);
		
		dbConn.closeStmt();
		
		return result;
	}
	
	private String cancelEBLHST(DBConn dbConn, String key) throws Exception {
		dbConn.prepareSQL("{call pkg_ebill.cancel_ebill_history(?,?,?,?)}");
		
		dbConn.setString(1, key);
		dbConn.setString(2, "BILFM REGEN EXCLUDED");
		dbConn.regOutParm(3, OracleTypes.NUMBER);
		dbConn.regOutParm(4, OracleTypes.VARCHAR);
		
		dbConn.execute();		
		String result = dbConn.getString(4);
		
		dbConn.closeStmt();
		
		return result;
	}
	
	public BillDataFile(File df) {
		this.dataFile = df;
	}

	
	public ArrayList<BillV2> getBills() {
		return bills;
	}

	public void setBills(ArrayList<BillV2> bills) {
		this.bills = bills;
	}


	public DBConn getConn() {
		return conn;
	}
	public void setConn(DBConn conn) {
		this.conn = conn;
	}


	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}

	public String getTySort() {
		return tySort;
	}
	public void setTySort(String tySort) {
		this.tySort = tySort;
	}

	public List<String> getExcludedEntities() {
		return excludedEntities;
	}
	public void setExcludedEntities(List<String> excludedEntities) {
		this.excludedEntities = excludedEntities;
	}


	private File dataFile;
	
//	private ArrayList<TextMessage> textMessages;
	
	private ArrayList<BillV2> bills;
	
	private int pageNumberOfEBill;
	private int pageNumberOfNonEBillMail;
	private int pageNumberOfNonEBillNoMail;
	
	private DBConn conn;
	
	private String action = "NORMAL_GEN";
	private String tySort = "0";
	private List<String> excludedEntities;
}
