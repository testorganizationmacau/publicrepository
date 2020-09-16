package com.macaowater.bill;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.OracleTypes;

import com.macaowater.db.CallResult;
import com.macaowater.db.DBConn;
import com.macaowater.defined.Labels;
import com.macaowater.entity.BillDataFile;
import com.macaowater.entity.EnvelopPNG;
import com.macaowater.util.StringUtil;

/**
 * OfflineBill
 * @author wugy
 * @date   03/08/2012
 * 
 * @note   Run in $LANG=en_US.ISO-8859-1
 */
public class OfflineBill {

	/**
	 * @param args
	 * 1 : BILL file directory
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("Parameter invalid!");
			return;
		}
		long start = System.currentTimeMillis();
		
		if (args.length == 3) {
			String billDATPath = args[0];
			System.out.println("BILL file is " + billDATPath);
			
			String envelopDirectoryPath = args[1];
			System.out.println("Envelop graph file directory is " + envelopDirectoryPath);
			
			String dbConnString = args[2];
			System.out.println("dbConnString=" + dbConnString);
			DBConn conn = new DBConn(dbConnString, true);
			
			EnvelopPNG.ENVELOP_PATH = envelopDirectoryPath;
			EnvelopPNG.setEnvelopImages();
			
			File billDAT = new File(billDATPath);
			
			Labels.initLabels();
			
			BillDataFile bdf = new BillDataFile(billDAT);
			bdf.setConn(conn);
			bdf.buildEntity();
			bdf.buildPDF();
						
			conn.closeConn();
		}
		else if (args.length == 5) {
			// D:\temp\offline_bill>java -Djava.awt.headless=true -Xmx1024m -jar D:\JavaSpace\icis_offline_bill_batch\jar\icis_offline_bill.jar LYODBA/lyo@ICIST3 D:\wugy\bill_resources REGEN 150710200624 1
			// Regenerate PDF
			String dbConnString = args[0];
			DBConn conn = new DBConn(dbConnString, true);
			
			String envelopDirectoryPath = args[1];
			System.out.println("Envelop graph file directory is " + envelopDirectoryPath);
			
			EnvelopPNG.ENVELOP_PATH = envelopDirectoryPath;
			EnvelopPNG.setEnvelopImages();
			
			Labels.initLabels();
			
			String action = args[2];
			String billDT = args[3];
			String tySort = args[4];
			
			conn.prepareSQL("{call pkg_pdf_bill.file_list(?, ?, ?, ?)}");
			conn.setString(1, billDT);
			conn.regOutParm(2, OracleTypes.VARCHAR);
			conn.regOutParm(3, OracleTypes.NUMBER);
			conn.regOutParm(4, OracleTypes.VARCHAR);
			conn.execute();
			
			CallResult cr = new CallResult(conn.getInt(3), conn.getString(4));
			String fileList = conn.getString(2);
			conn.closeStmt();
			
			if (cr.getRcd() != 0) {
				throw cr.buildException();
			}
			
			String[] files = fileList.split(",");
			for (int i = 0; i < files.length; i++) {
				File billDAT = new File(files[i] + ".done");
				
				// Get excluded body list
				conn.prepareSQL("{call pkg_pdf_bill.excluded_list(?, ?, ?, ?)}");
				conn.setString(1, files[i]);
				conn.regOutParm(2, OracleTypes.CLOB);
				conn.regOutParm(3, OracleTypes.NUMBER);
				conn.regOutParm(4, OracleTypes.VARCHAR);
				conn.execute();
				
				cr = new CallResult(conn.getInt(3), conn.getString(4));
				String excludedListString = StringUtil.lob2String(conn.getClob(2));
				conn.closeStmt();
				
				List<String> excludedList = new ArrayList<String>();
				String[] excludedEntities = excludedListString.split(",");
				for (int j = 0; j < excludedEntities.length; j++) {
					System.out.println("excludedEntities -> " + excludedEntities[j]);
					excludedList.add(excludedEntities[j]);
				}
				
				BillDataFile bdf = new BillDataFile(billDAT);
				
				bdf.setAction(action);
				bdf.setTySort(tySort);
				bdf.setConn(conn);
				bdf.setExcludedEntities(excludedList);
				bdf.buildEntity();
				bdf.buildPDF();
			}
						
			conn.closeConn();
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println("cost " + ((end - start) / 1000) + " seconds");
	}
}
