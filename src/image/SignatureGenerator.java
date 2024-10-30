package image;

import java.awt.image.BufferedImage;
import java.text.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata;
import utils.*;

public class SignatureGenerator {

	boolean trace=false;
	private static void trace(String s) {
		if (verbose) System.out.println(s);
	}
	private Parameters param = null;
    private String extensions[] = { ".jpeg", ".jpg", ".png", ".heic" };
    private List<String> exts = Arrays.asList(extensions);

	public SignatureGenerator() {

	}

	static private final String DEFAULT_VPROPORTION = "3/4";
	static private final String DEFAULT_HPROPORTION = "4/3";
	static private final String DEFAULT_MAXLENGTH = "10";
	static private final String DEFAULT_OFOLDER = "./";
	
	
	private IntPair getVproportion() {
		String proportion = param.getArg('v');
		if (null==proportion)proportion = DEFAULT_VPROPORTION;
		String props[] = proportion.split("/");
		IntPair ret = null;
		try {
			ret =  new IntPair(Integer.parseInt(props[0]),Integer.parseInt(props[1]));
		} catch (Exception e) {
			System.out.println("proportion="+proportion);
			System.out.println("props[0]="+props[0]);
			System.out.println("props[1]="+props[1]);
		}
		return ret;	
	}
	private int getMaxLength() {
		String slen = param.getArg('M');
		if (null==slen)slen = DEFAULT_MAXLENGTH;
		int ret=0;
		try {
			ret = Integer.parseInt(slen);
		} catch (Exception e) {
			System.out.println("slen="+slen);
		}
		return ret;
	}
	private IntPair getHproportion() {
		String proportion = param.getArg('h');
		if (null==proportion)proportion = DEFAULT_HPROPORTION;
		String props[] = proportion.split("/");
		IntPair ret = null;
		try {
			ret =  new IntPair(Integer.parseInt(props[0]),Integer.parseInt(props[1]));
		} catch (Exception e) {
			System.out.println("proportion="+proportion);
			System.out.println("props[0]="+props[0]);
			System.out.println("props[1]="+props[1]);
		}
		return ret;
	}
	String getBaseFolder() {
		return param.getArg('b').replace('\\','/');
	}
	String getOutputFolder() {
		String of = param.getArg('o');
		if (null==of )of = DEFAULT_OFOLDER;
		if (!of.endsWith("/")) of +="/";
		return of;
	}

	static boolean verbose=false;
	
	private boolean processArgs(String[] args) {
		int error = Parameters.ERROR_EXCEPTION;
		try {
			param = new Parameters();
			param.addMandatoryArgument('b', "basefolder", "Images Folder");
			param.addParam('V', "verbose", "Display Information");
			param.addParamWithArgument('o', "output",     "folder",    "Output Folder ("+DEFAULT_OFOLDER+")");
			param.addParamWithArgument('v', "vertical",   "vproportion", "Vertical Proportion ("+DEFAULT_VPROPORTION+")");
			param.addParamWithArgument('h', "horizontal", "hproportion", "Horizontal Proportion ("+DEFAULT_HPROPORTION+")");
//			param.addParamWithArgument('d', "depth", "depth", "Recursion Depth (999)");
			param.addParamWithArgument('M', "maxlen", "maxlen", "Max Image File Size in Mb ("+DEFAULT_MAXLENGTH+")");
		
			error = param.readCommandLine(args);
			if (error!=Parameters.NO_ERROR ) {
				param.printErrorMessage(System.err);
				param.printUsageMessage(System.out,"image.jar");
			} else {
				verbose = param.getFlag('V');
			}
		} catch (Exception e) {
			System.err.println(e);
		}
		return error==Parameters.NO_ERROR;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		SignatureGenerator sg = new SignatureGenerator();
		if (!sg.processArgs(args)) return;
		sg.processBase();
	}
	private boolean forbiddenFolderName(String fn) {
		boolean ret = false;
		if ("N".equalsIgnoreCase(fn)) ret = true;
		if ("old".equalsIgnoreCase(fn)) ret = true;
		if ("AVI".equalsIgnoreCase(fn)) ret = true;
		if (fn.toLowerCase().contains("slides")) ret = true;
		if (fn.toLowerCase().contains("trash")) ret = true;
		if (fn.startsWith(".")) ret = true;
		return ret;
	}
 
	private PrintWriter rootWriter = null;
	private String basedate=null;
	private void processBase() {
		SimpleDateFormat dt = new SimpleDateFormat("yyyy_MM_dd_hh_mm");
		try {
			basedate = dt.format(new Date());
			rootWriter = new PrintWriter(getOutputFolder()+basedate+"_root_sig.txt");
			rootWriter.println("@Hproportion="+getHproportion());
			rootWriter.println("@Vproportion="+getVproportion());
			rootWriter.println("@maxlen="+getMaxLength());
			rootWriter.println("@formato= filename,winicial,hinicial,wfinal,hfinal,mean_red,mean_green,mean_blue");
			processFolder("",getBaseFolder());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			if (null!=rootWriter) rootWriter.close();
		}
	}
	
	private void processFolder(String path, String folderName) {
		PrintWriter folderWriter = null;
		if (forbiddenFolderName(folderName)) return;
		if (!path.isEmpty()) folderName = path +"/" + folderName;
		rootWriter.println(folderName);
		File folder = new File(folderName);
		File[] listOfFiles = folder.listFiles();
		try {
			folderWriter = new PrintWriter(getOutputFolder()+basedate+"_"+FileName.escapeFolderName(folderName)+".txt");
		    for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isDirectory()) {
					processFolder(folderName,listOfFiles[i].getName());
				} else if (listOfFiles[i].isFile()) {
					String fileExtension = FileName.getFileNameExtension(listOfFiles[i].getName()).toLowerCase();
                    if (exts.contains(fileExtension)) {
                    	if (listOfFiles[i].length()<20*1024*1024) {
                    		folderWriter.println(generateSignature(folderName+"/"+listOfFiles[i].getName()));
                    	} else {
                        	trace ("ignore:"+listOfFiles[i].getName()+" with "+listOfFiles[i].length()+" bytes");
                    	}
                    } else {
                    	trace ("ignore:"+listOfFiles[i].getName()+" (extension not listed)");
                    }
				}
		    }
			rootWriter.flush();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			if (null!=rootWriter) folderWriter.close();
		}

	}
	
	private String generateSignature(String filename) {
		//mudar aqui, verificar se � vertical ou horizontal para secolher a propor��o 3/4 4/3 e escolher os pixels usados, 
		//centralizando horizontalmente e subindo verticalmente 
		BufferedImage image=ImageProcessingUtils.get(filename);
		/* type n�o importa porque image.getRGB(x,y); converte para TYPE_INT_ARGB */
		int type = image.getType();
		int w = image.getWidth();
		int h = image.getHeight();
		boolean vertical = h>w;
		int x,y;
        int color,count;	
		String s="";
		count=0;
		/* All BufferedImage objects have an upper left corner coordinate of (0, 0) */
		/* calcula as coordenadas a serem usadas */
		IntPair prop = vertical?getVproportion():getHproportion();
		double propConfigured = ((double)prop.x)/((double)prop.y);
		double propFile = ((double)w)/((double)h);
		double r=0;
		double g=0;
		double b=0;
		int winicial,hinicial,wfinal,hfinal;
		int largura,altura,sobraaltura,sobralargura;
		if (propConfigured>propFile) { 
			/* sobra altura, cortar de baixo */
			sobralargura = w % prop.x;
			winicial = sobralargura/2; 
			largura = w-sobralargura;
			wfinal = winicial+largura-1;
			altura = (largura/prop.x)*prop.y;
			sobraaltura = h-altura;
			hinicial = (int)(sobraaltura*0.1); /* Tirar mais de baixo*/
			hfinal = hinicial+altura-1;
		} else {
			/* sobra largura, cortar cantos e centralizar */
			sobraaltura = h % prop.y;
			hinicial = sobraaltura/2; 
			altura = h-sobraaltura;
			hfinal = hinicial+altura-1;
			largura = (altura/prop.y)*prop.x;
			sobralargura = w-largura;
			winicial = (int)(sobralargura*0.5); /* centralizar*/
			wfinal = winicial+largura-1;
		}
		if (trace) {
			System.out.println("type="+type);
			System.out.println("propFile, w, h: "+propFile+", "+w+", "+h);
			System.out.println("Vertical="+vertical);
			System.out.println("propConfigured, prop.x, prop,y: "+propConfigured+", "+prop.x+", "+prop.y);
			System.out.println("Proporcao configurada "+(prop.y>prop.x?"vertical":"horizontal"));			
			System.out.println((propConfigured>propFile?"1":"2")+"sobraaltura, hinicial, altura, hfinal: "+sobraaltura+", "+hinicial+", "+altura+", "+hfinal);			
			System.out.println((propConfigured>propFile?"2":"1")+"sobralargura, winicial, largura, wfinal: "+sobralargura+", "+winicial+", "+largura+", "+wfinal);			
		}

		for (x=winicial;x<=wfinal;++x){
			for (y=hinicial;y<=hfinal;++y){
				color = image.getRGB(x,y);
				r += (color>>16)&0xFF;
				g += (color>>8)&0xFF;
				b += (color)&0xFF;
				//a = (color>>24)&0xFF;
				count++;
			}
		}
		int mean_red  = (int) r/count;
		int mean_green = (int) g/count;
		int mean_blue  = (int) b/count;
		return filename+","+winicial+","+hinicial+","+wfinal+","+hfinal+","+mean_red+","+mean_green+","+mean_blue;
	}
}
