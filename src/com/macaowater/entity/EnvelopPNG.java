package com.macaowater.entity;

import com.itextpdf.text.Image;

public class EnvelopPNG {
	
	public static String ENVELOP_PATH = "";
	
	public static String SEPARATOR = System.getProperty("file.separator");

	public static Image ENVELOP_C = null;
	public static Image ENVELOP_E = null;
	public static Image ENVELOP_P = null;
	
	public static void setEnvelopImages() {
		try {
			ENVELOP_C = Image.getInstance(ENVELOP_PATH + SEPARATOR + "Envelop_C.png");
			ENVELOP_E = Image.getInstance(ENVELOP_PATH + SEPARATOR + "Envelop_E.png");
			ENVELOP_P = Image.getInstance(ENVELOP_PATH + SEPARATOR + "Envelop_P.png");
			
			ImageHelper.setImage("Envelop_C", ENVELOP_C);
			ImageHelper.setImage("Envelop_E", ENVELOP_E);
			ImageHelper.setImage("Envelop_P", ENVELOP_P);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
