package utils;

import java.io.FileNotFoundException;
import java.io.IOException;

import image.SignatureGenerator;

public class FileName {
	
	public static String escapeFolderName(String s) {
		s = s.replace('/','#');
		s = s.replace(':','#');
		return s;
	}
	public static String getPath(String s) {
		int pos  = s.lastIndexOf('/');
		int pos2 = s.lastIndexOf('\\');
		if (pos2>pos) pos = pos2;
		if (pos> -1) s=s.substring(0,pos+1);
		return s;
	}
	public static String getNameAndExt(String s) {
		int pos  = s.lastIndexOf('/');
		int pos2 = s.lastIndexOf('\\');
		if (pos2>pos) pos = pos2;
		if (pos> -1) s=s.substring(pos+1);
		return s;
	}
	public static String getNameDatePrefix(String s) {
		return getNameAndExt(s).substring(0,17);
	}

	public static String getFileNameExtension(String filename) {
		int pos = filename.lastIndexOf('.');
		return (-1==pos)?"":filename.substring(pos);
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		//"sig_2024_10_25\\2024_20_25_03_20_root_sig.txt"
	}

	
}
