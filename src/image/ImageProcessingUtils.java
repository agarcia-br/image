package image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;

import utils.IntPair;
import utils.Signature;

public class ImageProcessingUtils {
	
	private static String tagValue(final JpegImageMetadata jpegMetadata, final TagInfo tagInfo, String def) {
	    final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
	    return null==field?def:""+field.getValueDescription();
	}
	 
    public static BufferedImage colocarDePe(BufferedImage image, int orientation) {
        int width = image.getWidth();
        int height = image.getHeight();   
        AffineTransform affineTransform = new AffineTransform();
        switch (orientation) {
        case 1:
        	return image;
            //break;
        case 2: // Flip X
            affineTransform.scale(-1.0, 1.0);
            affineTransform.translate(-width, 0);
            break;
        case 3: // PI rotation
            affineTransform.translate(width, height);
            affineTransform.rotate(Math.PI);
            break;
        case 4: // Flip Y
            affineTransform.scale(1.0, -1.0);
            affineTransform.translate(0, -height);
            break;
        case 5: // - PI/2 and Flip X
            affineTransform.rotate(-Math.PI / 2);
            affineTransform.scale(-1.0, 1.0);
            break;
        case 6: // -PI/2 and -width
            affineTransform.translate(height, 0);
            affineTransform.rotate(Math.PI / 2);
            break;
        case 7: // PI/2 and Flip
            affineTransform.scale(-1.0, 1.0);
            affineTransform.translate(-height, 0);
            affineTransform.translate(0, width);
            affineTransform.rotate(3 * Math.PI / 2);
            break;
        case 8: // PI / 2
            affineTransform.translate(0, width);
            affineTransform.rotate(3 * Math.PI / 2);
            break;
        default:
            break;
        }       
        AffineTransformOp op = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BICUBIC);
        BufferedImage destinationImage = op.createCompatibleDestImage(image, (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null );
        destinationImage = op.filter(image, destinationImage);
        return destinationImage;
    }
 	
	public static BufferedImage get(String filename) {	
		BufferedImage image=null;
		File file=null;	
		IImageMetadata m=null;
		try { 
		    file = new File(filename);
		    image = ImageIO.read(file);
		    m = Imaging.getMetadata(file);
		} catch (IOException ex) {
			System.out.println("failt to process:"+filename);
		    ex.printStackTrace();
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    int orientation = getOrientation(m);
        image = colocarDePe( image, orientation);
        return image;
	}

	public static BufferedImage scaleImage(BufferedImage image, double scale) {
		BufferedImage destinationImage = image;
		if (1.0 != scale) {
	        AffineTransform affineTransform = new AffineTransform();
	        affineTransform.setToScale(scale, scale);
	        AffineTransformOp op = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
	        destinationImage = op.createCompatibleDestImage(image, (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null );
	        destinationImage = op.filter(image, destinationImage);
		}
        return destinationImage;
	}

	public static BufferedImage scaleImage(BufferedImage image, double xscale, double yscale) {
		BufferedImage destinationImage = image;
		if (1.0 != xscale && 1.0 != yscale) {
	        AffineTransform affineTransform = new AffineTransform();
	        affineTransform.setToScale(xscale, yscale);
	        AffineTransformOp op = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
	        destinationImage = op.createCompatibleDestImage(image, (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null );
	        destinationImage = op.filter(image, destinationImage);
		}
        return destinationImage;
	}

	public static BufferedImage get(Signature sig, IntPair prop, int pixelsPerProp) {	
		BufferedImage image=get(sig.filename);
		//getSubimage(int x,int y,int w,int h)
		image = image.getSubimage(sig.winicial, sig.hinicial, sig.getWidth(), sig.getHeight());
		if (sig.getWidth()%prop.x !=0) throw new RuntimeException("sig.getWidth()%prop.x !=0");
		int mypixelsPerPropX=sig.getWidth()/prop.x;
		int mypixelsPerPropY=sig.getHeight()/prop.y;
		if (mypixelsPerPropX !=mypixelsPerPropY) throw new RuntimeException("mypixelsPerPropX="+mypixelsPerPropX+" mypixelsPerPropY="+mypixelsPerPropY);
		double scale = ((double) pixelsPerProp)/((double)mypixelsPerPropX);
		image = scaleImage(image,scale);
		//System.out.println("image.getWidth()="+image.getWidth());
		//System.out.println("image.getHeight()="+image.getHeight());
        return image;
	}

	private static int getOrientation(IImageMetadata metadata) {
		int orientation = 1;
		  if (metadata instanceof JpegImageMetadata) {
		      final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		      String sori = tagValue(jpegMetadata, TiffTagConstants.TIFF_TAG_ORIENTATION,"1");
		      orientation = Integer.parseInt(sori);
		  } else {
			  //System.err.println("**getOrientation** else metadata="+metadata);
		  }
		return orientation;
	}
}
