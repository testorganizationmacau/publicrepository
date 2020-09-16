package com.macaowater.bill;

import java.io.File;

import com.macaowater.db.DBConn;
import com.macaowater.defined.Labels;
import com.macaowater.entity.BillDataFile;
import com.macaowater.entity.EnvelopPNG;
import com.macaowater.util.BillFileFilter;

public class OfflineBillLocal {

	/**
	 * Only for local running
	 * 
	 * Program arguments
	 * d:\temp\offline_bill D:\wugy\bill_resources
	 * 
	 * VM arguments
	 * -Xmx1024m
	 *
	 * JRE 
	 * jdk1.5
	 * 
	 * Encoding
	 * MS950
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length < 3) {
				System.out.println("Parameter invalid!");
				return;
			}
			long start = System.currentTimeMillis();
			
			String billDirectoryPath = args[0];
			System.out.println("BILL file directory is " + billDirectoryPath);
			
			String envelopDirectoryPath = args[1];
			System.out.println("Envelop graph file directory is " + envelopDirectoryPath);
			
			String dbConnString = args[2];
			System.out.println("dbConnString=" + dbConnString);
			DBConn conn = new DBConn(dbConnString, true);
			
			EnvelopPNG.ENVELOP_PATH = envelopDirectoryPath;
			EnvelopPNG.setEnvelopImages();
			
			File billDirectory = new File(billDirectoryPath);
			File[] billDataFiles = billDirectory.listFiles(new BillFileFilter());
			
			Labels.initLabels();
			
			for (int i = 0, len = billDataFiles.length; i < len; i++) {
				BillDataFile bdf = new BillDataFile(billDataFiles[i]);
				bdf.setConn(conn);
				bdf.buildEntity();
				bdf.buildPDF();
			}
			
			long end = System.currentTimeMillis();
			
			System.out.println("cost " + ((end - start) / 1000) + " seconds");
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

}
