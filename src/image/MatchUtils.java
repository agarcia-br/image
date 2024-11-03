package image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.imageio.ImageIO;

import utils.FileName;
import utils.IntPair;
import utils.Parameters;
import utils.Proportion;
import utils.RGB;
import utils.Signature;

public class MatchUtils {

	private BufferedImage inputImage;

	private boolean getVertical() {
		return inputImage.getHeight() > inputImage.getWidth();
	}

	private static final String DEFAULT_MFILE = "match.txt";
	private static final String DEFAULT_OFILE = "mosaic.jpg";
	private static final int DEFAULT_WEIGHT = 10;
	private static final int DEFAULT_LINES = 4;// 100;
	private static final int DEFAULT_COLUMNS = 4;// 150;
	private static final int SAME_IMG_SPACONG = 3;

	String getOutputFile() {
		String of = param.getArg('o');
		if (null == of)
			of = DEFAULT_OFILE;
		return of;
	}

	String getMatchFile() {
		String of = param.getArg('m');
		if (null == of)
			of = DEFAULT_MFILE;
		return of;
	}
	int getWeight() {
		int ret;
		String w = param.getArg('w');
		ret = DEFAULT_WEIGHT;
		if (null != w) {
			ret = Integer.parseInt(w);
		}
		return ret;
	}

	int getNumLines() {
		int ret;
		String nl = param.getArg('v');
		ret = DEFAULT_LINES;
		if (null != nl) {
			ret = Integer.parseInt(nl);
		}
		return ret;
	}

	int getNumCols() {
		int ret;
		String s = param.getArg('h');
		ret = DEFAULT_COLUMNS;
		if (null != s) {
			ret = Integer.parseInt(s);
		}
		return ret;
	}

	IntPair proportion = null;
	String sproportion = null;
	private void initProportion() {
		String oriPar = getVertical() ? "Vproportion" : "Hproportion";
		sproportion = rootParam.get(oriPar);
		String props[] = sproportion.split(",");
		if (verbose) {
			System.out.println("proportion=" + sproportion);
			System.out.println("props.length=" + props.length);
		}
		try {
			proportion = new IntPair(Integer.parseInt(props[0]), Integer.parseInt(props[1]));
		} catch (Exception e) {
			System.out.println("proportion=" + proportion);
			System.out.println("props[0]=" + props[0]);
			System.out.println("props[1]=" + props[1]);
		}
	}

	Map<String, String> rootParam = new HashMap<String, String>();

	private void loadRootSignatureParams(char fileParam) {
		String rootsignaturefile = param.getArg(fileParam);
		try (BufferedReader br = new BufferedReader(new FileReader(rootsignaturefile))) {
			for (String line; (line = br.readLine()) != null;) {
				if (line.startsWith("@")) {
					if (verbose) System.out.println("paramline="+line);
					line = line.substring(1);
					String[] fields = line.split("=");
					rootParam.put(fields[0], fields[1]);
				} else {
					break;
				}
			}
			if (verbose) System.out.println("rootParam.size()="+rootParam.size());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Parameters param;
	private boolean verbose;

	private boolean processArgs(String[] args) {
		int error = Parameters.ERROR_EXCEPTION;
		try {
			param = new Parameters();
			param.addMandatoryArgument('s', "signatures", "Root Signatures File");
			param.addMandatoryArgument('i', "intput", "Input Image");
			param.addParam('V', "verbose", "Display Information");
			param.addParamWithArgument('o', "output", "outfile", "Output Imag (" + DEFAULT_OFILE + ")");
			param.addParamWithArgument('w', "weight", "percentilweight",
					"Original Imag Weight %(" + DEFAULT_WEIGHT + ")");
			param.addParamWithArgument('v', "vertical", "lines", "Number of line (" + DEFAULT_LINES + ")");
			param.addParamWithArgument('h', "horizontal", "columns", "Number of columns (" + DEFAULT_COLUMNS + ")");
			param.addParamWithArgument('r', "rootfolder", "rootfolder", "Change prefix of filename");
			param.addParamWithArgument('m', "matchfile", "matchfile", "Save prematch file (" + DEFAULT_MFILE + ")");
			param.addParamWithArgument('l', "loadmfile", "loadmatchfile", "Load prematch file (" + DEFAULT_MFILE + ")");

			error = param.readCommandLine(args);
			if ( param.getFlag('m')&&param.getFlag('l') ) {
				error |= Parameters.ERROR_INCONSISTENT_PARAMETERS;
				System.out.println("Can't both save and load match file");
			}

			if (error != Parameters.NO_ERROR) {
				param.printErrorMessage(System.err);
				param.printUsageMessage(System.out, "image.jar");
			} else {
				verbose = param.getFlag('V');
			}
		} catch (Exception e) {
			System.err.println(e);
		}
		return error == Parameters.NO_ERROR;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		MatchUtils mu = new MatchUtils();
		if (!mu.processArgs(args))
			return;
		mu.match();
	}

	private void getInputImage() {
		inputImage = ImageProcessingUtils.get(param.getArg('i'));
	}

	private void resizeInputImage() {
		int sectorVlen = proportion.y * Proportion.getPropNumberOfPixels();
		int sectorHlen = proportion.x * Proportion.getPropNumberOfPixels();
		int divHorizontal = getNumCols();
		int divVertical = getNumLines();
		int totalWidth = sectorHlen * divHorizontal;
		int totalHeight = sectorVlen * divVertical;
		double xscale = ((double) totalWidth) / ((double) inputImage.getWidth());
		double yscale = ((double) totalHeight) / ((double) inputImage.getHeight());
		if (verbose) {
			System.out.println("**xscale=" + xscale);
			System.out.println("**yscale=" + yscale);
			System.out.println("**totalWidth=" + totalWidth);
			System.out.println("**totalHeight=" + totalHeight);
		}
		inputImage = ImageProcessingUtils.scaleImage(inputImage, xscale, yscale);
		if (verbose) {
			System.out.println("**inputImage.getWidth()=" + inputImage.getWidth());
			System.out.println("**inputImage.getHeight()=" + inputImage.getHeight());
		}
	}

	private void match() {
		boolean loadPreMatchedSignatures = param.getFlag('l');
		loadRootSignatureParams(loadPreMatchedSignatures?'l':'s'); // fills rootParam
		getInputImage(); // fills inputimage
		if ( !param.getFlag('l')) {
			System.out.println(""+new Date()+"Loading Signatures...");
			loadSignatureRoot(); // fills sigarray
		}
		initProportion();
		resizeInputImage();// must be after loading signatureRoot to get proportions
		System.out.println(""+new Date()+"Calculating sectors...");
		loadSectorsMeanColor(); // fills rgb
		if ( param.getFlag('l')) {
			System.out.println(""+new Date()+"Loading PreMatched Signatures...");
			loadMatchFile();
		} else {
			System.out.println(""+new Date()+"Begin matching...");
			matchSectorsWithSignatures(); // fills int match [][]
			if ( param.getFlag('m')) {
				saveMatchFile();
			} 
		}
		System.out.println(""+new Date()+"Begin getting matched images...");
		getMatchedImages(); // fills images[][]
		// mountOutputImage(); // fills ouputImage
		System.out.println(""+new Date()+"Begin mounting mosaic...");
		mountOutputImageAlternative(); // fills ouputImage
		System.out.println(""+new Date()+"Writing mosaic to file...");
		writeImage(outputImage);
		System.out.println(""+new Date()+"Success");
	}

	private void saveMatchFile() {
		PrintWriter matchWriter = null;
		try {
			matchWriter = new PrintWriter(getMatchFile());
			int divHorizontal = getNumCols();
			int divVertical = getNumLines();
			matchWriter.println("@divHorizontal="+divHorizontal);
			matchWriter.println("@divVertical="+divVertical);
			if (getVertical()) {
				matchWriter.println("@Vproportion="+sproportion);
			} else {
				matchWriter.println("@Hproportion="+sproportion);
			}

			for (int i = 0; i < divHorizontal; ++i) {
				if (verbose) System.out.println("***lin=" + i + "***");
				for (int j = 0; j < divVertical; ++j) {
					matchWriter.println(""+i+","+j+"="+sigarray[match[i][j]]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			if (null!=matchWriter) matchWriter.close();
		}
		

	}

	private void writeImage(BufferedImage image) {
		try {
			File outputfile = new File(getOutputFile());
			ImageIO.write(image, "jpg", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	BufferedImage images[][] = null;

	private void getMatchedImages() {
		int sectorVlen = proportion.x * Proportion.getPropNumberOfPixels();
		int sectorHlen = proportion.y * Proportion.getPropNumberOfPixels();
		int divHorizontal = getNumCols();
		int divVertical = getNumLines();
		images = new BufferedImage[divHorizontal][divVertical];
		for (int i = 0; i < divHorizontal; ++i) {
			if (verbose) System.out.println("***lin=" + i + "***");
			for (int j = 0; j < divVertical; ++j) {
				images[i][j] = ImageProcessingUtils.get(sigarray[match[i][j]], proportion,
						Proportion.getPropNumberOfPixels());
				if (verbose) System.out.println("images[i][j].getWidth()=" + images[i][j].getWidth());
				if (verbose) System.out.println("images[i][j].getHeight()=" + images[i][j].getHeight());
			}
		}
	}

	BufferedImage outputImage = null;

	private void mountOutputImage() {
		int sectorVlen = proportion.y * Proportion.getPropNumberOfPixels();
		int sectorHlen = proportion.x * Proportion.getPropNumberOfPixels();
		int divHorizontal = getNumCols();
		int divVertical = getNumLines();
		int totalwidth = sectorHlen * divHorizontal;
		int totalHeight = sectorVlen * divVertical;
		byte[] bb3 = new byte[totalwidth * totalHeight * 3];

		for (int j = 0; j < divVertical; ++j) {// linha
			for (int i = 0; i < divHorizontal; ++i) {// coluna
				int offsetSetor = j * totalwidth + i * sectorHlen;
				for (int y = 0; y < sectorVlen; ++y) {
					for (int x = 0; x < sectorHlen; ++x) {// usa LenV para tamanho
						int offsetPixel = offsetSetor + y * sectorHlen + x;
						int offsetPixelInBytes = offsetPixel * 3;
						try {
							bb3[offsetPixelInBytes + 0] = (byte) ((images[i][j].getRGB(x, y) >> 16) & 0xFF);
							bb3[offsetPixelInBytes + 1] = (byte) ((images[i][j].getRGB(x, y) >> 8) & 0xFF);
							bb3[offsetPixelInBytes + 2] = (byte) ((images[i][j].getRGB(x, y)) & 0xFF);
						} catch (ArrayIndexOutOfBoundsException e) {
							System.out.println("images[i][j].getWidth()=" + images[i][j].getWidth());
							System.out.println("images[i][j].getHeight()=" + images[i][j].getHeight());
							System.out.println("sectorVlen=" + sectorVlen + " sectorHlen=" + sectorHlen);
							System.out.println("i=" + i + " j=" + j + " x=" + x + " y=" + y + " offsetSetor="
									+ offsetSetor + " offset=" + offsetPixel);
							// e.printStackTrace();
							throw e;
						}
					}
				}
			}
		}
		DataBufferByte buffer = new DataBufferByte(bb3, bb3.length);
		ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8 },
				false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		outputImage = new BufferedImage(cm, Raster.createInterleavedRaster(buffer, totalwidth, totalHeight,
				totalwidth * 3, 3, new int[] { 0, 1, 2 }, null), false, null);
	}

	static int pesoC1 = 0;

	private static int mixColor(int c1, int c2) {
		int ret = (c1 * pesoC1 + c2 * (100 - pesoC1)) / 100;
		return ret;
	}

	private static int mixRgb(int rgb1, int rgb2) {
		int red1 = ((rgb1 >> 16) & 0xFF);
		int green1 = ((rgb1 >> 8) & 0xFF);
		int blue1 = ((rgb1) & 0xFF);

		int red2 = ((rgb2 >> 16) & 0xFF);
		int green2 = ((rgb2 >> 8) & 0xFF);
		int blue2 = ((rgb2) & 0xFF);

		red1 = mixColor(red1, red2);
		green1 = mixColor(green1, green2);
		blue1 = mixColor(blue1, blue2);

		int ret = (0xFF << 24) + (red1 << 16) + (green1 << 8) + blue1;
		/*
		 * System.out.println("rgb1="+String.format("0x%32X", rgb1));
		 * System.out.println("rgb2="+String.format("0x%32X", rgb2));
		 * System.out.println("red1="+String.format("0x%02X", red1));
		 * System.out.println("ret="+String.format("0x%32X", ret));
		 */
		return ret;
	}

	private void mountOutputImageAlternative() {
		int sectorVlen = proportion.y * Proportion.getPropNumberOfPixels();
		int sectorHlen = proportion.x * Proportion.getPropNumberOfPixels();
		int divHorizontal = getNumCols();
		int divVertical = getNumLines();
		int totalwidth = sectorHlen * divHorizontal;
		int totalHeight = sectorVlen * divVertical;

		outputImage = new BufferedImage(totalwidth, totalHeight, BufferedImage.TYPE_INT_RGB);
		pesoC1 = getWeight();
		if (verbose) System.out.println("totalwidth=" + totalwidth);
		if (verbose) System.out.println("totalHeight=" + totalHeight);

		for (int j = 0; j < divVertical; ++j) {// linha
			for (int i = 0; i < divHorizontal; ++i) {// coluna
				for (int y = 0; y < sectorVlen; ++y) {
					for (int x = 0; x < sectorHlen; ++x) {// usa LenV para tamanho
						try {
							// outputImage.setRGB(i*sectorHlen+x,j*sectorVlen+y,images[i][j].getRGB(x,y));
							outputImage.setRGB(i * sectorHlen + x, j * sectorVlen + y,
									mixRgb(inputImage.getRGB(i * sectorHlen + x, j * sectorVlen + y),
											images[i][j].getRGB(x, y)));
						} catch (ArrayIndexOutOfBoundsException e) {
							System.out.println("images[i][j].getWidth()=" + images[i][j].getWidth());
							System.out.println("images[i][j].getHeight()=" + images[i][j].getHeight());
							System.out.println("sectorVlen=" + sectorVlen + " sectorHlen=" + sectorHlen);
							throw e;
						}
					}
				}
			}
		}
	}

	List<Signature> siglist;
	Signature[] sigarray;

	private void loadMatchFile() {
		int divHorizontal = getNumCols();
		int divVertical = getNumLines();
		/*
		 * 			matchWriter.println("@divHorizontal="+divHorizontal);
			matchWriter.println("@divVertical="+divVertical);
			if (getVertical()) {
				matchWriter.println("@Vproportion="+sproportion);
			} else {
				matchWriter.println("@Hproportion="+sproportion);
		 */
		if (!(""+divVertical).equals(rootParam.get("divVertical"))) {
			throw new RuntimeException("divVertical="+divVertical+" rootParam.get(\"divVertical\"))="+rootParam.get("divVertical"));
		}
		if (!(""+divHorizontal).equals(rootParam.get("divHorizontal"))) {
			throw new RuntimeException("divHorizontal="+divHorizontal+" rootParam.get(\"divHorizontal\"))="+rootParam.get("divHorizontal"));
		}
		match = new int [divHorizontal][divVertical];
		sigarray = new Signature[divVertical*divHorizontal];
		String matchfile = param.getArg('l');
		int k=0;
		try (BufferedReader br = new BufferedReader(new FileReader(matchfile))) {
			for (String line; (line = br.readLine()) != null;) {
				if (line.startsWith("@")) {
					continue;
				} else if (line.startsWith("#")) {
					continue;
				} else {
					String []p1 = line.split("=");
					String []p2 = p1[0].split(",");
					Signature sig = new Signature(p1[1]);
					match[Integer.parseInt(p2[0])][Integer.parseInt(p2[1])]=k;
					sigarray[k++]=sig;
					/*
i,j="+sigarray[match[i][j]]
					 */
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadSignatureRoot() {
		siglist = new ArrayList<Signature>();
		String rootsignaturefile = param.getArg('s');
		try (BufferedReader br = new BufferedReader(new FileReader(rootsignaturefile))) {
			for (String line; (line = br.readLine()) != null;) {
				if (line.startsWith("@")) {
					continue;
				} else if (line.startsWith("#")) {
					continue;
				} else {
					String filename = FileName.getPath(rootsignaturefile)
							+ FileName.getNameDatePrefix(rootsignaturefile) + FileName.escapeFolderName(line) + ".txt";
					if (verbose) {
						System.out.println("line:" + line);
						System.out.println("rootsignaturefile:" + rootsignaturefile);
						System.out
								.println("FileName.getPath(rootsignaturefile):" + FileName.getPath(rootsignaturefile));
						System.out.println("FileName.getNameDatePrefix(rootsignaturefile):"
								+ FileName.getNameDatePrefix(rootsignaturefile));
						System.out.println("FileName.escapeFolderName(line):" + FileName.escapeFolderName(line));
						System.out.println("reading sig file:" + filename);
					}
					loadSignature(filename);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sigarray = siglist.toArray(new Signature[1]);
	}

	private void loadSignature(String signaturefile) {
		try (BufferedReader br = new BufferedReader(new FileReader(signaturefile))) {
			for (String line; (line = br.readLine()) != null;) {
				Signature sig = new Signature(line);
				if (getVertical() == sig.getVertical())
					siglist.add(sig);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("signaturefile=" + signaturefile);
		}
	}

	RGB rgb[][] = null;

	private void loadSectorsMeanColor() {
		int divHorizontal = getNumCols();
		int divVertical = getNumLines();
		rgb = new RGB[divHorizontal][divVertical];// coluna, linha = x,y

		int w = inputImage.getWidth();
		int h = inputImage.getHeight();

		for (int j = 0; j < divVertical; ++j) {
			for (int i = 0; i < divHorizontal; ++i) {
				int color, blue, green, red, count;
				count = 0;
				blue = 0;
				green = 0;
				red = 0;
				for (int y = (h * j) / divVertical; y < (h * (j + 1)) / divVertical; ++y) {
					for (int x = (w * i) / divHorizontal; x < (w * (i + 1)) / divHorizontal; ++x) {
						color = inputImage.getRGB(x, y);
						blue += (color) & 0xFF;
						green += (color >> 8) & 0xFF;
						red += (color >> 16) & 0xFF;
						count++;
					}
				}
				blue /= count;
				green /= count;
				red /= count;
				rgb[i][j] = new RGB(red, green, blue);
			}
		}
	}

	int[][] match;

	private void matchSectorsWithSignatures() {
		int divHorizontal = getNumCols();
		int divVertical = getNumLines();
		match = new int[divHorizontal][divVertical];
		for (int i = 0; i < divHorizontal; ++i) {
			for (int j = 0; j < divHorizontal; ++j) {
				int leastSqrDistance = 256 * 256 * 3;
				for (int k = 0; k < sigarray.length; ++k) {
					int sqrDistance = rgb[i][j].sqrDistance(sigarray[k].rgb);
					if (sqrDistance < leastSqrDistance) {
						if (sigarray[k].lastx + SAME_IMG_SPACONG < i && sigarray[k].lasty + SAME_IMG_SPACONG < j) {
							match[i][j] = k;
							leastSqrDistance = sqrDistance;
						}
					}
				}
				int picked = match[i][j]; 
				sigarray[picked].lastx = i;
				sigarray[picked].lasty = j;
				sigarray[match[i][j]].updateRootFolder(param.getArg('r'));
				/* Imagens com numeração próximas podem ser muito semelhantes */
				if (picked>1) {
					sigarray[picked-1].lastx = i;
					sigarray[picked-1].lasty = j;
					sigarray[picked-2].lastx = i;
					sigarray[picked-2].lasty = j;
				} 
				if (picked<sigarray.length-2) {
					sigarray[picked+1].lastx = i;
					sigarray[picked+1].lasty = j;
					sigarray[picked+2].lastx = i;
					sigarray[picked+2].lasty = j;
				}	
			}
		}
		if (verbose) {
			for (int i = 0; i < divHorizontal; ++i) {
				for (int j = 0; j < divHorizontal; ++j) {
					System.out.println("match[" + i + "][" + j + "]=" + sigarray[match[i][j]].filename);
				}
			}
		}
	}
}